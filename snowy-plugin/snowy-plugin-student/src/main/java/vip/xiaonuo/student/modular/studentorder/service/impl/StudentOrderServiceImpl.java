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
import cn.hutool.core.util.IdUtil;
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
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderAddParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderIdParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderOfflinePayParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPageParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPayCallbackParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderRenewParam;
import vip.xiaonuo.student.modular.studentorder.service.StudentOrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vip.xiaonuo.student.core.service.StudentDataScopeService;

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
    private StudentDataScopeService studentDataScopeService;

    @Resource
    private StudentService studentService;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    @Resource
    private CourseApi courseApi;

    @Resource
    private BizEventApi bizEventApi;

    @Override
    public Page<StudentOrder> page(StudentOrderPageParam studentOrderPageParam) {
        QueryWrapper<StudentOrder> queryWrapper = new QueryWrapper<StudentOrder>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentOrderPageParam.getStudentId()), StudentOrder::getStudentId, studentOrderPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentOrderPageParam.getStatus()), StudentOrder::getStatus, studentOrderPageParam.getStatus())
                .eq(ObjectUtil.isNotEmpty(studentOrderPageParam.getCampusId()), StudentOrder::getCampusId, studentOrderPageParam.getCampusId());
        // 下单时间区间
        if (ObjectUtil.isNotEmpty(studentOrderPageParam.getStartTime())) {
            queryWrapper.lambda().ge(StudentOrder::getCreateTime,
                    cn.hutool.core.date.DateUtil.parse(studentOrderPageParam.getStartTime()));
        }
        if (ObjectUtil.isNotEmpty(studentOrderPageParam.getEndTime())) {
            queryWrapper.lambda().le(StudentOrder::getCreateTime,
                    cn.hutool.core.date.DateUtil.parse(studentOrderPageParam.getEndTime()));
        }
        // 关键词：按学员姓名/家长手机过滤（子查询，参数化绑定防注入）
        if (ObjectUtil.isNotEmpty(studentOrderPageParam.getSearchKey())) {
            queryWrapper.apply("STUDENT_ID IN (SELECT ID FROM STUDENT WHERE NAME LIKE CONCAT('%', {0}, '%') "
                    + "OR PARENT_PHONE LIKE CONCAT('%', {0}, '%'))", studentOrderPageParam.getSearchKey());
        }
        // 数据范围：仅可见校区的订单
        List<String> visibleCampusIds = studentDataScopeService.visibleCampusIds();
        if (visibleCampusIds != null) {
            if (visibleCampusIds.isEmpty()) {
                queryWrapper.lambda().eq(StudentOrder::getId, StudentDataScopeService.NO_DATA_ID);
            } else {
                queryWrapper.lambda().in(StudentOrder::getCampusId, visibleCampusIds);
            }
        }
        if (ObjectUtil.isAllNotEmpty(studentOrderPageParam.getSortField(), studentOrderPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(studentOrderPageParam.getSortOrder());
            queryWrapper.orderBy(true, studentOrderPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    cn.hutool.core.util.StrUtil.toUnderlineCase(studentOrderPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(StudentOrder::getCreateTime);
        }
        Page<StudentOrder> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillStudentInfo(page.getRecords());
        return page;
    }

    /**
     * 批量回填学员姓名与家长手机（列表展示用）
     */
    private void fillStudentInfo(List<StudentOrder> orderList) {
        if (ObjectUtil.isEmpty(orderList)) {
            return;
        }
        List<String> studentIdList = orderList.stream().map(StudentOrder::getStudentId)
                .filter(cn.hutool.core.util.StrUtil::isNotBlank).distinct().collect(java.util.stream.Collectors.toList());
        if (ObjectUtil.isEmpty(studentIdList)) {
            return;
        }
        Map<String, Student> studentMap = new HashMap<>();
        studentMapper.selectBatchIds(studentIdList).forEach(student -> studentMap.put(student.getId(), student));
        for (StudentOrder order : orderList) {
            Student student = studentMap.get(order.getStudentId());
            if (ObjectUtil.isNotEmpty(student)) {
                order.setStudentName(student.getName());
                order.setParentPhone(student.getParentPhone());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String add(StudentOrderAddParam studentOrderAddParam) {
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
        order.setCampusId(student.getCampusId());
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
        try {
            this.save(order);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 数据库唯一约束兜底：同一学员同课包只允许一张待支付订单
            throw new CommonException("学员【{}】已有该课包的待支付订单", student.getName());
        }
        bizEventApi.log(BizEventCodeEnum.ORDER_CREATE.getValue(), "order", order.getId(), student.getName(),
                Map.of("课包", order.getPackageName(), "金额", String.valueOf(order.getAmount())));
        return order.getId();
    }

    /**
     * 续费：下单 + 收款在同一事务内完成
     * <p>
     * 避免「已下单未收款」留下半截订单；课时入账后学员的续费预警自动置为已续费。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String renew(StudentOrderRenewParam studentOrderRenewParam) {
        StudentOrderAddParam addParam = new StudentOrderAddParam();
        addParam.setStudentId(studentOrderRenewParam.getStudentId());
        addParam.setCoursePackageId(studentOrderRenewParam.getCoursePackageId());
        addParam.setPayChannel(studentOrderRenewParam.getPayChannel());
        String orderId = this.add(addParam);
        // 线下流水号：续费单号 + 时间戳，保证同一订单只入账一次
        String tradeNo = "RENEW_" + orderId + "_" + System.currentTimeMillis();
        this.markPaid(orderId, tradeNo, studentOrderRenewParam.getPaidAmount(), studentOrderRenewParam.getPayChannel());
        bizEventApi.log(BizEventCodeEnum.RENEW.getValue(), "order", orderId, null,
                Map.of("实收金额", String.valueOf(studentOrderRenewParam.getPaidAmount())));
        return orderId;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void payCallback(StudentOrderPayCallbackParam studentOrderPayCallbackParam) {
        // v1 线上支付对接后由支付网关调用；当前保留为联调入口
        markPaid(studentOrderPayCallbackParam.getOrderId(), studentOrderPayCallbackParam.getTradeNo(),
                studentOrderPayCallbackParam.getPaidAmount(), studentOrderPayCallbackParam.getPayChannel());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void offlinePay(StudentOrderOfflinePayParam studentOrderOfflinePayParam) {
        String payChannel = ObjectUtil.isNotEmpty(studentOrderOfflinePayParam.getPayChannel())
                ? studentOrderOfflinePayParam.getPayChannel() : "OFFLINE";
        if (!"OFFLINE".equals(payChannel) && !"CASH".equals(payChannel)) {
            throw new CommonException("v1 仅支持线下收款登记，线上支付将在后续版本开放");
        }
        if (ObjectUtil.isNotEmpty(studentOrderOfflinePayParam.getPaidAmount())
                && studentOrderOfflinePayParam.getPaidAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new CommonException("实收金额必须大于0");
        }
        markPaid(studentOrderOfflinePayParam.getOrderId(),
                "OFF" + IdUtil.getSnowflakeNextIdStr(),
                studentOrderOfflinePayParam.getPaidAmount(), payChannel);
        StudentOrder paidOrder = this.queryEntity(studentOrderOfflinePayParam.getOrderId());
        bizEventApi.log(BizEventCodeEnum.ORDER_PAY.getValue(), "order", paidOrder.getId(),
                paidOrder.getPackageName(),
                Map.of("实收金额", String.valueOf(studentOrderOfflinePayParam.getPaidAmount()), "渠道", payChannel));
    }

    /**
     * 订单置为已支付并入账（幂等）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void markPaid(String orderId, String tradeNo, java.math.BigDecimal paidAmount, String payChannel) {
        // 行锁读取订单，避免并发回调同时入账
        StudentOrder order = this.getOne(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getId, orderId)
                .last("FOR UPDATE"));
        if (ObjectUtil.isEmpty(order)) {
            throw new CommonException("订单不存在，id值为：{}", orderId);
        }
        // 幂等：已支付直接返回（防重复回调）
        if (StudentOrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
            return;
        }
        if (!StudentOrderStatusEnum.UNPAID.getValue().equals(order.getStatus())) {
            throw new CommonException("订单状态异常，无法支付");
        }
        // 同一支付流水号不能用于其他订单
        boolean tradeNoUsed = this.count(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getTradeNo, tradeNo)
                .ne(StudentOrder::getId, order.getId())) > 0;
        if (tradeNoUsed) {
            throw new CommonException("支付流水号已被其他订单使用");
        }
        // 条件更新防并发重复入账
        boolean paid = this.update(new LambdaUpdateWrapper<StudentOrder>()
                .eq(StudentOrder::getId, order.getId())
                .eq(StudentOrder::getStatus, StudentOrderStatusEnum.UNPAID.getValue())
                .set(StudentOrder::getStatus, StudentOrderStatusEnum.PAID.getValue())
                .set(StudentOrder::getTradeNo, tradeNo)
                .set(StudentOrder::getPayTime, new java.util.Date())
                .set(StudentOrder::getPaidAmount, ObjectUtil.isNotEmpty(paidAmount)
                        ? paidAmount : order.getAmount())
                .set(StudentOrder::getPayChannel, ObjectUtil.isNotEmpty(payChannel)
                        ? payChannel : order.getPayChannel()));
        if (!paid) {
            // 并发下已被其他回调处理，直接返回
            return;
        }
        // 入账：创建/追加课时账户 + 入账流水 + 续费预警置已续费
        studentLessonAccountService.recharge(order);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(StudentOrderIdParam studentOrderIdParam) {
        StudentOrder cancelOrder = this.queryEntity(studentOrderIdParam.getId());
        bizEventApi.log(BizEventCodeEnum.ORDER_CANCEL.getValue(), "order", cancelOrder.getId(), cancelOrder.getPackageName(), null);
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
