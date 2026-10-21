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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.enums.LessonAccountStatusEnum;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.service.StudentLessonFlowService;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentorder.service.StudentLessonAllocationService;
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
import vip.xiaonuo.student.core.service.StudentDataScopeService;

/**
 * 退费单Service接口实现类
 * <p>
 * 退费按订单核算：可退课时 = 订单购买课时 - 该订单已消耗课时（赠课不可退，随订单作废）。
 * 同一学员同一课程有多个课包时，互不影响；账户冻结/解冻/清零按订单退费结果维护。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentRefundServiceImpl extends ServiceImpl<StudentRefundMapper, StudentRefund> implements StudentRefundService {

    @Resource
    private StudentDataScopeService studentDataScopeService;

    @Resource
    private StudentOrderService studentOrderService;

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    @Resource
    private StudentLessonAccountMapper studentLessonAccountMapper;

    @Resource
    private StudentLessonAllocationService studentLessonAllocationService;

    @Resource
    private StudentLessonFlowService studentLessonFlowService;

    @Override
    public Page<StudentRefund> page(StudentRefundPageParam studentRefundPageParam) {
        QueryWrapper<StudentRefund> queryWrapper = new QueryWrapper<StudentRefund>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentRefundPageParam.getStudentId()), StudentRefund::getStudentId, studentRefundPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentRefundPageParam.getStatus()), StudentRefund::getStatus, studentRefundPageParam.getStatus());
        // 数据范围：仅可见校区的退费单
        List<String> visibleOrderIds = studentDataScopeService.visibleOrderIdList();
        if (visibleOrderIds != null) {
            if (visibleOrderIds.isEmpty()) {
                queryWrapper.lambda().eq(StudentRefund::getId, StudentDataScopeService.NO_DATA_ID);
            } else {
                queryWrapper.lambda().in(StudentRefund::getOrderId, visibleOrderIds);
            }
        }
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
        StudentLessonAccount account = studentLessonAccountService.getAccount(order.getStudentId(), order.getCourseId());
        if (!LessonAccountStatusEnum.NORMAL.getValue().equals(account.getStatus())) {
            throw new CommonException("课时账户状态异常（{}），无法申请退费", account.getStatus());
        }
        StudentLessonAllocationService.RefundCalc calc = studentLessonAllocationService.calcRefund(order);
        if (calc.refundLessons().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("该订单课时已消耗完毕，无可退课时");
        }
        if (ObjectUtil.isEmpty(order.getPackageLessons())
                || order.getPackageLessons().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("订单课时数异常，无法计算退费金额");
        }
        // 折算单价 = 实付 / 课包购买课时；退费金额 = 可退购买课时 × 单价
        BigDecimal unitPrice = nvl(order.getPaidAmount()).divide(order.getPackageLessons(), 4, RoundingMode.HALF_UP);
        StudentRefund refund = new StudentRefund();
        refund.setOrderId(order.getId());
        refund.setStudentId(order.getStudentId());
        refund.setCourseId(order.getCourseId());
        refund.setRefundLessons(calc.refundLessons());
        refund.setGiftLessons(calc.giftLessons());
        refund.setRefundAmount(calc.refundLessons().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
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
            StudentOrder order = studentOrderService.queryEntity(refund.getOrderId());
            if (!StudentOrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
                throw new CommonException("订单状态已变化，无法审批退费");
            }
            StudentLessonAccount account = studentLessonAccountService.getAccount(order.getStudentId(), order.getCourseId());
            if (!LessonAccountStatusEnum.NORMAL.getValue().equals(account.getStatus())) {
                throw new CommonException("课时账户状态异常（{}），无法审批退费", account.getStatus());
            }
            // 审批时按最新消耗重算，避免申请后继续上课导致金额失真
            StudentLessonAllocationService.RefundCalc calc = studentLessonAllocationService.calcRefund(order);
            if (calc.refundLessons().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CommonException("该订单课时已消耗完毕，无可退课时");
            }
            BigDecimal unitPrice = nvl(order.getPaidAmount()).divide(order.getPackageLessons(), 4, RoundingMode.HALF_UP);
            refund.setRefundLessons(calc.refundLessons());
            refund.setGiftLessons(calc.giftLessons());
            refund.setRefundAmount(calc.refundLessons().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            refund.setStatus(StudentRefundStatusEnum.TO_REFUND.getValue());
            // 冻结账户，防止退款完成前继续点名扣课
            int frozen = studentLessonAccountMapper.update(null, new LambdaUpdateWrapper<StudentLessonAccount>()
                    .eq(StudentLessonAccount::getId, account.getId())
                    .eq(StudentLessonAccount::getStatus, LessonAccountStatusEnum.NORMAL.getValue())
                    .set(StudentLessonAccount::getStatus, LessonAccountStatusEnum.FROZEN.getValue()));
            if (frozen == 0) {
                throw new CommonException("课时账户状态已变化，请刷新后重试");
            }
        } else {
            refund.setStatus(StudentRefundStatusEnum.REJECTED.getValue());
        }
        boolean updated = this.update(new LambdaUpdateWrapper<StudentRefund>()
                .eq(StudentRefund::getId, refund.getId())
                .eq(StudentRefund::getStatus, StudentRefundStatusEnum.PENDING.getValue())
                .set(StudentRefund::getStatus, refund.getStatus())
                .set(StudentRefund::getApprover, refund.getApprover())
                .set(StudentRefund::getOpinion, refund.getOpinion())
                .set(StudentRefund::getApproveTime, refund.getApproveTime())
                .set(StudentRefund::getRefundLessons, refund.getRefundLessons())
                .set(StudentRefund::getGiftLessons, refund.getGiftLessons())
                .set(StudentRefund::getRefundAmount, refund.getRefundAmount()));
        if (!updated) {
            throw new CommonException("退费单状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirm(StudentRefundIdParam studentRefundIdParam) {
        StudentRefund refund = this.queryEntity(studentRefundIdParam.getId());
        if (!StudentRefundStatusEnum.TO_REFUND.getValue().equals(refund.getStatus())) {
            throw new CommonException("退费单当前状态不允许确认退款");
        }
        StudentOrder order = studentOrderService.queryEntity(refund.getOrderId());
        if (!StudentOrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
            throw new CommonException("订单状态已变化，无法确认退款");
        }
        StudentLessonAccount account = studentLessonAccountService.getAccount(order.getStudentId(), order.getCourseId());
        if (!LessonAccountStatusEnum.FROZEN.getValue().equals(account.getStatus())) {
            throw new CommonException("课时账户未冻结，无法确认退款");
        }
        // 冻结后账户不应再变化，再校验一次防止数据异常
        BigDecimal refundGiftLessons = nvl(refund.getGiftLessons());
        StudentLessonAllocationService.RefundCalc calc = studentLessonAllocationService.calcRefund(order);
        if (calc.refundLessons().compareTo(refund.getRefundLessons()) != 0
                || calc.giftLessons().compareTo(refundGiftLessons) != 0) {
            throw new CommonException("退费单课时与订单最新数据不一致，请重新审批");
        }
        BigDecimal totalRemove = refund.getRefundLessons().add(refundGiftLessons);
        if (totalRemove.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("退费课时异常");
        }
        // 扣减账户总课时/剩余课时，并作废该订单赠课
        int accountUpdated = studentLessonAccountMapper.update(null, new LambdaUpdateWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getId, account.getId())
                .eq(StudentLessonAccount::getStatus, LessonAccountStatusEnum.FROZEN.getValue())
                .ge(StudentLessonAccount::getRemainLessons, totalRemove)
                .ge(StudentLessonAccount::getGiftLessons, refundGiftLessons)
                .setSql("TOTAL_LESSONS = TOTAL_LESSONS - " + totalRemove.toPlainString())
                .setSql("REMAIN_LESSONS = REMAIN_LESSONS - " + totalRemove.toPlainString())
                .setSql("GIFT_LESSONS = GIFT_LESSONS - " + refundGiftLessons.toPlainString()));
        if (accountUpdated == 0) {
            throw new CommonException("课时账户数据异常，退费失败");
        }
        StudentLessonAccount updatedAccount = studentLessonAccountService.getAccount(order.getStudentId(), order.getCourseId());
        String accountStatus = updatedAccount.getRemainLessons().compareTo(BigDecimal.ZERO) > 0
                ? LessonAccountStatusEnum.NORMAL.getValue() : LessonAccountStatusEnum.CLEARED.getValue();
        studentLessonAccountMapper.update(null, new LambdaUpdateWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getId, account.getId())
                .eq(StudentLessonAccount::getStatus, LessonAccountStatusEnum.FROZEN.getValue())
                .set(StudentLessonAccount::getStatus, accountStatus));
        // 退费销课流水（只记购买课时部分，赠课作废不产生收入冲回，备注中说明）
        StudentLessonFlow flow = new StudentLessonFlow();
        flow.setStudentId(refund.getStudentId());
        flow.setCourseId(refund.getCourseId());
        flow.setFlowType(LessonFlowTypeEnum.REFUND.getValue());
        flow.setLessons(refund.getRefundLessons().negate());
        flow.setRefOrderId(refund.getOrderId());
        flow.setRemark("退费销课（退款" + refund.getRefundLessons().stripTrailingZeros().toPlainString()
                + "课时，赠课作废" + refundGiftLessons.stripTrailingZeros().toPlainString() + "课时）");
        studentLessonFlowService.add(flow);
        // 订单置已退费并记录已退课时
        int orderUpdated = studentOrderMapper.update(null, new LambdaUpdateWrapper<StudentOrder>()
                .eq(StudentOrder::getId, order.getId())
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                .set(StudentOrder::getStatus, StudentOrderStatusEnum.REFUNDED.getValue())
                .setSql("REFUNDED_LESSONS = COALESCE(REFUNDED_LESSONS, 0) + " + refund.getRefundLessons().toPlainString()));
        if (orderUpdated == 0) {
            throw new CommonException("订单状态已变化，退费失败");
        }
        boolean refundUpdated = this.update(new LambdaUpdateWrapper<StudentRefund>()
                .eq(StudentRefund::getId, refund.getId())
                .eq(StudentRefund::getStatus, StudentRefundStatusEnum.TO_REFUND.getValue())
                .set(StudentRefund::getStatus, StudentRefundStatusEnum.REFUNDED.getValue())
                .set(StudentRefund::getRefundTime, new Date()));
        if (!refundUpdated) {
            throw new CommonException("退费单状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(StudentRefundIdParam studentRefundIdParam) {
        StudentRefund refund = this.queryEntity(studentRefundIdParam.getId());
        if (!StudentRefundStatusEnum.PENDING.getValue().equals(refund.getStatus())
                && !StudentRefundStatusEnum.TO_REFUND.getValue().equals(refund.getStatus())) {
            throw new CommonException("退费单当前状态不允许撤销");
        }
        if (StudentRefundStatusEnum.TO_REFUND.getValue().equals(refund.getStatus())) {
            StudentOrder order = studentOrderService.queryEntity(refund.getOrderId());
            StudentLessonAccount account = studentLessonAccountService.getAccount(order.getStudentId(), order.getCourseId());
            String accountStatus = account.getRemainLessons().compareTo(BigDecimal.ZERO) > 0
                    ? LessonAccountStatusEnum.NORMAL.getValue() : LessonAccountStatusEnum.CLEARED.getValue();
            studentLessonAccountMapper.update(null, new LambdaUpdateWrapper<StudentLessonAccount>()
                    .eq(StudentLessonAccount::getId, account.getId())
                    .eq(StudentLessonAccount::getStatus, LessonAccountStatusEnum.FROZEN.getValue())
                    .set(StudentLessonAccount::getStatus, accountStatus));
        }
        boolean updated = this.update(new LambdaUpdateWrapper<StudentRefund>()
                .eq(StudentRefund::getId, refund.getId())
                .in(StudentRefund::getStatus, StudentRefundStatusEnum.PENDING.getValue(),
                        StudentRefundStatusEnum.TO_REFUND.getValue())
                .set(StudentRefund::getStatus, StudentRefundStatusEnum.CANCELLED.getValue())
                .set(StudentRefund::getOpinion, "撤销退费")
                .set(StudentRefund::getApproveTime, new Date()));
        if (!updated) {
            throw new CommonException("退费单状态已变化，请刷新后重试");
        }
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

    /**
     * 空值转 0
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}