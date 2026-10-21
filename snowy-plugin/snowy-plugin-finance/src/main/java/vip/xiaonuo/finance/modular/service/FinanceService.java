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
package vip.xiaonuo.finance.modular.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.util.List;

/**
 * 财务Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface FinanceService {

    /** 收款单分页 */
    Page<FinancePaymentDTO> paymentPage(FinanceQueryParam financeQueryParam);

    /** 退费单分页 */
    Page<FinanceRefundDTO> refundPage(FinanceQueryParam financeQueryParam);

    /** 经营报表（按课程汇总） */
    List<FinanceStatDTO> courseStat(FinanceQueryParam financeQueryParam);

    /** 财务汇总 */
    FinanceSummaryDTO summary(FinanceQueryParam financeQueryParam);

    /** 财务趋势 */
    List<FinanceTrendDTO> trend(FinanceQueryParam financeQueryParam);

    /** 收款渠道占比 */
    List<FinanceChannelDTO> channelStat(FinanceQueryParam financeQueryParam);

    /** 课程收款排行 */
    List<FinanceCourseRankDTO> courseRank(FinanceQueryParam financeQueryParam);

    /** 校区经营对比 */
    List<FinanceCampusRankDTO> campusRank(FinanceQueryParam financeQueryParam);

    /** 课消趋势 */
    List<FinanceAttendTrendDTO> attendTrend(FinanceQueryParam financeQueryParam);

    /** 校区筛选项 */
    List<ReportOptionDTO> campusOptions();

    /** 经营看板 */
    DashboardOverviewDTO dashboard(FinanceQueryParam financeQueryParam);

    /** 导出报表（PAYMENT/REFUND/COURSE_SUMMARY） */
    void export(FinanceQueryParam financeQueryParam, String type, HttpServletResponse response) throws IOException;

    /** 日汇总数据（按日） */
    List<FinDailyStat> dailyStat(String startDate, String endDate);

    /** 重算某日日汇总 */
    void rebuildDailyStat(String statDate);

    /** 报表对账（报表数字与业务明细比对） */
    FinanceReconcileDTO reconcile(FinanceQueryParam financeQueryParam);

    /** 课消与收入确认（按课程 + 课包） */
    List<FinanceConsumeDTO> consume(FinanceQueryParam financeQueryParam);

    /** 清理当前租户的报表缓存 */
    void clearCache();
}
