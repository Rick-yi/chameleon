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
package vip.xiaonuo.student.modular.studentorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单课时分摊服务
 * <p>
 * 课时账户按“学员 × 课程”聚合，退费却按订单处理，因此需要把每次消课分摊到具体订单，
 * 才能准确计算单个订单的剩余课时。分摊规则：按订单创建时间先进先出（含赠课）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class StudentLessonAllocationService {

    /** 最大重试轮次，防止并发下无限循环 */
    private static final int MAX_ROUNDS = 8;

    @Resource
    private StudentOrderMapper studentOrderMapper;

    /**
     * 将消课课时分摊到订单（先进先出）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public List<OrderAllocation> allocate(String studentId, String courseId, BigDecimal lessons) {
        BigDecimal remaining = nvl(lessons);
        List<OrderAllocation> allocationList = new java.util.ArrayList<>();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return allocationList;
        }
        for (int round = 0; round < MAX_ROUNDS && remaining.compareTo(BigDecimal.ZERO) > 0; round++) {
            List<StudentOrder> orderList = listAllocatableOrders(studentId, courseId);
            if (orderList.isEmpty()) {
                throw new CommonException("订单课时分摊失败：未找到可分摊的已支付订单");
            }
            boolean progress = false;
            for (StudentOrder order : orderList) {
                BigDecimal total = nvl(order.getPackageLessons()).add(nvl(order.getPackageGiftLessons()));
                BigDecimal consumed = nvl(order.getConsumedLessons());
                BigDecimal available = total.subtract(consumed);
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal take = remaining.min(available);
                int updated = studentOrderMapper.update(null, new LambdaUpdateWrapper<StudentOrder>()
                        .eq(StudentOrder::getId, order.getId())
                        .eq(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                        .apply("COALESCE(CONSUMED_LESSONS, 0) = {0}", consumed)
                        .setSql("CONSUMED_LESSONS = COALESCE(CONSUMED_LESSONS, 0) + " + take.toPlainString()));
                if (updated > 0) {
                    remaining = remaining.subtract(take);
                    progress = true;
                    allocationList.add(new OrderAllocation(order.getId(), take));
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                }
            }
            if (!progress) {
                throw new CommonException("订单课时分摊失败：并发冲突，请重试");
            }
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new CommonException("订单课时分摊失败：订单剩余课时不足");
        }
        return allocationList;
    }

    /**
     * 回补课时到订单（按最近订单倒序回退）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public void restore(String studentId, String courseId, BigDecimal lessons) {
        BigDecimal remaining = nvl(lessons);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        for (int round = 0; round < MAX_ROUNDS && remaining.compareTo(BigDecimal.ZERO) > 0; round++) {
            List<StudentOrder> orderList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                    .eq(StudentOrder::getStudentId, studentId)
                    .eq(StudentOrder::getCourseId, courseId)
                    .eq(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                    .apply("COALESCE(CONSUMED_LESSONS, 0) > 0")
                    .orderByDesc(StudentOrder::getCreateTime)
                    .orderByDesc(StudentOrder::getId));
            if (orderList.isEmpty()) {
                throw new CommonException("订单课时回补失败：未找到已消耗的订单");
            }
            boolean progress = false;
            for (StudentOrder order : orderList) {
                BigDecimal consumed = nvl(order.getConsumedLessons());
                BigDecimal take = remaining.min(consumed);
                if (take.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                int updated = studentOrderMapper.update(null, new LambdaUpdateWrapper<StudentOrder>()
                        .eq(StudentOrder::getId, order.getId())
                        .apply("COALESCE(CONSUMED_LESSONS, 0) >= {0}", take)
                        .setSql("CONSUMED_LESSONS = COALESCE(CONSUMED_LESSONS, 0) - " + take.toPlainString()));
                if (updated > 0) {
                    remaining = remaining.subtract(take);
                    progress = true;
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                }
            }
            if (!progress) {
                throw new CommonException("订单课时回补失败：并发冲突，请重试");
            }
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new CommonException("订单课时回补失败：已消耗课时不足");
        }
    }

    /**
     * 计算订单当前可退课时与应作废赠课
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public RefundCalc calcRefund(StudentOrder order) {
        BigDecimal packageLessons = nvl(order.getPackageLessons());
        BigDecimal giftLessons = nvl(order.getPackageGiftLessons());
        BigDecimal consumed = nvl(order.getConsumedLessons());
        // 先进先出：先消耗购买课时，再消耗赠课
        BigDecimal purchasedUsed = consumed.min(packageLessons);
        BigDecimal refundLessons = packageLessons.subtract(purchasedUsed);
        BigDecimal giftUsed = consumed.subtract(packageLessons).max(BigDecimal.ZERO);
        BigDecimal giftRemaining = giftLessons.subtract(giftUsed).max(BigDecimal.ZERO);
        return new RefundCalc(refundLessons, giftRemaining);
    }

    /**
     * 查询仍可分摊课时的订单（先进先出）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private List<StudentOrder> listAllocatableOrders(String studentId, String courseId) {
        return studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getStudentId, studentId)
                .eq(StudentOrder::getCourseId, courseId)
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                .apply("COALESCE(CONSUMED_LESSONS, 0) < COALESCE(PACKAGE_LESSONS, 0) + COALESCE(PACKAGE_GIFT_LESSONS, 0)")
                .orderByAsc(StudentOrder::getCreateTime)
                .orderByAsc(StudentOrder::getId));
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

    /**
     * 订单退费计算结果
     *
     * @param refundLessons 可退购买课时
     * @param giftLessons   随订单作废的赠课
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    public record RefundCalc(BigDecimal refundLessons, BigDecimal giftLessons) {
    }

    /**
     * 课时分摊明细
     *
     * @param orderId 分摊到的订单
     * @param lessons 分摊课时
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public record OrderAllocation(String orderId, BigDecimal lessons) {
    }
}