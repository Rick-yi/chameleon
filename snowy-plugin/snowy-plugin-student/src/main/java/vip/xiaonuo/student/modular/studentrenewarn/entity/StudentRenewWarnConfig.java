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
package vip.xiaonuo.student.modular.studentrenewarn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;

/**
 * 续费预警阈值配置实体
 * <p>
 * 支持机构默认、按课程、按班级三级配置，扫描时按「班级 &gt; 课程 &gt; 机构默认」取生效值。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
@TableName("STUDENT_RENEW_WARN_CONFIG")
public class StudentRenewWarnConfig extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 配置范围：DEFAULT/COURSE/CLASS */
    @Schema(description = "配置范围(DEFAULT/COURSE/CLASS)")
    private String scopeType;

    /** 范围id：默认配置固定为0，课程/班级为对应id */
    @Schema(description = "范围id")
    private String scopeId;

    /** 剩余课时阈值（小于等于该值触发课时不足预警） */
    @Schema(description = "剩余课时阈值")
    private BigDecimal lessonThreshold;

    /** 课包到期前N天触发预警 */
    @Schema(description = "到期提前天数")
    private Integer expireDays;

    /** 重复提醒/扫描去重间隔（天） */
    @Schema(description = "重复提醒间隔(天)")
    private Integer repeatDays;

    /** 是否启用：YES/NO */
    @Schema(description = "是否启用(YES/NO)")
    private String enabled;

    /** 范围名称（列表展示用，非数据库字段） */
    @Schema(description = "范围名称")
    @TableField(exist = false)
    private String scopeName;
}
