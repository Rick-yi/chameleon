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
package vip.xiaonuo.platform.modular.billing.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.platform.modular.billing.dto.PlatformBillSummaryDTO;
import vip.xiaonuo.platform.modular.billing.entity.PlatformBill;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillAddParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillEditParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillIdParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillPageParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillPayParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillVoidParam;
import vip.xiaonuo.platform.modular.billing.service.PlatformBillService;

import java.io.IOException;
import java.util.List;

/**
 * 平台账单控制器（内部对账，不接真实支付/发票）
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Tag(name = "平台账单控制器")
@RestController
@Validated
public class PlatformBillController {

    @Resource
    private PlatformBillService platformBillService;

    @Operation(summary = "账单分页")
    @GetMapping("/platform/bill/page")
    public CommonResult<Page<PlatformBill>> page(PlatformBillPageParam pageParam) {
        return CommonResult.data(platformBillService.page(pageParam));
    }

    @Operation(summary = "账单汇总")
    @GetMapping("/platform/bill/summary")
    public CommonResult<PlatformBillSummaryDTO> summary(PlatformBillPageParam pageParam) {
        return CommonResult.data(platformBillService.summary(pageParam));
    }

    @Operation(summary = "新增账单")
    @CommonLog("新增平台账单")
    @PostMapping("/platform/bill/add")
    public CommonResult<String> add(@RequestBody @Valid PlatformBillAddParam addParam) {
        platformBillService.add(addParam);
        return CommonResult.ok();
    }

    @Operation(summary = "编辑账单")
    @CommonLog("编辑平台账单")
    @PostMapping("/platform/bill/edit")
    public CommonResult<String> edit(@RequestBody @Valid PlatformBillEditParam editParam) {
        platformBillService.edit(editParam);
        return CommonResult.ok();
    }

    @Operation(summary = "删除账单")
    @CommonLog("删除平台账单")
    @PostMapping("/platform/bill/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<PlatformBillIdParam> idParamList) {
        platformBillService.delete(idParamList);
        return CommonResult.ok();
    }

    @Operation(summary = "登记收款")
    @CommonLog("平台账单登记收款")
    @PostMapping("/platform/bill/pay")
    public CommonResult<String> pay(@RequestBody @Valid PlatformBillPayParam payParam) {
        platformBillService.markPaid(payParam);
        return CommonResult.ok();
    }

    @Operation(summary = "作废账单")
    @CommonLog("作废平台账单")
    @PostMapping("/platform/bill/void")
    public CommonResult<String> voidBill(@RequestBody @Valid PlatformBillVoidParam voidParam) {
        platformBillService.voidBill(voidParam);
        return CommonResult.ok();
    }

    @Operation(summary = "导出账单对账")
    @GetMapping("/platform/bill/export")
    public void export(PlatformBillPageParam pageParam, HttpServletResponse response) throws IOException {
        platformBillService.export(pageParam, response);
    }
}
