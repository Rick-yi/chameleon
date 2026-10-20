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
package vip.xiaonuo.course.modular.courseschedule.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Date;

/**
 * 调课参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class CourseScheduleChangeParam {

    /** 原排课id */
    @Schema(description = "原排课id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原排课id不能为空")
    private String scheduleId;

    /** 新上课日期 */
    @Schema(description = "新上课日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新上课日期不能为空")
    private Date scheduleDate;

    /** 新开始时间 */
    @Schema(description = "新开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新开始时间不能为空")
    private LocalTime startTime;

    /** 新结束时间 */
    @Schema(description = "新结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新结束时间不能为空")
    private LocalTime endTime;

    /** 新教室 */
    @Schema(description = "新教室")
    private String classroom;

    /** 新教师id */
    @Schema(description = "新教师id(引用SYS_USER)")
    private String teacherId;

    /** 调课原因 */
    @Schema(description = "调课原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调课原因不能为空")
    private String changeReason;
}
