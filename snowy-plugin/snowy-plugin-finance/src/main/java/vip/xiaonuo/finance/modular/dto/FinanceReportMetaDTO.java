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
package vip.xiaonuo.finance.modular.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 财务报表元信息DTO
 * <p>
 * 用于页面展示「数据更新时间」，说明实时聚合与日汇总的刷新规则。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26
 **/
@Getter
@Setter
public class FinanceReportMetaDTO {

    /** 本次生成时间（服务端时间） */
    @Schema(description = "本次生成时间")
    private String generatedAt;

    /** 数据来源：REALTIME 实时聚合 / DAILY_STAT 日汇总 */
    @Schema(description = "数据来源")
    private String dataSource = "REALTIME";

    /** 最近一天已落库的日汇总日期 */
    @Schema(description = "最近日汇总日期")
    private String dailyStatLatestDate;

    /** 最近一次日汇总更新时间 */
    @Schema(description = "最近日汇总更新时间")
    private String dailyStatUpdatedAt;

    /** 报表缓存有效期（秒） */
    @Schema(description = "报表缓存有效期（秒）")
    private Integer cacheTtlSeconds = 60;

    /** 时区 */
    @Schema(description = "时区")
    private String timezone = "Asia/Shanghai";

    /** 口径说明 */
    @Schema(description = "口径说明")
    private String note;
}
