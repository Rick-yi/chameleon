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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseScheduleClosure;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseTeacherUnavailable;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseScheduleClosureMapper;
import vip.xiaonuo.course.modular.courseschedule.mapper.CourseTeacherUnavailableMapper;

import java.util.Date;
import java.util.List;

/**
 * 排课配置：停课日与教师不可用时段
 *
 * @author 会飞的猪
 * @date 2026/10/23 14:00
 **/
@Service
public class CourseScheduleConfigService {

    @Resource
    private CourseScheduleClosureMapper closureMapper;

    @Resource
    private CourseTeacherUnavailableMapper unavailableMapper;

    // ---------- 停课日 ----------

    /** 区间内的停课日 */
    public List<CourseScheduleClosure> listClosures(Date startDate, Date endDate) {
        return closureMapper.selectList(new LambdaQueryWrapper<CourseScheduleClosure>()
                .ge(CourseScheduleClosure::getClosureDate, DateUtil.beginOfDay(startDate))
                .le(CourseScheduleClosure::getClosureDate, DateUtil.endOfDay(endDate))
                .orderByAsc(CourseScheduleClosure::getClosureDate));
    }

    /** 区间内停课日（yyyy-MM-dd -> 名称） */
    public java.util.Map<String, String> closureMap(Date startDate, Date endDate) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (CourseScheduleClosure closure : listClosures(startDate, endDate)) {
            map.put(DateUtil.formatDate(closure.getClosureDate()),
                    StrUtil.blankToDefault(closure.getName(), "停课"));
        }
        return map;
    }

    /** 某天是否停课，返回停课名称；不停课返回 null */
    public String closureNameOf(Date date) {
        CourseScheduleClosure closure = closureMapper.selectOne(new LambdaQueryWrapper<CourseScheduleClosure>()
                .eq(CourseScheduleClosure::getClosureDate, DateUtil.beginOfDay(date))
                .last("limit 1"));
        return closure == null ? null : StrUtil.blankToDefault(closure.getName(), "停课");
    }

    public void addClosure(Date closureDate, String name, String remark, String campusId) {
        if (closureDate == null) {
            throw new CommonException("停课日期不能为空");
        }
        CourseScheduleClosure closure = new CourseScheduleClosure();
        closure.setId(IdUtil.getSnowflakeNextIdStr());
        closure.setTenantId(TenantContext.getTenantId());
        closure.setCampusId(campusId);
        closure.setClosureDate(DateUtil.beginOfDay(closureDate));
        closure.setName(StrUtil.blankToDefault(name, "停课"));
        closure.setRemark(remark);
        try {
            closureMapper.insert(closure);
        } catch (DuplicateKeyException e) {
            throw new CommonException("{} 已设置过停课", DateUtil.formatDate(closureDate));
        }
    }

    public void deleteClosure(String id) {
        if (ObjectUtil.isEmpty(closureMapper.selectById(id))) {
            throw new CommonException("停课日不存在，id值为：{}", id);
        }
        closureMapper.deleteById(id);
    }

    // ---------- 教师不可用 ----------

    /** 某位教师在区间内的不可用时段（含边界重叠） */
    public List<CourseTeacherUnavailable> listUnavailable(String teacherId, Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return java.util.Collections.emptyList();
        }
        return unavailableMapper.selectList(new LambdaQueryWrapper<CourseTeacherUnavailable>()
                .eq(StrUtil.isNotBlank(teacherId), CourseTeacherUnavailable::getTeacherId, teacherId)
                .lt(CourseTeacherUnavailable::getStartTime, endTime)
                .gt(CourseTeacherUnavailable::getEndTime, startTime)
                .orderByAsc(CourseTeacherUnavailable::getStartTime));
    }

    /** 区间内所有教师的不可用时段，供批量生成一次载入 */
    public List<CourseTeacherUnavailable> listUnavailableInRange(Date startTime, Date endTime) {
        return unavailableMapper.selectList(new LambdaQueryWrapper<CourseTeacherUnavailable>()
                .lt(CourseTeacherUnavailable::getStartTime, endTime)
                .gt(CourseTeacherUnavailable::getEndTime, startTime)
                .orderByAsc(CourseTeacherUnavailable::getStartTime));
    }

    public void addUnavailable(String teacherId, Date startTime, Date endTime, String reason) {
        if (StrUtil.isBlank(teacherId) || startTime == null || endTime == null) {
            throw new CommonException("教师与起止时间不能为空");
        }
        if (!endTime.after(startTime)) {
            throw new CommonException("结束时间必须晚于开始时间");
        }
        CourseTeacherUnavailable unavailable = new CourseTeacherUnavailable();
        unavailable.setId(IdUtil.getSnowflakeNextIdStr());
        unavailable.setTenantId(TenantContext.getTenantId());
        unavailable.setTeacherId(teacherId);
        unavailable.setStartTime(startTime);
        unavailable.setEndTime(endTime);
        unavailable.setReason(reason);
        unavailableMapper.insert(unavailable);
    }

    public void deleteUnavailable(String id) {
        if (ObjectUtil.isEmpty(unavailableMapper.selectById(id))) {
            throw new CommonException("不可用时段不存在，id值为：{}", id);
        }
        unavailableMapper.deleteById(id);
    }
}
