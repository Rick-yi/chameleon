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
package vip.xiaonuo.student.modular.studentorder.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
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
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderAddParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderIdParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPageParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPayCallbackParam;
import vip.xiaonuo.student.modular.studentorder.service.StudentOrderService;

import java.util.List;

/**
 * 报名订单Service接口实现类
 * <p>
 * 报名下单：课包价格快照 + 防重复下单；
 * 支付回调：条件更新幂等（重复回调直接返回）+ 流水号唯一 + 入账联动。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentOrderServiceImpl extends ServiceImpl<StudentOrderMapper, StudentOrder> implements StudentOrderService {

    @Resource
    private StudentService studentService;

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    @Resource
    private CourseApi courseApi;

    @Override
    public Page<StudentOrder> page(StudentOrderPageParam studentOrderPageParam) {
        QueryWrapper<StudentOrder> queryWrapper = new QueryWrapper<StudentOrder>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentOrderPageParam.getStudentId()), StudentOrder::getStudentId, studentOrderPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentOrderPageParam.getStatus()), StudentOrder::getStatus, studentOrderPageParam.getStatus());
        if (ObjectUtil.isAllNotEmpty(studentOrderPageParam.getSortField(), studentOrderPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(studentOrderPageParam.getSortOrder());
            queryWrapper.orderBy(true, studentOrderPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    cn.hutool.core.util.StrUtil.toUnderlineCase(studentOrderPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(StudentOrder::getCreateTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(StudentOrderAddParam studentOrderAddParam) {
        Student student = studentService.queryEntity(studentOrderAddParam.getStudentId());
        JSONObject packageInfo = courseApi.getCoursePackageInfo(studentOrderAddParam.getCoursePackageId());
        if (ObjectUtil.isEmpty(packageInfo)) {
            throw new CommonException("课包不存在，id值为：{}", studentOrderAddParam.getCoursePackageId());
        }
        // 防重复下单：同学员同课包存在待支付订单则拒绝
        boolean hasUnpaid = this.count(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getStudentId, studentOrderAddParam.getStudentId())
                .eq(StudentOrder::getCoursePackageId, studentOrderAddParam.getCoursePackageId())
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.UNPAID.getValue())) > 0;
        if (hasUnpaid) {
            throw new CommonException("学员【{}】已有该课包的待支付订单", student.getName());
        }
        StudentOrder order = new StudentOrder();
        order.setStudentId(studentOrderAddParam.getStudentId());
        order.setCoursePackageId(studentOrderAddParam.getCoursePackageId());
        order.setCourseId(packageInfo.getStr("courseId"));
        // 课包快照（改价不影响已购）
        order.setPackageName(packageInfo.getStr("name"));
        order.setPackagePrice(packageInfo.getBigDecimal("price"));
        order.setPackageLessons(packageInfo.getBigDecimal("lessons"));
        order.setPackageGiftLessons(packageInfo.getBigDecimal("giftLessons"));
        order.setPackageValidityDays(packageInfo.getInt("validityDays"));
        order.setAmount(packageInfo.getBigDecimal("price"));
        order.setPayChannel(ObjectUtil.isNotEmpty(studentOrderAddParam.getPayChannel())
                ? studentOrderAddParam.getPayChannel() : "WECHAT");
        order.setStatus(StudentOrderStatusEnum.UNPAID.getValue());
        this.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void payCallback(StudentOrderPayCallbackParam studentOrderPayCallbackParam) {
        StudentOrder order = this.queryEntity(studentOrderPayCallbackParam.getOrderId());
        // 幂等：已支付直接返回（防重复回调）
        if (StudentOrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
            return;
        }
        if (!StudentOrderStatusEnum.UNPAID.getValue().equals(order.getStatus())) {
            throw new CommonException("订单状态异常，无法支付");
        }
        // 同一支付流水号不能用于其他订单
        boolean tradeNoUsed = this.count(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getTradeNo, studentOrderPayCallbackParam.getTradeNo())
                .ne(StudentOrder::getId, order.getId())) > 0;
        if (tradeNoUsed) {
            throw new CommonException("支付流水号已被其他订单使用");
        }
        // 条件更新防并发重复入账
        boolean paid = this.update(new LambdaUpdateWrapper<StudentOrder>()
                .eq(StudentOrder::getId, order.getId())
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.UNPAID.getValue())
                .set(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                .set(StudentOrder::getTradeNo, studentOrderPayCallbackParam.getTradeNo())
                .set(StudentOrder::getPayTime, new java.util.Date())
                .set(StudentOrder::getPaidAmount, ObjectUtil.isNotEmpty(studentOrderPayCallbackParam.getPaidAmount())
                        ? studentOrderPayCallbackParam.getPaidAmount() : order.getAmount())
                .set(StudentOrder::getPayChannel, ObjectUtil.isNotEmpty(studentOrderPayCallbackParam.getPayChannel())
                        ? studentOrderPayCallbackParam.getPayChannel() : order.getPayChannel()));
        if (!paid) {
            // 并发下已被其他回调处理，直接返回
            return;
        }
        // 入账：创建/追加课时账户 + 入账流水 + 续费预警置已续费
        studentLessonAccountService.recharge(order);
        // TODO(M6): 对接微信支付真实回调与退款
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(StudentOrderIdParam studentOrderIdParam) {
        StudentOrder order = this.queryEntity(studentOrderIdParam.getId());
        if (!StudentOrderStatusEnum.UNPAID.getValue().equals(order.getStatus())) {
            throw new CommonException("仅待支付订单可取消");
        }
        order.setStatus(StudentOrderStatusEnum.CANCELED.getValue());
        this.updateById(order);
    }

    @Override
    public StudentOrder detail(StudentOrderIdParam studentOrderIdParam) {
        return this.queryEntity(studentOrderIdParam.getId());
    }

    @Override
    public StudentOrder queryEntity(String id) {
        StudentOrder order = this.getById(id);
        if (ObjectUtil.isEmpty(order)) {
            throw new CommonException("订单不存在，id值为：{}", id);
        }
        return order;
    }

    @Override
    public void updateOrder(StudentOrder studentOrder) {
        this.updateById(studentOrder);
    }
}
