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
package vip.xiaonuo.common.context;

/**
 * 租户上下文
 * <p>
 * 请求级 ThreadLocal，由 TenantContextFilter 在请求入口写入，请求结束清除。
 * TenantLineHandler 从本上下文取租户id，用于行级租户隔离。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public class TenantContext {

    /** 租户id持有器 */
    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();

    /** 请求中携带的租户编码（未登录时用于识别机构，登录后不再使用） */
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();

    /**
     * 设置租户id
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static void setTenantId(String tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取租户id
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static String getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 设置登录前使用的租户编码
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static void setTenantCode(String tenantCode) {
        TENANT_CODE_HOLDER.set(tenantCode);
    }

    /**
     * 获取登录前使用的租户编码
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    /**
     * 清除租户上下文
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public static void clear() {
        TENANT_ID_HOLDER.remove();
        TENANT_CODE_HOLDER.remove();
    }
}
