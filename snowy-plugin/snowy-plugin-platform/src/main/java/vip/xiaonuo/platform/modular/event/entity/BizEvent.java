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
package vip.xiaonuo.platform.modular.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.util.Date;

/**
 * 业务埋点实体
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
@TableName("BIZ_EVENT")
public class BizEvent extends CommonEntity {

	/** id */
	@Schema(description = "id")
	@TableId(type = IdType.ASSIGN_ID)
	private String id;

	/** 租户id */
	private String tenantId;

	/** 事件码 */
	private String eventCode;

	/** 事件名称 */
	private String eventName;

	/** 操作人id */
	private String userId;

	/** 操作人姓名 */
	private String userName;

	/** 对象类型 */
	private String targetType;

	/** 对象id */
	private String targetId;

	/** 对象名称 */
	private String targetName;

	/** 业务属性(JSON) */
	private String props;

	/** 事件发生时间 */
	private Date eventTime;

	/** 事件日期（yyyy-MM-dd，便于按天统计） */
	@TableField(exist = false)
	private String eventDate;
}
