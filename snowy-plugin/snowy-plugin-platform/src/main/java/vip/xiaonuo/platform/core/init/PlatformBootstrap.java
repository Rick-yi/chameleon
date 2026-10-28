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
package vip.xiaonuo.platform.core.init;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.util.CommonCryptogramUtil;
import vip.xiaonuo.dev.modular.config.service.DevConfigService;
import vip.xiaonuo.dev.modular.job.entity.DevJob;
import vip.xiaonuo.dev.modular.job.enums.DevJobCategoryEnum;
import vip.xiaonuo.dev.modular.job.enums.DevJobStatusEnum;
import vip.xiaonuo.dev.modular.dict.service.DevDictService;
import vip.xiaonuo.platform.core.service.TenantCapabilityService;
import vip.xiaonuo.dev.modular.job.service.DevJobService;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.enums.TenantStatusEnum;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
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

import java.util.Date;
import java.util.List;
import vip.xiaonuo.common.context.StartupPhase;

/**
 * 平台运营账号初始化
 * <p>
 * 启动时确保：平台运营租户（id=0）、platformAdmin 角色与账号存在；
 * 为存量租户补齐租户编码；平台菜单只授权给平台运营角色。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Component
public class PlatformBootstrap implements ApplicationListener<ApplicationStartedEvent>, Ordered {

    /** 平台运营租户id */
    public static final String PLATFORM_TENANT_ID = "0";

    /** 平台运营角色编码 */
    public static final String PLATFORM_ROLE_CODE = "platformAdmin";

    /** 平台运营账号 */
    public static final String PLATFORM_ACCOUNT = "platformAdmin";

    /** 平台菜单所属模块 */
    private static final String PLATFORM_MODULE_ID = "mod_platform_001";

    /** 演示租户id（存量数据兜底编码） */
    private static final String DEMO_TENANT_ID = "1554740179362967450";

    @Resource
    private TenantMapper tenantMapper;

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
    private DevJobService devJobService;

    @Resource
    private DevDictService devDictService;

    @Resource
    private TenantCapabilityService tenantCapabilityService;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        String originalTenantId = TenantContext.getTenantId();
        try {
            ensurePlatformTenant();
            backfillTenantCodes();
            String defaultPassword = resolveDefaultPassword();
            TenantContext.setTenantId(PLATFORM_TENANT_ID);
            SysRole platformRole = ensurePlatformRole();
            SysUser platformUser = ensurePlatformUser(defaultPassword);
            sysRelationService.remove(new LambdaQueryWrapper<SysRelation>()
                    .eq(SysRelation::getObjectId, platformUser.getId())
                    .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_USER_HAS_ROLE.getValue())
                    .eq(SysRelation::getTargetId, platformRole.getId()));
            sysRelationService.saveRelationWithAppend(platformUser.getId(), platformRole.getId(),
                    SysRelationCategoryEnum.SYS_USER_HAS_ROLE.getValue());
            grantPlatformMenus(platformRole.getId());
            removePlatformMenusFromTenants();
            ensureJobs();
            warmUpTenantDictCache();
            tenantCapabilityService.refreshAll();
            log.info(">>> 平台运营账号已就绪：租户编码 platform，账号 {}（首次登录请修改默认密码）", PLATFORM_ACCOUNT);
        } catch (Exception e) {
            log.error(">>> 平台运营账号初始化失败，请检查 chameleon_platform.sql 与 chameleon_menu.sql 是否已执行", e);
        } finally {
            if (ObjectUtil.isNotEmpty(originalTenantId)) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    public int getOrder() {
        // 早于定时任务启动，保证本次新增的任务能在同一次启动中被调度
        return StartupPhase.PLATFORM_BOOTSTRAP;
    }

    /**
     * 确保业务定时任务存在并处于运行状态
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void ensureJobs() {
        ensureJob("租户到期扫描", "vip.xiaonuo.platform.core.timer.TenantExpireCheckTimerTaskRunner", "0 0 1 * * ?");
        ensureJob("续费预警扫描", "vip.xiaonuo.student.core.timer.RenewWarnScanTimerTaskRunner", "0 0 2 * * ?");
        ensureJob("财务日汇总", "vip.xiaonuo.finance.core.timer.FinDailyStatTimerTaskRunner", "0 30 0 * * ?");
    }

    /**
     * 确保单个定时任务存在且运行
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void warmUpTenantDictCache() {
        try {
            for (Tenant tenant : tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                    .ne(Tenant::getId, PLATFORM_TENANT_ID))) {
                TenantContext.setTenantId(tenant.getId());
                devDictService.refreshTransCache();
            }
            TenantContext.setTenantId(PLATFORM_TENANT_ID);
        } catch (Exception e) {
            log.error(">>> 租户字典缓存预热失败", e);
        }
    }

    private void ensureJob(String name, String actionClass, String cronExpression) {
        DevJob job = devJobService.getOne(new LambdaQueryWrapper<DevJob>()
                .eq(DevJob::getActionClass, actionClass).last("limit 1"));
        if (ObjectUtil.isEmpty(job)) {
            DevJob devJob = new DevJob();
            devJob.setName(name);
            // 平台级任务：归属平台租户（任务内部再按租户遍历）
            devJob.setTenantId(PLATFORM_TENANT_ID);
            devJob.setCode(RandomUtil.randomString(10));
            devJob.setCategory(DevJobCategoryEnum.BIZ.getValue());
            devJob.setActionClass(actionClass);
            devJob.setCronExpression(cronExpression);
            devJob.setJobStatus(DevJobStatusEnum.RUNNING.getValue());
            devJob.setSortCode(1);
            devJobService.save(devJob);
            log.info(">>> 已创建定时任务：{}（{}）", name, cronExpression);
        } else if (!DevJobStatusEnum.RUNNING.getValue().equals(job.getJobStatus())) {
            job.setJobStatus(DevJobStatusEnum.RUNNING.getValue());
            job.setCronExpression(cronExpression);
            devJobService.updateById(job);
            log.info(">>> 已启用定时任务：{}（{}）", name, cronExpression);
        }
    }

    /**
     * 确保平台运营租户存在
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void ensurePlatformTenant() {
        Tenant tenant = tenantMapper.selectById(PLATFORM_TENANT_ID);
        if (ObjectUtil.isNotEmpty(tenant)) {
            if (!"platform".equals(tenant.getCode())) {
                tenant.setCode("platform");
                tenantMapper.updateById(tenant);
            }
            return;
        }
        Tenant platformTenant = new Tenant();
        platformTenant.setId(PLATFORM_TENANT_ID);
        platformTenant.setCode("platform");
        platformTenant.setName("平台运营");
        platformTenant.setStatus(TenantStatusEnum.NORMAL.getValue());
        platformTenant.setExpireTime(null);
        platformTenant.setRemark("SaaS 平台运营方，不对外计费");
        tenantMapper.insert(platformTenant);
    }

    /**
     * 为存量租户补齐编码
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void backfillTenantCodes() {
        List<Tenant> tenantList = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>().orderByAsc(Tenant::getCreateTime));
        for (Tenant tenant : tenantList) {
            if (StrUtil.isNotBlank(tenant.getCode())) {
                continue;
            }
            if (PLATFORM_TENANT_ID.equals(tenant.getId())) {
                tenant.setCode("platform");
            } else if (DEMO_TENANT_ID.equals(tenant.getId())) {
                tenant.setCode("demo");
            } else if (tenant.getName().contains("联调")) {
                tenant.setCode("test");
            } else {
                String suffix = StrUtil.subSufByLength(tenant.getId(), 6);
                String code = "t" + suffix;
                int index = 1;
                while (tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code)) > 0) {
                    code = "t" + suffix + index;
                    index++;
                }
                tenant.setCode(code);
            }
            tenantMapper.updateById(tenant);
        }
    }

    /**
     * 确保平台运营角色存在
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private SysRole ensurePlatformRole() {
        SysRole role = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, PLATFORM_ROLE_CODE).last("limit 1"));
        if (ObjectUtil.isNotEmpty(role)) {
            return role;
        }
        SysRole platformRole = new SysRole();
        platformRole.setName("平台运营");
        platformRole.setCode(PLATFORM_ROLE_CODE);
        platformRole.setCategory("GLOBAL");
        platformRole.setSortCode(1);
        sysRoleService.save(platformRole);
        return platformRole;
    }

    /**
     * 确保平台运营账号存在
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private SysUser ensurePlatformUser(String defaultPassword) {
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, PLATFORM_ACCOUNT).last("limit 1"));
        if (ObjectUtil.isNotEmpty(user)) {
            return user;
        }
        SysUser platformUser = new SysUser();
        platformUser.setAccount(PLATFORM_ACCOUNT);
        platformUser.setPassword(CommonCryptogramUtil.doHashValue(defaultPassword));
        platformUser.setName("平台运营");
        platformUser.setNickname("平台运营");
        platformUser.setGender("男");
        platformUser.setUserStatus(SysUserStatusEnum.ENABLE.getValue());
        platformUser.setSortCode(1);
        sysUserService.save(platformUser);
        sysUserExtService.createExtInfo(platformUser.getId(), "SYSTEM_ADD");
        return platformUser;
    }

    /**
     * 授予平台运营角色平台菜单
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void grantPlatformMenus(String roleId) {
        List<SysMenu> menuList = sysMenuService.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getCategory, SysResourceCategoryEnum.MENU.getValue())
                .eq(SysMenu::getModule, PLATFORM_MODULE_ID));
        if (ObjectUtil.isEmpty(menuList)) {
            log.warn(">>> 未找到平台运营菜单，请先执行 chameleon_menu.sql");
            return;
        }
        List<String> menuIdList = CollectionUtil.newArrayList();
        List<String> extJsonList = CollectionUtil.newArrayList();
        for (SysMenu menu : menuList) {
            menuIdList.add(menu.getId());
            extJsonList.add(JSONUtil.createObj().set("menuId", menu.getId())
                    .set("buttonInfo", JSONUtil.createArray()).toString());
        }
        sysRelationService.remove(new LambdaQueryWrapper<SysRelation>()
                .eq(SysRelation::getObjectId, roleId)
                .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue()));
        sysRelationService.saveRelationBatchWithAppend(roleId, menuIdList,
                SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue(), extJsonList);
    }

    /**
     * 移除业务租户误授权的平台菜单
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void removePlatformMenusFromTenants() {
        List<SysMenu> platformMenus = sysMenuService.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getCategory, SysResourceCategoryEnum.MENU.getValue())
                .eq(SysMenu::getModule, PLATFORM_MODULE_ID));
        if (ObjectUtil.isEmpty(platformMenus)) {
            return;
        }
        List<String> platformMenuIdList = platformMenus.stream().map(SysMenu::getId).toList();
        List<Tenant> tenantList = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, PLATFORM_TENANT_ID));
        for (Tenant tenant : tenantList) {
            TenantContext.setTenantId(tenant.getId());
            sysRelationService.remove(new LambdaQueryWrapper<SysRelation>()
                    .eq(SysRelation::getCategory, SysRelationCategoryEnum.SYS_ROLE_HAS_RESOURCE.getValue())
                    .in(SysRelation::getTargetId, platformMenuIdList));
        }
        TenantContext.setTenantId(PLATFORM_TENANT_ID);
    }

    /**
     * 读取系统默认密码（优先演示租户配置）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String resolveDefaultPassword() {
        try {
            Tenant demo = tenantMapper.selectById(DEMO_TENANT_ID);
            if (ObjectUtil.isNotEmpty(demo)) {
                TenantContext.setTenantId(demo.getId());
                String password = devConfigService.getValueByKey("SNOWY_SYS_DEFAULT_PASSWORD_FOR_B");
                if (StrUtil.isNotBlank(password)) {
                    return password;
                }
            }
        } catch (Exception e) {
            log.warn(">>> 读取默认密码配置失败，使用兜底密码", e);
        }
        return "Chameleon@2026!";
    }
}