/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时需要注意以下几点：
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@163.com商议合作。
 * 6.如需更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为：https://www.xiaonuo.vip
 */
package vip.xiaonuo.course.modular.courseschedule.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.course.api.dto.CourseScheduleViewDTO;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleWeekParam;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleExportService;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课表导出服务实现
 * <p>
 * 导出两个工作表：
 * 1. 课表清单：一行一节课（日期/星期/时段/班级/课程/教师/教室/点名状态），可直接打印张贴；
 * 2. 周视图：行=时段、列=日期的矩阵，与页面上看到的排布一致。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class CourseScheduleExportServiceImpl implements CourseScheduleExportService {

    /** 单次导出最大行数 */
    private static final int MAX_ROWS = 5000;

    /** 周视图最多列数（区间过大时截断） */
    private static final int MAX_VIEW_COLUMNS = 14;

    private static final String[] WEEK_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    @Resource
    private CourseScheduleService courseScheduleService;

    @Override
    public void export(CourseScheduleWeekParam courseScheduleWeekParam, HttpServletResponse response) throws IOException {
        List<CourseScheduleViewDTO> scheduleList = courseScheduleService.week(courseScheduleWeekParam);
        if (ObjectUtil.isEmpty(scheduleList)) {
            throw new CommonException("所选区间没有可导出的排课");
        }
        if (scheduleList.size() > MAX_ROWS) {
            throw new CommonException("排课数量超过 {} 条，请缩小导出区间", MAX_ROWS);
        }
        String fileName = "课表_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
        File file = FileUtil.file(FileUtil.getTmpDir(), fileName);
        try {
            CommonExcelUtil.writeMulti(file, List.of(buildListSheet(scheduleList), buildViewSheet(scheduleList)));
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    /**
     * 工作表一：课表清单
     */
    private CommonExcelUtil.Sheet buildListSheet(List<CourseScheduleViewDTO> scheduleList) {
        List<List<String>> headList = new ArrayList<>();
        headList.add(List.of("日期"));
        headList.add(List.of("星期"));
        headList.add(List.of("时间段"));
        headList.add(List.of("班级"));
        headList.add(List.of("课程"));
        headList.add(List.of("教师"));
        headList.add(List.of("教室"));
        headList.add(List.of("点名状态"));
        List<List<String>> rowList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (CourseScheduleViewDTO schedule : scheduleList) {
            Date scheduleDate = schedule.getScheduleDate();
            List<String> row = new ArrayList<>();
            row.add(scheduleDate == null ? "" : dateFormat.format(scheduleDate));
            row.add(scheduleDate == null ? "" : weekName(scheduleDate));
            row.add(schedule.getStartTime() + "-" + schedule.getEndTime());
            row.add(StrUtil.blankToDefault(schedule.getClassName(), ""));
            row.add(StrUtil.blankToDefault(schedule.getCourseName(), ""));
            row.add(StrUtil.blankToDefault(schedule.getTeacherName(), ""));
            row.add(StrUtil.blankToDefault(schedule.getClassroom(), ""));
            row.add(attendText(schedule.getAttendStatus()));
            rowList.add(row);
        }
        return new CommonExcelUtil.Sheet("课表清单", headList, rowList);
    }

    /**
     * 工作表二：周视图（行=时段，列=日期）
     */
    private CommonExcelUtil.Sheet buildViewSheet(List<CourseScheduleViewDTO> scheduleList) {
        // 按日期分组，保留出现顺序
        Map<String, List<CourseScheduleViewDTO>> dateMap = new LinkedHashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (CourseScheduleViewDTO schedule : scheduleList) {
            Date scheduleDate = schedule.getScheduleDate();
            if (scheduleDate == null) {
                continue;
            }
            dateMap.computeIfAbsent(dateFormat.format(scheduleDate), k -> new ArrayList<>()).add(schedule);
        }
        List<String> dateList = new ArrayList<>(dateMap.keySet());
        if (dateList.size() > MAX_VIEW_COLUMNS) {
            dateList = dateList.subList(0, MAX_VIEW_COLUMNS);
        }
        // 时段去重并按开始时间排序
        List<String> timeSlotList = new ArrayList<>();
        for (CourseScheduleViewDTO schedule : scheduleList) {
            String slot = schedule.getStartTime() + "-" + schedule.getEndTime();
            if (!timeSlotList.contains(slot)) {
                timeSlotList.add(slot);
            }
        }
        timeSlotList.sort((a, b) -> a.compareTo(b));
        List<List<String>> headList = new ArrayList<>();
        headList.add(List.of("时间段"));
        for (String date : dateList) {
            headList.add(List.of(date + " " + weekNameOf(date)));
        }
        List<List<String>> rowList = new ArrayList<>();
        for (String slot : timeSlotList) {
            List<String> row = new ArrayList<>();
            row.add(slot);
            for (String date : dateList) {
                StringBuilder cell = new StringBuilder();
                for (CourseScheduleViewDTO schedule : dateMap.get(date)) {
                    if (!(schedule.getStartTime() + "-" + schedule.getEndTime()).equals(slot)) {
                        continue;
                    }
                    if (cell.length() > 0) {
                        cell.append("\\n");
                    }
                    cell.append(StrUtil.blankToDefault(schedule.getClassName(), ""));
                    if (StrUtil.isNotBlank(schedule.getTeacherName())) {
                        cell.append(" / ").append(schedule.getTeacherName());
                    }
                    if (StrUtil.isNotBlank(schedule.getClassroom())) {
                        cell.append(" / ").append(schedule.getClassroom());
                    }
                }
                row.add(cell.toString());
            }
            rowList.add(row);
        }
        return new CommonExcelUtil.Sheet("周视图", headList, rowList);
    }

    /**
     * 星期名称
     */
    private String weekName(Date date) {
        return WEEK_NAMES[(date.getDay() + 6) % 7];
    }

    /**
     * 星期名称（按 yyyy-MM-dd 字符串）
     */
    private String weekNameOf(String dateStr) {
        try {
            return weekName(DateUtil.parse(dateStr));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 点名状态文案
     */
    private String attendText(String attendStatus) {
        if ("DONE".equals(attendStatus)) {
            return "已点名";
        }
        if ("PARTIAL".equals(attendStatus)) {
            return "部分点名";
        }
        return "待点名";
    }
}
