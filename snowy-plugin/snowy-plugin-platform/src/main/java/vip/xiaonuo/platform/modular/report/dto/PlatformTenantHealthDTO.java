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
package vip.xiaonuo.platform.modular.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 租户健康度评分（规则评分，可解释）
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class PlatformTenantHealthDTO {

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 机构编码 */
    @Schema(description = "机构编码")
    private String tenantCode;

    /** 机构名称 */
    @Schema(description = "机构名称")
    private String tenantName;

    /** 套餐id */
    @Schema(description = "套餐id")
    private String packageId;

    /** 套餐名称 */
    @Schema(description = "套餐名称")
    private String packageName;

    /** 租户状态 */
    @Schema(description = "租户状态")
    private String status;

    /** 到期时间 */
    @Schema(description = "到期时间")
    private Date expireTime;

    /** 剩余天数（负数表示已过期） */
    @Schema(description = "剩余天数")
    private Long daysLeft;

    /** 最近登录时间 */
    @Schema(description = "最近登录时间")
    private Date lastLoginTime;

    /** 近30天登录次数 */
    @Schema(description = "近30天登录次数")
    private Long loginCount30d = 0L;

    /** 近30天到课次数（课消口径） */
    @Schema(description = "近30天到课次数")
    private Long consumeCount30d = 0L;

    /** 近90天收款金额 */
    @Schema(description = "近90天收款金额")
    private BigDecimal paidAmount90d = BigDecimal.ZERO;

    /** 登录得分（0-30） */
    @Schema(description = "登录得分")
    private Integer loginScore = 0;

    /** 课消得分（0-25） */
    @Schema(description = "课消得分")
    private Integer consumeScore = 0;

    /** 收款得分（0-25） */
    @Schema(description = "收款得分")
    private Integer paymentScore = 0;

    /** 到期得分（0-20） */
    @Schema(description = "到期得分")
    private Integer expiryScore = 0;

    /** 总分（0-100） */
    @Schema(description = "健康度总分")
    private Integer score = 0;

    /** 等级：HEALTHY/WARNING/RISK */
    @Schema(description = "健康度等级")
    private String level;

    /** 流失预警 */
    @Schema(description = "是否流失预警")
    private Boolean churnWarning = false;

    /** 扣分原因（可解释） */
    @Schema(description = "扣分原因")
    private List<String> reasons = new ArrayList<>();
}
