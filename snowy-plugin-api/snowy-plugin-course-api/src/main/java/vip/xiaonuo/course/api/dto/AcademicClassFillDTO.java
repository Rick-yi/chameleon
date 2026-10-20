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

import java.math.BigDecimal;

/**
 * 班级满班率DTO
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class AcademicClassFillDTO {

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

    /** 校区名称 */
    @Schema(description = "校区名称")
    private String campusName;

    /** 教师id */
    @Schema(description = "教师id")
    private String teacherId;

    /** 教师姓名 */
    @Schema(description = "教师姓名")
    private String teacherName;

    /** 容量 */
    @Schema(description = "容量")
    private Integer capacity = 0;

    /** 在学人数 */
    @Schema(description = "在学人数")
    private Long studyingCount = 0L;

    /** 满班率（百分比） */
    @Schema(description = "满班率")
    private BigDecimal fillRate = BigDecimal.ZERO;
}
