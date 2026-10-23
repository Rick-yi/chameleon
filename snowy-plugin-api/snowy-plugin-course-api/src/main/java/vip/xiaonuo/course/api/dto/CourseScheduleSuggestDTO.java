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
package vip.xiaonuo.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 推荐排课时段
 *
 * @author 会飞的猪
 * @date 2026/10/23 10:00
 **/
@Getter
@Setter
public class CourseScheduleSuggestDTO {

    /** 上课日期 */
    @Schema(description = "上课日期")
    private String scheduleDate;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;

    /** 展示文案（如「10月24日 周五 19:00-20:30」） */
    @Schema(description = "展示文案")
    private String label;

    /** 推荐分（越高越推荐） */
    @Schema(description = "推荐分")
    private Integer score;

    /** 推荐理由 */
    @Schema(description = "推荐理由")
    private String reason;
}
