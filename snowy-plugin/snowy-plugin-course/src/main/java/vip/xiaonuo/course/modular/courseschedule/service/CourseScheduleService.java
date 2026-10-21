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
package vip.xiaonuo.course.modular.courseschedule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.api.dto.CourseScheduleGenerateResultDTO;
import vip.xiaonuo.course.api.dto.CourseScheduleViewDTO;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAddParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAdhocParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleChangeParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleGenerateParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleIdParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseSchedulePageParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleWeekParam;

import cn.hutool.json.JSONObject;
import java.util.List;

/**
 * 排课Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface CourseScheduleService {

    /** 获取排课分页（周视图按日期区间） */
    Page<CourseSchedule> page(CourseSchedulePageParam courseSchedulePageParam);

    /** 手动排课（含冲突检测） */
    void add(CourseScheduleAddParam courseScheduleAddParam);

    /** 按班级周模板批量生成（含冲突检测，任一冲突整体失败） */
    CourseScheduleGenerateResultDTO generate(CourseScheduleGenerateParam courseScheduleGenerateParam);

    /**
     * 直接排课（不挂班级：老师带一群学生上课）
     * <p>
     * 排课直接关联课程与这节课的学员名单，点名与消课按课程扣减课时账户。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    CourseScheduleGenerateResultDTO generateAdhoc(CourseScheduleAdhocParam courseScheduleAdhocParam);

    /**
     * 查询某节课的上课名单（班级在读学员 ∪ 排课直接指定学员）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    List<String> listRosterStudentIds(String scheduleId);

    /**
     * 课表区间查询（不分页，返回带班级/教师名称的视图数据）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    List<CourseScheduleViewDTO> week(CourseScheduleWeekParam courseScheduleWeekParam);

    /**
     * 教师可选项（租户内用户）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    List<JSONObject> teacherOptions(String searchKey);

    /** 调课（原排课置已调 + 新排课 + 变更记录） */
    void change(CourseScheduleChangeParam courseScheduleChangeParam);

    /** 取消排课（已有点名流水的排课不允许取消，M3 校验） */
    void cancel(CourseScheduleIdParam courseScheduleIdParam);

    /** 获取排课详情 */
    CourseSchedule detail(CourseScheduleIdParam courseScheduleIdParam);

    /** 获取排课详情（内部用） */
    CourseSchedule queryEntity(String id);

    /** 冲突检测（返回冲突描述列表，空列表表示无冲突） */
    List<String> conflictCheck(String classId, java.util.Date scheduleDate,
                               java.time.LocalTime startTime, java.time.LocalTime endTime,
                               String classroom, String teacherId, String excludeScheduleId);
}
