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
package vip.xiaonuo.platform.modular.billing.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 账单查询参数
 *
 * @author 会飞的猪
 * @date 2026/10/26 12:00
 **/
@Getter
@Setter
public class PlatformBillPageParam {

    /** 当前页 */
    @Schema(description = "当前页码")
    private Integer current;

    /** 每页条数 */
    @Schema(description = "每页条数")
    private Integer size;

    /** 关键词：账单号/租户名称 */
    @Schema(description = "关键词")
    private String searchKey;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 套餐id */
    @Schema(description = "套餐id")
    private String packageId;

    /** 状态 */
    @Schema(description = "状态")
    private String status;

    /** 发票状态 */
    @Schema(description = "发票状态")
    private String invoiceStatus;

    /** 开始月份 yyyy-MM */
    @Schema(description = "开始月份")
    private String startMonth;

    /** 结束月份 yyyy-MM */
    @Schema(description = "结束月份")
    private String endMonth;
}
