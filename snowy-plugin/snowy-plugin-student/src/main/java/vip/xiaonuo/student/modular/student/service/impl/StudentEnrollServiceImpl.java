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
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.student.modular.student.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentQuickEnrollParam;
import vip.xiaonuo.student.modular.student.service.StudentEnrollService;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderAddParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderOfflinePayParam;
import vip.xiaonuo.student.modular.studentorder.service.StudentOrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学员快捷录入实现
 *
 * @author 会飞的猪
 * @date 2026/10/23 19:00
 **/
@Service
public class StudentEnrollServiceImpl implements StudentEnrollService {

    /** 支持手工登记的收款渠道 */
    private static final List<String> PAY_CHANNEL_LIST = List.of("CASH", "OFFLINE", "WECHAT", "ALIPAY");

    @Resource
    private StudentService studentService;

    @Resource
    private StudentOrderService studentOrderService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> quickEnroll(StudentQuickEnrollParam param) {
        if (Boolean.TRUE.equals(param.getPayNow()) && StrUtil.isBlank(param.getCoursePackageId())) {
            throw new CommonException("请选择课包后再登记收款");
        }
        String payChannel = StrUtil.blankToDefault(param.getPayChannel(), "CASH").toUpperCase();
        if (Boolean.TRUE.equals(param.getPayNow()) && !PAY_CHANNEL_LIST.contains(payChannel)) {
            throw new CommonException("不支持的收款渠道：{}", param.getPayChannel());
        }

        // 1. 建档
        StudentAddParam studentAddParam = BeanUtil.toBean(param, StudentAddParam.class);
        String studentId = studentService.add(studentAddParam);

        // 2. 选课包则报名，按需收款
        String orderId = null;
        if (StrUtil.isNotBlank(param.getCoursePackageId())) {
            StudentOrderAddParam orderAddParam = new StudentOrderAddParam();
            orderAddParam.setStudentId(studentId);
            orderAddParam.setCoursePackageId(param.getCoursePackageId());
            orderAddParam.setPayChannel(payChannel);
            orderId = studentOrderService.add(orderAddParam);
            if (Boolean.TRUE.equals(param.getPayNow())) {
                StudentOrderOfflinePayParam payParam = new StudentOrderOfflinePayParam();
                payParam.setOrderId(orderId);
                payParam.setPaidAmount(param.getPaidAmount());
                payParam.setPayChannel(payChannel);
                payParam.setRemark(StrUtil.blankToDefault(param.getPayRemark(), "工作台快速录入"));
                studentOrderService.offlinePay(payParam);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("orderId", orderId);
        return result;
    }
}
