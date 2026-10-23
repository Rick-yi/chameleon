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
package vip.xiaonuo.student.modular.student.service;

import vip.xiaonuo.student.modular.student.param.StudentQuickEnrollParam;

import java.util.Map;

/**
 * 学员快捷录入：建档 + 报名 + 收款一次完成
 *
 * @author 会飞的猪
 * @date 2026/10/23 19:00
 **/
public interface StudentEnrollService {

    /**
     * 快捷录入学员
     *
     * @param param 学员、家长、课包与收款信息
     * @return studentId / orderId
     */
    Map<String, Object> quickEnroll(StudentQuickEnrollParam param);
}
