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
package vip.xiaonuo.crm.modular.lead.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 线索添加参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class CrmLeadAddParam {

    /** 学员姓名 */
    @Schema(description = "学员姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "学员姓名不能为空")
    private String name;

    /** 家长手机 */
    @Schema(description = "家长手机", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "家长手机不能为空")
    private String parentPhone;

    /** 意向课程id */
    @Schema(description = "意向课程id")
    private String intentCourseId;

    /** 来源 */
    @Schema(description = "来源(GROUND/REFERRAL/ONLINE/VISIT)")
    private String source;

    /** 跟进人id */
    @Schema(description = "跟进人id")
    private String followUserId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
