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
package vip.xiaonuo.student.modular.studentlessonaccount.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 人工调整课时参数
 * <p>
 * 正数 = 补录/赠送课时，负数 = 收回课时；调整原因必填，余额不足时拒绝。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class StudentLessonAdjustParam {

    /** 学员id */
    @Schema(description = "学员id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "学员id不能为空")
    private String studentId;

    /** 课程id */
    @Schema(description = "课程id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "课程id不能为空")
    private String courseId;

    /** 调整课时数（正数增加，负数扣减） */
    @Schema(description = "调整课时数（正数增加，负数扣减）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调整课时数不能为空")
    private BigDecimal lessons;

    /** 调整原因 */
    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}
