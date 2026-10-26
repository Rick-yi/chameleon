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
package vip.xiaonuo.student.modular.student.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 学员编辑参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class StudentEditParam {

    /** id */
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "id不能为空")
    private String id;

    /** 学员姓名 */
    @Schema(description = "学员姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "学员姓名不能为空")
    private String name;

    /** 性别 */
    @Schema(description = "性别")
    private String gender;

    /** 生日 */
    @Schema(description = "生日")
    private Date birthday;

    /** 家长姓名 */
    @Schema(description = "家长姓名")
    private String parentName;

    /** 家长手机（选填） */
    @Schema(description = "家长手机")
    private String parentPhone;

    /** 校区id */
    @Schema(description = "校区id(引用SYS_ORG)")
    private String campusId;

    /** 状态 */
    @Schema(description = "状态(在读/停课/已退费)")
    private String status;

    /** 来源线索id */
    @Schema(description = "来源线索id")
    private String sourceLeadId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
