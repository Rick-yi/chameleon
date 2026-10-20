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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderAddParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderIdParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPageParam;
import vip.xiaonuo.student.modular.studentorder.param.StudentOrderPayCallbackParam;

import java.util.List;

/**
 * 报名订单Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentOrderService {

    /** 获取订单分页 */
    Page<StudentOrder> page(StudentOrderPageParam studentOrderPageParam);

    /** 报名下单（课包快照，防重复下单） */
    void add(StudentOrderAddParam studentOrderAddParam);

    /** 支付回调（幂等：条件更新 + 流水号唯一） */
    void payCallback(StudentOrderPayCallbackParam studentOrderPayCallbackParam);

    /** 取消订单 */
    void cancel(StudentOrderIdParam studentOrderIdParam);

    /** 获取订单详情 */
    StudentOrder detail(StudentOrderIdParam studentOrderIdParam);

    /** 获取订单详情（内部用） */
    StudentOrder queryEntity(String id);

    /** 更新订单（内部用） */
    void updateOrder(StudentOrder studentOrder);
}
