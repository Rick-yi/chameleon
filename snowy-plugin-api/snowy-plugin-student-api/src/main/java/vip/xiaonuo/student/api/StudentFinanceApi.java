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
package vip.xiaonuo.student.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.util.List;

/**
 * 财务域查询API接口（跨插件调用）
 * <p>
 * 财务数据来源于报名订单 / 退费单 / 课时流水，由 student 插件统一提供查询契约，
 * finance 插件负责对外暴露财务接口。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentFinanceApi {

    /**
     * 收款单分页（已支付 / 已退费订单）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    Page<FinancePaymentDTO> paymentPage(FinanceQueryParam financeQueryParam);

    /**
     * 收款单列表（导出用，不分页）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinancePaymentDTO> paymentList(FinanceQueryParam financeQueryParam);

    /**
     * 退费单分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    Page<FinanceRefundDTO> refundPage(FinanceQueryParam financeQueryParam);

    /**
     * 退费单列表（导出用，不分页）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceRefundDTO> refundList(FinanceQueryParam financeQueryParam);

    /**
     * 经营报表（按课程汇总：收款 / 退费 / 消课课时 / 净营收）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceStatDTO> courseStat(FinanceQueryParam financeQueryParam);

    /**
     * 财务汇总（收款/退费/净营收/订单/学员/课消）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    FinanceSummaryDTO summary(FinanceQueryParam financeQueryParam);

    /**
     * 财务趋势（按日/月）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceTrendDTO> trend(FinanceQueryParam financeQueryParam);

    /**
     * 收款渠道占比
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceChannelDTO> channelStat(FinanceQueryParam financeQueryParam);

    /**
     * 课程收款排行
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceCourseRankDTO> courseRank(FinanceQueryParam financeQueryParam);

    /**
     * 校区经营对比
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceCampusRankDTO> campusRank(FinanceQueryParam financeQueryParam);

    /**
     * 课消趋势（默认近 30 天）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<FinanceAttendTrendDTO> attendTrend(FinanceQueryParam financeQueryParam);

    /**
     * 校区筛选项
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<ReportOptionDTO> campusOptions();

    /**
     * 报表对账（报表数字与业务明细比对）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    FinanceReconcileDTO reconcile(FinanceQueryParam financeQueryParam);

    /**
     * 课消与收入确认（按课程 + 课包）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    List<FinanceConsumeDTO> consume(FinanceQueryParam financeQueryParam);

    /**
     * 经营看板聚合
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    DashboardOverviewDTO dashboard(FinanceQueryParam financeQueryParam);
}
