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

import org.springframework.core.Ordered;

/**
 * 启动期迁移阶段
 * <p>
 * 各迁移监听器原先用 {@code LOWEST_PRECEDENCE - 10 / -13 / -14 / -15 / -20} 这样的数字间距排队，
 * 存在两个问题：一是同值项（-14、-15 各挤了多个）之间顺序未定义；
 * 二是新增迁移只能靠「挑一个没被占的数字」插入，没有任何约束，改错也不会报错。
 * </p>
 * <p>
 * 这里把顺序收敛成一条显式阶段链，每级由上一级推导，因此不可能出现取值冲突：
 * 结构调整必须在权限登记之前（否则权限指向的表还不存在），
 * 自检必须在全部结构与权限就绪之后，定时任务放在最后避免结构未就绪时触发业务。
 * </p>
 * <p>
 * 同一阶段内的多个迁移视为相互独立、顺序无关；若确实存在依赖，应放入不同阶段。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class StartupPhase {

    /** 阶段间的固定跨度，留出余量便于将来在同一层级内再细分 */
    private static final int STEP = 1000;

    /**
     * 链路起点相对 LOWEST_PRECEDENCE 的偏移。
     * <p>
     * 链上一共 6 次递增，必须预留 7 个跨度的余量，
     * 否则最后一级会算到 LOWEST_PRECEDENCE 本身，与「严格早于」的前提冲突。
     * </p>
     */
    private static final int BASE_OFFSET = 7 * STEP;

    /** 底座结构：P0 补表补列、租户编码回填 */
    public static final int BASE_SCHEMA = Ordered.LOWEST_PRECEDENCE - BASE_OFFSET;

    /** 平台侧结构：平台运营相关表 */
    public static final int PLATFORM_SCHEMA = BASE_SCHEMA + STEP;

    /** 业务域结构：各插件自建的表与列 */
    public static final int DOMAIN_SCHEMA = PLATFORM_SCHEMA + STEP;

    /** 业务域权限：依赖上面已建好的表，登记接口权限与菜单按钮 */
    public static final int DOMAIN_PERMISSION = DOMAIN_SCHEMA + STEP;

    /** 平台账号初始化 */
    public static final int PLATFORM_BOOTSTRAP = DOMAIN_PERMISSION + STEP;

    /** 启动自检：需要全部结构与权限就绪 */
    public static final int AUDIT = PLATFORM_BOOTSTRAP + STEP;

    /** 定时任务启动：放最后，避免结构未就绪就触行业务逻辑 */
    public static final int TIMER_START = AUDIT + STEP;

    private StartupPhase() {
    }
}
