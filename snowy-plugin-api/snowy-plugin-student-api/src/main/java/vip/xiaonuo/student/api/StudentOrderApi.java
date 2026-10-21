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
package vip.xiaonuo.student.api;

import vip.xiaonuo.student.api.dto.StudentOrderRefDTO;

import java.util.List;

/**
 * 订单域查询API（跨插件调用）
 * <p>
 * 供课务域解析「学员所购课包」，用于按课包维度决定扣课规则。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public interface StudentOrderApi {

    /**
     * 查询学员的已支付订单引用（按下单时间倒序）
     *
     * @param studentIdList 学员id集合
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    List<StudentOrderRefDTO> listPaidOrderRefs(List<String> studentIdList);
}
