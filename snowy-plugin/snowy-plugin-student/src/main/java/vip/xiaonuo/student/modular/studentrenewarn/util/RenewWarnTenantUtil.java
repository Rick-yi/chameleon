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
package vip.xiaonuo.student.modular.studentrenewarn.util;

import vip.xiaonuo.common.context.TenantContext;

/**
 * 续费闭环租户工具
 * <p>
 * 续费配置/回访记录表现已登记到 {@code GlobalConfigure.TENANT_TABLES}，由拦截器自动拼接租户条件。
 * 本类保留是为了兼容存量调用点，实际取值统一委托给 {@link TenantContext#currentTenantId()}，
 * 避免兜底值在多处各写一份。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
public final class RenewWarnTenantUtil {

    /** 平台/未识别租户的兜底值 */
    public static final String DEFAULT_TENANT_ID = TenantContext.DEFAULT_TENANT_ID;

    private RenewWarnTenantUtil() {
    }

    /**
     * 当前租户id
     *
     * @deprecated 统一使用 {@link TenantContext#currentTenantId()}
     */
    @Deprecated
    public static String currentTenantId() {
        return TenantContext.currentTenantId();
    }
}
