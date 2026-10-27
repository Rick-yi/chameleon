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
package vip.xiaonuo.student.modular.student.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.core.enums.StudentStatusMachineEnum;
import vip.xiaonuo.student.modular.student.dto.StudentTimelineItemDTO;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.student.service.StudentTimelineService;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.mapper.StudentLessonFlowMapper;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentrefund.entity.StudentRefund;
import vip.xiaonuo.student.modular.studentrefund.enums.StudentRefundStatusEnum;
import vip.xiaonuo.student.modular.studentrefund.mapper.StudentRefundMapper;
import vip.xiaonuo.student.modular.studentstatus.entity.StudentStatusLog;
import vip.xiaonuo.student.modular.studentstatus.mapper.StudentStatusLogMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学员动态时间线实现
 * <p>
 * 全部事件实时读取现有业务表（订单/退费/课时流水/状态记录/线索跟进），不重复落库。
 * CRM 跟进通过 JdbcTemplate 只读查询，避免为一条时间线引入插件间强依赖。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Service
public class StudentTimelineServiceImpl implements StudentTimelineService {

    /** 单次最多返回条数 */
    private static final int MAX_ITEMS = 300;

    @Resource
    private StudentService studentService;

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Resource
    private StudentRefundMapper studentRefundMapper;

    @Resource
    private StudentLessonFlowMapper studentLessonFlowMapper;

    @Resource
    private StudentStatusLogMapper studentStatusLogMapper;

