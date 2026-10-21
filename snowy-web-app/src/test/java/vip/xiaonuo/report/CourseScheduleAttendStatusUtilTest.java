/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@163.com商议合作。
 * 6.如需更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为：https://www.xiaonuo.vip
 */
package vip.xiaonuo.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.course.core.util.CourseScheduleAttendStatusUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 排课点名状态测试
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@DisplayName("排课点名状态")
class CourseScheduleAttendStatusUtilTest {

    @Test
    @DisplayName("无人报名时为未点名")
    void testNoStudent() {
        assertEquals(CourseScheduleAttendStatusUtil.NONE, CourseScheduleAttendStatusUtil.attendStatus(0, 0));
    }

    @Test
    @DisplayName("尚未点名时为未点名")
    void testNone() {
        assertEquals(CourseScheduleAttendStatusUtil.NONE, CourseScheduleAttendStatusUtil.attendStatus(0, 8));
    }

    @Test
    @DisplayName("部分点名")
    void testPartial() {
        assertEquals(CourseScheduleAttendStatusUtil.PARTIAL, CourseScheduleAttendStatusUtil.attendStatus(3, 8));
    }

    @Test
    @DisplayName("全部点名时为已完成")
    void testDone() {
        assertEquals(CourseScheduleAttendStatusUtil.DONE, CourseScheduleAttendStatusUtil.attendStatus(8, 8));
        assertEquals(CourseScheduleAttendStatusUtil.DONE, CourseScheduleAttendStatusUtil.attendStatus(9, 8));
    }
}
