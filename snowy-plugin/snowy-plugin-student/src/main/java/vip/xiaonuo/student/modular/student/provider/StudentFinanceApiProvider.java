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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.student.api.StudentFinanceApi;
import vip.xiaonuo.student.api.dto.FinancePaymentDTO;
import vip.xiaonuo.student.api.dto.FinanceRefundDTO;
import vip.xiaonuo.student.api.dto.FinanceStatDTO;
import vip.xiaonuo.student.api.param.FinanceQueryParam;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.mapper.StudentLessonFlowMapper;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentrefund.entity.StudentRefund;
import vip.xiaonuo.student.modular.studentrefund.enums.StudentRefundStatusEnum;
import vip.xiaonuo.student.modular.studentrefund.mapper.StudentRefundMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Resource
    private StudentRefundMapper studentRefundMapper;

    @Resource
    private StudentLessonFlowMapper studentLessonFlowMapper;

    @Resource
    private StudentMapper studentMapper;

    @Override
    public Page<FinancePaymentDTO> paymentPage(FinanceQueryParam financeQueryParam) {
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
        queryWrapper.lambda().orderByDesc(StudentOrder::getPayTime);
        Page<StudentOrder> orderPage = studentOrderMapper.selectPage(CommonPageRequest.defaultPage(), queryWrapper);
        Map<String, String> studentNameMap = loadStudentNameMap(orderPage.getRecords().stream()
                .map(StudentOrder::getStudentId).collect(Collectors.toList()));
        List<FinancePaymentDTO> dtoList = new ArrayList<>();
        for (StudentOrder order : orderPage.getRecords()) {
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
        Page<FinancePaymentDTO> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public Page<FinanceRefundDTO> refundPage(FinanceQueryParam financeQueryParam) {
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
        queryWrapper.lambda().orderByDesc(StudentRefund::getCreateTime);
        Page<StudentRefund> refundPage = studentRefundMapper.selectPage(CommonPageRequest.defaultPage(), queryWrapper);
        Map<String, String> studentNameMap = loadStudentNameMap(refundPage.getRecords().stream()
                .map(StudentRefund::getStudentId).collect(Collectors.toList()));
        List<FinanceRefundDTO> dtoList = new ArrayList<>();
        for (StudentRefund refund : refundPage.getRecords()) {
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
        Page<FinanceRefundDTO> resultPage = new Page<>(refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());
        resultPage.setRecords(dtoList);
        return resultPage;
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
            return stat;
        });
    }

    /**
     * 批量加载学员姓名
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
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
