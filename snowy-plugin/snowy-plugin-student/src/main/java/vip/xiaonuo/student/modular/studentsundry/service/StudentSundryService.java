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
package vip.xiaonuo.student.modular.studentsundry.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.student.modular.studentsundry.entity.StudentSundryOrder;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryOrderAddParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryPageParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryPayParam;
import vip.xiaonuo.student.modular.studentsundry.param.StudentSundryRefundParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 学杂费Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public interface StudentSundryService {

    /** 分页查询 */
    Page<StudentSundryOrder> page(StudentSundryPageParam param);

    /** 按学员查询全部单据（学员详情用） */
    List<StudentSundryOrder> listByStudent(String studentId);

    /** 新开单据（可选立即收款），返回单据id */
    String add(StudentSundryOrderAddParam param);

    /** 收款 */
    void pay(StudentSundryPayParam param);

    /** 退费 */
    void refund(StudentSundryRefundParam param);

    /** 取消（未收款才允许） */
    void cancel(String id);

    /** 删除 */
    void delete(String id);

    /**
     * 统计学员的学杂费汇总：应收、已收、已退
     *
     * @param studentId 学员id
     * @return 汇总，键为 receivable / received / refunded
     */
    Map<String, BigDecimal> summaryByStudent(String studentId);

    /**
     * 批量统计学员的学杂费已收金额（列表页用，避免逐人查询）
     *
     * @param studentIdList 学员id集合
     * @return 学员id -> 已收金额
     */
    Map<String, BigDecimal> receivedAmountByStudentIds(List<String> studentIdList);
}
