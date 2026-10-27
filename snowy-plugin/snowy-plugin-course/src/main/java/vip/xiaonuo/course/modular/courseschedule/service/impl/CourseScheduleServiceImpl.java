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
package vip.xiaonuo.course.modular.courseschedule.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.mapper.CourseMapper;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;
import vip.xiaonuo.course.modular.courseclassattend.entity.CourseClassAttend;
import vip.xiaonuo.course.modular.courseclassattend.mapper.CourseClassAttendMapper;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.service.CourseClassStudentService;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseTeacherUnavailable;
import vip.xiaonuo.course.modular.courseschedule.enums.CourseScheduleStatusEnum;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleMapper;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAddParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAdhocParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleChangeParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleGenerateParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleIdParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseSchedulePageParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleSuggestParam;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleConfigService;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;
import vip.xiaonuo.course.modular.courseschedulechange.entity.CourseScheduleChange;
import vip.xiaonuo.course.modular.courschedulestudent.service.CourseScheduleStudentService;
import vip.xiaonuo.course.modular.courseschedulechange.service.CourseScheduleChangeService;
import vip.xiaonuo.student.api.StudentNotifyApi;
import vip.xiaonuo.sys.api.SysUserApi;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.course.api.dto.CourseScheduleGenerateResultDTO;
import vip.xiaonuo.course.api.dto.CourseScheduleViewDTO;
import vip.xiaonuo.course.core.util.CourseScheduleAttendStatusUtil;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleWeekParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import vip.xiaonuo.auth.core.util.StpDataScopeUtil;
import vip.xiaonuo.auth.core.util.StpRoleUtil;

