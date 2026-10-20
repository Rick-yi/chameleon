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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.finance.modular.service.FinanceService;
import vip.xiaonuo.student.api.dto.DashboardOverviewDTO;
import vip.xiaonuo.student.api.dto.FinanceAttendTrendDTO;
import vip.xiaonuo.student.api.dto.FinanceCampusRankDTO;
import vip.xiaonuo.student.api.dto.FinanceChannelDTO;
import vip.xiaonuo.student.api.dto.FinanceCourseRankDTO;
import vip.xiaonuo.student.api.dto.FinancePaymentDTO;
import vip.xiaonuo.student.api.dto.FinanceRefundDTO;
import vip.xiaonuo.student.api.dto.FinanceStatDTO;
import vip.xiaonuo.student.api.dto.FinanceSummaryDTO;
import vip.xiaonuo.student.api.dto.FinanceTrendDTO;
import vip.xiaonuo.student.api.dto.ReportOptionDTO;
import vip.xiaonuo.student.api.param.FinanceQueryParam;

import java.io.IOException;
import java.util.List;

/**
 * 财务控制器
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

    /**
     * 获取收款单分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取收款单分页")
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
    @GetMapping("/finance/report/dashboard")
    public CommonResult<DashboardOverviewDTO> dashboard(FinanceQueryParam financeQueryParam) {
        return CommonResult.data(financeService.dashboard(financeQueryParam));
    }
}
