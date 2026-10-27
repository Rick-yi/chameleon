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
package vip.xiaonuo.student.modular.student.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 学员实体
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("STUDENT")
public class Student extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 学员姓名 */
    @Schema(description = "学员姓名")
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

    /** 家长手机 */
    @Schema(description = "家长手机")
    private String parentPhone;

    /** 校区id */
    @Schema(description = "校区id(引用SYS_ORG)")
    private String campusId;

    /** 状态 */
    @Schema(description = "状态(在读/停课/休学/退学/结课/已退费)")
    private String status;

    /** 状态变更原因 */
    @Schema(description = "状态变更原因")
    private String statusReason;

    /** 状态变更时间 */
    @Schema(description = "状态变更时间")
    private Date statusChangeTime;

    /** 来源线索id */
    @Schema(description = "来源线索id")
    private String sourceLeadId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 扩展信息 */
    @Schema(description = "扩展信息")
    private String extJson;

    /** 剩余课时合计（列表/详情展示用，非数据库字段） */
    @Schema(description = "剩余课时合计")
    @TableField(exist = false)
    private BigDecimal remainLessons;

    /** 最近课包到期时间（非数据库字段） */
    @Schema(description = "最近课包到期时间")
    @TableField(exist = false)
    private Date accountExpireTime;

    /** 所在班级（非数据库字段） */
    @Schema(description = "所在班级")
    @TableField(exist = false)
    private String classNames;

    /** 待收款金额（非数据库字段） */
    @Schema(description = "待收款金额")
    @TableField(exist = false)
    private BigDecimal unpaidAmount;

    /** 是否已有课时账户（非数据库字段，用于区分“未报名”和“课时用完”） */
    @Schema(description = "是否已有课时账户")
    @TableField(exist = false)
    private Boolean hasLessonAccount;

    /** 学员标签（非数据库字段，列表/详情回填） */
    @Schema(description = "学员标签")
    @TableField(exist = false)
    private List<vip.xiaonuo.student.modular.studenttag.entity.StudentTag> tagList;
}
