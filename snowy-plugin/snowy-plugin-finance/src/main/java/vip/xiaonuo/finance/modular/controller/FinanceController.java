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
package vip.xiaonuo.finance.modular.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.finance.modular.export.FinExportTask;
import vip.xiaonuo.finance.modular.export.FinExportTaskService;
import vip.xiaonuo.finance.modular.service.FinanceService;
import vip.xiaonuo.finance.modular.stat.entity.FinDailyStat;
import vip.xiaonuo.student.api.dto.DashboardOverviewDTO;
import vip.xiaonuo.student.api.dto.FinanceAttendTrendDTO;
import vip.xiaonuo.student.api.dto.FinanceCampusRankDTO;
import vip.xiaonuo.student.api.dto.FinanceChannelDTO;
import vip.xiaonuo.student.api.dto.FinanceConsumeDTO;
import vip.xiaonuo.student.api.dto.FinanceCourseRankDTO;
import vip.xiaonuo.student.api.dto.FinancePaymentDTO;
import vip.xiaonuo.student.api.dto.FinanceReconcileDTO;
import vip.xiaonuo.student.api.dto.FinanceRefundDTO;
import vip.xiaonuo.student.api.dto.FinanceStatDTO;
import vip.xiaonuo.student.api.dto.FinanceSummaryDTO;
import vip.xiaonuo.student.api.dto.FinanceTrendDTO;
import vip.xiaonuo.student.api.dto.ReportOptionDTO;
import vip.xiaonuo.student.api.param.FinanceQueryParam;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 财务控制器
 * <p>
 * 报表类接口按 Snowy 的接口权限模型控制（@SaCheckPermission），由角色授权维护；
 * 收款/退费明细与导出为敏感操作，权限点与汇总类接口区分，便于按角色细粒度授权。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "财务控制器")
@RestController
@Validated
public class FinanceController {

    @Resource
    private FinanceService financeService;

    @Resource
    private FinExportTaskService finExportTaskService;

