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
package vip.xiaonuo.student.modular.studentrefund.controller;

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
import vip.xiaonuo.student.modular.studentrefund.entity.StudentRefund;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundAddParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundApproveParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundIdParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundPageParam;
import vip.xiaonuo.student.modular.studentrefund.service.StudentRefundService;

import javax.validation.Valid;

/**
 * 退费单控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "退费单控制器")
@RestController
@Validated
public class StudentRefundController {

    @Resource
    private StudentRefundService studentRefundService;

    /**
     * 获取退费单分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取退费单分页")
    @GetMapping("/student/refund/page")
    public CommonResult<Page<StudentRefund>> page(StudentRefundPageParam studentRefundPageParam) {
        return CommonResult.data(studentRefundService.page(studentRefundPageParam));
    }

    /**
     * 退费申请
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "退费申请")
    @CommonLog("退费申请")
    @PostMapping("/student/refund/apply")
    public CommonResult<String> apply(@RequestBody @Valid StudentRefundAddParam studentRefundAddParam) {
        studentRefundService.apply(studentRefundAddParam);
        return CommonResult.ok();
    }

    /**
     * 退费审批
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "退费审批")
    @CommonLog("退费审批")
    @PostMapping("/student/refund/approve")
    public CommonResult<String> approve(@RequestBody @Valid StudentRefundApproveParam studentRefundApproveParam) {
        studentRefundService.approve(studentRefundApproveParam);
        return CommonResult.ok();
    }

    /**
     * 确认退款完成
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "确认退款完成")
    @CommonLog("确认退款完成")
    @PostMapping("/student/refund/confirm")
    public CommonResult<String> confirm(@RequestBody @Valid StudentRefundIdParam studentRefundIdParam) {
        studentRefundService.confirm(studentRefundIdParam);
        return CommonResult.ok();
    }

    /**
     * 撤销退费
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "撤销退费")
    @CommonLog("撤销退费")
    @PostMapping("/student/refund/cancel")
    public CommonResult<String> cancel(@RequestBody @Valid StudentRefundIdParam studentRefundIdParam) {
        studentRefundService.cancel(studentRefundIdParam);
        return CommonResult.ok();
    }

    /**
     * 获取退费单详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取退费单详情")
    @GetMapping("/student/refund/detail")
    public CommonResult<StudentRefund> detail(@Valid StudentRefundIdParam studentRefundIdParam) {
        return CommonResult.data(studentRefundService.detail(studentRefundIdParam));
    }
}
