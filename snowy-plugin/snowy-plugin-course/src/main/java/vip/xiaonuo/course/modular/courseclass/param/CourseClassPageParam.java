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
package vip.xiaonuo.course.modular.courseclass.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 班级查询参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class CourseClassPageParam {

    /** 当前页 */
    @Schema(description = "当前页码")
    private Integer current;

    /** 每页条数 */
    @Schema(description = "每页条数")
    private Integer size;

    /** 排序字段 */
    @Schema(description = "排序字段，字段驼峰名称")
    private String sortField;

    /** 排序方式 */
    @Schema(description = "排序方式，升序：ASCEND；降序：DESCEND")
    private String sortOrder;

    /** 名称关键词 */
    @Schema(description = "名称关键词")
    private String searchKey;

    /** 课程id */
    @Schema(description = "课程id")
    private String courseId;

    /** 班型：GROUP一对多 / ONE2ONE一对一 */
    @Schema(description = "班型：GROUP一对多 / ONE2ONE一对一")
    private String classType;

    /** 一对一班绑定的学员id */
    @Schema(description = "一对一班绑定的学员id")
    private String studentId;

    /** 授课教师id */
    @Schema(description = "授课教师id")
    private String teacherId;

    /** 教室 */
    @Schema(description = "教室")
    private String classroom;

    /** 状态 */
    @Schema(description = "状态(启用/停用)")
    private String status;

    /** 是否已排课 */
    @Schema(description = "是否已排课")
    private Boolean hasSchedule;

    /** 是否满班 */
    @Schema(description = "是否满班")
    private Boolean full;
}
