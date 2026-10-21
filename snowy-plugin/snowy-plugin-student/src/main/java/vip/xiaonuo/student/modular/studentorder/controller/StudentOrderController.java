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
package vip.xiaonuo.student.modular.studentorder.controller;

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
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderAddParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderIdParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderOfflinePayParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPageParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPayCallbackParam;
import vip.xiaonuo.student.modular.studentorder.service.StudentOrderService;

import javax.validation.Valid;

/**
 * 报名订单控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "报名订单控制器")
@RestController
@Validated
public class StudentOrderController {

    @Resource
    private StudentOrderService studentOrderService;

    /**
     * 获取订单分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取订单分页")
    @GetMapping("/student/order/page")
    public CommonResult<Page<StudentOrder>> page(StudentOrderPageParam studentOrderPageParam) {
        return CommonResult.data(studentOrderService.page(studentOrderPageParam));
    }

    /**
     * 报名下单
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "报名下单")
    @CommonLog("报名下单")
    @PostMapping("/student/order/add")
    public CommonResult<String> add(@RequestBody @Valid StudentOrderAddParam studentOrderAddParam) {
        return CommonResult.data(studentOrderService.add(studentOrderAddParam));
    }

    /**
     * 支付回调（幂等）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "支付回调")
    @CommonLog("支付回调")
    @PostMapping("/student/order/payCallback")
    public CommonResult<String> payCallback(@RequestBody @Valid StudentOrderPayCallbackParam studentOrderPayCallbackParam) {
        studentOrderService.payCallback(studentOrderPayCallbackParam);
        return CommonResult.ok();
    }

    /**
     * 线下收款登记（v1 支付统一走线下登记）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "线下收款登记")
    @CommonLog("线下收款登记")
    @PostMapping("/student/order/offlinePay")
    public CommonResult<String> offlinePay(@RequestBody @Valid StudentOrderOfflinePayParam studentOrderOfflinePayParam) {
        studentOrderService.offlinePay(studentOrderOfflinePayParam);
        return CommonResult.ok();
    }

    /**
     * 取消订单
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "取消订单")
    @CommonLog("取消订单")
    @PostMapping("/student/order/cancel")
    public CommonResult<String> cancel(@RequestBody @Valid StudentOrderIdParam studentOrderIdParam) {
        studentOrderService.cancel(studentOrderIdParam);
        return CommonResult.ok();
    }

    /**
     * 获取订单详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取订单详情")
    @GetMapping("/student/order/detail")
    public CommonResult<StudentOrder> detail(@Valid StudentOrderIdParam studentOrderIdParam) {
        return CommonResult.data(studentOrderService.detail(studentOrderIdParam));
    }
}
