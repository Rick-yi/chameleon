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
package vip.xiaonuo.finance.modular.stat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 财务日汇总表
 * <p>
 * 每日 00:30 汇总前一天数据，用于报表加速与对账；维度为租户 + 日期 + 校区 + 课程。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
@TableName("FIN_DAILY_STAT")
public class FinDailyStat extends CommonEntity {

    /** 主键 */
    @Schema(description = "主键")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 统计日期 */
    @Schema(description = "统计日期")
    private Date statDate;

    /** 校区id */
    @Schema(description = "校区id")
    private String campusId;

    /** 课程id */
    @Schema(description = "课程id")
    private String courseId;

    /** 收款金额 */
    @Schema(description = "收款金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 退费金额 */
    @Schema(description = "退费金额")
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** 净营收 */
    @Schema(description = "净营收")
    private BigDecimal netAmount = BigDecimal.ZERO;

    /** 订单数 */
    @Schema(description = "订单数")
    private Long orderCount = 0L;

    /** 付费学员数 */
    @Schema(description = "付费学员数")
    private Long payingStudentCount = 0L;

    /** 点名消课课时 */
    @Schema(description = "点名消课课时")
    private BigDecimal attendLessons = BigDecimal.ZERO;

    /** 回补课时 */
    @Schema(description = "回补课时")
    private BigDecimal restoreLessons = BigDecimal.ZERO;

    /** 净课消课时 */
    @Schema(description = "净课消课时")
    private BigDecimal netLessons = BigDecimal.ZERO;

    /** 新增学员数 */
    @Schema(description = "新增学员数")
    private Long newStudentCount = 0L;
}
