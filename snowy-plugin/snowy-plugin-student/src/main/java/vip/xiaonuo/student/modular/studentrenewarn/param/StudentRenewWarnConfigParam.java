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
package vip.xiaonuo.student.modular.studentrenewarn.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 续费预警阈值配置保存参数
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class StudentRenewWarnConfigParam {

    /** id（修改时传） */
    @Schema(description = "id")
    private String id;

    /** 配置范围：DEFAULT/COURSE/CLASS */
    @Schema(description = "配置范围(DEFAULT/COURSE/CLASS)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置范围不能为空")
    private String scopeType;

    /** 范围id：默认配置可为空，课程/班级必填 */
    @Schema(description = "范围id")
    private String scopeId;

    /** 剩余课时阈值 */
    @Schema(description = "剩余课时阈值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "剩余课时阈值不能为空")
    @DecimalMin(value = "0", message = "剩余课时阈值不能小于0")
    private BigDecimal lessonThreshold;

    /** 到期提前天数 */
    @Schema(description = "到期提前天数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "到期提前天数不能为空")
    @Min(value = 0, message = "到期提前天数不能小于0")
    @Max(value = 365, message = "到期提前天数不能大于365")
    private Integer expireDays;

    /** 重复提醒间隔（天） */
    @Schema(description = "重复提醒间隔(天)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "重复提醒间隔不能为空")
    @Min(value = 1, message = "重复提醒间隔不能小于1天")
    @Max(value = 365, message = "重复提醒间隔不能大于365天")
    private Integer repeatDays;

    /** 是否启用：YES/NO */
    @Schema(description = "是否启用(YES/NO)")
    private String enabled;
}
