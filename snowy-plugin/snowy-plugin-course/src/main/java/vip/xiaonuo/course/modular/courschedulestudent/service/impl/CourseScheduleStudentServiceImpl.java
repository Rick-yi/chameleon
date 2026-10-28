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
package vip.xiaonuo.course.modular.courschedulestudent.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.course.modular.courschedulestudent.entity.CourseScheduleStudent;
import vip.xiaonuo.course.modular.courschedulestudent.mapper.CourseScheduleStudentMapper;
import vip.xiaonuo.course.modular.courschedulestudent.service.CourseScheduleStudentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 排课学员Service实现
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class CourseScheduleStudentServiceImpl extends ServiceImpl<CourseScheduleStudentMapper, CourseScheduleStudent>
		implements CourseScheduleStudentService {

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveBatchStudent(String scheduleId, List<String> studentIdList) {
		if (StrUtil.isBlank(scheduleId) || CollUtil.isEmpty(studentIdList)) {
			return;
		}
		List<CourseScheduleStudent> toSaveList = new ArrayList<>();
		for (String studentId : studentIdList) {
			if (StrUtil.isBlank(studentId)) {
				continue;
			}
			CourseScheduleStudent item = new CourseScheduleStudent();
			item.setTenantId(TenantContext.getTenantId());
			item.setScheduleId(scheduleId);
			item.setStudentId(studentId);
			item.setId(IdUtil.getSnowflakeNextIdStr());
			toSaveList.add(item);
		}
		if (CollUtil.isNotEmpty(toSaveList)) {
			this.saveBatch(toSaveList);
		}
	}

	@Override
	public List<String> listStudentIdsByScheduleIds(List<String> scheduleIdList) {
		if (CollUtil.isEmpty(scheduleIdList)) {
			return new ArrayList<>();
		}
		return this.list(new LambdaQueryWrapper<CourseScheduleStudent>()
						.in(CourseScheduleStudent::getScheduleId, scheduleIdList))
				.stream().map(CourseScheduleStudent::getStudentId).distinct().toList();
	}

	@Override
	public List<String> listStudentIdsByScheduleId(String scheduleId) {
		if (StrUtil.isBlank(scheduleId)) {
			return new ArrayList<>();
		}
		return this.list(new LambdaQueryWrapper<CourseScheduleStudent>()
						.eq(CourseScheduleStudent::getScheduleId, scheduleId))
				.stream().map(CourseScheduleStudent::getStudentId).distinct().toList();
	}

	@Override
	public Map<String, Integer> countStudentByScheduleIds(List<String> scheduleIdList) {
		Map<String, Integer> countMap = new HashMap<>();
		if (CollUtil.isEmpty(scheduleIdList)) {
			return countMap;
		}
		List<String> idList = scheduleIdList.stream().filter(StrUtil::isNotBlank).distinct().toList();
		if (idList.isEmpty()) {
			return countMap;
		}
		// 依赖 UK_SCHEDULE_STUDENT(SCHEDULE_ID, STUDENT_ID) 唯一索引：同一课节同一学员不会重复记录，
		// 因此直接计数与「查学员id再 distinct 后取 size」结果一致
		this.list(new LambdaQueryWrapper<CourseScheduleStudent>()
						.select(CourseScheduleStudent::getScheduleId, CourseScheduleStudent::getStudentId)
						.in(CourseScheduleStudent::getScheduleId, idList))
				.forEach(item -> countMap.merge(item.getScheduleId(), 1, Integer::sum));
		return countMap;
	}
}
