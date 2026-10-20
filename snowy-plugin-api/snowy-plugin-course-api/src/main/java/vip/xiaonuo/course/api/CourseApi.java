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
package vip.xiaonuo.course.api;

import cn.hutool.json.JSONObject;

import java.util.List;

/**
 * 课程域API接口（跨插件调用）
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface CourseApi {

    /**
     * 获取班级信息（easy-trans 等跨插件引用用）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    JSONObject getCourseClassInfo(String classId);

    /**
     * 获取排课信息（点名/扣课联动用）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    JSONObject getCourseScheduleInfo(String scheduleId);

    /**
     * 获取班级在学学员id列表（点名/扣课用）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    List<String> getStudyingStudentIdList(String classId);

    /**
     * 获取课包信息（报名下单快照用）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    JSONObject getCoursePackageInfo(String packageId);
}
