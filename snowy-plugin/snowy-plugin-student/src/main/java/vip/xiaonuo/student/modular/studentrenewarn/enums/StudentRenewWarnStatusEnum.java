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
package vip.xiaonuo.student.modular.studentrenewarn.enums;

import lombok.Getter;

/**
 * 续费预警状态/原因枚举
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
public class StudentRenewWarnStatusEnum {

    /** 待处理 */
    public static final String PENDING = "PENDING";

    /** 已续费 */
    public static final String RENEWED = "RENEWED";

    /** 已处理 */
    public static final String PROCESSED = "PROCESSED";

    /** 已忽略 */
    public static final String IGNORED = "IGNORED";

    /** 课时不足 */
    public static final String REASON_LESSON_LOW = "LESSON_LOW";

    /** 即将到期 */
    public static final String REASON_EXPIRE_SOON = "EXPIRE_SOON";

    /** 长期未消课 */
    public static final String REASON_NO_ATTEND = "NO_ATTEND";

    /** 停课/流失风险 */
    public static final String REASON_LOSS_RISK = "LOSS_RISK";

    /** 配置范围：机构默认 */
    public static final String SCOPE_DEFAULT = "DEFAULT";

    /** 配置范围：课程 */
    public static final String SCOPE_COURSE = "COURSE";

    /** 配置范围：班级 */
    public static final String SCOPE_CLASS = "CLASS";

    /** 跟进业务类型：续费预警 */
    public static final String FOLLOW_BIZ_RENEW_WARN = "RENEW_WARN";

    /** 跟进业务类型：风险学员 */
    public static final String FOLLOW_BIZ_RISK = "RISK";

    /** 跟进业务类型：退学/结课流失 */
    public static final String FOLLOW_BIZ_LOSS = "LOSS";
}
