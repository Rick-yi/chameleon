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
package vip.xiaonuo.student.modular.studentsundry.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.student.core.service.StudentDataScopeService;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentsundry.entity.StudentSundryOrder;
import vip.xiaonuo.student.modular.studentsundry.enums.StudentSundryStatusEnum;
import vip.xiaonuo.student.modular.studentsundry.mapper.StudentSundryOrderMapper;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryOrderAddParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryPageParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryPayParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryRefundParam;
import vip.xiaonuo.student.modular.studentsundry.service.StudentSundryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学杂费Service实现
 * <p>
 * 与课包订单分账：不产生课时账户、不做课消、不参与客单价与续费率。
 * 收款即收入确认；退费走独立的退款中/已退款状态，不牵连课时回补。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
@Slf4j
@Service
public class StudentSundryServiceImpl extends ServiceImpl<StudentSundryOrderMapper, StudentSundryOrder>
        implements StudentSundryService {

    /** 允许的费用类型 */
    private static final List<String> FEE_TYPES = List.of("UNIFORM", "BOOK", "ACTIVITY", "MEAL", "OTHER");

    @Resource
    private StudentService studentService;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private StudentDataScopeService studentDataScopeService;

    @Override
    public Page<StudentSundryOrder> page(StudentSundryPageParam param) {
        LambdaQueryWrapper<StudentSundryOrder> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(param.getStudentId())) {
            wrapper.eq(StudentSundryOrder::getStudentId, param.getStudentId());
        }
        if (StrUtil.isNotBlank(param.getCampusId())) {
            wrapper.eq(StudentSundryOrder::getCampusId, param.getCampusId());
        }
        if (StrUtil.isNotBlank(param.getFeeType())) {
            wrapper.eq(StudentSundryOrder::getFeeType, param.getFeeType());
        }
        if (StrUtil.isNotBlank(param.getStatus())) {
            wrapper.eq(StudentSundryOrder::getStatus, param.getStatus());
        }
        if (StrUtil.isNotBlank(param.getSearchKey())) {
            wrapper.like(StudentSundryOrder::getItemName, param.getSearchKey());
        }
        // 数据范围：仅可见学员
        List<String> visibleStudentIds = studentDataScopeService.visibleStudentIdList();
        if (visibleStudentIds != null) {
            if (visibleStudentIds.isEmpty()) {
                return new Page<>(1, 10, 0);
            }
            wrapper.in(StudentSundryOrder::getStudentId, visibleStudentIds);
        }
        wrapper.orderByDesc(StudentSundryOrder::getCreateTime);
        Page<StudentSundryOrder> page = this.page(CommonPageRequest.defaultPage(), wrapper);
        fillStudentName(page.getRecords());
        return page;
    }

    @Override
    public List<StudentSundryOrder> listByStudent(String studentId) {
        if (StrUtil.isBlank(studentId)) {
            return new ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<StudentSundryOrder>()
                .eq(StudentSundryOrder::getStudentId, studentId)
                .orderByDesc(StudentSundryOrder::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String add(StudentSundryOrderAddParam param) {
        if (StrUtil.isBlank(param.getStudentId())) {
            throw new CommonException("请选择学员");
        }
        if (StrUtil.isBlank(param.getItemName())) {
            throw new CommonException("请填写物品名称");
        }
        String feeType = StrUtil.blankToDefault(param.getFeeType(), "OTHER").toUpperCase();
        if (!FEE_TYPES.contains(feeType)) {
            throw new CommonException("不支持的费用类型：{}", param.getFeeType());
        }
        // 学员必须存在
        Student student = studentService.queryEntity(param.getStudentId());
        if (student == null) {
            throw new CommonException("学员不存在，id值为：{}", param.getStudentId());
        }
        BigDecimal quantity = nvl(param.getQuantity());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("数量必须大于0");
        }
        BigDecimal unitPrice = nvl(param.getUnitPrice());
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CommonException("单价不能为负");
        }
        // 应收金额：允许手工指定（如打包价），否则按数量×单价
        BigDecimal amount = param.getAmount() != null
                ? param.getAmount()
                : quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CommonException("应收金额不能为负");
        }

        StudentSundryOrder order = new StudentSundryOrder();
        order.setStudentId(param.getStudentId());
        order.setCampusId(StrUtil.blankToDefault(param.getCampusId(), student.getCampusId()));
        order.setFeeType(feeType);
        order.setItemName(param.getItemName().trim());
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setAmount(amount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRefundedAmount(BigDecimal.ZERO);
        order.setRemark(param.getRemark());
        order.setStatus(StudentSundryStatusEnum.UNPAID);
        order.setReceiptNo("SF" + IdUtil.getSnowflakeNextIdStr());

        // 立即收款
        if (Boolean.TRUE.equals(param.getPayNow())) {
            BigDecimal paid = param.getPaidAmount() != null ? param.getPaidAmount() : amount;
            if (paid.compareTo(BigDecimal.ZERO) < 0) {
                throw new CommonException("实收金额不能为负");
            }
            order.setPaidAmount(paid);
            order.setPayChannel(StrUtil.blankToDefault(param.getPayChannel(), "CASH"));
            order.setPayTime(new Date());
            order.setStatus(StudentSundryStatusEnum.PAID);
        }
        this.save(order);
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void pay(StudentSundryPayParam param) {
        StudentSundryOrder order = queryEntity(param.getId());
        if (!StudentSundryStatusEnum.UNPAID.equals(order.getStatus())
                && !StudentSundryStatusEnum.PENDING.equals(order.getStatus())) {
            throw new CommonException("单据当前状态【{}】不允许收款",
                    StudentSundryStatusEnum.messageOf(order.getStatus()));
        }
        BigDecimal paid = param.getPaidAmount() != null ? param.getPaidAmount() : nvl(order.getAmount());
        if (paid.compareTo(BigDecimal.ZERO) < 0) {
            throw new CommonException("实收金额不能为负");
        }
        // 条件更新防重复收款：只有未收款状态才能改
        boolean updated = this.update(new LambdaUpdateWrapper<StudentSundryOrder>()
                .eq(StudentSundryOrder::getId, order.getId())
                .in(StudentSundryOrder::getStatus,
                        StudentSundryStatusEnum.UNPAID, StudentSundryStatusEnum.PENDING)
                .set(StudentSundryOrder::getStatus, StudentSundryStatusEnum.PAID)
                .set(StudentSundryOrder::getPaidAmount, paid)
                .set(StudentSundryOrder::getPayChannel, StrUtil.blankToDefault(param.getPayChannel(), "CASH"))
                .set(StudentSundryOrder::getPayTime, new Date())
                .set(StrUtil.isNotBlank(param.getRemark()), StudentSundryOrder::getRemark, param.getRemark()));
        if (!updated) {
            throw new CommonException("单据状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refund(StudentSundryRefundParam param) {
        StudentSundryOrder order = queryEntity(param.getId());
        if (!StudentSundryStatusEnum.canRefund(order.getStatus())) {
            throw new CommonException("单据当前状态【{}】不允许退费",
                    StudentSundryStatusEnum.messageOf(order.getStatus()));
        }
        BigDecimal paid = nvl(order.getPaidAmount());
        BigDecimal alreadyRefunded = nvl(order.getRefundedAmount());
        BigDecimal refundable = paid.subtract(alreadyRefunded);
        if (refundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("该单据已无可退金额");
        }
        BigDecimal refundAmount = param.getRefundAmount() != null ? param.getRefundAmount() : refundable;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonException("退费金额必须大于0");
        }
        if (refundAmount.compareTo(refundable) > 0) {
            throw new CommonException("退费金额不能超过可退金额（可退 {}）", refundable);
        }
        BigDecimal totalRefunded = alreadyRefunded.add(refundAmount);
        // 全额退完置为已退款，部分退保持已收款（允许再次退）
        String nextStatus = totalRefunded.compareTo(paid) >= 0
                ? StudentSundryStatusEnum.REFUNDED
                : StudentSundryStatusEnum.PAID;
        String remark = StrUtil.isBlank(param.getReason()) ? order.getRemark()
                : (StrUtil.isBlank(order.getRemark()) ? "退费：" + param.getReason()
                : order.getRemark() + "；退费：" + param.getReason());

        // 条件更新：已退金额未变才允许，防并发重复退
        boolean updated = this.update(new LambdaUpdateWrapper<StudentSundryOrder>()
                .eq(StudentSundryOrder::getId, order.getId())
                .eq(StudentSundryOrder::getStatus, StudentSundryStatusEnum.PAID)
                .eq(StudentSundryOrder::getRefundedAmount, alreadyRefunded)
                .set(StudentSundryOrder::getRefundedAmount, totalRefunded)
                .set(StudentSundryOrder::getStatus, nextStatus)
                .set(StudentSundryOrder::getRefundTime, new Date())
                .set(StudentSundryOrder::getRemark, remark));
        if (!updated) {
            throw new CommonException("单据状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(String id) {
        StudentSundryOrder order = queryEntity(id);
        if (!StudentSundryStatusEnum.canCancel(order.getStatus())) {
            throw new CommonException("单据当前状态【{}】不允许取消",
                    StudentSundryStatusEnum.messageOf(order.getStatus()));
        }
        boolean updated = this.update(new LambdaUpdateWrapper<StudentSundryOrder>()
                .eq(StudentSundryOrder::getId, id)
                .in(StudentSundryOrder::getStatus,
                        StudentSundryStatusEnum.UNPAID, StudentSundryStatusEnum.PENDING)
                .set(StudentSundryOrder::getStatus, StudentSundryStatusEnum.CANCELED));
        if (!updated) {
            throw new CommonException("单据状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String id) {
        StudentSundryOrder order = queryEntity(id);
        // 已收款的单据不允许删除，必须先退费，避免收入凭空消失
        if (StudentSundryStatusEnum.isPaid(order.getStatus())
                || StudentSundryStatusEnum.REFUNDED.equals(order.getStatus())) {
            throw new CommonException("已收款单据不允许删除，请先办理退费");
        }
        this.removeById(id);
    }

    @Override
    public Map<String, BigDecimal> summaryByStudent(String studentId) {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("receivable", BigDecimal.ZERO);
        result.put("received", BigDecimal.ZERO);
        result.put("refunded", BigDecimal.ZERO);
        if (StrUtil.isBlank(studentId)) {
            return result;
        }
        List<StudentSundryOrder> list = this.list(new LambdaQueryWrapper<StudentSundryOrder>()
                .eq(StudentSundryOrder::getStudentId, studentId)
                .ne(StudentSundryOrder::getStatus, StudentSundryStatusEnum.CANCELED));
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (StudentSundryOrder item : list) {
            receivable = receivable.add(nvl(item.getAmount()));
            received = received.add(nvl(item.getPaidAmount()));
            refunded = refunded.add(nvl(item.getRefundedAmount()));
        }
        result.put("receivable", receivable);
        result.put("received", received);
        result.put("refunded", refunded);
        return result;
    }

    @Override
    public Map<String, BigDecimal> receivedAmountByStudentIds(List<String> studentIdList) {
        Map<String, BigDecimal> resultMap = new HashMap<>();
        if (studentIdList == null || studentIdList.isEmpty()) {
            return resultMap;
        }
        List<String> idList = studentIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (idList.isEmpty()) {
            return resultMap;
        }
        // 一次查询后在内存按学员汇总，避免逐人查询
        this.list(new LambdaQueryWrapper<StudentSundryOrder>()
                        .select(StudentSundryOrder::getStudentId, StudentSundryOrder::getPaidAmount,
                                StudentSundryOrder::getRefundedAmount)
                        .in(StudentSundryOrder::getStudentId, idList)
                        .eq(StudentSundryOrder::getStatus, StudentSundryStatusEnum.PAID))
                .forEach(item -> resultMap.merge(item.getStudentId(),
                        nvl(item.getPaidAmount()).subtract(nvl(item.getRefundedAmount())),
                        BigDecimal::add));
        return resultMap;
    }

    /**
     * 批量填充学员姓名
     */
    private void fillStudentName(List<StudentSundryOrder> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<String> studentIdList = list.stream().map(StudentSundryOrder::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, String> nameMap = new HashMap<>();
        if (!studentIdList.isEmpty()) {
            studentMapper.selectBatchIds(studentIdList)
                    .forEach(student -> nameMap.put(student.getId(), student.getName()));
        }
        list.forEach(item -> item.setStudentName(nameMap.get(item.getStudentId())));
    }

    private StudentSundryOrder queryEntity(String id) {
        if (StrUtil.isBlank(id)) {
            throw new CommonException("单据id不能为空");
        }
        StudentSundryOrder order = this.getById(id);
        if (order == null) {
            throw new CommonException("学杂费单据不存在，id值为：{}", id);
        }
        return order;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
