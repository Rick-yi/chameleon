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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.finance.modular.service.FinanceService;
import vip.xiaonuo.student.api.StudentFinanceApi;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
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
    public List<FinanceCampusRankDTO> campusRank(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.campusRank(financeQueryParam);
    }

    @Override
    public List<FinanceAttendTrendDTO> attendTrend(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.attendTrend(financeQueryParam);
    }

    @Override
    public List<ReportOptionDTO> campusOptions() {
        return studentFinanceApi.campusOptions();
    }

    @Override
    public DashboardOverviewDTO dashboard(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.dashboard(financeQueryParam);
    }

    @Override
    public void export(FinanceQueryParam financeQueryParam, String type, HttpServletResponse response) throws IOException {
        String exportType = StrUtil.blankToDefault(type, "PAYMENT").toUpperCase();
        String sheetName;
        String fileName;
        List<List<String>> headList = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        if ("PAYMENT".equals(exportType)) {
            sheetName = "收款明细";
            fileName = "收款明细_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
            headList.add(List.of("学员"));
            headList.add(List.of("课包"));
            headList.add(List.of("应收金额"));
            headList.add(List.of("实收金额"));
            headList.add(List.of("收款渠道"));
            headList.add(List.of("支付时间"));
            headList.add(List.of("状态"));
            headList.add(List.of("订单号"));
            for (FinancePaymentDTO dto : studentFinanceApi.paymentList(financeQueryParam)) {
                rowList.add(List.of(text(dto.getStudentName()), text(dto.getPackageName()),
                        money(dto.getAmount()), money(dto.getPaidAmount()), channelName(dto.getPayChannel()),
                        time(dto.getPayTime() == null ? dto.getCreateTime() : dto.getPayTime()),
                        orderStatusName(dto.getStatus()), text(dto.getId())));
            }
        } else if ("REFUND".equals(exportType)) {
            sheetName = "退费明细";
            fileName = "退费明细_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
            headList.add(List.of("学员"));
            headList.add(List.of("退费金额"));
            headList.add(List.of("退费课时"));
            headList.add(List.of("状态"));
            headList.add(List.of("审批时间"));
            headList.add(List.of("退费单号"));
            for (FinanceRefundDTO dto : studentFinanceApi.refundList(financeQueryParam)) {
                rowList.add(List.of(text(dto.getStudentName()), money(dto.getRefundAmount()),
                        money(dto.getRefundLessons()), refundStatusName(dto.getStatus()),
                        time(dto.getApproveTime()), text(dto.getId())));
            }
        } else if ("COURSE_SUMMARY".equals(exportType)) {
            sheetName = "课程汇总";
            fileName = "课程汇总_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
            headList.add(List.of("课程"));
            headList.add(List.of("收款金额"));
            headList.add(List.of("退费金额"));
            headList.add(List.of("净营收"));
            headList.add(List.of("课消课时"));
            headList.add(List.of("订单数"));
            for (FinanceStatDTO dto : studentFinanceApi.courseStat(financeQueryParam)) {
                rowList.add(List.of(text(dto.getCourseName()), money(dto.getPaidAmount()),
                        money(dto.getRefundAmount()), money(dto.getNetAmount()),
                        money(dto.getAttendLessons()), String.valueOf(dto.getOrderCount() == null ? 0L : dto.getOrderCount())));
            }
        } else {
            throw new CommonException("不支持的导出类型：{}", exportType);
        }
        if (rowList.isEmpty()) {
            throw new CommonException("无数据可导出");
        }
        File file = FileUtil.file(FileUtil.getTmpDir(), fileName);
        try {
            EasyExcel.write(file)
                    .head(headList)
                    .sheet(sheetName)
                    .doWrite(rowList);
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String time(Date value) {
        return value == null ? "" : DateUtil.formatDateTime(value);
    }

    private String channelName(String channel) {
        if ("OFFLINE".equals(channel)) {
            return "线下转账";
        }
        if ("CASH".equals(channel)) {
            return "现金";
        }
        if ("WECHAT".equals(channel)) {
            return "微信";
        }
        if ("ALIPAY".equals(channel)) {
            return "支付宝";
        }
        return text(channel);
    }

    private String orderStatusName(String status) {
        if ("PAID".equals(status)) {
            return "已支付";
        }
        if ("REFUNDED".equals(status)) {
            return "已退费";
        }
        return text(status);
    }

    private String refundStatusName(String status) {
        if ("PENDING".equals(status)) {
            return "待审批";
        }
        if ("TO_REFUND".equals(status)) {
            return "待退款";
        }
        if ("REFUNDED".equals(status)) {
            return "已退款";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("CANCELLED".equals(status)) {
            return "已撤销";
        }
        return text(status);
    }
}
