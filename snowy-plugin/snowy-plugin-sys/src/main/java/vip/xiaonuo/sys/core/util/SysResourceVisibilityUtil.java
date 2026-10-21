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
package vip.xiaonuo.sys.core.util;

import cn.hutool.core.util.StrUtil;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.tenant.TenantCapability;
import vip.xiaonuo.sys.modular.resource.entity.SysMenu;

import java.util.List;
import java.util.Map;

/**
 * 平台专用资源可见性
 * <p>
 * 全局运维、代码生成、移动端菜单、平台运营资源只属于平台运营租户（id=0）。
 * 业务租户即使历史上被授予过这些菜单，也在登录菜单和角色授权树中隐藏。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public class SysResourceVisibilityUtil {

    /** 平台专用模块id */
    private static final String PLATFORM_MODULE_ID = "mod_platform_001";

    /** 平台专用路径前缀（同时覆盖前端菜单路径与后端接口路径） */
    private static final List<String> PLATFORM_ONLY_PREFIXES = List.of(
            "/platform",
            "/auth/session",
            "/auth/third",
            "/auth/monitor",
            "/dev/file",
            "/dev/email",
            "/dev/sms",
            "/dev/push",
            "/dev/message",
            "/dev/job",
            "/dev/log",
            "/dev/monitor",
            "/dev/vislog",
            "/dev/oplog",
            "/dev/slideshow",
            "/gen",
            "/mobile/menu",
            "/mobile/module"
    );

    /**
     * 当前是否为平台运营租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static boolean isPlatformTenant() {
        return "0".equals(TenantContext.getTenantId());
    }

    /**
     * 是否为平台专用菜单/资源
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static boolean isPlatformOnlyMenu(SysMenu menu) {
        if (menu == null) {
            return false;
        }
        if (PLATFORM_MODULE_ID.equals(menu.getId()) || PLATFORM_MODULE_ID.equals(menu.getModule())) {
            return true;
        }
        return isPlatformOnlyPath(menu.getPath());
    }

    /**
     * 是否为平台专用路径
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static boolean isPlatformOnlyPath(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        return PLATFORM_ONLY_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 菜单编码 → 套餐功能键
     * <p>
     * 套餐 `funcConfig` 中配置的功能键关闭后，对应菜单在下发时隐藏（机构侧看不到入口）。
     * </p>
     */
    private static final Map<String, String> MENU_FUNC_KEY_MAP = Map.ofEntries(
            Map.entry("crm", "crm"),
            Map.entry("student", "student"),
            Map.entry("renewWarn", "student"),
            Map.entry("course", "course"),
            Map.entry("coursePkg", "course"),
            Map.entry("courseClass", "course"),
            Map.entry("courseSchedule", "course"),
            Map.entry("courseAttend", "course"),
            Map.entry("finance", "finance"),
            Map.entry("report", "report"),
            Map.entry("reportDashboard", "report"),
            Map.entry("reportFinance", "report"),
            Map.entry("reportAcademic", "report"),
            Map.entry("reportCrm", "report")
    );

    /**
     * 菜单对应的功能是否开启
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public static boolean isFuncEnabled(SysMenu menu) {
        if (menu == null) {
            return true;
        }
        String menuCode = menu.getCode();
        if (StrUtil.isBlank(menuCode)) {
            return true;
        }
        String funcKey = MENU_FUNC_KEY_MAP.get(menuCode);
        if (funcKey == null) {
            return true;
        }
        TenantCapability capability = TenantCapability.getInstance();
        return capability == null || capability.enabled(funcKey);
    }

    /**
     * 按当前租户过滤菜单资源（平台专用资源 + 套餐功能开关）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static List<SysMenu> filterForCurrentTenant(List<SysMenu> menuList) {
        if (isPlatformTenant() || menuList == null) {
            return menuList;
        }
        return menuList.stream().filter(menu -> !isPlatformOnlyMenu(menu))
                .filter(SysResourceVisibilityUtil::isFuncEnabled).toList();
    }

    /**
     * 按当前租户过滤接口权限路径
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static List<String> filterApiForCurrentTenant(List<String> apiList) {
        if (isPlatformTenant() || apiList == null) {
            return apiList;
        }
        return apiList.stream().filter(api -> !isPlatformOnlyPath(api)).toList();
    }
}