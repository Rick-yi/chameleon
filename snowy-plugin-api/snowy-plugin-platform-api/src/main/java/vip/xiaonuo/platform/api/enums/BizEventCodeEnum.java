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
package vip.xiaonuo.platform.api.enums;

import lombok.Getter;

/**
 * 业务埋点事件码
 * <p>
 * 埋点按教培机构的业务漏斗划分：招生 → 报名收款 → 教务交付 → 续费。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
public enum BizEventCodeEnum {

	// ---------- 招生 ----------
	/** 线索创建 */
	LEAD_CREATE("LEAD_CREATE", "线索创建", "lead"),
	/** 线索跟进 */
	LEAD_FOLLOW("LEAD_FOLLOW", "线索跟进", "lead"),
	/** 试听预约 */
	TRIAL_BOOK("TRIAL_BOOK", "试听预约", "lead"),
	/** 转学员 */
	LEAD_CONVERT("LEAD_CONVERT", "转学员", "lead"),

	// ---------- 报名与收款 ----------
	/** 报名开单 */
	ORDER_CREATE("ORDER_CREATE", "报名开单", "order"),
	/** 收款成功 */
	ORDER_PAY("ORDER_PAY", "收款成功", "order"),
	/** 订单取消 */
	ORDER_CANCEL("ORDER_CANCEL", "订单取消", "order"),
	/** 退费申请 */
	REFUND_APPLY("REFUND_APPLY", "退费申请", "order"),
	/** 退费审批通过 */
	REFUND_APPROVE("REFUND_APPROVE", "退费审批", "order"),
	/** 退费完成 */
	REFUND_CONFIRM("REFUND_CONFIRM", "退费完成", "order"),
	/** 续费 */
	RENEW("RENEW", "续费", "order"),

	// ---------- 教务交付 ----------
	/** 入班 */
	CLASS_ENROLL("CLASS_ENROLL", "学员入班", "class"),
	/** 退班 */
	CLASS_QUIT("CLASS_QUIT", "学员退班", "class"),
	/** 批量生成排课 */
	SCHEDULE_GENERATE("SCHEDULE_GENERATE", "批量排课", "schedule"),
	/** 调课 */
	SCHEDULE_CHANGE("SCHEDULE_CHANGE", "调课", "schedule"),
	/** 取消排课 */
	SCHEDULE_CANCEL("SCHEDULE_CANCEL", "取消排课", "schedule"),
	/** 点名消课 */
	ATTEND_CONFIRM("ATTEND_CONFIRM", "点名消课", "schedule"),
	/** 撤销点名 */
	ATTEND_CANCEL("ATTEND_CANCEL", "撤销点名", "schedule"),

	// ---------- 基础档案 ----------
	/** 新增学员 */
	STUDENT_CREATE("STUDENT_CREATE", "新增学员", "student"),
	/** 新增课程 */
	COURSE_CREATE("COURSE_CREATE", "新增课程", "course"),
	/** 新增课包 */
	PACKAGE_CREATE("PACKAGE_CREATE", "新增课包", "course"),
	/** 新增班级 */
	CLASS_CREATE("CLASS_CREATE", "新增班级", "class"),

	// ---------- 平台行为 ----------
	/** 页面访问 */
	PAGE_VIEW("PAGE_VIEW", "页面访问", "page"),
	/** 功能点击 */
	FEATURE_CLICK("FEATURE_CLICK", "功能点击", "page");

	private final String value;

	private final String message;

	/** 默认对象类型 */
	private final String targetType;

	BizEventCodeEnum(String value, String message, String targetType) {
		this.value = value;
		this.message = message;
		this.targetType = targetType;
	}

	/**
	 * 按事件码取名称
	 *
	 * @param eventCode 事件码
	 * @return 事件名称
	 */
	public static String messageOf(String eventCode) {
		for (BizEventCodeEnum item : values()) {
			if (item.value.equals(eventCode)) {
				return item.message;
			}
		}
		return eventCode;
	}
}
