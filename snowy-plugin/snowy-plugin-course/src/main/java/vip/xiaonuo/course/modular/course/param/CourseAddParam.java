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
package vip.xiaonuo.course.modular.course.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 课程添加参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class CourseAddParam {

    /** 课程大类id */
    @Schema(description = "课程大类id")
    private String categoryId;

    /** 课程名称 */
    @Schema(description = "课程名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "课程名称不能为空")
    private String name;

    /** 班型 */
    @Schema(description = "班型(小班/大班/一对一)")
    private String classType;

    /** 适合年龄 */
    @Schema(description = "适合年龄")
    private String ageRange;

    /** 每课扣课时数 */
    @Schema(description = "每课扣课时数")
    private BigDecimal lessonsPerClass;

    /** 请假是否扣课时 */
    @Schema(description = "请假是否扣课时")
    private Boolean leaveDeduct;

    /** 缺勤是否扣课时 */
    @Schema(description = "缺勤是否扣课时")
    private Boolean absentDeduct;

    /** 状态 */
    @Schema(description = "状态(启用/停用)")
    private String status;
}
