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
package vip.xiaonuo.student.modular.studentrefund.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.enums.LessonAccountStatusEnum;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.service.StudentLessonFlowService;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.service.StudentOrderService;
import vip.xiaonuo.student.modular.studentrefund.entity.StudentRefund;
import vip.xiaonuo.student.modular.studentrefund.enums.StudentRefundStatusEnum;
import vip.xiaonuo.student.modular.studentrefund.mapper.StudentRefundMapper;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundAddParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundApproveParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundIdParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundPageParam;
import vip.xiaonuo.student.modular.studentrefund.service.StudentRefundService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 退费单Service接口实现类
 * <p>
 * 退费规则：可退课时 = 剩余 - 赠课（赠课不可退）；
 * 折算单价 = 实付金额 / 课包课时；金额保留 2 位小数。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentRefundServiceImpl extends ServiceImpl<StudentRefundMapper, StudentRefund> implements StudentRefundService {

    @Resource
    private StudentOrderService studentOrderService;

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    @Resource
    private StudentLessonFlowService studentLessonFlowService;

    @Resource
    private CourseApi courseApi;

    @Override
    public Page<StudentRefund> page(StudentRefundPageParam studentRefundPageParam) {
        QueryWrapper<StudentRefund> queryWrapper = new QueryWrapper<StudentRefund>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentRefundPageParam.getStudentId()), StudentRefund::getStudentId, studentRefundPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentRefundPageParam.getStatus()), StudentRefund::getStatus, studentRefundPageParam.getStatus());
        if (ObjectUtil.isAllNotEmpty(studentRefundPageParam.getSortField(), studentRefundPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(studentRefundPageParam.getSortOrder());
            queryWrapper.orderBy(true, studentRefundPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(studentRefundPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(StudentRefund::getCreateTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void apply(StudentRefundAddParam studentRefundAddParam) {
        StudentOrder order = studentOrderService.queryEntity(studentRefundAddParam.getOrderId());
        if (!StudentOrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
            throw new CommonException("仅已支付订单可申请退费");
        }
        // 同一订单只允许一张在途退费单
        boolean inflight = this.count(new LambdaQueryWrapper<StudentRefund>()
                .eq(StudentRefund::getOrderId, order.getId())
                .in(StudentRefund::getStatus, StudentRefundStatusEnum.PENDING.getValue(),
                        StudentRefundStatusEnum.TO_REFUND.getValue())) > 0;
        if (inflight) {
            throw new CommonException("该订单已有在途退费单");
        }
        JSONObject packageInfo = courseApi.getCoursePackageInfo(order.getCoursePackageId());
        if (ObjectUtil.isEmpty(packageInfo)) {
            throw new CommonException("课包不存在，id值为：{}", order.getCoursePackageId());
        }
        String courseId = packageInfo.getStr("courseId");
        StudentLessonAccount account = studentLessonAccountService.getAccount(order.getStudentId(), courseId);
        // 可退课时 = 剩余 - 赠课
        BigDecimal refundLessons = account.getRemainLessons().subtract(account.getGiftLessons());
        if (refundLessons.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("无可退课时（赠课不可退）");
        }
        // 折算单价 = 实付 / 课包课时，退费金额 = 可退课时 × 单价
        BigDecimal unitPrice = order.getPaidAmount().divide(order.getPackageLessons(), 4, RoundingMode.HALF_UP);
        BigDecimal refundAmount = refundLessons.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        StudentRefund refund = new StudentRefund();
        refund.setOrderId(order.getId());
        refund.setStudentId(order.getStudentId());
        refund.setCourseId(courseId);
        refund.setRefundAmount(refundAmount);
        refund.setRefundLessons(refundLessons);
        refund.setStatus(StudentRefundStatusEnum.PENDING.getValue());
        refund.setOpinion(studentRefundAddParam.getRemark());
        this.save(refund);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approve(StudentRefundApproveParam studentRefundApproveParam) {
        StudentRefund refund = this.queryEntity(studentRefundApproveParam.getRefundId());
        if (!StudentRefundStatusEnum.PENDING.getValue().equals(refund.getStatus())) {
            throw new CommonException("退费单当前状态不允许审批");
        }
        refund.setApprover(StpUtil.getLoginIdAsString());
        refund.setOpinion(studentRefundApproveParam.getOpinion());
        refund.setApproveTime(new Date());
        if (Boolean.TRUE.equals(studentRefundApproveParam.getPass())) {
            refund.setStatus(StudentRefundStatusEnum.TO_REFUND.getValue());
            // 冻结账户（退费期间不允许扣课）
            StudentLessonAccount account = studentLessonAccountService.getAccount(refund.getStudentId(), refund.getCourseId());
            account.setStatus(LessonAccountStatusEnum.FROZEN.getValue());
            studentLessonAccountService.updateAccount(account);
        } else {
            refund.setStatus(StudentRefundStatusEnum.REJECTED.getValue());
        }
        this.updateById(refund);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirm(StudentRefundIdParam studentRefundIdParam) {
        StudentRefund refund = this.queryEntity(studentRefundIdParam.getId());
        if (!StudentRefundStatusEnum.TO_REFUND.getValue().equals(refund.getStatus())) {
            throw new CommonException("退费单当前状态不允许确认退款");
        }
        // 账户清零
        StudentLessonAccount account = studentLessonAccountService.getAccount(refund.getStudentId(), refund.getCourseId());
        account.setUsedLessons(account.getTotalLessons());
        account.setRemainLessons(BigDecimal.ZERO);
        account.setStatus(LessonAccountStatusEnum.CLEARED.getValue());
        studentLessonAccountService.updateAccount(account);
        // 退费销课流水
        StudentLessonFlow flow = new StudentLessonFlow();
        flow.setStudentId(refund.getStudentId());
        flow.setCourseId(refund.getCourseId());
        flow.setFlowType(LessonFlowTypeEnum.REFUND.getValue());
        flow.setLessons(refund.getRefundLessons().negate());
        flow.setRefOrderId(refund.getOrderId());
        flow.setRemark("退费销课");
        studentLessonFlowService.add(flow);
        // 订单置已退费
        StudentOrder order = studentOrderService.queryEntity(refund.getOrderId());
        order.setStatus(StudentOrderStatusEnum.REFUNDED.getValue());
        studentOrderService.updateOrder(order);
        // 退费单置已退款
        refund.setStatus(StudentRefundStatusEnum.REFUNDED.getValue());
        this.updateById(refund);
    }

    @Override
    public StudentRefund detail(StudentRefundIdParam studentRefundIdParam) {
        return this.queryEntity(studentRefundIdParam.getId());
    }

    @Override
    public StudentRefund queryEntity(String id) {
        StudentRefund refund = this.getById(id);
        if (ObjectUtil.isEmpty(refund)) {
            throw new CommonException("退费单不存在，id值为：{}", id);
        }
        return refund;
    }
}
