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
package vip.xiaonuo.finance.modular.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.finance.modular.service.FinanceService;
import vip.xiaonuo.student.api.StudentFinanceApi;
import vip.xiaonuo.student.api.dto.DashboardOverviewDTO;
import vip.xiaonuo.student.api.dto.FinanceChannelDTO;
import vip.xiaonuo.student.api.dto.FinanceCourseRankDTO;
import vip.xiaonuo.student.api.dto.FinancePaymentDTO;
import vip.xiaonuo.student.api.dto.FinanceRefundDTO;
import vip.xiaonuo.student.api.dto.FinanceStatDTO;
import vip.xiaonuo.student.api.dto.FinanceSummaryDTO;
import vip.xiaonuo.student.api.dto.FinanceTrendDTO;
import vip.xiaonuo.student.api.param.FinanceQueryParam;

import java.util.List;

/**
 * 财务Service接口实现类
 * <p>
 * 财务不维护独立流水表，直接基于报名订单/退费单/课时流水聚合，
 * 保证报表与业务数据天然一致。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class FinanceServiceImpl implements FinanceService {

    @Resource
    private StudentFinanceApi studentFinanceApi;

    @Override
    public Page<FinancePaymentDTO> paymentPage(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.paymentPage(financeQueryParam);
    }

    @Override
    public Page<FinanceRefundDTO> refundPage(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.refundPage(financeQueryParam);
    }

    @Override
    public List<FinanceStatDTO> courseStat(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.courseStat(financeQueryParam);
    }

    @Override
    public FinanceSummaryDTO summary(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.summary(financeQueryParam);
    }

    @Override
    public List<FinanceTrendDTO> trend(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.trend(financeQueryParam);
    }

    @Override
    public List<FinanceChannelDTO> channelStat(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.channelStat(financeQueryParam);
    }

    @Override
    public List<FinanceCourseRankDTO> courseRank(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.courseRank(financeQueryParam);
    }

    @Override
    public DashboardOverviewDTO dashboard(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.dashboard(financeQueryParam);
    }
}
