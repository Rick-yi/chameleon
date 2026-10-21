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
package vip.xiaonuo.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 课表视图DTO（周视图/日视图用）
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class CourseScheduleViewDTO {

    /** 排课id */
    @Schema(description = "排课id")
    private String id;

    /** 班级id */
    @Schema(description = "班级id")
    private String classId;

    /** 班级名称 */
    @Schema(description = "班级名称")
    private String className;

    /** 课程id */
    @Schema(description = "课程id")
    private String courseId;

    /** 课程名称 */
    @Schema(description = "课程名称")
    private String courseName;

    /** 校区id */
    @Schema(description = "校区id")
    private String campusId;

    /** 教师id */
    @Schema(description = "教师id")
    private String teacherId;

    /** 教师姓名 */
    @Schema(description = "教师姓名")
    private String teacherName;

    /** 上课日期 */
    @Schema(description = "上课日期")
    private Date scheduleDate;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;

    /** 教室 */
    @Schema(description = "教室")
    private String classroom;

    /** 状态 */
    @Schema(description = "状态")
    private String status;
}