    @Resource
    private CourseApi courseApi;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<StudentTimelineItemDTO> timeline(String studentId) {
        Student student = studentService.queryEntity(studentId);
        List<StudentTimelineItemDTO> itemList = new ArrayList<>();

        // 1. 报名与收款（订单）
        List<StudentOrder> orderList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getStudentId, studentId)
                .orderByDesc(StudentOrder::getCreateTime));
        for (StudentOrder order : orderList) {
            itemList.add(buildTimelineItem(order.getCreateTime(), "SIGNUP", "报名",
                    "报名：" + StrUtil.blankToDefault(order.getPackageName(), "课程包"),
                    "应收 ¥" + money(order.getAmount()) + " · " + orderStatusText(order.getStatus()),
                    order.getAmount(), null, order.getId(), order.getCreateUser()));
            if (order.getPayTime() != null) {
                itemList.add(buildTimelineItem(order.getPayTime(), "PAY", "收款",
                        "收款：" + StrUtil.blankToDefault(order.getPackageName(), "课程包"),
                        "实收 ¥" + money(order.getPaidAmount()) + " · "
                                + StrUtil.blankToDefault(order.getPayChannel(), "线下")
                                + (StrUtil.isBlank(order.getTradeNo()) ? "" : " · 流水号 " + order.getTradeNo()),
                        order.getPaidAmount(), null, order.getId(), order.getUpdateUser()));
            }
        }

        // 2. 退费：申请 / 审批 / 完成
        List<StudentRefund> refundList = studentRefundMapper.selectList(new LambdaQueryWrapper<StudentRefund>()
                .eq(StudentRefund::getStudentId, studentId)
                .orderByDesc(StudentRefund::getCreateTime));
        for (StudentRefund refund : refundList) {
            itemList.add(buildTimelineItem(refund.getCreateTime(), "REFUND_APPLY", "退费申请",
                    "退费申请",
                    "申请退费 ¥" + money(refund.getRefundAmount()) + " · " + refundStatusText(refund.getStatus())
                            + (StrUtil.isBlank(refund.getApplyRemark()) ? "" : " · " + refund.getApplyRemark()),
                    refund.getRefundAmount(), null, refund.getId(), refund.getCreateUser()));
            if (refund.getApproveTime() != null) {
                itemList.add(buildTimelineItem(refund.getApproveTime(), "REFUND_APPROVE", "退费审批",
                        "退费审批",
                        StrUtil.blankToDefault(refund.getOpinion(), "无审批意见"),
                        null, null, refund.getId(), refund.getApprover()));
            }
            if (refund.getRefundTime() != null) {
                itemList.add(buildTimelineItem(refund.getRefundTime(), "REFUND", "退款完成",
                        "退款完成",
                        "退款 ¥" + money(refund.getRefundAmount()) + " · 退课时 " + plain(refund.getRefundLessons()),
                        refund.getRefundAmount(), null, refund.getId(), refund.getUpdateUser()));
            }
        }

        // 3. 消课 / 回补 / 人工调整（入账与退费销课已由订单/退费覆盖，不重复展示）
        List<StudentLessonFlow> flowList = studentLessonFlowMapper.selectList(
                new LambdaQueryWrapper<StudentLessonFlow>()
                        .eq(StudentLessonFlow::getStudentId, studentId)
                        .in(StudentLessonFlow::getFlowType, List.of(
                                LessonFlowTypeEnum.ATTEND.getValue(),
                                LessonFlowTypeEnum.RESTORE.getValue(),
                                LessonFlowTypeEnum.ADJUST.getValue()))
                        .orderByDesc(StudentLessonFlow::getCreateTime));
        Set<String> courseIdSet = new LinkedHashSet<>();
        Set<String> scheduleIdSet = new LinkedHashSet<>();
        for (StudentLessonFlow flow : flowList) {
            if (StrUtil.isNotBlank(flow.getCourseId())) {
                courseIdSet.add(flow.getCourseId());
            }
            if (StrUtil.isNotBlank(flow.getScheduleId())) {
                scheduleIdSet.add(flow.getScheduleId());
            }
        }
        Map<String, String> courseNameMap = courseIdSet.isEmpty() ? Map.of()
                : courseApi.listCourseNameMap(new ArrayList<>(courseIdSet));
        Map<String, String> classNameMap = scheduleIdSet.isEmpty() ? Map.of()
                : courseApi.listScheduleClassNameMap(new ArrayList<>(scheduleIdSet));
        for (StudentLessonFlow flow : flowList) {
            String typeName = flowTypeName(flow.getFlowType());
            String courseName = courseNameMap.getOrDefault(flow.getCourseId(), "课程");
            String className = classNameMap.get(flow.getScheduleId());
            String content = courseName
                    + (StrUtil.isBlank(className) ? "" : " · " + className)
                    + (StrUtil.isBlank(flow.getRemark()) ? "" : " · " + flow.getRemark());
            itemList.add(buildTimelineItem(flow.getCreateTime(), flow.getFlowType(), typeName,
                    typeName + "：" + courseName, content, null, flow.getLessons(),
                    flow.getScheduleId() == null ? flow.getId() : flow.getScheduleId(), flow.getCreateUser()));
        }

        // 4. 状态变更
        List<StudentStatusLog> statusLogList = studentStatusLogMapper.selectList(
                new LambdaQueryWrapper<StudentStatusLog>()
                        .eq(StudentStatusLog::getTenantId, currentTenantId())
                        .eq(StudentStatusLog::getStudentId, studentId)
                        .orderByDesc(StudentStatusLog::getCreateTime));
        for (StudentStatusLog statusLog : statusLogList) {
            itemList.add(buildTimelineItem(statusLog.getCreateTime(), "STATUS", "状态变更",
                    "状态变更：" + StudentStatusMachineEnum.messageOf(statusLog.getFromStatus())
                            + " → " + StudentStatusMachineEnum.messageOf(statusLog.getToStatus()),
                    StrUtil.blankToDefault(statusLog.getReason(), ""),
                    null, null, statusLog.getId(), statusLog.getCreateUser()));
        }

        // 5. 线索跟进（转化前历史）
        itemList.addAll(buildFollowItems(student));

        return itemList.stream()
                .sorted(Comparator.comparing((StudentTimelineItemDTO item) -> item.getTime() == null ? 0L : item.getTime().getTime()).reversed())
                .limit(MAX_ITEMS)
                .toList();
    }

    /**
     * 线索跟进：优先按来源线索id，缺失时按家长手机号兜底匹配历史线索
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private List<StudentTimelineItemDTO> buildFollowItems(Student student) {
        Set<String> leadIdSet = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(student.getSourceLeadId())) {
            leadIdSet.add(student.getSourceLeadId());
        }
        String tenantId = currentTenantId();
        if (leadIdSet.isEmpty() && StrUtil.isNotBlank(student.getParentPhone())) {
            List<Map<String, Object>> leadRows = jdbcTemplate.queryForList(
                    "SELECT `ID` FROM `CRM_LEAD` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'"
                            + " AND `PARENT_PHONE` = ? LIMIT 20", tenantId, student.getParentPhone());
            for (Map<String, Object> leadRow : leadRows) {
                String leadId = strValue(leadRow.get("ID"));
                if (StrUtil.isNotBlank(leadId)) {
                    leadIdSet.add(leadId);
                }
            }
        }
        List<StudentTimelineItemDTO> itemList = new ArrayList<>();
        for (String leadId : leadIdSet) {
            List<Map<String, Object>> followRows = jdbcTemplate.queryForList(
                    "SELECT `ID`, `FOLLOW_TYPE`, `CONTENT`, `FOLLOW_USER_ID`, `CREATE_TIME` FROM `CRM_LEAD_FOLLOW`"
                            + " WHERE `TENANT_ID` = ? AND `LEAD_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'"
                            + " ORDER BY `CREATE_TIME` DESC LIMIT 100", tenantId, leadId);
            for (Map<String, Object> row : followRows) {
                String content = StrUtil.blankToDefault(strValue(row.get("CONTENT")), "");
                String followType = strValue(row.get("FOLLOW_TYPE"));
                itemList.add(buildTimelineItem(dateValue(row.get("CREATE_TIME")), "FOLLOW", "线索跟进",
                        "线索跟进" + (StrUtil.isBlank(followType) ? "" : "（" + followType + "）"),
                        content, null, null, "lead:" + leadId, strValue(row.get("FOLLOW_USER_ID"))));
            }
        }
        return itemList;
    }

    private StudentTimelineItemDTO buildTimelineItem(Date time, String type, String typeName, String title,
                                                     String content, BigDecimal amount, BigDecimal lessons,
                                                     String refId, String operator) {
        StudentTimelineItemDTO item = new StudentTimelineItemDTO();
        item.setTime(time);
        item.setType(type);
        item.setTypeName(typeName);
        item.setTitle(title);
        item.setContent(content);
        item.setAmount(amount);
        item.setLessons(lessons);
        item.setRefId(refId);
        item.setOperator(operator);
        return item;
    }

    private String flowTypeName(String flowType) {
        if (LessonFlowTypeEnum.ATTEND.getValue().equals(flowType)) {
            return "消课";
        }
        if (LessonFlowTypeEnum.RESTORE.getValue().equals(flowType)) {
            return "回补";
        }
        if (LessonFlowTypeEnum.ADJUST.getValue().equals(flowType)) {
            return "人工调整";
        }
        return flowType;
    }

    private String orderStatusText(String status) {
        for (StudentOrderStatusEnum item : StudentOrderStatusEnum.values()) {
            if (item.getValue().equals(status)) {
                return item.getMessage();
            }
        }
        return status;
    }

    private String refundStatusText(String status) {
        for (StudentRefundStatusEnum item : StudentRefundStatusEnum.values()) {
            if (item.getValue().equals(status)) {
                return item.getMessage();
            }
        }
        return status;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String strValue(Object value) {
        return ObjectUtil.isEmpty(value) ? null : String.valueOf(value);
    }

    private Date dateValue(Object value) {
        if (value instanceof Date date) {
            return date;
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        return null;
    }

    private String currentTenantId() {
        return StrUtil.blankToDefault(TenantContext.getTenantId(), "0");
    }
}
