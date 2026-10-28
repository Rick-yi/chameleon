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
package vip.xiaonuo.student.modular.studentsundry.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.student.modular.studentsundry.entity.StudentSundryOrder;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryOrderAddParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryPageParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryPayParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryRefundParam;
import vip.xiaonuo.student.modular.studentsundry.service.StudentSundryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 学杂费控制器
 * <p>
 * 学杂费与课包订单分账，接口独立，权限码按 /student/sundry/** 单独登记。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@Tag(name = "学杂费控制器")
@RestController
@Validated
public class StudentSundryController {

    @Resource
    private StudentSundryService studentSundryService;

    /**
     * 分页查询
     */
    @Operation(summary = "学杂费分页")
    @GetMapping("/student/sundry/page")
    public CommonResult<Page<StudentSundryOrder>> page(StudentSundryPageParam param) {
        return CommonResult.data(studentSundryService.page(param));
    }

    /**
     * 按学员查询
     */
    @Operation(summary = "学员学杂费列表")
    @GetMapping("/student/sundry/listByStudent")
    public CommonResult<List<StudentSundryOrder>> listByStudent(@RequestParam String studentId) {
        return CommonResult.data(studentSundryService.listByStudent(studentId));
    }

    /**
     * 学杂费汇总
     */
    @Operation(summary = "学员学杂费汇总")
    @GetMapping("/student/sundry/summary")
    public CommonResult<Map<String, BigDecimal>> summary(@RequestParam String studentId) {
        return CommonResult.data(studentSundryService.summaryByStudent(studentId));
    }

    /**
     * 新开单据
     */
    @Operation(summary = "新增学杂费")
    @CommonLog("新增学杂费")
    @PostMapping("/student/sundry/add")
    public CommonResult<String> add(@RequestBody @Valid StudentSundryOrderAddParam param) {
        return CommonResult.data(studentSundryService.add(param));
    }

    /**
     * 收款
     */
    @Operation(summary = "学杂费收款")
    @CommonLog("学杂费收款")
    @PostMapping("/student/sundry/pay")
    public CommonResult<String> pay(@RequestBody @Valid StudentSundryPayParam param) {
        studentSundryService.pay(param);
        return CommonResult.ok();
    }

    /**
     * 退费
     */
    @Operation(summary = "学杂费退费")
    @CommonLog("学杂费退费")
    @PostMapping("/student/sundry/refund")
    public CommonResult<String> refund(@RequestBody @Valid StudentSundryRefundParam param) {
        studentSundryService.refund(param);
        return CommonResult.ok();
    }

    /**
     * 取消
     */
    @Operation(summary = "取消学杂费单")
    @CommonLog("取消学杂费单")
    @PostMapping("/student/sundry/cancel")
    public CommonResult<String> cancel(@RequestParam String id) {
        studentSundryService.cancel(id);
        return CommonResult.ok();
    }

    /**
     * 删除
     */
    @Operation(summary = "删除学杂费单")
    @CommonLog("删除学杂费单")
    @PostMapping("/student/sundry/delete")
    public CommonResult<String> delete(@RequestParam String id) {
        studentSundryService.delete(id);
        return CommonResult.ok();
    }
}
