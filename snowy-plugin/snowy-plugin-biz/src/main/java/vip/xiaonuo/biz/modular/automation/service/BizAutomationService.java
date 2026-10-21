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
package vip.xiaonuo.biz.modular.automation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.biz.api.automation.BizAutomationRuleDTO;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationLog;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationLogPageParam;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationToggleParam;

import java.util.List;

/**
 * 自动化规则Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
public interface BizAutomationService {

    /** 规则列表（含当前租户的开关、参数与最近执行情况） */
    List<BizAutomationRuleDTO> ruleList();

    /** 开启/关闭规则并保存参数 */
    void toggle(BizAutomationToggleParam bizAutomationToggleParam);

    /** 立即执行一条规则 */
    BizAutomationHandler.AutomationResult run(String ruleCode, String triggerType);

    /** 执行记录分页 */
    Page<BizAutomationLog> logPage(BizAutomationLogPageParam bizAutomationLogPageParam);

    /** 规则是否开启 */
    boolean isEnabled(String ruleCode);

    /** 规则参数 */
    java.util.Map<String, String> getParams(String ruleCode);

    /** 执行当前租户下所有已开启的定时型规则，返回执行的规则数 */
    int runTimerRules();
}
