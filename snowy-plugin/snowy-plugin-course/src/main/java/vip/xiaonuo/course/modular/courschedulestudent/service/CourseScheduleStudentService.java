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
package vip.xiaonuo.course.modular.courschedulestudent.service;

import vip.xiaonuo.course.modular.courschedulestudent.entity.CourseScheduleStudent;

import java.util.List;

/**
 * 排课学员Service
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public interface CourseScheduleStudentService {

	/**
	 * 给排课批量指定学员
	 *
	 * @param scheduleId     排课id
	 * @param studentIdList  学员id集合
	 */
	void saveBatchStudent(String scheduleId, List<String> studentIdList);

	/**
	 * 查询排课直接指定的学员id
	 *
	 * @param scheduleIdList 排课id集合
	 */
	List<String> listStudentIdsByScheduleIds(List<String> scheduleIdList);

	/**
	 * 查询某节课直接指定的学员id
	 *
	 * @param scheduleId 排课id
	 */
	List<String> listStudentIdsByScheduleId(String scheduleId);
}
