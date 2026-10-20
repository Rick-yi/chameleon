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
package vip.xiaonuo.student.modular.studentorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;

/**
 * 报名订单实体
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("STUDENT_ORDER")
public class StudentOrder extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 课包id */
    @Schema(description = "课包id")
    private String coursePackageId;

    /** 课程id(快照) */
    @Schema(description = "课程id(快照)")
    private String courseId;

    /** 校区id(快照) */
    @Schema(description = "校区id(快照)")
    private String campusId;

    /** 课包名称快照 */
    @Schema(description = "课包名称快照")
    private String packageName;

    /** 课包价格快照 */
    @Schema(description = "课包价格快照")
    private BigDecimal packagePrice;

    /** 课包课时快照 */
    @Schema(description = "课包课时快照")
    private BigDecimal packageLessons;

    /** 课包赠课快照 */
    @Schema(description = "课包赠课快照")
    private BigDecimal packageGiftLessons;

    /** 课包有效期快照(天) */
    @Schema(description = "课包有效期快照(天)")
    private Integer packageValidityDays;

    /** 应收 */
    @Schema(description = "应收")
    private BigDecimal amount;

    /** 实收 */
    @Schema(description = "实收")
    private BigDecimal paidAmount;

    /** 支付渠道 */
    @Schema(description = "支付渠道(微信/支付宝/线下)")
    private String payChannel;

    /** 支付流水号 */
    @Schema(description = "支付流水号")
    private String tradeNo;

    /** 支付时间 */
    @Schema(description = "支付时间")
    private java.util.Date payTime;

    /** 已消耗课时（按订单先进先出分摊，含赠课消耗） */
    @Schema(description = "已消耗课时")
    private BigDecimal consumedLessons;

    /** 已退课时（不含赠课） */
    @Schema(description = "已退课时")
    private BigDecimal refundedLessons;

    /** 状态 */
    @Schema(description = "状态(待支付/已支付/已退费/已取消)")
    private String status;
}
