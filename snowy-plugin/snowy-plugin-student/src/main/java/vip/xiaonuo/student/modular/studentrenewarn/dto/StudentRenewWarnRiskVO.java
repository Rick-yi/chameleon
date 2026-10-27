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
package vip.xiaonuo.student.modular.studentrenewarn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 风险学员（可处理）视图
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class StudentRenewWarnRiskVO {

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 学员姓名 */
    @Schema(description = "学员姓名")
    private String studentName;

    /** 家长手机 */
    @Schema(description = "家长手机")
    private String parentPhone;

    /** 校区id */
    @Schema(description = "校区id")
    private String campusId;

    /** 课程id */
    @Schema(description = "课程id")
    private String courseId;

    /** 课程名称 */
    @Schema(description = "课程名称")
    private String courseName;

    /** 所在班级 */
    @Schema(description = "所在班级")
    private String classNames;

    /** 剩余课时 */
    @Schema(description = "剩余课时")
    private BigDecimal remainLessons;

    /** 课时到期时间 */
    @Schema(description = "课时到期时间")
    private Date expireTime;

    /** 最近一次消课时间 */
    @Schema(description = "最近一次消课时间")
    private Date lastAttendTime;

    /** 距最近一次消课天数 */
    @Schema(description = "距最近一次消课天数")
    private Long daysWithoutAttend;

    /** 风险类型 */
    @Schema(description = "风险类型")
    private String riskType;

    /** 风险说明 */
    @Schema(description = "风险说明")
    private String riskDesc;

    /** 是否已有待处理预警/待办 */
    @Schema(description = "是否已有待处理预警")
    private Boolean hasPendingWarn;

    /** 最近一次跟进内容 */
    @Schema(description = "最近一次跟进内容")
    private String lastFollowContent;

    /** 最近一次跟进时间 */
    @Schema(description = "最近一次跟进时间")
    private Date lastFollowTime;

    /** 跟进人姓名 */
    @Schema(description = "跟进人姓名")
    private String followUserName;
}
