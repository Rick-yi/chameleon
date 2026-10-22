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
package vip.xiaonuo.platform.modular.roletemplate.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.platform.modular.roletemplate.PlatformRoleTemplate;
import vip.xiaonuo.platform.modular.roletemplate.PlatformRoleTemplates;
import vip.xiaonuo.platform.modular.roletemplate.service.PlatformRoleTemplateService;
import vip.xiaonuo.sys.modular.relation.enums.SysRelationCategoryEnum;
import vip.xiaonuo.sys.modular.relation.entity.SysRelation;
import vip.xiaonuo.sys.modular.relation.service.SysRelationService;
import vip.xiaonuo.sys.modular.resource.entity.SysButton;
import vip.xiaonuo.sys.modular.resource.entity.SysMenu;
import vip.xiaonuo.sys.modular.resource.enums.SysResourceCategoryEnum;
import vip.xiaonuo.sys.modular.resource.service.SysButtonService;
import vip.xiaonuo.sys.modular.resource.service.SysMenuService;
import vip.xiaonuo.sys.modular.role.entity.SysRole;
import vip.xiaonuo.sys.modular.role.enums.SysRoleCategoryEnum;
import vip.xiaonuo.sys.modular.role.service.SysRoleService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色模板Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Service
public class PlatformRoleTemplateServiceImpl implements PlatformRoleTemplateService {

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private SysButtonService sysButtonService;

    @Resource
    private SysRelationService sysRelationService;

