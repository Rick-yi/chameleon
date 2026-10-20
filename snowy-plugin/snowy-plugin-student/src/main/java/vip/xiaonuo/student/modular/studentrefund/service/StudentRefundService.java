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
package vip.xiaonuo.student.modular.studentrefund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.student.modular.studentrefund.entity.StudentRefund;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundAddParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundApproveParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundIdParam;
import vip.xiaonuo.student.modular.studentrefund.param.StudentRefundPageParam;

/**
 * 退费单Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentRefundService {

    /** 获取退费单分页 */
    Page<StudentRefund> page(StudentRefundPageParam studentRefundPageParam);

    /** 退费申请（可退=剩余-赠课，折算单价=实付/课包课时） */
    void apply(StudentRefundAddParam studentRefundAddParam);

    /** 退费审批（通过→账户冻结；驳回→结束） */
    void approve(StudentRefundApproveParam studentRefundApproveParam);

    /** 确认退款完成（账户扣减/清零 + 退费销课流水 + 订单置已退费） */
    void confirm(StudentRefundIdParam studentRefundIdParam);

    /** 撤销退费（待审批/待退款 → 已撤销，待退款时解冻账户） */
    void cancel(StudentRefundIdParam studentRefundIdParam);

    /** 获取退费单详情 */
    StudentRefund detail(StudentRefundIdParam studentRefundIdParam);

    /** 获取退费单详情（内部用） */
    StudentRefund queryEntity(String id);
}
