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
package vip.xiaonuo.student.modular.student.provider;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.api.StudentFinanceApi;
import vip.xiaonuo.student.api.dto.DashboardOverviewDTO;
import vip.xiaonuo.student.api.dto.DashboardTodoDTO;
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
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.enums.StudentStatusEnum;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.mapper.StudentLessonFlowMapper;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentrefund.entity.StudentRefund;
import vip.xiaonuo.student.modular.studentrefund.enums.StudentRefundStatusEnum;
import vip.xiaonuo.student.modular.studentrefund.mapper.StudentRefundMapper;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;
import vip.xiaonuo.student.modular.studentrenewarn.mapper.StudentRenewWarnMapper;
import vip.xiaonuo.sys.api.SysOrgApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 财务域查询API接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentFinanceApiProvider implements StudentFinanceApi {

    /** 未知课程占位 */
    private static final String UNKNOWN_COURSE = "UNKNOWN";

    /** 未分配校区占位 */
    private static final String UNKNOWN_CAMPUS = "UNKNOWN";

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Resource
    private StudentRefundMapper studentRefundMapper;

    @Resource
    private StudentLessonFlowMapper studentLessonFlowMapper;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private StudentRenewWarnMapper studentRenewWarnMapper;

    @Resource
    private StudentLessonAccountMapper studentLessonAccountMapper;

    @Resource
    private CourseApi courseApi;

    @Resource
    private SysOrgApi sysOrgApi;

    @Override
    public Page<FinancePaymentDTO> paymentPage(FinanceQueryParam financeQueryParam) {
        Page<StudentOrder> orderPage = studentOrderMapper.selectPage(CommonPageRequest.defaultPage(),
                buildPaymentQuery(financeQueryParam));
        Page<FinancePaymentDTO> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        resultPage.setRecords(toPaymentDtoList(orderPage.getRecords()));
        return resultPage;
    }

    @Override
    public List<FinancePaymentDTO> paymentList(FinanceQueryParam financeQueryParam) {
        return toPaymentDtoList(studentOrderMapper.selectList(buildPaymentQuery(financeQueryParam)));
    }

    @Override
    public Page<FinanceRefundDTO> refundPage(FinanceQueryParam financeQueryParam) {
        Page<StudentRefund> refundPage = studentRefundMapper.selectPage(CommonPageRequest.defaultPage(),
                buildRefundQuery(financeQueryParam));
        Page<FinanceRefundDTO> resultPage = new Page<>(refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());
        resultPage.setRecords(toRefundDtoList(refundPage.getRecords()));
        return resultPage;
    }

    @Override
    public List<FinanceRefundDTO> refundList(FinanceQueryParam financeQueryParam) {
        return toRefundDtoList(studentRefundMapper.selectList(buildRefundQuery(financeQueryParam)));
    }

    /**
     * 收款单查询条件
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private QueryWrapper<StudentOrder> buildPaymentQuery(FinanceQueryParam financeQueryParam) {
        QueryWrapper<StudentOrder> queryWrapper = new QueryWrapper<StudentOrder>().checkSqlInjection();
        queryWrapper.lambda()
                .in(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue(), StudentOrderStatusEnum.REFUNDED.getValue());
        if (ObjectUtil.isNotEmpty(financeQueryParam.getSearchKey())) {
            queryWrapper.lambda().and(q -> q.like(StudentOrder::getId, financeQueryParam.getSearchKey())
                    .or().like(StudentOrder::getPackageName, financeQueryParam.getSearchKey()));
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getStartTime())) {
            // 支付时间缺失时回落到创建时间（兼容历史数据）
            queryWrapper.lambda().apply("COALESCE(PAY_TIME, CREATE_TIME) >= {0}", DateUtil.parse(financeQueryParam.getStartTime()));
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getEndTime())) {
            queryWrapper.lambda().apply("COALESCE(PAY_TIME, CREATE_TIME) <= {0}", DateUtil.parse(financeQueryParam.getEndTime()));
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getChannel())) {
            queryWrapper.lambda().eq(StudentOrder::getPayChannel, financeQueryParam.getChannel());
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getCampusId())) {
            queryWrapper.lambda().eq(StudentOrder::getCampusId, financeQueryParam.getCampusId());
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getCourseId())) {
            queryWrapper.lambda().eq(StudentOrder::getCourseId, financeQueryParam.getCourseId());
        }
        queryWrapper.lambda().orderByDesc(StudentOrder::getPayTime);
        return queryWrapper;
    }

    /**
     * 退费单查询条件
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private QueryWrapper<StudentRefund> buildRefundQuery(FinanceQueryParam financeQueryParam) {
        QueryWrapper<StudentRefund> queryWrapper = new QueryWrapper<StudentRefund>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(financeQueryParam.getStatus())) {
            queryWrapper.lambda().eq(StudentRefund::getStatus, financeQueryParam.getStatus());
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getStartTime())) {
            queryWrapper.lambda().ge(StudentRefund::getCreateTime, DateUtil.parse(financeQueryParam.getStartTime()));
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getEndTime())) {
            queryWrapper.lambda().le(StudentRefund::getCreateTime, DateUtil.parse(financeQueryParam.getEndTime()));
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getCourseId())) {
            queryWrapper.lambda().eq(StudentRefund::getCourseId, financeQueryParam.getCourseId());
        }
        if (ObjectUtil.isNotEmpty(financeQueryParam.getCampusId())) {
            List<String> orderIdList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                            .eq(StudentOrder::getCampusId, financeQueryParam.getCampusId()))
                    .stream().map(StudentOrder::getId).collect(Collectors.toList());
            if (orderIdList.isEmpty()) {
                queryWrapper.apply("1 = 0");
            } else {
                queryWrapper.lambda().in(StudentRefund::getOrderId, orderIdList);
            }
        }
        queryWrapper.lambda().orderByDesc(StudentRefund::getCreateTime);
        return queryWrapper;
    }

    /**
     * 收款单DTO转换
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private List<FinancePaymentDTO> toPaymentDtoList(List<StudentOrder> orderList) {
        Map<String, String> studentNameMap = loadStudentNameMap(orderList.stream()
                .map(StudentOrder::getStudentId).collect(Collectors.toList()));
        List<FinancePaymentDTO> dtoList = new ArrayList<>();
        for (StudentOrder order : orderList) {
            FinancePaymentDTO dto = new FinancePaymentDTO();
            dto.setId(order.getId());
            dto.setStudentId(order.getStudentId());
            dto.setStudentName(studentNameMap.get(order.getStudentId()));
            dto.setCourseId(order.getCourseId());
            dto.setPackageName(order.getPackageName());
            dto.setAmount(order.getAmount());
            dto.setPaidAmount(order.getPaidAmount());
            dto.setPayChannel(order.getPayChannel());
            dto.setTradeNo(order.getTradeNo());
            dto.setStatus(order.getStatus());
            dto.setPayTime(order.getPayTime());
            dto.setCreateTime(order.getCreateTime());
            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * 退费单DTO转换
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private List<FinanceRefundDTO> toRefundDtoList(List<StudentRefund> refundList) {
        Map<String, String> studentNameMap = loadStudentNameMap(refundList.stream()
                .map(StudentRefund::getStudentId).collect(Collectors.toList()));
        List<FinanceRefundDTO> dtoList = new ArrayList<>();
        for (StudentRefund refund : refundList) {
            FinanceRefundDTO dto = new FinanceRefundDTO();
            dto.setId(refund.getId());
            dto.setOrderId(refund.getOrderId());
            dto.setStudentId(refund.getStudentId());
            dto.setStudentName(studentNameMap.get(refund.getStudentId()));
            dto.setCourseId(refund.getCourseId());
            dto.setRefundAmount(refund.getRefundAmount());
            dto.setRefundLessons(refund.getRefundLessons());
            dto.setStatus(refund.getStatus());
            dto.setOpinion(refund.getOpinion());
            dto.setApproveTime(refund.getApproveTime());
            dto.setCreateTime(refund.getCreateTime());
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    public List<FinanceStatDTO> courseStat(FinanceQueryParam financeQueryParam) {
        Date startTime = StrUtil.isNotEmpty(financeQueryParam.getStartTime())
                ? DateUtil.parse(financeQueryParam.getStartTime()) : null;
        Date endTime = StrUtil.isNotEmpty(financeQueryParam.getEndTime())
                ? DateUtil.parse(financeQueryParam.getEndTime()) : null;
        Map<String, FinanceStatDTO> statMap = new HashMap<>();
        // 收款（已支付/已退费订单，按支付时间；缺失时回落创建时间）
        LambdaQueryWrapper<StudentOrder> orderWrapper = new LambdaQueryWrapper<StudentOrder>()
                .in(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue(), StudentOrderStatusEnum.REFUNDED.getValue());
        if (startTime != null) {
            orderWrapper.apply("COALESCE(PAY_TIME, CREATE_TIME) >= {0}", startTime);
        }
        if (endTime != null) {
            orderWrapper.apply("COALESCE(PAY_TIME, CREATE_TIME) <= {0}", endTime);
        }
        for (StudentOrder order : studentOrderMapper.selectList(orderWrapper)) {
            FinanceStatDTO stat = statOf(statMap, order.getCourseId());
            stat.setPaidAmount(stat.getPaidAmount().add(ObjectUtil.isNotEmpty(order.getPaidAmount())
                    ? order.getPaidAmount() : BigDecimal.ZERO));
            stat.setOrderCount(stat.getOrderCount() + 1);
        }
        // 退费（已退款，按创建时间）
        LambdaQueryWrapper<StudentRefund> refundWrapper = new LambdaQueryWrapper<StudentRefund>()
                .eq(StudentRefund::getStatus, StudentRefundStatusEnum.REFUNDED.getValue())
                .ge(startTime != null, StudentRefund::getCreateTime, startTime)
                .le(endTime != null, StudentRefund::getCreateTime, endTime);
        for (StudentRefund refund : studentRefundMapper.selectList(refundWrapper)) {
            FinanceStatDTO stat = statOf(statMap, refund.getCourseId());
            stat.setRefundAmount(stat.getRefundAmount().add(ObjectUtil.isNotEmpty(refund.getRefundAmount())
                    ? refund.getRefundAmount() : BigDecimal.ZERO));
        }
        // 消课（点名消课流水，按创建时间）
        LambdaQueryWrapper<StudentLessonFlow> flowWrapper = new LambdaQueryWrapper<StudentLessonFlow>()
                .eq(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue())
                .ge(startTime != null, StudentLessonFlow::getCreateTime, startTime)
                .le(endTime != null, StudentLessonFlow::getCreateTime, endTime);
        for (StudentLessonFlow flow : studentLessonFlowMapper.selectList(flowWrapper)) {
            FinanceStatDTO stat = statOf(statMap, flow.getCourseId());
            stat.setAttendLessons(stat.getAttendLessons().add(ObjectUtil.isNotEmpty(flow.getLessons())
                    ? flow.getLessons().abs() : BigDecimal.ZERO));
        }
        // 净营收 = 收款 - 退费
        List<FinanceStatDTO> resultList = new ArrayList<>(statMap.values());
        resultList.forEach(stat -> stat.setNetAmount(stat.getPaidAmount().subtract(stat.getRefundAmount())));
        resultList.sort((a, b) -> b.getPaidAmount().compareTo(a.getPaidAmount()));
        return resultList;
    }

    /**
     * 取或创建课程统计对象
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private FinanceStatDTO statOf(Map<String, FinanceStatDTO> statMap, String courseId) {
        String key = StrUtil.isEmpty(courseId) ? UNKNOWN_COURSE : courseId;
        return statMap.computeIfAbsent(key, k -> {
            FinanceStatDTO stat = new FinanceStatDTO();
            stat.setCourseId(k);
            stat.setCourseName(UNKNOWN_COURSE.equals(k) ? "未关联课程" : courseApi.getCourseName(k));
            return stat;
        });
    }

    /**
     * 批量加载学员姓名
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    @Override
    public FinanceSummaryDTO summary(FinanceQueryParam financeQueryParam) {
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        if (start == null) {
            start = DateUtil.beginOfMonth(new Date());
        }
        if (end == null) {
            end = new Date();
        }
        List<StudentOrder> orderList = studentOrderMapper.selectList(paidOrderWrapper(financeQueryParam, start, end));
        List<StudentRefund> refundList = studentRefundMapper.selectList(refundWrapper(financeQueryParam, start, end));

        BigDecimal paidAmount = orderList.stream().map(this::orderPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = refundList.stream().map(item -> nvl(item.getRefundAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinanceSummaryDTO summary = new FinanceSummaryDTO();
        summary.setPaidAmount(paidAmount);
        summary.setRefundAmount(refundAmount);
        summary.setNetAmount(paidAmount.subtract(refundAmount));
        summary.setOrderCount((long) orderList.size());
        summary.setPayingStudentCount(orderList.stream().map(StudentOrder::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().count());
        summary.setAvgOrderAmount(orderList.isEmpty() ? BigDecimal.ZERO
                : paidAmount.divide(BigDecimal.valueOf(orderList.size()), 2, RoundingMode.HALF_UP));
        summary.setRefundRate(paidAmount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : refundAmount.multiply(BigDecimal.valueOf(100)).divide(paidAmount, 2, RoundingMode.HALF_UP));

        LambdaQueryWrapper<StudentOrder> unpaidWrapper = new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.UNPAID.getValue())
                .ge(StudentOrder::getCreateTime, start)
                .le(StudentOrder::getCreateTime, end)
                .eq(StrUtil.isNotBlank(financeQueryParam.getCampusId()), StudentOrder::getCampusId, financeQueryParam.getCampusId())
                .eq(StrUtil.isNotBlank(financeQueryParam.getCourseId()), StudentOrder::getCourseId, financeQueryParam.getCourseId());
        List<StudentOrder> unpaidList = studentOrderMapper.selectList(unpaidWrapper);
        summary.setUnpaidOrderCount((long) unpaidList.size());
        summary.setUnpaidAmount(unpaidList.stream().map(item -> nvl(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        LambdaQueryWrapper<Student> newStudentWrapper = new LambdaQueryWrapper<Student>()
                .ge(Student::getCreateTime, start)
                .le(Student::getCreateTime, end)
                .eq(StrUtil.isNotBlank(financeQueryParam.getCampusId()), Student::getCampusId, financeQueryParam.getCampusId());
        summary.setNewStudentCount(studentMapper.selectCount(newStudentWrapper));

        LambdaQueryWrapper<Student> studyingWrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getStatus, StudentStatusEnum.STUDYING.getValue())
                .eq(StrUtil.isNotBlank(financeQueryParam.getCampusId()), Student::getCampusId, financeQueryParam.getCampusId());
        summary.setStudyingStudentCount(studentMapper.selectCount(studyingWrapper));

        List<StudentLessonFlow> flowList = studentLessonFlowMapper.selectList(flowWrapper(financeQueryParam, start, end));
        summary.setAttendLessons(netAttendLessons(flowList));

        LambdaQueryWrapper<StudentRenewWarn> warnWrapper = new LambdaQueryWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING);
        if (StrUtil.isNotBlank(financeQueryParam.getCampusId())) {
            List<String> studentIdList = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .eq(Student::getCampusId, financeQueryParam.getCampusId()))
                    .stream().map(Student::getId).collect(Collectors.toList());
            if (studentIdList.isEmpty()) {
                warnWrapper.apply("1 = 0");
            } else {
                warnWrapper.in(StudentRenewWarn::getStudentId, studentIdList);
            }
        }
        summary.setRenewWarnCount(studentRenewWarnMapper.selectCount(warnWrapper));

        summary.setRecognizedAmount(consume(financeQueryParam).stream()
                .map(FinanceConsumeDTO::getRecognizedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (summary.getPayingStudentCount() > 0) {
            summary.setPerStudentRevenue(paidAmount.divide(BigDecimal.valueOf(summary.getPayingStudentCount()),
                    2, RoundingMode.HALF_UP));
        }
        BigDecimal receivableAmount = paidAmount.add(nvl(summary.getUnpaidAmount()));
        if (receivableAmount.compareTo(BigDecimal.ZERO) > 0) {
            summary.setCollectionRate(paidAmount.multiply(BigDecimal.valueOf(100))
                    .divide(receivableAmount, 2, RoundingMode.HALF_UP));
        }
        return summary;
    }

    @Override
    public List<FinanceTrendDTO> trend(FinanceQueryParam financeQueryParam) {
        boolean monthly = !"DAY".equalsIgnoreCase(financeQueryParam.getDimension());
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        if (start == null) {
            start = monthly
                    ? DateUtil.beginOfMonth(DateUtil.offsetMonth(new Date(), -5))
                    : DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -29));
        }
        if (end == null) {
            end = new Date();
        }
        Map<String, FinanceTrendDTO> resultMap = new LinkedHashMap<>();
        if (monthly) {
            Date cursor = DateUtil.beginOfMonth(start);
            while (!cursor.after(end) && resultMap.size() < 36) {
                String period = DateUtil.format(cursor, "yyyy-MM");
                resultMap.putIfAbsent(period, newTrend(period));
                cursor = DateUtil.offsetMonth(cursor, 1);
            }
        } else {
            Date cursor = DateUtil.beginOfDay(start);
            int guard = 0;
            while (!cursor.after(end) && guard < 93) {
                String period = DateUtil.formatDate(cursor);
                resultMap.putIfAbsent(period, newTrend(period));
                cursor = DateUtil.offsetDay(cursor, 1);
                guard++;
            }
        }

        List<StudentOrder> orderList = studentOrderMapper.selectList(paidOrderWrapper(financeQueryParam, start, end));
        for (StudentOrder order : orderList) {
            FinanceTrendDTO trend = resultMap.get(periodKey(orderTime(order), monthly));
            if (trend != null) {
                trend.setPaidAmount(trend.getPaidAmount().add(orderPaidAmount(order)));
            }
        }
        List<StudentRefund> refundList = studentRefundMapper.selectList(refundWrapper(financeQueryParam, start, end));
        for (StudentRefund refund : refundList) {
            FinanceTrendDTO trend = resultMap.get(periodKey(refundTime(refund), monthly));
            if (trend != null) {
                trend.setRefundAmount(trend.getRefundAmount().add(nvl(refund.getRefundAmount())));
            }
        }
        List<StudentLessonFlow> flowList = studentLessonFlowMapper.selectList(flowWrapper(financeQueryParam, start, end));
        for (StudentLessonFlow flow : flowList) {
            FinanceTrendDTO trend = resultMap.get(periodKey(flow.getCreateTime(), monthly));
            if (trend != null) {
                trend.setAttendLessons(trend.getAttendLessons().add(flowLessons(flow)));
            }
        }
        List<FinanceTrendDTO> resultList = new ArrayList<>(resultMap.values());
        resultList.forEach(item -> item.setNetAmount(item.getPaidAmount().subtract(item.getRefundAmount())));
        return resultList;
    }

    @Override
    public List<FinanceChannelDTO> channelStat(FinanceQueryParam financeQueryParam) {
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        List<StudentOrder> orderList = studentOrderMapper.selectList(paidOrderWrapper(financeQueryParam, start, end));
        Map<String, FinanceChannelDTO> channelMap = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (StudentOrder order : orderList) {
            String channel = StrUtil.blankToDefault(order.getPayChannel(), "UNKNOWN");
            FinanceChannelDTO dto = channelMap.computeIfAbsent(channel, key -> {
                FinanceChannelDTO item = new FinanceChannelDTO();
                item.setChannel(key);
                return item;
            });
            BigDecimal amount = orderPaidAmount(order);
            dto.setPaidAmount(dto.getPaidAmount().add(amount));
            dto.setOrderCount(dto.getOrderCount() + 1);
            total = total.add(amount);
        }
        List<FinanceChannelDTO> resultList = new ArrayList<>(channelMap.values());
        for (FinanceChannelDTO dto : resultList) {
            dto.setRatio(total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : dto.getPaidAmount().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
        }
        resultList.sort((a, b) -> b.getPaidAmount().compareTo(a.getPaidAmount()));
        return resultList;
    }

    @Override
    public List<FinanceCourseRankDTO> courseRank(FinanceQueryParam financeQueryParam) {
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        Map<String, FinanceCourseRankDTO> rankMap = new LinkedHashMap<>();
        for (StudentOrder order : studentOrderMapper.selectList(paidOrderWrapper(financeQueryParam, start, end))) {
            FinanceCourseRankDTO dto = rankOf(rankMap, order.getCourseId());
            dto.setPaidAmount(dto.getPaidAmount().add(orderPaidAmount(order)));
            dto.setOrderCount(dto.getOrderCount() + 1);
        }
        for (StudentRefund refund : studentRefundMapper.selectList(refundWrapper(financeQueryParam, start, end))) {
            FinanceCourseRankDTO dto = rankOf(rankMap, refund.getCourseId());
            dto.setRefundAmount(dto.getRefundAmount().add(nvl(refund.getRefundAmount())));
        }
        for (StudentLessonFlow flow : studentLessonFlowMapper.selectList(flowWrapper(financeQueryParam, start, end))) {
            FinanceCourseRankDTO dto = rankOf(rankMap, flow.getCourseId());
            dto.setAttendLessons(dto.getAttendLessons().add(flowLessons(flow)));
        }
        List<FinanceCourseRankDTO> resultList = new ArrayList<>(rankMap.values());
        resultList.forEach(dto -> dto.setNetAmount(dto.getPaidAmount().subtract(dto.getRefundAmount())));
        resultList.sort((a, b) -> b.getPaidAmount().compareTo(a.getPaidAmount()));
        int limit = ObjectUtil.isNotEmpty(financeQueryParam.getLimit()) ? financeQueryParam.getLimit() : 10;
        return resultList.stream().limit(Math.max(limit, 1)).collect(Collectors.toList());
    }

    @Override
    public List<FinanceCampusRankDTO> campusRank(FinanceQueryParam financeQueryParam) {
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        if (start == null) {
            start = DateUtil.beginOfMonth(new Date());
        }
        if (end == null) {
            end = new Date();
        }
        Map<String, FinanceCampusRankDTO> rankMap = new LinkedHashMap<>();
        List<StudentOrder> orderList = studentOrderMapper.selectList(paidOrderWrapper(financeQueryParam, start, end));
        Map<String, String> orderCampusMap = new HashMap<>();
        for (StudentOrder order : orderList) {
            String campusId = StrUtil.blankToDefault(order.getCampusId(), UNKNOWN_CAMPUS);
            orderCampusMap.put(order.getId(), campusId);
            FinanceCampusRankDTO dto = campusRankOf(rankMap, campusId);
            dto.setPaidAmount(dto.getPaidAmount().add(orderPaidAmount(order)));
            dto.setOrderCount(dto.getOrderCount() + 1);
        }
        List<StudentRefund> refundList = studentRefundMapper.selectList(refundWrapper(financeQueryParam, start, end));
        List<String> refundOrderIdList = refundList.stream().map(StudentRefund::getOrderId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(refundOrderIdList)) {
            for (StudentOrder order : studentOrderMapper.selectBatchIds(refundOrderIdList)) {
                orderCampusMap.put(order.getId(), StrUtil.blankToDefault(order.getCampusId(), UNKNOWN_CAMPUS));
            }
        }
        for (StudentRefund refund : refundList) {
            String campusId = StrUtil.blankToDefault(orderCampusMap.get(refund.getOrderId()), UNKNOWN_CAMPUS);
            FinanceCampusRankDTO dto = campusRankOf(rankMap, campusId);
            dto.setRefundAmount(dto.getRefundAmount().add(nvl(refund.getRefundAmount())));
        }
        Map<String, String> scheduleCampusCache = new HashMap<>();
        for (StudentLessonFlow flow : studentLessonFlowMapper.selectList(flowWrapper(financeQueryParam, start, end))) {
            String campusId;
            if (StrUtil.isBlank(flow.getScheduleId())) {
                campusId = UNKNOWN_CAMPUS;
            } else {
                campusId = scheduleCampusCache.computeIfAbsent(flow.getScheduleId(),
                        scheduleId -> StrUtil.blankToDefault(courseApi.getScheduleCampusId(scheduleId), UNKNOWN_CAMPUS));
            }
            FinanceCampusRankDTO dto = campusRankOf(rankMap, campusId);
            dto.setAttendLessons(dto.getAttendLessons().add(flowLessons(flow)));
        }
        Map<String, String> campusNameMap = loadCampusNameMap(rankMap.keySet());
        List<FinanceCampusRankDTO> resultList = new ArrayList<>(rankMap.values());
        resultList.forEach(dto -> {
            dto.setCampusName(UNKNOWN_CAMPUS.equals(dto.getCampusId()) ? "未分配校区"
                    : StrUtil.blankToDefault(campusNameMap.get(dto.getCampusId()), dto.getCampusId()));
            dto.setNetAmount(dto.getPaidAmount().subtract(dto.getRefundAmount()));
        });
        resultList.sort((a, b) -> b.getPaidAmount().compareTo(a.getPaidAmount()));
        return resultList;
    }

    @Override
    public List<FinanceAttendTrendDTO> attendTrend(FinanceQueryParam financeQueryParam) {
        Date end = parseEnd(financeQueryParam.getEndTime());
        if (end == null) {
            end = new Date();
        }
        Date start = parseStart(financeQueryParam.getStartTime());
        if (start == null) {
            start = DateUtil.beginOfDay(DateUtil.offsetDay(end, -29));
        }
        Map<String, FinanceAttendTrendDTO> resultMap = new LinkedHashMap<>();
        Date cursor = DateUtil.beginOfDay(start);
        int guard = 0;
        while (!cursor.after(end) && guard < 93) {
            String period = DateUtil.formatDate(cursor);
            FinanceAttendTrendDTO dto = new FinanceAttendTrendDTO();
            dto.setPeriod(period);
            resultMap.put(period, dto);
            cursor = DateUtil.offsetDay(cursor, 1);
            guard++;
        }
        for (StudentLessonFlow flow : studentLessonFlowMapper.selectList(flowWrapper(financeQueryParam, start, end))) {
            FinanceAttendTrendDTO dto = resultMap.get(DateUtil.formatDate(flow.getCreateTime()));
            if (dto == null) {
                continue;
            }
            dto.setAttendLessons(dto.getAttendLessons().add(flowLessons(flow)));
            if (LessonFlowTypeEnum.ATTEND.getValue().equals(flow.getFlowType())) {
                dto.setAttendCount(dto.getAttendCount() + 1);
            }
        }
        return new ArrayList<>(resultMap.values());
    }

    @Override
    public List<ReportOptionDTO> campusOptions() {
        Set<String> campusIdSet = new LinkedHashSet<>();
        for (Object campusId : studentMapper.selectObjs(new QueryWrapper<Student>()
                .select("DISTINCT CAMPUS_ID").isNotNull("CAMPUS_ID"))) {
            if (campusId != null) {
                campusIdSet.add(campusId.toString());
            }
        }
        for (Object campusId : studentOrderMapper.selectObjs(new QueryWrapper<StudentOrder>()
                .select("DISTINCT CAMPUS_ID").isNotNull("CAMPUS_ID"))) {
            if (campusId != null) {
                campusIdSet.add(campusId.toString());
            }
        }
        if (campusIdSet.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, String> campusNameMap = loadCampusNameMap(campusIdSet);
        return campusIdSet.stream()
                .map(campusId -> new ReportOptionDTO(campusId,
                        StrUtil.blankToDefault(campusNameMap.get(campusId), campusId)))
                .sorted((a, b) -> a.getLabel().compareTo(b.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FinanceConsumeDTO> consume(FinanceQueryParam financeQueryParam) {
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        if (start == null) {
            start = DateUtil.beginOfMonth(new Date());
        }
        if (end == null) {
            end = new Date();
        }
        LambdaQueryWrapper<StudentLessonFlow> wrapper = new LambdaQueryWrapper<StudentLessonFlow>()
                .in(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue(),
                        LessonFlowTypeEnum.RESTORE.getValue(), LessonFlowTypeEnum.REFUND.getValue())
                .ge(StudentLessonFlow::getCreateTime, start)
                .le(StudentLessonFlow::getCreateTime, end)
                .eq(StrUtil.isNotBlank(financeQueryParam.getCourseId()), StudentLessonFlow::getCourseId,
                        financeQueryParam.getCourseId());
        List<StudentLessonFlow> flowList = studentLessonFlowMapper.selectList(wrapper);
        if (ObjectUtil.isEmpty(flowList)) {
            return new ArrayList<>();
        }
        List<String> orderIdList = flowList.stream().map(StudentLessonFlow::getRefOrderId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, StudentOrder> orderMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(orderIdList)) {
            studentOrderMapper.selectBatchIds(orderIdList).forEach(order -> orderMap.put(order.getId(), order));
        }
        Map<String, FinanceConsumeDTO> resultMap = new LinkedHashMap<>();
        Map<String, Set<String>> orderIdSetMap = new HashMap<>();
        Map<String, Set<String>> studentIdSetMap = new HashMap<>();
        for (StudentLessonFlow flow : flowList) {
            StudentOrder order = orderMap.get(flow.getRefOrderId());
            BigDecimal lessons = nvl(flow.getLessons()).abs();
            boolean isAttend = LessonFlowTypeEnum.ATTEND.getValue().equals(flow.getFlowType());
            boolean isRestore = LessonFlowTypeEnum.RESTORE.getValue().equals(flow.getFlowType());
            boolean isRefund = LessonFlowTypeEnum.REFUND.getValue().equals(flow.getFlowType());
            String courseId = flow.getCourseId();
            String coursePackageId = order == null ? null : order.getCoursePackageId();
            String key = courseId + "|" + StrUtil.blankToDefault(coursePackageId, "");
            FinanceConsumeDTO dto = resultMap.computeIfAbsent(key, k -> {
                FinanceConsumeDTO item = new FinanceConsumeDTO();
                item.setCourseId(courseId);
                item.setCoursePackageId(coursePackageId);
                return item;
            });
            if (isAttend) {
                dto.setAttendLessons(dto.getAttendLessons().add(lessons));
            } else if (isRestore) {
                dto.setAttendLessons(dto.getAttendLessons().subtract(lessons));
            }
            BigDecimal unitPrice = order == null ? null : orderUnitPrice(order);
            if (unitPrice != null) {
                if (isAttend) {
                    dto.setRecognizedAmount(dto.getRecognizedAmount().add(lessons.multiply(unitPrice)));
                } else if (isRestore || isRefund) {
                    dto.setRecognizedAmount(dto.getRecognizedAmount().subtract(lessons.multiply(unitPrice)));
                }
            }
            if (order != null) {
                orderIdSetMap.computeIfAbsent(key, k -> new java.util.LinkedHashSet<>()).add(order.getId());
            }
            studentIdSetMap.computeIfAbsent(key, k -> new java.util.LinkedHashSet<>()).add(flow.getStudentId());
        }
        List<FinanceConsumeDTO> resultList = new ArrayList<>(resultMap.values());
        for (FinanceConsumeDTO dto : resultList) {
            String key = dto.getCourseId() + "|" + StrUtil.blankToDefault(dto.getCoursePackageId(), "");
            dto.setOrderCount((long) orderIdSetMap.getOrDefault(key, Set.of()).size());
            dto.setStudentCount((long) studentIdSetMap.getOrDefault(key, Set.of()).size());
            dto.setCourseName(StrUtil.blankToDefault(courseApi.getCourseName(dto.getCourseId()), "未关联课程"));
            if (StrUtil.isNotBlank(dto.getCoursePackageId())) {
                JSONObject packageInfo = courseApi.getCoursePackageInfo(dto.getCoursePackageId());
                dto.setCoursePackageName(packageInfo == null ? null : packageInfo.getStr("name"));
            }
        }
        resultList.sort((a, b) -> b.getRecognizedAmount().compareTo(a.getRecognizedAmount()));
        return resultList;
    }

    /**
     * 订单购买课时单价（实收 ÷ 购买课时）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private BigDecimal orderUnitPrice(StudentOrder order) {
        BigDecimal packageLessons = nvl(order.getPackageLessons());
        if (packageLessons.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal paid = order.getPaidAmount() == null ? nvl(order.getAmount()) : order.getPaidAmount();
        return paid.divide(packageLessons, 6, RoundingMode.HALF_UP);
    }

    @Override
    public FinanceReconcileDTO reconcile(FinanceQueryParam financeQueryParam) {
        Date start = parseStart(financeQueryParam.getStartTime());
        Date end = parseEnd(financeQueryParam.getEndTime());
        if (start == null) {
            start = DateUtil.beginOfMonth(new Date());
        }
        if (end == null) {
            end = new Date();
        }
        FinanceQueryParam param = copyParam(financeQueryParam);
        param.setStartTime(DateUtil.formatDateTime(start));
        param.setEndTime(DateUtil.formatDateTime(end));

        BigDecimal orderPaid = studentOrderMapper.selectList(paidOrderWrapper(param, start, end)).stream()
                .map(this::orderPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundDetail = studentRefundMapper.selectList(refundWrapper(param, start, end)).stream()
                .map(item -> nvl(item.getRefundAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal flowLessons = netAttendLessons(studentLessonFlowMapper.selectList(
                new LambdaQueryWrapper<StudentLessonFlow>()
                        .in(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue(),
                                LessonFlowTypeEnum.RESTORE.getValue())));
        List<StudentLessonAccount> accountList = studentLessonAccountMapper.selectList(
                new LambdaQueryWrapper<StudentLessonAccount>());
        BigDecimal accountUsed = accountList.stream().map(item -> nvl(item.getUsedLessons()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinanceSummaryDTO summary = summary(param);

        FinanceReconcileDTO result = new FinanceReconcileDTO();
        result.setOrderPaidAmount(orderPaid);
        result.setSummaryPaidAmount(summary.getPaidAmount());
        result.setPaidDiff(orderPaid.subtract(summary.getPaidAmount()));
        result.setRefundDetailAmount(refundDetail);
        result.setSummaryRefundAmount(summary.getRefundAmount());
        result.setRefundDiff(refundDetail.subtract(summary.getRefundAmount()));
        result.setFlowAttendLessons(flowLessons);
        result.setAccountUsedLessons(accountUsed);
        result.setLessonDiff(flowLessons.subtract(accountUsed));

        // 有流水支撑的账户与「历史数据无流水」的账户分开统计，避免误报
        Map<String, BigDecimal> accountFlowMap = new HashMap<>();
        for (StudentLessonFlow flow : studentLessonFlowMapper.selectList(
                new LambdaQueryWrapper<StudentLessonFlow>()
                        .in(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue(),
                                LessonFlowTypeEnum.RESTORE.getValue()))) {
            accountFlowMap.merge(flow.getStudentId() + "|" + flow.getCourseId(), flowLessons(flow), BigDecimal::add);
        }
        BigDecimal trackedUsed = BigDecimal.ZERO;
        BigDecimal trackedFlow = BigDecimal.ZERO;
        BigDecimal untrackedUsed = BigDecimal.ZERO;
        for (StudentLessonAccount account : accountList) {
            BigDecimal used = nvl(account.getUsedLessons());
            BigDecimal flow = accountFlowMap.getOrDefault(account.getStudentId() + "|" + account.getCourseId(),
                    BigDecimal.ZERO);
            if (flow.compareTo(BigDecimal.ZERO) == 0 && used.compareTo(BigDecimal.ZERO) != 0) {
                untrackedUsed = untrackedUsed.add(used);
            } else {
                trackedUsed = trackedUsed.add(used);
                trackedFlow = trackedFlow.add(flow);
            }
        }
        result.setTrackedUsedLessons(trackedUsed);
        result.setTrackedFlowLessons(trackedFlow);
        result.setUntrackedUsedLessons(untrackedUsed);

        if (result.getPaidDiff().compareTo(BigDecimal.ZERO) != 0) {
            result.getMessages().add("收款口径差异：" + result.getPaidDiff().toPlainString() + " 元");
        }
        if (result.getRefundDiff().compareTo(BigDecimal.ZERO) != 0) {
            result.getMessages().add("退费口径差异：" + result.getRefundDiff().toPlainString() + " 元");
        }
        BigDecimal trackedDiff = trackedFlow.subtract(trackedUsed);
        if (trackedDiff.compareTo(BigDecimal.ZERO) != 0) {
            result.getMessages().add("课消课时差异（有流水账户：流水净消耗 " + trackedFlow.toPlainString()
                    + " 与账户已用 " + trackedUsed.toPlainString() + "）");
        }
        if (untrackedUsed.compareTo(BigDecimal.ZERO) > 0) {
            result.getMessages().add("另有 " + untrackedUsed.toPlainString()
                    + " 课时为历史数据（无消课流水支撑，不计入差异）");
        }
        result.setConsistent(result.getMessages().isEmpty()
                || (trackedDiff.compareTo(BigDecimal.ZERO) == 0
                && result.getPaidDiff().compareTo(BigDecimal.ZERO) == 0
                && result.getRefundDiff().compareTo(BigDecimal.ZERO) == 0));
        return result;
    }

    @Override
    public DashboardOverviewDTO dashboard(FinanceQueryParam financeQueryParam) {
        DashboardOverviewDTO overview = new DashboardOverviewDTO();
        overview.setSummary(summary(financeQueryParam));
        overview.setChannelList(channelStat(financeQueryParam));
        overview.setCourseRankList(courseRank(financeQueryParam));

        FinanceQueryParam trendParam = copyParam(financeQueryParam);
        trendParam.setDimension("MONTH");
        trendParam.setStartTime(DateUtil.formatDateTime(DateUtil.beginOfMonth(DateUtil.offsetMonth(new Date(), -5))));
        trendParam.setEndTime(DateUtil.formatDateTime(new Date()));
        overview.setTrendList(trend(trendParam));

        overview.setCampusRankList(campusRank(financeQueryParam));

        FinanceQueryParam attendParam = copyParam(financeQueryParam);
        attendParam.setStartTime(DateUtil.formatDateTime(DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -29))));
        attendParam.setEndTime(DateUtil.formatDateTime(new Date()));
        overview.setAttendTrendList(attendTrend(attendParam));

        List<StudentRenewWarn> warnList = studentRenewWarnMapper.selectList(new LambdaQueryWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING)
                .orderByDesc(StudentRenewWarn::getCreateTime)
                .last("limit 10"));
        Map<String, String> warnStudentNameMap = loadStudentNameMap(warnList.stream()
                .map(StudentRenewWarn::getStudentId).collect(Collectors.toList()));
        List<DashboardTodoDTO> renewWarnList = new ArrayList<>();
        for (StudentRenewWarn warn : warnList) {
            DashboardTodoDTO todo = new DashboardTodoDTO();
            todo.setType("RENEW_WARN");
            todo.setId(warn.getId());
            todo.setStudentId(warn.getStudentId());
            todo.setStudentName(warnStudentNameMap.get(warn.getStudentId()));
            todo.setTitle("续费预警");
            todo.setRemainLessons(warn.getRemainLessons());
            todo.setContent("剩余" + nvl(warn.getRemainLessons()).stripTrailingZeros().toPlainString() + "课时，请及时跟进续费");
            todo.setTime(warn.getCreateTime());
            renewWarnList.add(todo);
        }
        overview.setRenewWarnList(renewWarnList);

        List<StudentOrder> unpaidList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.UNPAID.getValue())
                .orderByAsc(StudentOrder::getCreateTime)
                .last("limit 10"));
        Map<String, String> unpaidStudentNameMap = loadStudentNameMap(unpaidList.stream()
                .map(StudentOrder::getStudentId).collect(Collectors.toList()));
        List<DashboardTodoDTO> unpaidTodoList = new ArrayList<>();
        for (StudentOrder order : unpaidList) {
            DashboardTodoDTO todo = new DashboardTodoDTO();
            todo.setType("UNPAID_ORDER");
            todo.setId(order.getId());
            todo.setStudentId(order.getStudentId());
            todo.setStudentName(unpaidStudentNameMap.get(order.getStudentId()));
            todo.setTitle("待收款");
            todo.setAmount(nvl(order.getAmount()));
            todo.setContent(StrUtil.blankToDefault(order.getPackageName(), "报名订单")
                    + "，应收" + nvl(order.getAmount()).stripTrailingZeros().toPlainString() + "元");
            todo.setTime(order.getCreateTime());
            unpaidTodoList.add(todo);
        }
        overview.setUnpaidOrderList(unpaidTodoList);
        return overview;
    }

    /**
     * 已支付/已退费订单查询条件
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private LambdaQueryWrapper<StudentOrder> paidOrderWrapper(FinanceQueryParam param, Date start, Date end) {
        return new LambdaQueryWrapper<StudentOrder>()
                .in(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue(),
                        StudentOrderStatusEnum.REFUNDED.getValue())
                .apply(start != null, "COALESCE(PAY_TIME, CREATE_TIME) >= {0}", start)
                .apply(end != null, "COALESCE(PAY_TIME, CREATE_TIME) <= {0}", end)
                .eq(StrUtil.isNotBlank(param.getCampusId()), StudentOrder::getCampusId, param.getCampusId())
                .eq(StrUtil.isNotBlank(param.getCourseId()), StudentOrder::getCourseId, param.getCourseId())
                .eq(StrUtil.isNotBlank(param.getChannel()), StudentOrder::getPayChannel, param.getChannel());
    }

    /**
     * 已退款退费单查询条件
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private LambdaQueryWrapper<StudentRefund> refundWrapper(FinanceQueryParam param, Date start, Date end) {
        LambdaQueryWrapper<StudentRefund> wrapper = new LambdaQueryWrapper<StudentRefund>()
                .eq(StudentRefund::getStatus, StudentRefundStatusEnum.REFUNDED.getValue())
                .apply(start != null, "COALESCE(REFUND_TIME, APPROVE_TIME, UPDATE_TIME, CREATE_TIME) >= {0}", start)
                .apply(end != null, "COALESCE(REFUND_TIME, APPROVE_TIME, UPDATE_TIME, CREATE_TIME) <= {0}", end)
                .eq(StrUtil.isNotBlank(param.getCourseId()), StudentRefund::getCourseId, param.getCourseId());
        if (StrUtil.isNotBlank(param.getCampusId())) {
            List<String> orderIdList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                            .eq(StudentOrder::getCampusId, param.getCampusId()))
                    .stream().map(StudentOrder::getId).collect(Collectors.toList());
            if (orderIdList.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(StudentRefund::getOrderId, orderIdList);
            }
        }
        return wrapper;
    }

    /**
     * 课时流水查询条件（ATTEND/RESTORE）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private LambdaQueryWrapper<StudentLessonFlow> flowWrapper(FinanceQueryParam param, Date start, Date end) {
        LambdaQueryWrapper<StudentLessonFlow> wrapper = new LambdaQueryWrapper<StudentLessonFlow>()
                .in(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue(),
                        LessonFlowTypeEnum.RESTORE.getValue())
                .ge(start != null, StudentLessonFlow::getCreateTime, start)
                .le(end != null, StudentLessonFlow::getCreateTime, end)
                .eq(StrUtil.isNotBlank(param.getCourseId()), StudentLessonFlow::getCourseId, param.getCourseId());
        if (StrUtil.isNotBlank(param.getCampusId())) {
            List<String> scheduleIdList = courseApi.listScheduleIdsByCampus(param.getCampusId());
            if (scheduleIdList.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(StudentLessonFlow::getScheduleId, scheduleIdList);
            }
        }
        return wrapper;
    }

    /**
     * 时段key
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String periodKey(Date date, boolean monthly) {
        if (date == null) {
            return null;
        }
        return monthly ? DateUtil.format(date, "yyyy-MM") : DateUtil.formatDate(date);
    }

    /**
     * 新建趋势项
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private FinanceTrendDTO newTrend(String period) {
        FinanceTrendDTO trend = new FinanceTrendDTO();
        trend.setPeriod(period);
        return trend;
    }

    /**
     * 课程排行项
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private FinanceCourseRankDTO rankOf(Map<String, FinanceCourseRankDTO> rankMap, String courseId) {
        String key = StrUtil.blankToDefault(courseId, UNKNOWN_COURSE);
        return rankMap.computeIfAbsent(key, k -> {
            FinanceCourseRankDTO dto = new FinanceCourseRankDTO();
            dto.setCourseId(k);
            dto.setCourseName(UNKNOWN_COURSE.equals(k) ? "未关联课程" : courseApi.getCourseName(k));
            return dto;
        });
    }

    /**
     * 订单实收金额
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private BigDecimal orderPaidAmount(StudentOrder order) {
        return order.getPaidAmount() == null ? nvl(order.getAmount()) : order.getPaidAmount();
    }

    /**
     * 订单支付时间（历史数据回落到创建时间）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Date orderTime(StudentOrder order) {
        return order.getPayTime() == null ? order.getCreateTime() : order.getPayTime();
    }

    /**
     * 退费完成时间（历史数据逐级回落）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Date refundTime(StudentRefund refund) {
        if (refund.getRefundTime() != null) {
            return refund.getRefundTime();
        }
        if (refund.getApproveTime() != null) {
            return refund.getApproveTime();
        }
        return refund.getUpdateTime() == null ? refund.getCreateTime() : refund.getUpdateTime();
    }

    /**
     * 流水净课时
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private BigDecimal flowLessons(StudentLessonFlow flow) {
        BigDecimal lessons = nvl(flow.getLessons()).abs();
        return LessonFlowTypeEnum.RESTORE.getValue().equals(flow.getFlowType()) ? lessons.negate() : lessons;
    }

    /**
     * 净课消课时
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private BigDecimal netAttendLessons(List<StudentLessonFlow> flowList) {
        return flowList.stream().map(this::flowLessons).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 复制查询参数
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private FinanceQueryParam copyParam(FinanceQueryParam source) {
        FinanceQueryParam target = new FinanceQueryParam();
        target.setCampusId(source.getCampusId());
        target.setCourseId(source.getCourseId());
        target.setChannel(source.getChannel());
        target.setLimit(source.getLimit());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setDimension(source.getDimension());
        return target;
    }

    /**
     * 解析开始时间
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Date parseStart(String value) {
        return StrUtil.isBlank(value) ? null : DateUtil.parse(value);
    }

    /**
     * 解析结束时间
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Date parseEnd(String value) {
        return StrUtil.isBlank(value) ? null : DateUtil.parse(value);
    }

    /**
     * 空值转0
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 取或创建校区统计对象
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private FinanceCampusRankDTO campusRankOf(Map<String, FinanceCampusRankDTO> rankMap, String campusId) {
        return rankMap.computeIfAbsent(campusId, key -> {
            FinanceCampusRankDTO dto = new FinanceCampusRankDTO();
            dto.setCampusId(key);
            return dto;
        });
    }

    /**
     * 批量加载校区名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, String> loadCampusNameMap(Collection<String> campusIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = campusIdList.stream()
                .filter(StrUtil::isNotBlank)
                .filter(id -> !UNKNOWN_CAMPUS.equals(id))
                .distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (JSONObject org : sysOrgApi.getOrgListByIdListWithoutException(distinctIdList)) {
            nameMap.put(org.getStr("id"), org.getStr("name"));
        }
        return nameMap;
    }

    private Map<String, String> loadStudentNameMap(List<String> studentIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = studentIdList.stream().filter(StrUtil::isNotEmpty).distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (Student student : studentMapper.selectBatchIds(distinctIdList)) {
            nameMap.put(student.getId(), student.getName());
        }
        return nameMap;
    }
}