/**
 * 排课Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements CourseScheduleService {

    @Resource
    private CourseClassService courseClassService;

    @Resource
    private CourseClassMapper courseClassMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Resource
    private CourseScheduleChangeService courseScheduleChangeService;

    @Resource
    private CourseScheduleConfigService courseScheduleConfigService;

    @Resource
    private CourseClassStudentService courseClassStudentService;

    @Resource
    private BizEventApi bizEventApi;

    @Resource
    private CourseScheduleStudentService courseScheduleStudentService;

    @Resource
    private StudentNotifyApi studentNotifyApi;

    @Resource
    private CourseClassAttendMapper courseClassAttendMapper;

    @Override
    public Page<CourseSchedule> page(CourseSchedulePageParam courseSchedulePageParam) {
        QueryWrapper<CourseSchedule> queryWrapper = new QueryWrapper<CourseSchedule>().checkSqlInjection();
        // 数据范围：仅可见校区的排课
        List<String> visibleClassIdList = visibleClassIdList();
        // GET 查询参数为字符串日期，解析后再比较
        Date startDate = ObjectUtil.isNotEmpty(courseSchedulePageParam.getStartDate())
                ? cn.hutool.core.date.DateUtil.parse(courseSchedulePageParam.getStartDate()) : null;
        Date endDate = ObjectUtil.isNotEmpty(courseSchedulePageParam.getEndDate())
                ? cn.hutool.core.date.DateUtil.parse(courseSchedulePageParam.getEndDate()) : null;
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(courseSchedulePageParam.getClassId()), CourseSchedule::getClassId, courseSchedulePageParam.getClassId())
                .in(ObjectUtil.isNotEmpty(visibleClassIdList), CourseSchedule::getClassId, visibleClassIdList)
                .ge(startDate != null, CourseSchedule::getScheduleDate, startDate)
                .le(endDate != null, CourseSchedule::getScheduleDate, endDate)
                .orderByAsc(CourseSchedule::getScheduleDate)
                .orderByAsc(CourseSchedule::getStartTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    /**
     * 可见校区的班级id（机构数据范围）
     *
     * @return null 表示不限制；否则为可见班级id集合
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    /**
     * 取老师所属校区（直接排课时用于数据范围）
     */
    private String teacherCampusId(String teacherId) {
        if (StrUtil.isBlank(teacherId)) {
            return null;
        }
        cn.hutool.json.JSONObject user = sysUserApi.getUserByIdWithoutException(teacherId);
        return user == null ? null : user.getStr("orgId");
    }

    @Override
    public List<vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO> suggest(CourseScheduleSuggestParam param) {
        int duration = ObjectUtil.defaultIfNull(param.getDurationMinutes(), 90);
        int limit = ObjectUtil.defaultIfNull(param.getLimit(), 8);
        duration = Math.max(30, Math.min(duration, 240));
        LocalDate start = parseLocalDate(param.getStartDate(), LocalDate.now());
        LocalDate end = parseLocalDate(param.getEndDate(), start.plusDays(6));
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        // 最多看 14 天，避免一次扫太多
        if (end.isAfter(start.plusDays(13))) {
            end = start.plusDays(13);
        }
        Date rangeStart = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date rangeEnd = Date.from(end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        List<CourseSchedule> existList = this.list(new LambdaQueryWrapper<CourseSchedule>()
                .ge(CourseSchedule::getScheduleDate, rangeStart)
                .le(CourseSchedule::getScheduleDate, rangeEnd)
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue()));
        // 班级固定课表：命中星期与时间时优先推荐
        Map<Integer, List<LocalTime[]>> templateMap = new HashMap<>();
        if (StrUtil.isNotBlank(param.getClassId())) {
            CourseClass courseClass = courseClassMapper.selectById(param.getClassId());
            // 未显式指定教师时按班级默认教师判断占用，否则会推荐出教师已满的时段
            if (courseClass != null && StrUtil.isBlank(param.getTeacherId())) {
                param.setTeacherId(courseClass.getTeacherId());
            }
            if (courseClass != null && StrUtil.isNotBlank(courseClass.getWeekTemplateJson())) {
                for (Object item : JSONUtil.parseArray(courseClass.getWeekTemplateJson())) {
                    JSONObject template = (JSONObject) item;
                    int dayOfWeek = template.getInt("dayOfWeek", 0);
                    String startText = template.getStr("startTime");
                    String endText = template.getStr("endTime");
                    if (dayOfWeek < 1 || dayOfWeek > 7 || StrUtil.isBlank(startText) || StrUtil.isBlank(endText)) {
                        continue;
                    }
                    templateMap.computeIfAbsent(dayOfWeek, k -> new ArrayList<>())
                            .add(new LocalTime[]{LocalTime.parse(startText), LocalTime.parse(endText)});
                }
            }
        }
        // 停课日与教师不可用时段：推荐时一并排除
        java.util.Map<String, String> closureMap = courseScheduleConfigService.closureMap(rangeStart, rangeEnd);
        List<CourseTeacherUnavailable> unavailableList = StrUtil.isBlank(param.getTeacherId())
                ? java.util.Collections.emptyList()
                : courseScheduleConfigService.listUnavailableInRange(rangeStart, rangeEnd);
        List<vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO> resultList = new ArrayList<>();
        String[] weekText = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (closureMap.containsKey(date.toString())) {
                continue;
            }
            final LocalDate currentDate = date;
            List<CourseSchedule> dayList = existList.stream()
                    .filter(item -> item.getScheduleDate() != null
                            && item.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(currentDate))
                    .collect(Collectors.toList());
            int dayOfWeek = date.getDayOfWeek().getValue();
            List<LocalTime[]> templateRanges = templateMap.getOrDefault(dayOfWeek, List.of());
            long daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(start, date);
            for (int minutes = 9 * 60; minutes + duration <= 21 * 60; minutes += 30) {
                LocalTime slotStart = LocalTime.of(minutes / 60, minutes % 60);
                LocalTime slotEnd = slotStart.plusMinutes(duration);
                if (hasConflict(dayList, param, slotStart, slotEnd)) {
                    continue;
                }
                // 教师不可用：跳过重叠的候选
                if (StrUtil.isNotBlank(param.getTeacherId())) {
                    Date slotStartDate = Date.from(date.atTime(slotStart).atZone(ZoneId.systemDefault()).toInstant());
                    Date slotEndDate = Date.from(date.atTime(slotEnd).atZone(ZoneId.systemDefault()).toInstant());
                    boolean unavailableHit = unavailableList.stream()
                            .filter(item -> param.getTeacherId().equals(item.getTeacherId()))
                            .anyMatch(item -> item.getStartTime().before(slotEndDate)
                                    && item.getEndTime().after(slotStartDate));
                    if (unavailableHit) {
                        continue;
                    }
                }
                int score = 0;
                String reason;
                boolean inTemplate = templateRanges.stream().anyMatch(range ->
                        range[0].compareTo(slotStart) <= 0 && range[1].compareTo(slotEnd) >= 0);
                if (inTemplate) {
                    score += 40;
                    reason = "与班级固定课表一致";
                } else if (!templateRanges.isEmpty()) {
                    score += 12;
                    reason = "避开了固定课表时段";
                } else {
                    score += 20;
                    reason = "该时段全部空闲";
                }
                if (minutes < 12 * 60) {
                    score += 12;
                } else if (minutes < 17 * 60) {
                    score += 8;
                } else {
                    score += 5;
                }
                score += (int) Math.max(0, 7 - daysFromStart);
                vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO dto = new vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO();
                dto.setScheduleDate(date.toString());
                dto.setStartTime(slotStart.toString());
                dto.setEndTime(slotEnd.toString());
                dto.setLabel(date.getMonthValue() + "月" + date.getDayOfMonth() + "日 "
                        + weekText[dayOfWeek - 1] + " " + slotStart + "-" + slotEnd);
                dto.setScore(score);
                dto.setReason(reason);
                resultList.add(dto);
            }
        }
        resultList.sort((a, b) -> b.getScore().compareTo(a.getScore()) != 0
                ? b.getScore().compareTo(a.getScore())
                : (a.getScheduleDate() + a.getStartTime()).compareTo(b.getScheduleDate() + b.getStartTime()));
        // 按分数从高到低挑选，同一天内互相重叠的候选只保留分更高的那个
        Map<String, List<LocalTime[]>> pickedMap = new HashMap<>();
        List<vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO> selectedList = new ArrayList<>();
        for (vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO dto : resultList) {
            LocalTime slotStart = LocalTime.parse(dto.getStartTime());
            LocalTime slotEnd = LocalTime.parse(dto.getEndTime());
            List<LocalTime[]> pickedRanges = pickedMap.computeIfAbsent(dto.getScheduleDate(), k -> new ArrayList<>());
            boolean overlapped = pickedRanges.stream()
                    .anyMatch(range -> range[0].isBefore(slotEnd) && range[1].isAfter(slotStart));
            if (overlapped) {
                continue;
            }
            pickedRanges.add(new LocalTime[]{slotStart, slotEnd});
            selectedList.add(dto);
            if (selectedList.size() >= Math.max(limit, 1)) {
                break;
            }
        }
        return selectedList;
    }

    /** 该时段是否与班级/教师/教室冲突 */
    private boolean hasConflict(List<CourseSchedule> dayList, CourseScheduleSuggestParam param,
                                LocalTime startTime, LocalTime endTime) {
        for (CourseSchedule exist : dayList) {
            if (exist.getStartTime() == null || exist.getEndTime() == null) {
                continue;
            }
            boolean overlap = exist.getStartTime().isBefore(endTime) && exist.getEndTime().isAfter(startTime);
            if (!overlap) {
                continue;
            }
            if (StrUtil.isNotBlank(param.getClassId()) && param.getClassId().equals(exist.getClassId())) {
                return true;
            }
            if (StrUtil.isNotBlank(param.getTeacherId()) && param.getTeacherId().equals(exist.getTeacherId())) {
                return true;
            }
            if (StrUtil.isNotBlank(param.getClassroom()) && param.getClassroom().equals(exist.getClassroom())) {
                return true;
            }
        }
        return false;
    }

    /** 教室名比较：忽略首尾空格与大小写，避免 B201 / b201 被当成两个教室 */
    private boolean sameClassroom(String left, String right) {
        return StrUtil.isNotBlank(left) && StrUtil.equalsIgnoreCase(StrUtil.trim(left), StrUtil.trim(right));
    }

    /** 解析 yyyy-MM-dd */
    private LocalDate parseLocalDate(String text, LocalDate defaultDate) {
        if (StrUtil.isBlank(text)) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
        } catch (Exception e) {
            return defaultDate;
        }
    }

    private List<String> visibleClassIdList() {
        List<String> visibleCampusIds = StpDataScopeUtil.visibleOrgIds();
        if (visibleCampusIds == null) {
            return null;
        }
        if (visibleCampusIds.isEmpty()) {
            return List.of("__NO_DATA__");
        }
        return courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .select(CourseClass::getId)
                        .in(CourseClass::getCampusId, visibleCampusIds))
                .stream().map(CourseClass::getId).collect(java.util.stream.Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseScheduleAddParam courseScheduleAddParam) {
        String closureName = courseScheduleConfigService.closureNameOf(courseScheduleAddParam.getScheduleDate());
        if (StrUtil.isNotBlank(closureName)) {
            throw new CommonException("{} 是停课日【{}】，不能排课", 
                    cn.hutool.core.date.DateUtil.formatDate(courseScheduleAddParam.getScheduleDate()), closureName);
        }
        List<String> conflicts = this.conflictCheck(courseScheduleAddParam.getClassId(), courseScheduleAddParam.getScheduleDate(),
                courseScheduleAddParam.getStartTime(), courseScheduleAddParam.getEndTime(),
                courseScheduleAddParam.getClassroom(), courseScheduleAddParam.getTeacherId(), null);
        if (ObjectUtil.isNotEmpty(conflicts)) {
            throw new CommonException("排课存在冲突：{}", String.join("；", conflicts));
        }
        CourseSchedule courseSchedule = BeanUtil.toBean(courseScheduleAddParam, CourseSchedule.class);
        courseSchedule.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
        try {
            this.save(courseSchedule);
        } catch (DuplicateKeyException e) {
            // 唯一约束兜住并发：班级/教师/教室任一被他人抢先占用
            String detail = StrUtil.blankToDefault(e.getMessage(), "");
            String slotText = cn.hutool.core.date.DateUtil.formatDate(courseScheduleAddParam.getScheduleDate())
                    + " " + courseScheduleAddParam.getStartTime();
            if (detail.contains("UK_SCHEDULE_ACTIVE_TEACHER")) {
                throw new CommonException("该教师在 {} 已有排课（可能有其他人在同时排课），请刷新后重试", slotText);
            }
            if (detail.contains("UK_SCHEDULE_ACTIVE_ROOM")) {
                throw new CommonException("该教室在 {} 已被占用（可能有其他人在同时排课），请刷新后重试", slotText);
            }
            throw new CommonException("该班级在 {} 已有排课（可能有其他人在同时排课），请刷新后重试", slotText);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CourseScheduleGenerateResultDTO generate(CourseScheduleGenerateParam courseScheduleGenerateParam) {
        CourseScheduleGenerateResultDTO result = new CourseScheduleGenerateResultDTO();
        // 校验班级及其周课表模板
        CourseClass courseClass = courseClassService.queryEntity(courseScheduleGenerateParam.getClassId());
        if (ObjectUtil.isEmpty(courseClass.getWeekTemplateJson())) {
            throw new CommonException("班级【{}】未配置周课表模板", courseClass.getName());
        }
        JSONArray templateList = JSONUtil.parseArray(courseClass.getWeekTemplateJson());
        if (ObjectUtil.isEmpty(templateList)) {
            throw new CommonException("班级【{}】周课表模板为空", courseClass.getName());
        }
        Integer weekCount = courseScheduleGenerateParam.getWeekCount();
        if (ObjectUtil.isEmpty(weekCount) || weekCount <= 0 || weekCount > 26) {
            throw new CommonException("生成周数需在1-26之间");
        }
        // 以开始日期所在周的周一为基准
        LocalDate startLocalDate = courseScheduleGenerateParam.getStartDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate monday = startLocalDate.with(DayOfWeek.MONDAY);
        LocalDate lastDay = monday.plusWeeks(weekCount - 1).plusDays(6);
        // 一次性载入区间内已有排课，冲突检测走内存比对（避免逐条查库）
        List<CourseSchedule> existList = this.list(new LambdaQueryWrapper<CourseSchedule>()
                .ge(CourseSchedule::getScheduleDate, Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .le(CourseSchedule::getScheduleDate, Date.from(lastDay.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue()));
        Set<String> existKeySet = new HashSet<>();
        for (CourseSchedule exist : existList) {
            existKeySet.add(scheduleKey(exist.getClassId(), exist.getScheduleDate(), exist.getStartTime(), exist.getEndTime()));
        }
        // 冲突提示需要写清「和谁撞了」：先批量取已有课节的班级名与模板里涉及的教师名
        Map<String, String> existClassNameMap = new HashMap<>();
        List<String> existClassIdList = existList.stream().map(CourseSchedule::getClassId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(existClassIdList)) {
            courseClassMapper.selectBatchIds(existClassIdList)
                    .forEach(item -> existClassNameMap.put(item.getId(), item.getName()));
        }
        Map<String, String> teacherNameMap = new HashMap<>();
        List<String> templateTeacherIdList = templateList.stream()
                .map(item -> ((JSONObject) item).getStr("teacherId"))
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (StrUtil.isNotBlank(courseClass.getTeacherId())) {
            templateTeacherIdList.add(courseClass.getTeacherId());
        }
        if (ObjectUtil.isNotEmpty(templateTeacherIdList)) {
            sysUserApi.getUserListByIdListWithoutException(templateTeacherIdList)
                    .forEach(user -> teacherNameMap.put(user.getStr("id"), user.getStr("name")));
        }
        // 停课日与教师不可用时段：一次载入区间内数据，循环里内存判断
        java.util.Map<String, String> closureMap = courseScheduleConfigService.closureMap(
                Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(lastDay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        List<CourseTeacherUnavailable> unavailableList = courseScheduleConfigService.listUnavailableInRange(
                Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(lastDay.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        List<CourseSchedule> toSaveList = new ArrayList<>();
        List<String> allConflicts = new ArrayList<>();
        int plannedCount = 0;
        int skippedCount = 0;
        for (int week = 0; week < weekCount; week++) {
            for (int i = 0; i < templateList.size(); i++) {
                JSONObject template = templateList.getJSONObject(i);
                int dayOfWeek = template.getInt("dayOfWeek");
                if (dayOfWeek < 1 || dayOfWeek > 7) {
                    throw new CommonException("周课表模板星期配置非法：{}", dayOfWeek);
                }
                LocalDate date = monday.plusWeeks(week).plusDays(dayOfWeek - 1);
                LocalTime startTime = LocalTime.parse(template.getStr("startTime"));
                LocalTime endTime = LocalTime.parse(template.getStr("endTime"));
                // 课表模板未填教室/教师时，回退到班级默认配置
                String classroom = ObjectUtil.isNotEmpty(template.getStr("classroom"))
                        ? template.getStr("classroom") : courseClass.getClassroom();
                String teacherId = ObjectUtil.isNotEmpty(template.getStr("teacherId"))
                        ? template.getStr("teacherId") : courseClass.getTeacherId();
                Date scheduleDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                plannedCount++;
                // 幂等：与已有排课完全一致时跳过，不重复生成
                if (existKeySet.contains(scheduleKey(courseScheduleGenerateParam.getClassId(), scheduleDate, startTime, endTime))) {
                    skippedCount++;
                    continue;
                }
                // 停课日：跳过，不算冲突
                String closureName = closureMap.get(cn.hutool.core.date.DateUtil.formatDate(scheduleDate));
                if (StrUtil.isNotBlank(closureName)) {
                    skippedCount++;
                    result.getSkipReasons().add(cn.hutool.core.date.DateUtil.formatDate(scheduleDate)
                            + " 停课日【" + closureName + "】，已跳过");
                    continue;
                }
                // 与区间内已有排课冲突（教室 / 教师 / 班级同时段重叠）
                String slotText = cn.hutool.core.date.DateUtil.formatDate(scheduleDate) + " " + startTime + "-" + endTime;
                String teacherName = StrUtil.blankToDefault(teacherNameMap.get(teacherId), teacherId);
                for (CourseSchedule exist : existList) {
                    if (!sameDate(exist.getScheduleDate(), scheduleDate)) {
                        continue;
                    }
                    if (!(exist.getStartTime().isBefore(endTime) && exist.getEndTime().isAfter(startTime))) {
                        continue;
                    }
                    String existClassName = existClassNameMap.get(exist.getClassId());
                    String existDesc = StrUtil.isBlank(existClassName) ? "其他课程" : "【" + existClassName + "】";
                    if (sameClassroom(classroom, exist.getClassroom())) {
                        allConflicts.add(slotText + " 教室【" + classroom + "】已被" + existDesc + "占用");
                    }
                    if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(exist.getTeacherId())) {
                        allConflicts.add(slotText + " 教师【" + teacherName + "】已有排课" + existDesc);
                    }
                    if (courseScheduleGenerateParam.getClassId().equals(exist.getClassId())) {
                        allConflicts.add(slotText + " 班级【" + courseClass.getName() + "】已有排课");
                    }
                }
                // 教师不可用时段
                if (StrUtil.isNotBlank(teacherId)) {
                    Date slotStart = Date.from(date.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant());
                    Date slotEnd = Date.from(date.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant());
                    for (CourseTeacherUnavailable unavailable : unavailableList) {
                        if (!teacherId.equals(unavailable.getTeacherId())) {
                            continue;
                        }
                        if (unavailable.getStartTime().before(slotEnd) && unavailable.getEndTime().after(slotStart)) {
                            allConflicts.add(slotText + " 教师【" + teacherName + "】不可用"
                                    + (StrUtil.isBlank(unavailable.getReason()) ? "" : "（" + unavailable.getReason() + "）"));
                        }
                    }
                }
                // 与同批生成排课冲突（教室 / 教师）
                for (CourseSchedule planned : toSaveList) {
                    if (!sameDate(planned.getScheduleDate(), scheduleDate)) {
                        continue;
                    }
                    if (!(planned.getStartTime().isBefore(endTime) && planned.getEndTime().isAfter(startTime))) {
                        continue;
                    }
                    if (sameClassroom(classroom, planned.getClassroom())) {
                        allConflicts.add(slotText + " 教室【" + classroom + "】与同批生成的其他课节冲突");
                    }
                    if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(planned.getTeacherId())) {
                        allConflicts.add(slotText + " 教师【" + teacherName + "】与同批生成的其他课节冲突");
                    }
                }
                CourseSchedule courseSchedule = new CourseSchedule();
                courseSchedule.setClassId(courseScheduleGenerateParam.getClassId());
                courseSchedule.setCourseId(courseClass.getCourseId());
                courseSchedule.setScheduleDate(scheduleDate);
                courseSchedule.setStartTime(startTime);
                courseSchedule.setEndTime(endTime);
                courseSchedule.setClassroom(classroom);
                courseSchedule.setTeacherId(teacherId);
                courseSchedule.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
                toSaveList.add(courseSchedule);
            }
        }
        result.setPlannedCount(plannedCount);
        result.setSkippedCount(skippedCount);
        result.setConflicts(new ArrayList<>(new HashSet<>(allConflicts)));
        // 预览模式：只给出将生成的结果与冲突，不落库
        if (Boolean.TRUE.equals(courseScheduleGenerateParam.getPreview())) {
            result.setPreview(Boolean.TRUE);
            result.setCreatedCount(toSaveList.size());
            return result;
        }
        // 埋点：批量生成排课
        bizEventApi.log(BizEventCodeEnum.SCHEDULE_GENERATE.getValue(), "schedule", courseClass.getId(), courseClass.getName(),
                Map.of("计划", String.valueOf(result.getPlannedCount()), "新增", String.valueOf(result.getCreatedCount()),
                        "跳过", String.valueOf(result.getSkippedCount())));
        // 任一冲突整体失败（不落库）
        if (ObjectUtil.isNotEmpty(allConflicts)) {
            throw new CommonException("存在排课冲突，未生成：{}", String.join("；", result.getConflicts()));
        }
        if (ObjectUtil.isNotEmpty(toSaveList)) {
            try {
                this.saveBatch(toSaveList);
            } catch (DuplicateKeyException e) {
                throw new CommonException("部分时段已被其他排课占用，请重新预览后再生成");
            }
        }
        result.setCreatedCount(toSaveList.size());
        return result;
    }

    /**
     * 排课唯一键（班级 + 日期 + 起止时间），用于生成幂等判断
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private String scheduleKey(String classId, Date scheduleDate, LocalTime startTime, LocalTime endTime) {
        return classId + "|" + DateUtil.formatDate(scheduleDate) + "|" + startTime + "|" + endTime;
    }

    /**
     * 是否同一天
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private boolean sameDate(Date left, Date right) {
        return left != null && right != null && DateUtil.formatDate(left).equals(DateUtil.formatDate(right));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void change(CourseScheduleChangeParam courseScheduleChangeParam) {
        CourseSchedule from = this.queryEntity(courseScheduleChangeParam.getScheduleId());
        if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(from.getStatus())) {
            throw new CommonException("排课当前状态【{}】不允许调课", from.getStatus());
        }
        String closureName = courseScheduleConfigService.closureNameOf(courseScheduleChangeParam.getScheduleDate());
        if (StrUtil.isNotBlank(closureName)) {
            throw new CommonException("目标日期 {} 是停课日【{}】，不能调课",
                    cn.hutool.core.date.DateUtil.formatDate(courseScheduleChangeParam.getScheduleDate()), closureName);
        }
        List<String> conflicts = this.conflictCheck(from.getClassId(), courseScheduleChangeParam.getScheduleDate(),
                courseScheduleChangeParam.getStartTime(), courseScheduleChangeParam.getEndTime(),
                courseScheduleChangeParam.getClassroom(), courseScheduleChangeParam.getTeacherId(), from.getId());
        if (ObjectUtil.isNotEmpty(conflicts)) {
            throw new CommonException("调课存在冲突：{}", String.join("；", conflicts));
        }
        // 原排课置已调（保留历史，供流水/报表追溯）
        from.setStatus(CourseScheduleStatusEnum.CHANGED.getValue());
        this.updateById(from);
        // 生成新排课
        CourseSchedule to = new CourseSchedule();
        to.setClassId(from.getClassId());
        to.setScheduleDate(courseScheduleChangeParam.getScheduleDate());
        to.setStartTime(courseScheduleChangeParam.getStartTime());
        to.setEndTime(courseScheduleChangeParam.getEndTime());
        to.setClassroom(courseScheduleChangeParam.getClassroom());
        to.setTeacherId(courseScheduleChangeParam.getTeacherId());
        to.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
        try {
            this.save(to);
        } catch (DuplicateKeyException e) {
            throw new CommonException("调课目标时段已被本班其他排课占用，请刷新后重试");
        }
        // 已点名的课节调课：把出勤记录迁移到新课节（课时不重复扣，出勤跟着课节走）
        List<CourseClassAttend> attendList = courseClassAttendMapper.selectList(
                new LambdaQueryWrapper<CourseClassAttend>().eq(CourseClassAttend::getScheduleId, from.getId()));
        for (CourseClassAttend attend : attendList) {
            attend.setScheduleId(to.getId());
        }
        for (CourseClassAttend attend : attendList) {
            courseClassAttendMapper.updateById(attend);
        }
        // 调课记录
        CourseScheduleChange change = new CourseScheduleChange();
        change.setFromScheduleId(from.getId());
        change.setToScheduleId(to.getId());
        change.setChangeReason(courseScheduleChangeParam.getChangeReason());
        courseScheduleChangeService.add(change);
        bizEventApi.log(BizEventCodeEnum.SCHEDULE_CHANGE.getValue(), "schedule", to.getId(),
                courseClassService.queryEntity(from.getClassId()).getName(),
                Map.of("原时间", String.valueOf(from.getScheduleDate()) + " " + from.getStartTime(),
                        "新时间", String.valueOf(to.getScheduleDate()) + " " + to.getStartTime(),
                        "原因", String.valueOf(courseScheduleChangeParam.getChangeReason())));
        // 调课通知落库（含已点名学员：点名与扣课保持原样）
        List<CourseClassStudent> studentList = courseClassStudentService.listByClassId(from.getClassId());
        for (CourseClassStudent classStudent : studentList) {
            studentNotifyApi.log("SCHEDULE_CHANGE", "PARENT", classStudent.getStudentId(), classStudent.getStudentId(),
                    "调课通知", "课程调至" + DateUtil.formatDate(to.getScheduleDate()) + " "
                            + to.getStartTime() + "-" + to.getEndTime()
                            + (ObjectUtil.isNotEmpty(to.getClassroom()) ? "，教室：" + to.getClassroom() : "")
                            + (ObjectUtil.isNotEmpty(courseScheduleChangeParam.getChangeReason())
                            ? "，原因：" + courseScheduleChangeParam.getChangeReason() : ""),
                    "IN_APP");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(CourseScheduleIdParam courseScheduleIdParam) {
        CourseSchedule courseSchedule = this.queryEntity(courseScheduleIdParam.getId());
        bizEventApi.log(BizEventCodeEnum.SCHEDULE_CANCEL.getValue(), "schedule", courseSchedule.getId(), null, null);
        if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(courseSchedule.getStatus())) {
            throw new CommonException("排课当前状态【{}】不允许取消", courseSchedule.getStatus());
        }
        // 已产生点名记录的排课不允许取消（先处理点名）
        Long attendCount = courseClassAttendMapper.selectCount(new LambdaQueryWrapper<CourseClassAttend>()
                .eq(CourseClassAttend::getScheduleId, courseSchedule.getId()));
        if (attendCount > 0) {
            throw new CommonException("该排课已点名，请先撤销点名记录再取消");
        }
        courseSchedule.setStatus(CourseScheduleStatusEnum.CANCELED.getValue());
        this.updateById(courseSchedule);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public vip.xiaonuo.course.api.dto.CourseScheduleCancelResultDTO cancelBatch(List<String> idList) {
        vip.xiaonuo.course.api.dto.CourseScheduleCancelResultDTO result =
                new vip.xiaonuo.course.api.dto.CourseScheduleCancelResultDTO();
        if (ObjectUtil.isEmpty(idList)) {
            return result;
        }
        List<String> distinctIdList = idList.stream()
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, CourseSchedule> scheduleMap = this.listByIds(distinctIdList).stream()
                .collect(Collectors.toMap(CourseSchedule::getId, item -> item));
        // 一次查出哪些课节已经点过名，避免逐条查库
        Set<String> attendedIdSet = courseClassAttendMapper.selectList(new LambdaQueryWrapper<CourseClassAttend>()
                        .select(CourseClassAttend::getScheduleId)
                        .in(CourseClassAttend::getScheduleId, distinctIdList))
                .stream().map(CourseClassAttend::getScheduleId)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        int canceledCount = 0;
        for (String id : distinctIdList) {
            CourseSchedule schedule = scheduleMap.get(id);
            if (schedule == null) {
                result.getSkippedList().add("课节不存在：" + id);
                continue;
            }
            String label = cn.hutool.core.date.DateUtil.formatDate(schedule.getScheduleDate())
                    + " " + schedule.getStartTime() + "-" + schedule.getEndTime();
            if (!CourseScheduleStatusEnum.NORMAL.getValue().equals(schedule.getStatus())) {
                result.getSkippedList().add(label + " 当前状态不允许取消");
                continue;
            }
            if (attendedIdSet.contains(id)) {
                result.getSkippedList().add(label + " 已点名，请先撤销点名记录");
                continue;
            }
            schedule.setStatus(CourseScheduleStatusEnum.CANCELED.getValue());
            this.updateById(schedule);
            canceledCount++;
        }
        result.setCanceledCount(canceledCount);
        result.setSkippedCount(result.getSkippedList().size());
        if (canceledCount > 0) {
            bizEventApi.log(BizEventCodeEnum.SCHEDULE_CANCEL.getValue(), "schedule", null, "批量取消排课",
                    Map.of("数量", String.valueOf(canceledCount)));
        }
        return result;
    }

    @Override
    public CourseSchedule detail(CourseScheduleIdParam courseScheduleIdParam) {
        return this.queryEntity(courseScheduleIdParam.getId());
    }

    @Override
    public CourseSchedule queryEntity(String id) {
        CourseSchedule courseSchedule = this.getById(id);
        if (ObjectUtil.isEmpty(courseSchedule)) {
            throw new CommonException("排课不存在，id值为：{}", id);
        }
        return courseSchedule;
    }

    @Override
    public List<String> conflictCheck(String classId, Date scheduleDate, LocalTime startTime, LocalTime endTime,
                                      String classroom, String teacherId, String excludeScheduleId) {
        // 同日同时段重叠的"正常"排课
        LambdaQueryWrapper<CourseSchedule> queryWrapper = new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getScheduleDate, scheduleDate)
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue())
                .lt(CourseSchedule::getStartTime, endTime)
                .gt(CourseSchedule::getEndTime, startTime);
        if (ObjectUtil.isNotEmpty(excludeScheduleId)) {
            queryWrapper.ne(CourseSchedule::getId, excludeScheduleId);
        }
        List<CourseSchedule> existList = this.list(queryWrapper);
        // 不能因为「同一时段没有其他排课」就提前返回：教师不可用时段也要检查
        Set<String> conflictSet = new HashSet<>();
        String slotText = cn.hutool.core.date.DateUtil.formatDate(scheduleDate) + " "
                + startTime + "-" + endTime;
        // 冲突提示要能定位到具体对象，否则用户只能自己去周视图里找
        Map<String, String> classNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(existList)) {
            List<String> classIdList = existList.stream().map(CourseSchedule::getClassId)
                    .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(classIdList)) {
                courseClassMapper.selectBatchIds(classIdList)
                        .forEach(item -> classNameMap.put(item.getId(), item.getName()));
            }
        }
        String teacherName = null;
        if (ObjectUtil.isNotEmpty(teacherId)) {
            cn.hutool.json.JSONObject teacher = sysUserApi.getUserByIdWithoutException(teacherId);
            teacherName = teacher == null ? teacherId : teacher.getStr("name");
        }
        for (CourseSchedule exist : existList) {
            String existClassName = classNameMap.get(exist.getClassId());
            String existSlot = cn.hutool.core.date.DateUtil.formatDate(exist.getScheduleDate()) + " "
                    + exist.getStartTime() + "-" + exist.getEndTime();
            if (sameClassroom(classroom, exist.getClassroom())) {
                conflictSet.add("教室【" + classroom + "】在 " + existSlot + " 已被"
                        + (StrUtil.isBlank(existClassName) ? "其他班级" : "【" + existClassName + "】") + "占用");
            }
            if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(exist.getTeacherId())) {
                conflictSet.add("教师【" + teacherName + "】在 " + existSlot + " 已有排课"
                        + (StrUtil.isBlank(existClassName) ? "" : "（" + existClassName + "）"));
            }
            if (ObjectUtil.isNotEmpty(classId) && classId.equals(exist.getClassId())) {
                conflictSet.add("班级【" + StrUtil.blankToDefault(classNameMap.get(classId), classId) + "】在 "
                        + slotText + " 已有排课");
            }
        }
        // 教师不可用时段（请假、教研等）
        if (ObjectUtil.isNotEmpty(teacherId)) {
            LocalDate day = scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            Date slotStart = Date.from(day.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant());
            Date slotEnd = Date.from(day.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant());
            for (CourseTeacherUnavailable unavailable : courseScheduleConfigService
                    .listUnavailable(teacherId, slotStart, slotEnd)) {
                conflictSet.add("教师【" + teacherName + "】在 " + slotText + " 不可用"
                        + (StrUtil.isBlank(unavailable.getReason()) ? "" : "（" + unavailable.getReason() + "）"));
            }
        }
        return new ArrayList<>(conflictSet);
    }

    /**
     * 直接排课（不挂班级）
     * <p>
     * 老师带一群学生上课时无需先建班级：直接指定课程、老师、教室与这节课的学员名单，
     * 按周模板展开生成排课；点名名单 = 本表学员；消课按课程扣减学员的课时账户。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CourseScheduleGenerateResultDTO generateAdhoc(CourseScheduleAdhocParam courseScheduleAdhocParam) {
        CourseScheduleGenerateResultDTO result = new CourseScheduleGenerateResultDTO();
        if (ObjectUtil.isEmpty(courseScheduleAdhocParam.getStudentIdList())) {
            throw new CommonException("请选择上课学员");
        }
        if (ObjectUtil.isEmpty(courseScheduleAdhocParam.getWeekTemplateJson())) {
            throw new CommonException("请配置每周上课时段");
        }
        JSONArray templateList = JSONUtil.parseArray(courseScheduleAdhocParam.getWeekTemplateJson());
        if (ObjectUtil.isEmpty(templateList)) {
            throw new CommonException("每周上课时段为空");
        }
        Integer weekCount = courseScheduleAdhocParam.getWeekCount();
        if (ObjectUtil.isEmpty(weekCount) || weekCount <= 0 || weekCount > 26) {
            throw new CommonException("生成周数需在1-26之间");
        }
        LocalDate startLocalDate = courseScheduleAdhocParam.getStartDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate monday = startLocalDate.with(DayOfWeek.MONDAY);
        LocalDate lastDay = monday.plusWeeks(weekCount - 1).plusDays(6);
        List<CourseSchedule> existList = this.list(new LambdaQueryWrapper<CourseSchedule>()
                .ge(CourseSchedule::getScheduleDate, Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .le(CourseSchedule::getScheduleDate, Date.from(lastDay.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue()));
        Set<String> existKeySet = new HashSet<>();
        for (CourseSchedule exist : existList) {
            existKeySet.add(scheduleKey(null, exist.getScheduleDate(), exist.getStartTime(), exist.getEndTime()));
        }
        // 冲突提示带上教师名
        Map<String, String> teacherNameMap = new HashMap<>();
        List<String> templateTeacherIdList = templateList.stream()
                .map(item -> ((JSONObject) item).getStr("teacherId"))
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (StrUtil.isNotBlank(courseScheduleAdhocParam.getTeacherId())) {
            templateTeacherIdList.add(courseScheduleAdhocParam.getTeacherId());
        }
        if (ObjectUtil.isNotEmpty(templateTeacherIdList)) {
            sysUserApi.getUserListByIdListWithoutException(templateTeacherIdList)
                    .forEach(user -> teacherNameMap.put(user.getStr("id"), user.getStr("name")));
        }
        // 停课日与教师不可用时段
        java.util.Map<String, String> closureMap = courseScheduleConfigService.closureMap(
                Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(lastDay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        List<CourseTeacherUnavailable> unavailableList = courseScheduleConfigService.listUnavailableInRange(
                Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(lastDay.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        List<CourseSchedule> toSaveList = new ArrayList<>();
        List<String> allConflicts = new ArrayList<>();
        int plannedCount = 0;
        int skippedCount = 0;
        for (int week = 0; week < weekCount; week++) {
            for (int i = 0; i < templateList.size(); i++) {
                JSONObject template = templateList.getJSONObject(i);
                int dayOfWeek = template.getInt("dayOfWeek");
                if (dayOfWeek < 1 || dayOfWeek > 7) {
                    throw new CommonException("上课时段星期配置非法：{}", dayOfWeek);
                }
                LocalDate date = monday.plusWeeks(week).plusDays(dayOfWeek - 1);
                LocalTime startTime = LocalTime.parse(template.getStr("startTime"));
                LocalTime endTime = LocalTime.parse(template.getStr("endTime"));
                String classroom = ObjectUtil.isNotEmpty(template.getStr("classroom"))
                        ? template.getStr("classroom") : courseScheduleAdhocParam.getClassroom();
                String teacherId = ObjectUtil.isNotEmpty(template.getStr("teacherId"))
                        ? template.getStr("teacherId") : courseScheduleAdhocParam.getTeacherId();
                Date scheduleDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                plannedCount++;
                if (existKeySet.contains(scheduleKey(null, scheduleDate, startTime, endTime))) {
                    skippedCount++;
                    continue;
                }
                String closureName = closureMap.get(cn.hutool.core.date.DateUtil.formatDate(scheduleDate));
                if (StrUtil.isNotBlank(closureName)) {
                    skippedCount++;
                    result.getSkipReasons().add(cn.hutool.core.date.DateUtil.formatDate(scheduleDate)
                            + " 停课日【" + closureName + "】，已跳过");
                    continue;
                }
                String slotText = cn.hutool.core.date.DateUtil.formatDate(scheduleDate) + " " + startTime + "-" + endTime;
                String teacherName = StrUtil.blankToDefault(teacherNameMap.get(teacherId), teacherId);
                for (CourseSchedule exist : existList) {
                    if (!sameDate(exist.getScheduleDate(), scheduleDate)) {
                        continue;
                    }
                    if (!(exist.getStartTime().isBefore(endTime) && exist.getEndTime().isAfter(startTime))) {
                        continue;
                    }
                    if (sameClassroom(classroom, exist.getClassroom())) {
                        allConflicts.add(slotText + " 教室【" + classroom + "】已被其他课节占用");
                    }
                    if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(exist.getTeacherId())) {
                        allConflicts.add(slotText + " 教师【" + teacherName + "】已有排课");
                    }
                }
                if (StrUtil.isNotBlank(teacherId)) {
                    Date slotStart = Date.from(date.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant());
                    Date slotEnd = Date.from(date.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant());
                    for (CourseTeacherUnavailable unavailable : unavailableList) {
                        if (!teacherId.equals(unavailable.getTeacherId())) {
                            continue;
                        }
                        if (unavailable.getStartTime().before(slotEnd) && unavailable.getEndTime().after(slotStart)) {
                            allConflicts.add(slotText + " 教师【" + teacherName + "】不可用"
                                    + (StrUtil.isBlank(unavailable.getReason()) ? "" : "（" + unavailable.getReason() + "）"));
                        }
                    }
                }
                for (CourseSchedule planned : toSaveList) {
                    if (!sameDate(planned.getScheduleDate(), scheduleDate)) {
                        continue;
                    }
                    if (!(planned.getStartTime().isBefore(endTime) && planned.getEndTime().isAfter(startTime))) {
                        continue;
                    }
                    if (sameClassroom(classroom, planned.getClassroom())) {
                        allConflicts.add(slotText + " 教室【" + classroom + "】与同批生成的其他课节冲突");
                    }
                    if (ObjectUtil.isNotEmpty(teacherId) && teacherId.equals(planned.getTeacherId())) {
                        allConflicts.add(slotText + " 教师【" + teacherName + "】与同批生成的其他课节冲突");
                    }
                }
                CourseSchedule courseSchedule = new CourseSchedule();
                courseSchedule.setClassId(null);
                courseSchedule.setCourseId(courseScheduleAdhocParam.getCourseId());
                courseSchedule.setCampusId(teacherCampusId(courseScheduleAdhocParam.getTeacherId()));
                courseSchedule.setScheduleDate(scheduleDate);
                courseSchedule.setStartTime(startTime);
                courseSchedule.setEndTime(endTime);
                courseSchedule.setClassroom(classroom);
                courseSchedule.setTeacherId(teacherId);
                courseSchedule.setStatus(CourseScheduleStatusEnum.NORMAL.getValue());
                toSaveList.add(courseSchedule);
            }
        }
        result.setPlannedCount(plannedCount);
        result.setSkippedCount(skippedCount);
        result.setConflicts(new ArrayList<>(new HashSet<>(allConflicts)));
        if (ObjectUtil.isNotEmpty(allConflicts)) {
            throw new CommonException("存在排课冲突，未生成：{}", String.join("；", result.getConflicts()));
        }
        if (ObjectUtil.isEmpty(toSaveList)) {
            result.setCreatedCount(0);
            return result;
        }
        try {
            this.saveBatch(toSaveList);
        } catch (DuplicateKeyException e) {
            throw new CommonException("部分时段已被其他排课占用，请重新预览后再生成");
        }
        // 绑定这节课的学员名单
        for (CourseSchedule schedule : toSaveList) {
            courseScheduleStudentService.saveBatchStudent(schedule.getId(), courseScheduleAdhocParam.getStudentIdList());
        }
        result.setCreatedCount(toSaveList.size());
        bizEventApi.log(BizEventCodeEnum.SCHEDULE_GENERATE.getValue(), "schedule", null, "直接排课",
                Map.of("计划", String.valueOf(result.getPlannedCount()), "新增", String.valueOf(result.getCreatedCount()),
                        "学员", String.valueOf(courseScheduleAdhocParam.getStudentIdList().size())));
        return result;
    }

    /**
     * 某节课的上课名单：班级在读学员 ∪ 排课直接指定学员
     */
    @Override
    public List<String> listRosterStudentIds(String scheduleId) {
        Set<String> studentIdSet = new LinkedHashSet<>();
        if (StrUtil.isBlank(scheduleId)) {
            return new ArrayList<>();
        }
        CourseSchedule schedule = this.getById(scheduleId);
        if (ObjectUtil.isNotEmpty(schedule) && StrUtil.isNotBlank(schedule.getClassId())) {
            for (CourseClassStudent classStudent : courseClassStudentService.listByClassId(schedule.getClassId())) {
                studentIdSet.add(classStudent.getStudentId());
            }
        }
        studentIdSet.addAll(courseScheduleStudentService.listStudentIdsByScheduleId(scheduleId));
        return new ArrayList<>(studentIdSet);
    }

    @Override
    public List<CourseScheduleViewDTO> week(CourseScheduleWeekParam courseScheduleWeekParam) {
        Date start = cn.hutool.core.date.DateUtil.beginOfDay(parseDate(courseScheduleWeekParam.getStartDate(), defaultWeekStart()));
        Date end = cn.hutool.core.date.DateUtil.endOfDay(parseDate(courseScheduleWeekParam.getEndDate(), defaultWeekEnd()));
        List<String> visibleClassIdList = visibleClassIdList();
        List<String> visibleCampusIds = StpDataScopeUtil.visibleOrgIds();
        // 岗位数据范围：授课老师只看自己带的课
        // （课节上的授课人 = 自己，或自己带的班级下的课节）
        String selfTeacherId = StpRoleUtil.hasRole(StpRoleUtil.ROLE_TEACHER) ? StpRoleUtil.currentUserId() : null;
        List<String> selfClassIdList = StrUtil.isBlank(selfTeacherId) ? List.of()
                : courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .select(CourseClass::getId)
                        .eq(CourseClass::getTeacherId, selfTeacherId))
                .stream().map(CourseClass::getId).collect(Collectors.toList());
        List<CourseSchedule> scheduleList = this.list(new LambdaQueryWrapper<CourseSchedule>()
                .and(StrUtil.isNotBlank(selfTeacherId), q -> {
                    q.eq(CourseSchedule::getTeacherId, selfTeacherId);
                    if (ObjectUtil.isNotEmpty(selfClassIdList)) {
                        q.or().in(CourseSchedule::getClassId, selfClassIdList);
                    }
                })
                .eq(StrUtil.isNotBlank(courseScheduleWeekParam.getClassId()), CourseSchedule::getClassId, courseScheduleWeekParam.getClassId())
                .eq(StrUtil.isNotBlank(courseScheduleWeekParam.getTeacherId()), CourseSchedule::getTeacherId, courseScheduleWeekParam.getTeacherId())
                .eq(StrUtil.isNotBlank(courseScheduleWeekParam.getClassroom()), CourseSchedule::getClassroom, courseScheduleWeekParam.getClassroom())
                // 数据范围：挂班级的按班级可见；直接排课（无班级）的按校区可见
                .and(ObjectUtil.isNotEmpty(visibleClassIdList) || ObjectUtil.isNotEmpty(visibleCampusIds), q -> {
                    if (ObjectUtil.isNotEmpty(visibleClassIdList)) {
                        q.in(CourseSchedule::getClassId, visibleClassIdList);
                    }
                    if (ObjectUtil.isNotEmpty(visibleCampusIds)) {
                        q.or(i -> i.isNull(CourseSchedule::getClassId)
                                .in(CourseSchedule::getCampusId, visibleCampusIds));
                    }
                })
                .ge(CourseSchedule::getScheduleDate, start)
                .le(CourseSchedule::getScheduleDate, end)
                .eq(CourseSchedule::getStatus, CourseScheduleStatusEnum.NORMAL.getValue())
                .orderByAsc(CourseSchedule::getScheduleDate)
                .orderByAsc(CourseSchedule::getStartTime));
        if (ObjectUtil.isEmpty(scheduleList)) {
            return new ArrayList<>();
        }
        // 批量填充班级 / 课程 / 教师信息
        List<String> classIdList = scheduleList.stream().map(CourseSchedule::getClassId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, CourseClass> classMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(classIdList)) {
            courseClassMapper.selectBatchIds(classIdList).forEach(item -> classMap.put(item.getId(), item));
        }
        List<String> courseIdList = classMap.values().stream().map(CourseClass::getCourseId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, String> courseNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(courseIdList)) {
            courseMapper.selectBatchIds(courseIdList).forEach(item -> courseNameMap.put(item.getId(), item.getName()));
        }
        List<String> teacherIdList = scheduleList.stream().map(CourseSchedule::getTeacherId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, String> teacherNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(teacherIdList)) {
            sysUserApi.getUserListByIdListWithoutException(teacherIdList)
                    .forEach(user -> teacherNameMap.put(user.getStr("id"), user.getStr("name")));
        }
        List<CourseScheduleViewDTO> resultList = new ArrayList<>();
        // 批量统计：在读学员数（按班级）与已点名人数（按排课），供课表标注点名状态
        Map<String, Integer> studentCountMap = new HashMap<>();
        for (String classId : classIdList) {
            long studyingCount = courseClassStudentService.listByClassId(classId).stream()
                    .filter(item -> CourseClassStudentStatusEnum.STUDYING.getValue().equals(item.getStatus()))
                    .count();
            studentCountMap.put(classId, (int) studyingCount);
        }
        Map<String, Integer> attendCountMap = new HashMap<>();
        List<String> scheduleIdList = scheduleList.stream().map(CourseSchedule::getId).collect(Collectors.toList());
        courseClassAttendMapper.selectList(new LambdaQueryWrapper<CourseClassAttend>()
                        .in(CourseClassAttend::getScheduleId, scheduleIdList))
                .forEach(item -> attendCountMap.merge(item.getScheduleId(), 1, Integer::sum));
        // 直接排课（无班级）的应到人数取这节课指定学员数，课表卡片才能展示「应到/已点」进度
        Map<String, Integer> directStudentCountMap = new HashMap<>();
        for (CourseSchedule schedule : scheduleList) {
            if (StrUtil.isBlank(schedule.getClassId())) {
                directStudentCountMap.put(schedule.getId(),
                        courseScheduleStudentService.listStudentIdsByScheduleId(schedule.getId()).size());
            }
        }
        for (CourseSchedule schedule : scheduleList) {
            CourseScheduleViewDTO dto = new CourseScheduleViewDTO();
            dto.setId(schedule.getId());
            dto.setClassId(schedule.getClassId());
            dto.setTeacherId(schedule.getTeacherId());
            dto.setTeacherName(teacherNameMap.get(schedule.getTeacherId()));
            dto.setScheduleDate(schedule.getScheduleDate());
            dto.setStartTime(schedule.getStartTime() == null ? null : schedule.getStartTime().toString());
            dto.setEndTime(schedule.getEndTime() == null ? null : schedule.getEndTime().toString());
            dto.setClassroom(schedule.getClassroom());
            dto.setStatus(schedule.getStatus());
            int studentCount = StrUtil.isNotBlank(schedule.getClassId())
                    ? studentCountMap.getOrDefault(schedule.getClassId(), 0)
                    : directStudentCountMap.getOrDefault(schedule.getId(), 0);
            int attendCount = attendCountMap.getOrDefault(schedule.getId(), 0);
            dto.setStudentCount(studentCount);
            dto.setAttendCount(attendCount);
            dto.setAttendStatus(CourseScheduleAttendStatusUtil.attendStatus(attendCount, studentCount));
            CourseClass courseClass = classMap.get(schedule.getClassId());
            if (ObjectUtil.isNotEmpty(courseClass)) {
                dto.setClassName(courseClass.getName());
                dto.setCourseId(courseClass.getCourseId());
                dto.setCampusId(courseClass.getCampusId());
                dto.setCourseName(courseNameMap.get(courseClass.getCourseId()));
            } else {
                // 直接排课（不挂班级）：展示课程名
                dto.setClassName("直接排课");
                dto.setCourseId(schedule.getCourseId());
                dto.setCampusId(schedule.getCampusId());
                dto.setCourseName(courseNameMap.get(schedule.getCourseId()));
            }
            resultList.add(dto);
        }
        return resultList;
    }

    @Override
    public List<JSONObject> teacherOptions(String searchKey) {
        List<JSONObject> userList = sysUserApi.listUserOptions(searchKey);
        List<JSONObject> teacherList = new ArrayList<>();
        for (JSONObject user : userList) {
            String userId = user.getStr("id");
            List<String> roleCodeList = sysUserApi.ownRoleCode(userId);
            if (roleCodeList != null
                    && (roleCodeList.contains("teacher") || roleCodeList.contains("superAdmin"))) {
                JSONObject option = new JSONObject();
                option.set("value", userId);
                option.set("label", user.getStr("name"));
                teacherList.add(option);
            }
        }
        // 兼容尚未维护 teacher 角色的存量租户：没有老师角色时保留原全量选项
        if (!teacherList.isEmpty()) {
            return teacherList;
        }
        List<JSONObject> optionList = new ArrayList<>();
        for (JSONObject user : userList) {
            JSONObject option = new JSONObject();
            option.set("value", user.getStr("id"));
            option.set("label", user.getStr("name"));
            optionList.add(option);
        }
        return optionList;
    }

    private Date parseDate(String date, Date defaultDate) {
        return StrUtil.isNotBlank(date) ? cn.hutool.core.date.DateUtil.parse(date) : defaultDate;
    }

    private Date defaultWeekStart() {
        LocalDate today = LocalDate.now();
        return Date.from(today.with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date defaultWeekEnd() {
        LocalDate today = LocalDate.now();
        return Date.from(today.with(DayOfWeek.SUNDAY).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
