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

import cn.hutool.json.JSONObject;
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
import vip.xiaonuo.student.modular.studentrenewarn.dto.StudentRenewWarnRiskVO;
import vip.xiaonuo.student.modular.studentrenewarn.dto.StudentRenewWarnStatsVO;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentFollowRecord;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentFollowAddParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentFollowPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnAssignParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnNotifyParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnProcessParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnRiskPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnRiskTodoParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnStatsParam;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnService;

import java.util.List;

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
     * 预警扫描
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

    /**
     * 标记预警已处理
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "标记预警已处理")
    @CommonLog("标记预警已处理")
    @PostMapping("/student/renewWarn/process")
    public CommonResult<String> process(@RequestBody @Valid StudentRenewWarnProcessParam param) {
        studentRenewWarnService.process(param);
        return CommonResult.ok();
    }

    /**
     * 发送续费提醒（仅落库通知记录）
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "发送续费提醒")
    @CommonLog("发送续费提醒")
    @PostMapping("/student/renewWarn/notify")
    public CommonResult<String> notify(@RequestBody @Valid StudentRenewWarnNotifyParam param) {
        studentRenewWarnService.notifyWarn(param);
        return CommonResult.ok();
    }

    /**
     * 转顾问（指派跟进人）
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "转顾问")
    @CommonLog("转顾问")
    @PostMapping("/student/renewWarn/assign")
    public CommonResult<String> assign(@RequestBody @Valid StudentRenewWarnAssignParam param) {
        return CommonResult.data(studentRenewWarnService.assign(param));
    }

    /**
     * 可指派顾问选项
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "可指派顾问选项")
    @GetMapping("/student/renewWarn/assigneeOptions")
    public CommonResult<List<JSONObject>> assigneeOptions(@RequestParam(required = false) String searchKey) {
        return CommonResult.data(studentRenewWarnService.assigneeOptions(searchKey));
    }

    /**
     * 预警阈值配置列表
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "预警阈值配置列表")
    @GetMapping("/student/renewWarn/config/list")
    public CommonResult<List<StudentRenewWarnConfig>> configList() {
        return CommonResult.data(studentRenewWarnService.configList());
    }

    /**
     * 保存预警阈值配置
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "保存预警阈值配置")
    @CommonLog("保存预警阈值配置")
    @PostMapping("/student/renewWarn/config/save")
    public CommonResult<String> configSave(@RequestBody @Valid StudentRenewWarnConfigParam param) {
        studentRenewWarnService.saveConfig(param);
        return CommonResult.ok();
    }

    /**
     * 删除预警阈值配置
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "删除预警阈值配置")
    @CommonLog("删除预警阈值配置")
    @PostMapping("/student/renewWarn/config/delete")
    public CommonResult<String> configDelete(@RequestBody @Valid StudentRenewWarnConfigIdParam param) {
        studentRenewWarnService.deleteConfig(param);
        return CommonResult.ok();
    }

    /**
     * 风险学员分页
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "风险学员分页")
    @GetMapping("/student/renewWarn/risk/page")
    public CommonResult<Page<StudentRenewWarnRiskVO>> riskPage(StudentRenewWarnRiskPageParam param) {
        return CommonResult.data(studentRenewWarnService.riskPage(param));
    }

    /**
     * 风险学员生成续费待办
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "风险学员生成续费待办")
    @CommonLog("风险学员生成续费待办")
    @PostMapping("/student/renewWarn/risk/todo")
    public CommonResult<String> riskTodo(@RequestBody @Valid StudentRenewWarnRiskTodoParam param) {
        return CommonResult.data(studentRenewWarnService.createRiskTodo(param));
    }

    /**
     * 新增回访/跟进记录
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "新增回访/跟进记录")
    @CommonLog("新增回访/跟进记录")
    @PostMapping("/student/renewWarn/follow/add")
    public CommonResult<String> followAdd(@RequestBody @Valid StudentFollowAddParam param) {
        return CommonResult.data(studentRenewWarnService.addFollow(param));
    }

    /**
     * 回访/跟进记录分页
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "回访/跟进记录分页")
    @GetMapping("/student/renewWarn/follow/page")
    public CommonResult<Page<StudentFollowRecord>> followPage(StudentFollowPageParam param) {
        return CommonResult.data(studentRenewWarnService.followPage(param));
    }

    /**
     * 续费率/流失率统计
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "续费率/流失率统计")
    @GetMapping("/student/renewWarn/stats")
    public CommonResult<StudentRenewWarnStatsVO> stats(StudentRenewWarnStatsParam param) {
        return CommonResult.data(studentRenewWarnService.stats(param));
    }
}
