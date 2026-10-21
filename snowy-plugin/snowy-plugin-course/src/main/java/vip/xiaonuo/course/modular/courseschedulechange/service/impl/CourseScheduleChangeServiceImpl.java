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
package vip.xiaonuo.course.modular.courseschedulechange.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;
import vip.xiaonuo.sys.api.SysUserApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vip.xiaonuo.course.modular.courseschedulechange.entity.CourseScheduleChange;
import vip.xiaonuo.course.modular.courseschedulechange.mapper.CourseScheduleChangeMapper;
import vip.xiaonuo.course.modular.courseschedulechange.param.CourseScheduleChangePageParam;
import vip.xiaonuo.course.modular.courseschedulechange.service.CourseScheduleChangeService;

/**
 * 调课记录Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseScheduleChangeServiceImpl extends ServiceImpl<CourseScheduleChangeMapper, CourseScheduleChange> implements CourseScheduleChangeService {

    @Resource
    private CourseScheduleMapper courseScheduleMapper;

    @Resource
    private CourseClassMapper courseClassMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Override
    public Page<CourseScheduleChange> page(CourseScheduleChangePageParam courseScheduleChangePageParam) {
        QueryWrapper<CourseScheduleChange> queryWrapper = new QueryWrapper<CourseScheduleChange>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(courseScheduleChangePageParam.getFromScheduleId()), CourseScheduleChange::getFromScheduleId, courseScheduleChangePageParam.getFromScheduleId())
                .orderByDesc(CourseScheduleChange::getCreateTime);
        Page<CourseScheduleChange> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillDetail(page.getRecords());
        return page;
    }

    /**
     * 填充调课前后的课节信息（日期 / 时段 / 班级）与调课人
     */
    private void fillDetail(List<CourseScheduleChange> changeList) {
        if (ObjectUtil.isEmpty(changeList)) {
            return;
        }
        List<String> scheduleIdList = new ArrayList<>();
        for (CourseScheduleChange change : changeList) {
            if (StrUtil.isNotBlank(change.getFromScheduleId())) scheduleIdList.add(change.getFromScheduleId());
            if (StrUtil.isNotBlank(change.getToScheduleId())) scheduleIdList.add(change.getToScheduleId());
        }
        Map<String, CourseSchedule> scheduleMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(scheduleIdList)) {
            courseScheduleMapper.selectBatchIds(scheduleIdList).forEach(s -> scheduleMap.put(s.getId(), s));
        }
        Map<String, String> classNameMap = new HashMap<>();
        List<String> classIdList = scheduleMap.values().stream().map(CourseSchedule::getClassId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(classIdList)) {
            courseClassMapper.selectBatchIds(classIdList).forEach(c -> classNameMap.put(c.getId(), c.getName()));
        }
        for (CourseScheduleChange change : changeList) {
            CourseSchedule from = scheduleMap.get(change.getFromScheduleId());
            if (ObjectUtil.isNotEmpty(from)) {
                change.setFromDate(from.getScheduleDate() == null ? null : DateUtil.formatDate(from.getScheduleDate()));
                change.setFromTime(timeText(from));
                change.setFromClassName(classNameMap.get(from.getClassId()));
            }
            CourseSchedule to = scheduleMap.get(change.getToScheduleId());
            if (ObjectUtil.isNotEmpty(to)) {
                change.setToDate(to.getScheduleDate() == null ? null : DateUtil.formatDate(to.getScheduleDate()));
                change.setToTime(timeText(to));
                change.setToClassName(classNameMap.get(to.getClassId()));
            }
            if (StrUtil.isNotBlank(change.getCreateUser())) {
                cn.hutool.json.JSONObject user = sysUserApi.getUserByIdWithoutException(change.getCreateUser());
                change.setChangeUserName(user == null ? null : user.getStr("name"));
            }
        }
    }

    /**
     * 课节时间段文案
     */
    private String timeText(CourseSchedule schedule) {
        if (schedule.getStartTime() == null) {
            return null;
        }
        String start = String.valueOf(schedule.getStartTime());
        String end = schedule.getEndTime() == null ? "" : String.valueOf(schedule.getEndTime());
        if (start.length() > 5) start = start.substring(0, 5);
        if (end.length() > 5) end = end.substring(0, 5);
        return StrUtil.isBlank(end) ? start : start + "-" + end;
    }

    @Override
    public void add(CourseScheduleChange courseScheduleChange) {
        this.save(courseScheduleChange);
    }
}