    @Override
    public List<PlatformRoleTemplate> listTemplates() {
        return PlatformRoleTemplates.all();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int ensureTenantRoles(String tenantId) {
        return apply(tenantId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int applyTemplates(String tenantId) {
        return apply(tenantId, true);
    }

    /**
     * 落地角色模板
     *
     * @param tenantId 租户id
     * @param rebuild   是否覆盖式重建（false = 只补齐缺失的授权）
     */
    private int apply(String tenantId, boolean rebuild) {
        String originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            int count = 0;
            for (PlatformRoleTemplate template : PlatformRoleTemplates.all()) {
                SysRole role = findOrCreateRole(tenantId, template);
                if (rebuild) {
                    clearTemplateGrant(role.getId(), template);
                }
                grantMenus(role.getId(), template);
                grantApis(role.getId(), template);
                count++;
            }
            // 机构管理员：补齐全部业务菜单与业务接口，保证管理员不被兜底校验拦住
            if (rebuild) {
                clearAdminBusinessGrant(tenantId);
            }
            topUpAdminGrant(tenantId);
            log.info(">>> 租户[{}]业务角色模板落地完成，共 {} 个角色", tenantId, count);
            return count;
        } finally {
            TenantContext.setTenantId(originalTenantId);
        }
    }

    /**
     * 清掉机构管理员的业务菜单授权（覆盖式重建时用，让 buttonInfo 重新写入）
     */
    private void clearAdminBusinessGrant(String tenantId) {
        SysRole adminRole = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, PlatformRoleTemplates.ROLE_ADMIN).last("limit 1"), false);
        if (adminRole == null) {
            return;
        }
        List<String> businessMenuIdList = sysMenuService.list(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getCategory, SysResourceCategoryEnum.MENU.getValue()))
                .stream()
                .filter(menu -> StrUtil.isNotBlank(menu.getPath())
                        && PlatformRoleTemplates.BUSINESS_MENU_PATH_PREFIXES.stream().anyMatch(menu.getPath()::startsWith))
                .map(SysMenu::getId).toList();
        List<SysRelation> relationList = sysRelationService.list(new LambdaQueryWrapper<SysRelation>()
                .eq(SysRelation::getObjectId, adminRole.getId())
                .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue())
                .in(SysRelation::getTargetId, businessMenuIdList));
        for (SysRelation relation : relationList) {
            sysRelationService.removeById(relation.getId());
        }
    }

    /**
     * 机构管理员补齐全量业务权限
     * <p>
     * 业务域接口有「必须在权限清单内」的兜底校验，机构管理员必须拥有全部业务接口，
     * 否则会误伤自己（例如新增的业务接口还没加进角色）。
     * </p>
     */
    private void topUpAdminGrant(String tenantId) {
        SysRole adminRole = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, PlatformRoleTemplates.ROLE_ADMIN).last("limit 1"), false);
        if (adminRole == null) {
            return;
        }
        // 业务接口：全部按钮资源
        List<String> apiUrlList = matchButtonCodes(List.of("/student", "/course", "/crm", "/finance", "/biz"));
        List<String> allBusinessApiList = apiUrlList;
        // 业务菜单
        List<String> menuIdList = new ArrayList<>();
        List<String> menuExtJsonList = new ArrayList<>();
        for (SysMenu menu : sysMenuService.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getCategory, SysResourceCategoryEnum.MENU.getValue()))) {
            String path = menu.getPath();
            if (StrUtil.isBlank(path) || !PlatformRoleTemplates.BUSINESS_MENU_PATH_PREFIXES.stream()
                    .anyMatch(path::startsWith)) {
                continue;
            }
            if (sysRelationService.count(new LambdaQueryWrapper<SysRelation>()
                    .eq(SysRelation::getObjectId, adminRole.getId())
                    .eq(SysRelation::getTargetId, menu.getId())
                    .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue())) > 0) {
                continue;
            }
            menuIdList.add(menu.getId());
            menuExtJsonList.add(JSONUtil.toJsonStr(Map.of(
                    "menuId", menu.getId(),
                    "buttonInfo", actionButtonIdList(menu.getId(), allBusinessApiList))));
        }
        if (!menuIdList.isEmpty()) {
            sysRelationService.saveRelationBatchWithAppend(adminRole.getId(), menuIdList,
                    SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue(), menuExtJsonList);
        }
        List<String> targetIdList = new ArrayList<>();
        List<String> extJsonList = new ArrayList<>();
        for (String apiUrl : apiUrlList) {
            if (sysRelationService.count(new LambdaQueryWrapper<SysRelation>()
                    .eq(SysRelation::getObjectId, adminRole.getId())
                    .eq(SysRelation::getTargetId, apiUrl)
                    .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_PERMISSION.getValue())) > 0) {
                continue;
            }
            targetIdList.add(apiUrl);
            extJsonList.add(JSONUtil.toJsonStr(Map.of(
                    "apiUrl", apiUrl,
                    "scopeCategory", PlatformRoleTemplates.SCOPE_ALL,
                    "scopeDefineOrgIdList", List.of())));
        }
        if (!targetIdList.isEmpty()) {
            sysRelationService.saveRelationBatchWithAppend(adminRole.getId(), targetIdList,
                    SysRelationCategoryEnum.SYS_ROLE_HAS_PERMISSION.getValue(), extJsonList);
        }
    }

    /** 查找或创建角色 */
    private SysRole findOrCreateRole(String tenantId, PlatformRoleTemplate template) {
        SysRole role = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, template.getCode()).last("limit 1"), false);
        if (role == null) {
            role = new SysRole();
            role.setId(IdUtil.getSnowflakeNextIdStr());
            role.setCode(template.getCode());
            role.setName(template.getName());
            role.setCategory(SysRoleCategoryEnum.GLOBAL.getValue());
            role.setSortCode(template.getSortCode());
            role.setExtJson(JSONUtil.toJsonStr(Map.of("remark", template.getRemark(), "selfScope", template.getSelfScope())));
            sysRoleService.save(role);
        } else if (!template.getName().equals(role.getName())) {
            role.setName(template.getName());
            sysRoleService.updateById(role);
        }
        return role;
    }

    /** 清掉模板角色旧的授权（覆盖式重建时用） */
    private void clearTemplateGrant(String roleId, PlatformRoleTemplate template) {
        List<String> menuIdList = template.getMenuPaths().stream()
                .map(this::findMenuByPath).filter(ObjectUtil::isNotNull).map(SysMenu::getId).toList();
        List<SysRelation> relationList = sysRelationService.list(new LambdaQueryWrapper<SysRelation>()
                .eq(SysRelation::getObjectId, roleId)
                .in(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue(),
                        SysRelationCategoryEnum.SYS_ROLE_HAS_PERMISSION.getValue()));
        for (SysRelation relation : relationList) {
            boolean isTemplateMenu = SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue().equals(relation.getCategory())
                    && menuIdList.contains(relation.getTargetId());
            boolean isTemplateApi = SysRelationCategoryEnum.SYS_ROLE_HAS_PERMISSION.getValue().equals(relation.getCategory())
                    && matchApiPrefix(relation.getTargetId(), template.getApiPrefixes());
            if (isTemplateMenu || isTemplateApi) {
                sysRelationService.removeById(relation.getId());
            }
        }
    }

    /** 授予菜单（同时写入按钮级权限：buttonInfo = 该角色可用的动作按钮） */
    private void grantMenus(String roleId, PlatformRoleTemplate template) {
        List<String> apiUrlList = matchButtonCodes(template.getApiPrefixes());
        List<String> menuIdList = new ArrayList<>();
        List<String> extJsonList = new ArrayList<>();
        for (String path : template.getMenuPaths()) {
            SysMenu menu = findMenuByPath(path);
            if (menu == null) {
                continue;
            }
            if (sysRelationService.count(new LambdaQueryWrapper<SysRelation>()
                    .eq(SysRelation::getObjectId, roleId)
                    .eq(SysRelation::getTargetId, menu.getId())
                    .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue())) > 0) {
                continue;
            }
            menuIdList.add(menu.getId());
            extJsonList.add(JSONUtil.toJsonStr(Map.of(
                    "menuId", menu.getId(),
                    "buttonInfo", actionButtonIdList(menu.getId(), apiUrlList))));
        }
        if (!menuIdList.isEmpty()) {
            sysRelationService.saveRelationBatchWithAppend(roleId, menuIdList,
                    SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue(), extJsonList);
        }
    }

    /**
     * 菜单下该角色可用的动作按钮id
     * <p>
     * 写操作接口（新增/编辑/删除/审批…）登记为动作按钮，写入角色-菜单关系的 buttonInfo，
     * 前端 hasPerm(接口地址) 即可判断按钮是否显示。
     * </p>
     */
    private List<String> actionButtonIdList(String menuId, List<String> apiUrlList) {
        List<SysButton> buttonList = sysButtonService.list(new LambdaQueryWrapper<SysButton>()
                .eq(SysButton::getParentId, menuId)
                .eq(SysButton::getCategory, SysResourceCategoryEnum.BUTTON.getValue()));
        List<String> resultList = new ArrayList<>();
        for (SysButton button : buttonList) {
            String code = button.getCode();
            if (StrUtil.isBlank(code) || !apiUrlList.contains(code)) {
                continue;
            }
            // EXT_JSON 里带 action 标记的才是动作按钮（读接口不算）
            if (StrUtil.isNotBlank(button.getExtJson()) && button.getExtJson().contains("action")) {
                resultList.add(button.getId());
            }
        }
        return resultList;
    }

    /** 授予接口权限（带数据范围） */
    private void grantApis(String roleId, PlatformRoleTemplate template) {
        List<String> apiUrlList = matchButtonCodes(template.getApiPrefixes());
        List<String> targetIdList = new ArrayList<>();
        List<String> extJsonList = new ArrayList<>();
        for (String apiUrl : apiUrlList) {
            if (sysRelationService.count(new LambdaQueryWrapper<SysRelation>()
                    .eq(SysRelation::getObjectId, roleId)
                    .eq(SysRelation::getTargetId, apiUrl)
                    .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_PERMISSION.getValue())) > 0) {
                continue;
            }
            targetIdList.add(apiUrl);
            extJsonList.add(JSONUtil.toJsonStr(Map.of(
                    "apiUrl", apiUrl,
                    "scopeCategory", template.getScopeCategory(),
                    "scopeDefineOrgIdList", List.of())));
        }
        if (!targetIdList.isEmpty()) {
            sysRelationService.saveRelationBatchWithAppend(roleId, targetIdList,
                    SysRelationCategoryEnum.SYS_ROLE_HAS_PERMISSION.getValue(), extJsonList);
        }
    }

    /** 按路径找菜单 */
    private SysMenu findMenuByPath(String path) {
        return sysMenuService.getOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, path)
                .eq(SysMenu::getCategory, SysResourceCategoryEnum.MENU.getValue())
                .last("limit 1"), false);
    }

    /** 找出按钮资源中命中的接口地址 */
    private List<String> matchButtonCodes(List<String> apiPrefixes) {
        Set<String> result = new LinkedHashSet<>();
        List<SysButton> buttonList = sysButtonService.list(new LambdaQueryWrapper<SysButton>()
                .eq(SysButton::getCategory, SysResourceCategoryEnum.BUTTON.getValue()));
        for (SysButton button : buttonList) {
            String code = button.getCode();
            if (StrUtil.isBlank(code) || !code.startsWith("/")) {
                continue;
            }
            if (matchApiPrefix(code, apiPrefixes)) {
                result.add(code);
            }
        }
        return new ArrayList<>(result);
    }

    /** 接口地址是否命中前缀 */
    private boolean matchApiPrefix(String apiUrl, List<String> apiPrefixes) {
        if (apiPrefixes == null || apiPrefixes.isEmpty()) {
            return false;
        }
        for (String prefix : apiPrefixes) {
            if (apiUrl.equals(prefix) || apiUrl.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")
                    || apiUrl.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
