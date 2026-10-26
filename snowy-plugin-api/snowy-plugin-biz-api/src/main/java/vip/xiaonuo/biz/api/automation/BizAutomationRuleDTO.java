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
package vip.xiaonuo.biz.api.automation;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 自动化规则视图（含当前租户的开关与参数）
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Getter
@Setter
public class BizAutomationRuleDTO {

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 规则说明 */
    private String ruleDesc;

    /** 执行模式：TIMER / TRIGGER */
    private String mode;

    /** 是否开启 */
    private Boolean enabled;

    /** 当前参数 */
    private java.util.Map<String, String> params;

    /** 每日执行上限（0 表示不限） */
    private Integer dailyCap;

    /** holdout 对照组比例（0-100） */
    private Integer holdoutPercent;

    /** 配置版本 */
    private Integer version;

    /** 最近一次配置变更时间 */
    private String lastChangeTime;

    /** 最近一次执行成功数 */
    private Integer lastSuccessCount;

    /** 最近一次执行失败数 */
    private Integer lastFailCount;

    /** 是否提醒类规则 */
    private Boolean notificationRule;

    /** 是否支持 holdout */
    private Boolean supportsHoldout;

    /** 参数定义 */
    private List<BizAutomationHandler.AutomationParam> paramDef;

    /** 最近一次执行时间 */
    private String lastRunTime;

    /** 最近一次执行结果 */
    private String lastRunMessage;

    /** 最近一次是否成功 */
    private Boolean lastRunSuccess;

    /** 累计执行次数 */
    private Long totalRunCount;
}
