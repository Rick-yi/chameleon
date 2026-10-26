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
package vip.xiaonuo.platform.modular.tenant.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 租户添加参数
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class TenantAddParam {

    /** 租户编码 */
    @Schema(description = "租户编码（登录识别机构，全局唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "租户编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]{2,31}$", message = "租户编码需为3-32位字母、数字、下划线或短横线")
    private String code;

    /** 机构名称 */
    @Schema(description = "机构名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "机构名称不能为空")
    private String name;

    /** 联系人 */
    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /** 联系电话 */
    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入11位手机号")
    private String contactPhone;

    /** 套餐id */
    @Schema(description = "套餐id")
    private String packageId;

    /** 到期时间 */
    @Schema(description = "到期时间")
    private Date expireTime;

    /** 管理员账号，默认 admin */
    @Schema(description = "管理员账号")
    private String adminAccount;

    /** 管理员姓名，默认“机构管理员” */
    @Schema(description = "管理员姓名")
    private String adminName;

    /** 管理员初始密码，为空时使用系统默认密码 */
    @Schema(description = "管理员初始密码")
    private String adminPassword;

    /** 默认校区名称，默认“默认校区” */
    @Schema(description = "默认校区名称")
    private String campusName;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