    /**
     * 获取收款单分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取收款单分页")
    @SaCheckPermission("/finance/payment/page")
    @GetMapping("/finance/payment/page")
    public CommonResult<Page<FinancePaymentDTO>> paymentPage(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.paymentPage(financeQueryParam));
    }

    /**
     * 获取退费单分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取退费单分页")
    @SaCheckPermission("/finance/refund/page")
    @GetMapping("/finance/refund/page")
    public CommonResult<Page<FinanceRefundDTO>> refundPage(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.refundPage(financeQueryParam));
    }

    /**
     * 经营报表（按课程）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "经营报表")
    @SaCheckPermission("/finance/stat")
    @GetMapping("/finance/stat")
    public CommonResult<List<FinanceStatDTO>> stat(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.courseStat(financeQueryParam));
    }

    /**
     * 财务汇总
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "财务汇总")
    @SaCheckPermission("/finance/report/summary")
    @GetMapping("/finance/report/summary")
    public CommonResult<FinanceSummaryDTO> summary(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.summary(financeQueryParam));
    }

    /**
     * 财务趋势
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "财务趋势")
    @SaCheckPermission("/finance/report/trend")
    @GetMapping("/finance/report/trend")
    public CommonResult<List<FinanceTrendDTO>> trend(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.trend(financeQueryParam));
    }

    /**
     * 收款渠道占比
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "收款渠道占比")
    @SaCheckPermission("/finance/report/channel")
    @GetMapping("/finance/report/channel")
    public CommonResult<List<FinanceChannelDTO>> channel(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.channelStat(financeQueryParam));
    }

    /**
     * 课程收款排行
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "课程收款排行")
    @SaCheckPermission("/finance/report/courseRank")
    @GetMapping("/finance/report/courseRank")
    public CommonResult<List<FinanceCourseRankDTO>> courseRank(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.courseRank(financeQueryParam));
    }

    /**
     * 校区经营对比
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "校区经营对比")
    @SaCheckPermission("/finance/report/campusRank")
    @GetMapping("/finance/report/campusRank")
    public CommonResult<List<FinanceCampusRankDTO>> campusRank(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.campusRank(financeQueryParam));
    }

    /**
     * 课消趋势
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "课消趋势")
    @SaCheckPermission("/finance/report/attendTrend")
    @GetMapping("/finance/report/attendTrend")
    public CommonResult<List<FinanceAttendTrendDTO>> attendTrend(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.attendTrend(financeQueryParam));
    }

    /**
     * 校区筛选项
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "校区筛选项")
    @SaCheckPermission("/finance/report/campusOptions")
    @GetMapping("/finance/report/campusOptions")
    public CommonResult<List<ReportOptionDTO>> campusOptions() {
        return CommonResult.data(financeService.campusOptions());
    }

    /**
     * 导出报表
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "导出报表")
    @SaCheckPermission("/finance/report/export")
    @GetMapping("/finance/report/export")
    public void export(FinanceQueryParam financeQueryParam,
                       @RequestParam(required = false, defaultValue = "PAYMENT") String type,
                       HttpServletResponse response) throws IOException {
        financeService.export(financeQueryParam, type, response);
    }

    /**
     * 经营看板
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "经营看板")
    @SaCheckPermission("/finance/report/dashboard")
    @GetMapping("/finance/report/dashboard")
    public CommonResult<DashboardOverviewDTO> dashboard(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.dashboard(financeQueryParam));
    }

    /**
     * 日汇总数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "日汇总数据")
    @SaCheckPermission("/finance/report/dailyStat")
    @GetMapping("/finance/report/dailyStat")
    public CommonResult<List<FinDailyStat>> dailyStat(@RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return CommonResult.data(financeService.dailyStat(startDate, endDate));
    }

    /**
     * 重算指定日期的日汇总
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "重算日汇总")
    @SaCheckPermission("/finance/report/dailyStat/rebuild")
    @PostMapping("/finance/report/dailyStat/rebuild")
    public CommonResult<String> rebuildDailyStat(@RequestParam(required = false) String date) {
        financeService.rebuildDailyStat(date);
        return CommonResult.ok();
    }

    /**
     * 报表对账
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "报表对账")
    @SaCheckPermission("/finance/report/reconcile")
    @GetMapping("/finance/report/reconcile")
    public CommonResult<FinanceReconcileDTO> reconcile(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.reconcile(financeQueryParam));
    }

    /**
     * 课消与收入确认
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "课消与收入确认")
    @SaCheckPermission("/finance/report/consume")
    @GetMapping("/finance/report/consume")
    public CommonResult<List<FinanceConsumeDTO>> consume(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.consume(financeQueryParam));
    }

    /**
     * 创建异步导出任务
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "创建异步导出任务")
    @SaCheckPermission("/finance/report/export/async")
    @GetMapping("/finance/report/export/async")
    public CommonResult<FinExportTask> exportAsync(FinanceQueryParam financeQueryParam,
                                                    @RequestParam(required = false, defaultValue = "PAYMENT") String type) {
        return CommonResult.data(finExportTaskService.create(type, financeQueryParam));
    }

    /**
     * 查询异步导出任务状态
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "查询异步导出任务状态")
    @SaCheckPermission("/finance/report/export/async/status")
    @GetMapping("/finance/report/export/async/status")
    public CommonResult<FinExportTask> exportAsyncStatus(@RequestParam String id) {
        return CommonResult.data(finExportTaskService.get(id));
    }

    /**
     * 下载异步导出文件
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "下载异步导出文件")
    @SaCheckPermission("/finance/report/export/async/download")
    @GetMapping("/finance/report/export/async/download")
    public void exportAsyncDownload(@RequestParam String id, HttpServletResponse response) throws IOException {
        FinExportTask task = finExportTaskService.get(id);
        if (!FinExportTaskService.STATUS_SUCCESS.equals(task.getStatus())) {
            throw new CommonException("导出任务尚未完成，当前状态：{}", task.getStatus());
        }
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            throw new CommonException("导出文件已过期，请重新导出");
        }
        CommonDownloadUtil.download(task.getFileName(), FileUtil.readBytes(file), response);
    }

    /**
     * 清理报表缓存
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "清理报表缓存")
    @SaCheckPermission("/finance/report/cache/clear")
    @PostMapping("/finance/report/cache/clear")
    public CommonResult<String> clearCache() {
        financeService.clearCache();
        return CommonResult.ok();
    }
}
