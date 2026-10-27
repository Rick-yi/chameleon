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
import lombok.Getter;
import lombok.Setter;

/**
 * 排课冲突预检参数
 * <p>
 * 日期与时间使用字符串，避免 GET 查询参数直接绑定 java.util.Date / LocalTime 失败；
 * 班级可为空（直接排课场景只校验教师与教室）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class CourseScheduleConflictCheckParam {

    /** 班级id（直接排课可为空） */
    @Schema(description = "班级id")
    private String classId;

    /** 上课日期，格式 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    @Schema(description = "上课日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "上课日期不能为空")
    private String scheduleDate;

    /** 开始时间，格式 HH:mm 或 HH:mm:ss */
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间，格式 HH:mm 或 HH:mm:ss */
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 教室 */
    @Schema(description = "教室")
    private String classroom;

    /** 教师id */
    @Schema(description = "教师id")
    private String teacherId;

    /** 需要排除的排课id（调课预检时排除自身） */
    @Schema(description = "排除的排课id")
    private String excludeScheduleId;
}
