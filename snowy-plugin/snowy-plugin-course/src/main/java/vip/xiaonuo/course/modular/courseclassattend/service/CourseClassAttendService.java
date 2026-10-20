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
package vip.xiaonuo.course.modular.courseclassattend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendConfirmParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendIdParam;
import vip.xiaonuo.course.modular.courseclassattend.param.CourseClassAttendPageParam;

/**
 * 点名记录Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface CourseClassAttendService {

    /** 获取点名记录分页 */
    Page<CourseClassAttend> page(CourseClassAttendPageParam courseClassAttendPageParam);

    /** 点名确认（保存点名记录 + 扣课联动，任一学员余额不足整体回滚） */
    void confirm(CourseClassAttendConfirmParam courseClassAttendConfirmParam);

    /** 撤销点名（回补课时 + 删除记录） */
    void cancel(CourseClassAttendIdParam courseClassAttendIdParam);

    /** 获取点名记录详情（内部用） */
    CourseClassAttend queryEntity(String id);
}
