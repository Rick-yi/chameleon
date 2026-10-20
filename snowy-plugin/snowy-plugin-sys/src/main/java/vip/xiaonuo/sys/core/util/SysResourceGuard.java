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

import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;

/**
 * 系统资源维护权限校验
 * <p>
 * SYS_RESOURCE 是全局菜单/按钮定义表，不属于任何租户。
 * 业务租户只能给自己的角色授权，不能修改全局菜单定义。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public class SysResourceGuard {

    /**
     * 校验当前请求是否来自平台运营租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static void checkPlatformMaintainable() {
        if (!"0".equals(TenantContext.getTenantId())) {
            throw new CommonException("菜单与按钮资源仅平台运营可维护");
        }
    }
}