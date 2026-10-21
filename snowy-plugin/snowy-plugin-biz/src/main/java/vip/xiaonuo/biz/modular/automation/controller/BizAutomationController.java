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
package vip.xiaonuo.biz.modular.automation.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.biz.api.automation.BizAutomationRuleDTO;
import vip.xiaonuo.biz.modular.automation.entity.BizAutomationLog;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationLogPageParam;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationRunParam;
import vip.xiaonuo.biz.modular.automation.param.BizAutomationToggleParam;
import vip.xiaonuo.biz.modular.automation.service.BizAutomationService;
import vip.xiaonuo.biz.modular.automation.service.impl.BizAutomationServiceImpl;
import vip.xiaonuo.common.pojo.CommonResult;

import java.util.List;

/**
 * 自动化中心控制器
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@RestController
@RequestMapping("/biz/automation")
public class BizAutomationController {

    @Resource
    private BizAutomationService bizAutomationService;

    /**
     * 自动化规则列表
     */
    @SaCheckPermission("/biz/automation/rule")
    @GetMapping("/rule")
    public CommonResult<List<BizAutomationRuleDTO>> rule() {
        return CommonResult.data(bizAutomationService.ruleList());
    }

    /**
     * 开启/关闭规则并保存参数
     */
    @SaCheckPermission("/biz/automation/toggle")
    @PostMapping("/toggle")
    public CommonResult<String> toggle(@RequestBody @Valid BizAutomationToggleParam bizAutomationToggleParam) {
        bizAutomationService.toggle(bizAutomationToggleParam);
        return CommonResult.ok();
    }

    /**
     * 立即执行一条规则
     */
    @SaCheckPermission("/biz/automation/run")
    @PostMapping("/run")
    public CommonResult<BizAutomationHandler.AutomationResult> run(@RequestBody @Valid BizAutomationRunParam bizAutomationRunParam) {
        return CommonResult.data(bizAutomationService.run(bizAutomationRunParam.getRuleCode(),
                BizAutomationServiceImpl.TRIGGER_TYPE_MANUAL));
    }

    /**
     * 执行记录分页
     */
    @SaCheckPermission("/biz/automation/log")
    @GetMapping("/log")
    public CommonResult<Page<BizAutomationLog>> log(BizAutomationLogPageParam bizAutomationLogPageParam) {
        return CommonResult.data(bizAutomationService.logPage(bizAutomationLogPageParam));
    }
}
