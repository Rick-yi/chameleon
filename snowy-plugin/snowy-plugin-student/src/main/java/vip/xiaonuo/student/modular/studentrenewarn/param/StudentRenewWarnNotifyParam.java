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
package vip.xiaonuo.student.modular.studentrenewarn.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 续费提醒参数
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Getter
@Setter
public class StudentRenewWarnNotifyParam {

    /** 预警id（列表行内提醒时传） */
    @Schema(description = "预警id")
    private String id;

    /** 学员id（风险学员提醒时传） */
    @Schema(description = "学员id")
    private String studentId;

    /** 课程id（可选） */
    @Schema(description = "课程id")
    private String courseId;

    /** 渠道：IN_APP/SMS/WECHAT，当前仅落库不真实发送 */
    @Schema(description = "渠道")
    private String channel;

    /** 提醒内容（为空时按预警信息自动生成） */
    @Schema(description = "提醒内容")
    private String content;
}
