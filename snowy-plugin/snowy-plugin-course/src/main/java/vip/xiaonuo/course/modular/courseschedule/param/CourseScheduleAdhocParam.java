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
package vip.xiaonuo.course.modular.courseschedule.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 直接排课参数（不挂班级：老师带一群学生上课）
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class CourseScheduleAdhocParam {

	/** 课程id（消课按此课程扣减课时账户） */
	@Schema(description = "课程id", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "课程不能为空")
	private String courseId;

	/** 老师id */
	@Schema(description = "老师id")
	private String teacherId;

	/** 教室 */
	@Schema(description = "教室")
	private String classroom;

	/** 学员id集合（这节课的上课名单） */
	@Schema(description = "学员id集合", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "学员不能为空")
	private List<String> studentIdList;

	/** 开始日期 */
	@Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
	private Date startDate;

	/** 生成周数 */
	@Schema(description = "生成周数", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer weekCount;

	/** 每周时段模板（JSON数组，与班级周模板同结构） */
	@Schema(description = "每周时段模板JSON数组", requiredMode = Schema.RequiredMode.REQUIRED)
	private String weekTemplateJson;
}
