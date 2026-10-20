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

import vip.xiaonuo.student.api.pojo.LessonDeductParam;

import java.util.List;

/**
 * 课时账务API接口（跨插件调用）
 * <p>
 * 供排课域（course 插件）在点名确认/撤销时联动扣课与回补。
 * 调用方与实现方处于同一事务（单数据源），任一学员余额不足将抛异常导致整体回滚。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentLessonApi {

    /**
     * 点名扣课（事务内：行锁账户 → 校验余额 → 扣减 → 写流水）
     * <p>
     * 任一学员余额不足时抛出业务异常（含学员姓名与剩余课时），由调用方事务整体回滚。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    void deductLessons(List<LessonDeductParam> deductParamList);

    /**
     * 撤销点名回补课时（按排课+学员定位原消课流水，写回补流水）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    void restoreLessons(String scheduleId, String studentId);
}
