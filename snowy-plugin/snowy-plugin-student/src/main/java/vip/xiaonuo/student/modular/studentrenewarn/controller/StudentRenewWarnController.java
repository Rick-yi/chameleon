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
package vip.xiaonuo.student.modular.studentrenewarn.controller;

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
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnService;

import javax.validation.Valid;

/**
 * 续费预警控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "续费预警控制器")
@RestController
@Validated
public class StudentRenewWarnController {

    @Resource
    private StudentRenewWarnService studentRenewWarnService;

    /**
     * 获取预警分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取预警分页")
    @GetMapping("/student/renewWarn/page")
    public CommonResult<Page<StudentRenewWarn>> page(StudentRenewWarnPageParam studentRenewWarnPageParam) {
        return CommonResult.data(studentRenewWarnService.page(studentRenewWarnPageParam));
    }

    /**
     * 预警扫描（手动触发，定时任务后续接入）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "预警扫描")
    @CommonLog("预警扫描")
    @PostMapping("/student/renewWarn/scan")
    public CommonResult<String> scan() {
        studentRenewWarnService.scan();
        return CommonResult.ok();
    }

    /**
     * 忽略预警
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "忽略预警")
    @CommonLog("忽略预警")
    @PostMapping("/student/renewWarn/ignore")
    public CommonResult<String> ignore(@RequestBody @Valid StudentRenewWarnIdParam studentRenewWarnIdParam) {
        studentRenewWarnService.ignore(studentRenewWarnIdParam);
        return CommonResult.ok();
    }
}
