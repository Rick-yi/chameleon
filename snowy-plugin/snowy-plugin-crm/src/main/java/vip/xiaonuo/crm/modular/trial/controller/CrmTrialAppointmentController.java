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
package vip.xiaonuo.crm.modular.trial.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.crm.modular.trial.entity.CrmTrialAppointment;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentAddParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentIdParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentPageParam;
import vip.xiaonuo.crm.modular.trial.service.CrmTrialAppointmentService;

import javax.validation.Valid;

/**
 * 试听预约控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "试听预约控制器")
@RestController
@Validated
public class CrmTrialAppointmentController {

    @Resource
    private CrmTrialAppointmentService crmTrialAppointmentService;

    /**
     * 获取试听预约分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取试听预约分页")
    @GetMapping("/crm/trial/page")
    public CommonResult<Page<CrmTrialAppointment>> page(CrmTrialAppointmentPageParam crmTrialAppointmentPageParam) {
        return CommonResult.data(crmTrialAppointmentService.page(crmTrialAppointmentPageParam));
    }

    /**
     * 添加试听预约
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加试听预约")
    @CommonLog("添加试听预约")
    @PostMapping("/crm/trial/add")
    public CommonResult<String> add(@RequestBody @Valid CrmTrialAppointmentAddParam crmTrialAppointmentAddParam) {
        crmTrialAppointmentService.add(crmTrialAppointmentAddParam);
        return CommonResult.ok();
    }

    /**
     * 取消试听预约
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "取消试听预约")
    @CommonLog("取消试听预约")
    @PostMapping("/crm/trial/cancel")
    public CommonResult<String> cancel(@RequestBody @Valid CrmTrialAppointmentIdParam crmTrialAppointmentIdParam) {
        crmTrialAppointmentService.cancel(crmTrialAppointmentIdParam);
        return CommonResult.ok();
    }
}
