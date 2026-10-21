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
package vip.xiaonuo.student.modular.studentlessonaccount.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.student.api.pojo.LessonDeductParam;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.param.StudentLessonAccountPageParam;
import vip.xiaonuo.student.modular.studentlessonaccount.param.StudentLessonAdjustParam;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;

import java.util.List;

/**
 * 课时账户Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentLessonAccountService {

    /** 获取课时账户分页 */
    Page<StudentLessonAccount> page(StudentLessonAccountPageParam studentLessonAccountPageParam);

    /** 获取账户（内部用，不存在抛异常） */
    StudentLessonAccount getAccount(String studentId, String courseId);

    /** 支付后入账（创建/追加账户 + 入账流水 + 续费预警置已续费） */
    void recharge(StudentOrder studentOrder);

    /** 更新账户（内部用） */
    void updateAccount(StudentLessonAccount studentLessonAccount);

    /** 获取全部正常账户（续费预警扫描用） */
    List<StudentLessonAccount> listNormalAccounts();

    /** 批量查询学员在指定课程下的课时账户 */
    List<StudentLessonAccount> listByStudentAndCourse(List<String> studentIdList, String courseId);

    /** 点名扣课（条件更新防并发，任一不足抛异常整体回滚） */
    void deductLessons(List<LessonDeductParam> deductParamList);

    /** 撤销点名回补课时 */
    void restoreLessons(String scheduleId, String studentId);

    /**
     * 人工调整课时（赠送/收回，必填原因，余额不足拒绝）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    void adjust(StudentLessonAdjustParam param);
}
