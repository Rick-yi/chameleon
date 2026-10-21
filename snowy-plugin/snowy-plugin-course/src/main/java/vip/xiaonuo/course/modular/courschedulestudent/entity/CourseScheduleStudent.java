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
package vip.xiaonuo.course.modular.courschedulestudent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 排课学员实体
 * <p>
 * 老师带一群学生上课、没有固定班级时，排课直接关联这批学员；点名名单 = 班级在读学员 ∪ 本表学员。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
@TableName("COURSE_SCHEDULE_STUDENT")
public class CourseScheduleStudent extends CommonEntity {

	/** id */
	@Schema(description = "id")
	@TableId(type = IdType.ASSIGN_ID)
	private String id;

	/** 租户id */
	private String tenantId;

	/** 排课id */
	private String scheduleId;

	/** 学员id */
	private String studentId;
}
