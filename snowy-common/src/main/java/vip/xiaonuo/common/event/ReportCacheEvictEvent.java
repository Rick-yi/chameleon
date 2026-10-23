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
package vip.xiaonuo.common.event;

import lombok.Getter;

/**
 * 报表数据变更事件：收款、退费、消课等写入后发出，报表侧据此清理缓存。
 * <p>
 * 报表缓存按「租户 + 查询参数」存储，业务数据一变就必须失效，否则前台刚收的款
 * 在报表里要等缓存过期才看得到。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/23 12:00
 **/
@Getter
public class ReportCacheEvictEvent {

    /** 数据所属租户 */
    private final String tenantId;

    /** 变更来源（PAYMENT/REFUND/ATTEND 等，便于排查） */
    private final String source;

    public ReportCacheEvictEvent(String tenantId, String source) {
        this.tenantId = tenantId;
        this.source = source;
    }
}
