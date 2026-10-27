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
package vip.xiaonuo.platform.modular.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 租户实体（SaaS 平台级，不参与租户隔离）
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("T_TENANT")
public class Tenant extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户编码（登录时识别机构，全局唯一） */
    @Schema(description = "租户编码")
    private String code;

    /** 机构名称 */
    @Schema(description = "机构名称")
    private String name;

    /** 联系人 */
    @Schema(description = "联系人")
    private String contactName;

    /** 联系电话 */
    @Schema(description = "联系电话")
    private String contactPhone;

    /** 套餐id */
    @Schema(description = "套餐id")
    private String packageId;

    /** 到期时间 */
    @Schema(description = "到期时间")
    private Date expireTime;

    /** 状态(试用/正常/停用) */
    @Schema(description = "状态(试用/正常/停用)")
    private String status;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 套餐名称（展示用） */
    @Schema(description = "套餐名称")
    @TableField(exist = false)
    private String packageName;

    /** 学员数上限（展示用） */
    @Schema(description = "学员数上限")
    @TableField(exist = false)
    private Integer studentLimit;

    /** 校区数上限（展示用） */
    @Schema(description = "校区数上限")
    @TableField(exist = false)
    private Integer campusLimit;

    /** 账号数上限（展示用） */
    @Schema(description = "账号数上限")
    @TableField(exist = false)
    private Integer accountLimit;

    /** 账号数（展示用） */
    @Schema(description = "账号数")
    @TableField(exist = false)
    private Long accountCount = 0L;

    /** 健康度评分（展示用，0-100） */
    @Schema(description = "健康度评分")
    @TableField(exist = false)
    private Integer healthScore;

    /** 健康度等级（展示用：HEALTHY/WARNING/RISK） */
    @Schema(description = "健康度等级")
    @TableField(exist = false)
    private String healthLevel;

    /** 健康度扣分原因（展示用，逗号分隔） */
    @Schema(description = "健康度扣分原因")
    @TableField(exist = false)
    private String healthReasons;

    /** 剩余天数（展示用，负数表示已过期） */
    @Schema(description = "剩余天数")
    @TableField(exist = false)
    private Long expireDays;

    /** 最近登录时间（展示用） */
    @Schema(description = "最近登录时间")
    @TableField(exist = false)
    private Date lastLoginTime;

    /** 学员数（展示用） */
    @Schema(description = "学员数")
    @TableField(exist = false)
    private Long studentCount = 0L;

    /** 订单数（展示用） */
    @Schema(description = "订单数")
    @TableField(exist = false)
    private Long orderCount = 0L;

    /** 收款金额（展示用） */
    @Schema(description = "收款金额")
    @TableField(exist = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 扩展信息 */
    @Schema(description = "扩展信息")
    private String extJson;
}
