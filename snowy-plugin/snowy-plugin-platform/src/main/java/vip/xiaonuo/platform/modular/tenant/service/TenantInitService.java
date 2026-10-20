/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.platform.modular.tenant.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonCryptogramUtil;
import vip.xiaonuo.dev.modular.config.entity.DevConfig;
import vip.xiaonuo.dev.modular.config.param.DevConfigListParam;
import vip.xiaonuo.dev.modular.config.service.DevConfigService;
import vip.xiaonuo.dev.modular.dict.entity.DevDict;
import vip.xiaonuo.dev.modular.dict.service.DevDictService;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.sys.core.util.SysResourceVisibilityUtil;
import vip.xiaonuo.sys.modular.org.entity.SysOrg;
import vip.xiaonuo.sys.modular.org.service.SysOrgExtService;
import vip.xiaonuo.sys.modular.org.service.SysOrgService;
import vip.xiaonuo.sys.modular.relation.entity.SysRelation;
import vip.xiaonuo.sys.modular.relation.enums.SysRelationCategoryEnum;
import vip.xiaonuo.sys.modular.relation.service.SysRelationService;
import vip.xiaonuo.sys.modular.resource.entity.SysMenu;
import vip.xiaonuo.sys.modular.resource.enums.SysResourceCategoryEnum;
import vip.xiaonuo.sys.modular.resource.service.SysMenuService;
import vip.xiaonuo.sys.modular.role.entity.SysRole;
import vip.xiaonuo.sys.modular.role.service.SysRoleService;
import vip.xiaonuo.sys.modular.user.entity.SysUser;
import vip.xiaonuo.sys.modular.user.enums.SysUserStatusEnum;
import vip.xiaonuo.sys.modular.user.service.SysUserExtService;
import vip.xiaonuo.sys.modular.user.service.SysUserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户开通初始化服务
 * <p>
 * 新租户开通时创建：总部与默认校区、机构管理员、机构管理员角色、业务菜单授权，
 * 并从演示租户复制系统配置与字典。所有数据均在目标租户上下文中落库。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class TenantInitService {

    /** 模板租户编码 */
    private static final String TEMPLATE_TENANT_CODE = "demo";

    /** 系统默认密码配置键 */
    private static final String DEFAULT_PASSWORD_KEY = "SNOWY_SYS_DEFAULT_PASSWORD_FOR_B";

    /** 兜底默认密码（与初始化数据保持一致） */
    private static final String FALLBACK_PASSWORD = "Chameleon@2026!";

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private SysOrgService sysOrgService;

    @Resource
    private SysOrgExtService sysOrgExtService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysUserExtService sysUserExtService;

    @Resource
    private SysRelationService sysRelationService;

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private DevConfigService devConfigService;

    @Resource
    private DevDictService devDictService;

    /**
     * 初始化新租户数据
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    @Transactional(rollbackFor = Exception.class)
    public void initTenant(Tenant tenant, TenantAddParam param) {
        String originalTenantId = TenantContext.getTenantId();
        String newTenantId = tenant.getId();
        Tenant templateTenant = queryTemplateTenant();
        try {
            // 1. 读取模板租户的配置与字典
            List<DevConfig> templateConfigList = new ArrayList<>();
            List<DevDict> templateDictList = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(templateTenant)) {
                TenantContext.setTenantId(templateTenant.getId());
                templateConfigList = devConfigService.list(new DevConfigListParam());
                templateDictList = devDictService.list();
            }

            // 2. 切换到新租户上下文
            TenantContext.setTenantId(newTenantId);

            // 3. 复制系统配置
            copyConfig(templateConfigList, tenant);

            // 4. 复制字典（保留父子层级）
            copyDict(templateDictList);

            // 5. 创建总部与默认校区
            SysOrg headOffice = createOrg("0", tenant.getName(), tenant.getCode() + "_HQ",
                    "COMPANY", 1);
            SysOrg campus = createOrg(headOffice.getId(),
                    StrUtil.blankToDefault(param.getCampusName(), "默认校区"),
                    tenant.getCode() + "_CAMPUS", "DEPT", 1);

            // 6. 创建机构管理员角色
            SysRole adminRole = new SysRole();
            adminRole.setName("机构管理员");
            adminRole.setCode("superAdmin");
            adminRole.setCategory("GLOBAL");
            adminRole.setSortCode(1);
            sysRoleService.save(adminRole);

            // 7. 创建管理员账号
            String adminAccount = StrUtil.blankToDefault(param.getAdminAccount(), "admin");
            String adminName = StrUtil.blankToDefault(param.getAdminName(), "机构管理员");
            String rawPassword = StrUtil.isNotBlank(param.getAdminPassword())
                    ? param.getAdminPassword() : resolveDefaultPassword();
            SysUser adminUser = new SysUser();
            adminUser.setAccount(adminAccount);
            adminUser.setPassword(CommonCryptogramUtil.doHashValue(rawPassword));
            adminUser.setName(adminName);
            adminUser.setNickname(adminName);
            adminUser.setGender("男");
            adminUser.setUserStatus(SysUserStatusEnum.ENABLE.getValue());
            adminUser.setSortCode(1);
            adminUser.setOrgId(headOffice.getId());
            sysUserService.save(adminUser);
            sysUserExtService.createExtInfo(adminUser.getId(), "SYSTEM_ADD");

            // 8. 授权：用户 -> 角色，角色 -> 系统+业务资源
            sysRelationService.saveRelationWithAppend(adminUser.getId(), adminRole.getId(),
                    SysRelationCategoryEnum.SYS_USER_HAS_ROLE.getValue());
            grantAdminResources(templateTenant, adminRole.getId());

            // 9. 总部主管设为管理员
            headOffice.setDirectorId(adminUser.getId());
            sysOrgService.updateById(headOffice);

            log.info(">>> 租户[{}]初始化完成：管理员={}，总部={}，校区={}",
                    newTenantId, adminAccount, headOffice.getId(), campus.getId());
        } finally {
            if (ObjectUtil.isNotEmpty(originalTenantId)) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * 查询模板租户（优先 demo，找不到则取任一非平台租户）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Tenant queryTemplateTenant() {
        Tenant template = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, TEMPLATE_TENANT_CODE).last("limit 1"));
        if (ObjectUtil.isNotEmpty(template)) {
            return template;
        }
        return tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, "0").last("limit 1"));
    }

    /**
     * 复制系统配置到新租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void copyConfig(List<DevConfig> templateConfigList, Tenant tenant) {
        if (ObjectUtil.isEmpty(templateConfigList)) {
            return;
        }
        List<DevConfig> toSaveList = new ArrayList<>();
        for (DevConfig template : templateConfigList) {
            DevConfig config = new DevConfig();
            config.setConfigKey(template.getConfigKey());
            config.setConfigValue(template.getConfigValue());
            config.setCategory(template.getCategory());
            config.setRemark(template.getRemark());
            config.setSortCode(template.getSortCode());
            config.setExtJson(template.getExtJson());
            toSaveList.add(config);
        }
        devConfigService.saveBatch(toSaveList);
        // 机构名称写入本租户系统配置
        DevConfig sysNameConfig = devConfigService.getOne(new LambdaQueryWrapper<DevConfig>()
                .eq(DevConfig::getConfigKey, "SNOWY_SYS_NAME").last("limit 1"));
        if (ObjectUtil.isNotEmpty(sysNameConfig)) {
            sysNameConfig.setConfigValue(tenant.getName());
            devConfigService.updateById(sysNameConfig);
        }
    }

    /**
     * 复制字典到新租户，保留父子层级
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void copyDict(List<DevDict> templateDictList) {
        if (ObjectUtil.isEmpty(templateDictList)) {
            return;
        }
        Map<String, String> idMapping = new HashMap<>();
        List<DevDict> toSaveList = new ArrayList<>();
        for (DevDict template : templateDictList) {
            DevDict dict = new DevDict();
            dict.setParentId(template.getParentId());
            dict.setDictLabel(template.getDictLabel());
            dict.setDictValue(template.getDictValue());
            dict.setDictColor(template.getDictColor());
            dict.setCode(template.getCode());
            dict.setCategory(template.getCategory());
            dict.setSortCode(template.getSortCode());
            dict.setExtJson(template.getExtJson());
            toSaveList.add(dict);
            idMapping.put(template.getId(), null);
        }
        devDictService.saveBatch(toSaveList);
        for (int i = 0; i < toSaveList.size(); i++) {
            idMapping.put(templateDictList.get(i).getId(), toSaveList.get(i).getId());
        }
        List<DevDict> toUpdateList = new ArrayList<>();
        for (DevDict dict : toSaveList) {
            String mappedParentId = idMapping.get(dict.getParentId());
            if (ObjectUtil.isNotEmpty(mappedParentId) && !mappedParentId.equals(dict.getParentId())) {
                dict.setParentId(mappedParentId);
                toUpdateList.add(dict);
            }
        }
        if (ObjectUtil.isNotEmpty(toUpdateList)) {
            devDictService.updateBatchById(toUpdateList);
        }
    }

    /**
     * 创建组织
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private SysOrg createOrg(String parentId, String name, String code, String category, int sortCode) {
        SysOrg org = new SysOrg();
        org.setParentId(parentId);
        org.setName(name);
        org.setCode(code);
        org.setCategory(category);
        org.setSortCode(sortCode);
        sysOrgService.save(org);
        sysOrgExtService.createExtInfo(org.getId(), "SYSTEM_ADD");
        return org;
    }

    /**
     * 授予机构管理员系统 + 业务资源
     * <p>
     * 优先复制模板租户超管角色的资源授权（排除平台运营菜单）；
     * 模板缺失时退化为授予全部非平台菜单。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void grantAdminResources(Tenant templateTenant, String roleId) {
        String newTenantId = TenantContext.getTenantId();
        if (ObjectUtil.isNotEmpty(templateTenant)) {
            try {
                TenantContext.setTenantId(templateTenant.getId());
                SysRole templateRole = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getCode, "superAdmin").last("limit 1"));
                if (ObjectUtil.isNotEmpty(templateRole)) {
                    List<SysRelation> relationList = sysRelationService.list(new LambdaQueryWrapper<SysRelation>()
                            .eq(SysRelation::getObjectId, templateRole.getId())
                            .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue()));
                    if (ObjectUtil.isNotEmpty(relationList)) {
                        Set<String> platformResourceIds = platformResourceIds();
                        List<String> targetIdList = new ArrayList<>();
                        List<String> extJsonList = new ArrayList<>();
                        for (SysRelation relation : relationList) {
                            if (platformResourceIds.contains(relation.getTargetId())) {
                                continue;
                            }
                            targetIdList.add(relation.getTargetId());
                            extJsonList.add(relation.getExtJson());
                        }
                        TenantContext.setTenantId(newTenantId);
                        if (ObjectUtil.isNotEmpty(targetIdList)) {
                            sysRelationService.saveRelationBatchWithAppend(roleId, targetIdList,
                                    SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue(), extJsonList);
                        }
                        return;
                    }
                }
            } finally {
                TenantContext.setTenantId(newTenantId);
            }
        }
        grantFallbackMenus(roleId);
    }

    /**
     * 模板缺失时的兜底：授予全部非平台菜单
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void grantFallbackMenus(String roleId) {
        Set<String> platformResourceIds = platformResourceIds();
        List<SysMenu> menuList = sysMenuService.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getCategory, SysResourceCategoryEnum.MENU.getValue()));
        List<String> menuIdList = new ArrayList<>();
        List<String> extJsonList = new ArrayList<>();
        for (SysMenu menu : menuList) {
            if (platformResourceIds.contains(menu.getId())) {
                continue;
            }
            menuIdList.add(menu.getId());
            extJsonList.add(JSONUtil.createObj().set("menuId", menu.getId())
                    .set("buttonInfo", JSONUtil.createArray()).toString());
        }
        if (ObjectUtil.isEmpty(menuIdList)) {
            throw new CommonException("菜单资源未初始化，请先执行 chameleon_menu.sql");
        }
        sysRelationService.saveRelationBatchWithAppend(roleId, menuIdList,
                SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue(), extJsonList);
    }

    /**
     * 平台运营资源id集合
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Set<String> platformResourceIds() {
        Set<String> ids = new HashSet<>();
        sysMenuService.list().forEach(menu -> {
            if (SysResourceVisibilityUtil.isPlatformOnlyMenu(menu)) {
                ids.add(menu.getId());
            }
        });
        ids.add("mod_platform_001");
        return ids;
    }

    /**
     * 获取系统默认密码
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String resolveDefaultPassword() {
        try {
            String password = devConfigService.getValueByKey(DEFAULT_PASSWORD_KEY);
            if (StrUtil.isNotBlank(password)) {
                return password;
            }
        } catch (Exception e) {
            log.warn(">>> 读取系统默认密码失败，使用兜底密码", e);
        }
        return FALLBACK_PASSWORD;
    }
}