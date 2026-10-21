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
package vip.xiaonuo.student.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 课消与收入确认DTO（按课程 + 课包）
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class FinanceConsumeDTO {

    /** 课程id */
    @Schema(description = "课程id")
    private String courseId;

    /** 课程名称 */
    @Schema(description = "课程名称")
    private String courseName;

    /** 课包id */
    @Schema(description = "课包id")
    private String coursePackageId;

    /** 课包名称 */
    @Schema(description = "课包名称")
    private String coursePackageName;

    /** 消课课时（点名扣课 - 回补） */
    @Schema(description = "消课课时")
    private BigDecimal attendLessons = BigDecimal.ZERO;

    /** 课消金额（权责口径：消课课时 × 订单购买课时单价，退费冲回为负） */
    @Schema(description = "课消金额")
    private BigDecimal recognizedAmount = BigDecimal.ZERO;

    /** 涉及订单数 */
    @Schema(description = "涉及订单数")
    private Long orderCount = 0L;

    /** 涉及学员数 */
    @Schema(description = "涉及学员数")
    private Long studentCount = 0L;
}
