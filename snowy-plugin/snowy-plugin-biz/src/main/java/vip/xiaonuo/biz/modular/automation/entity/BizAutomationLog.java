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
package vip.xiaonuo.biz.modular.automation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 自动化执行记录
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Getter
@Setter
@TableName("BIZ_AUTOMATION_LOG")
public class BizAutomationLog extends CommonEntity {

    /** 主键 */
    @TableId
    @Schema(description = "主键")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 规则编码 */
    @Schema(description = "规则编码")
    private String ruleCode;

    /** 规则名称 */
    @Schema(description = "规则名称")
    private String ruleName;

    /** 触发方式：TIMER 定时 / MANUAL 手动 / TRIGGER 实时 */
    @Schema(description = "触发方式")
    private String triggerType;

    /** 是否成功：YES/NO */
    @Schema(description = "是否成功")
    private String success;

    /** 结果描述 */
    @Schema(description = "结果描述")
    private String message;

    /** 影响条数 */
    @Schema(description = "影响条数")
    private Integer affectedCount;

    /** 规则版本 */
    @Schema(description = "规则版本")
    private Integer ruleVersion;

    /** 输入哈希（租户+规则+版本+参数） */
    @Schema(description = "输入哈希")
    private String inputHash;

    /** 目标总数 */
    @Schema(description = "目标总数")
    private Integer targetCount;

    /** 成功数 */
    @Schema(description = "成功数")
    private Integer successCount;

    /** 失败数 */
    @Schema(description = "失败数")
    private Integer failCount;

    /** 跳过数 */
    @Schema(description = "跳过数")
    private Integer skipCount;

    /** 触达状态：CREATED/NONE/FAILED */
    @Schema(description = "触达状态")
    private String deliveryStatus;

    /** 触达条数 */
    @Schema(description = "触达条数")
    private Integer deliveryCount;

    /** 站内信id（单条消息时） */
    @Schema(description = "站内信id")
    private String messageId;

    /** 是否预览 */
    @Schema(description = "是否预览")
    private String dryRun;

    /** 实验分组：TREATMENT/HOLDOUT/MIXED/NONE */
    @Schema(description = "实验分组")
    private String experimentGroup;

    /** 归因状态：PENDING/SUCCESS/FAILED/NA */
    @Schema(description = "归因状态")
    private String attributionStatus;

    /** 明细（JSON） */
    @Schema(description = "明细")
    private String detail;

    /** 耗时（毫秒） */
    @Schema(description = "耗时")
    private Long costMs;
}
