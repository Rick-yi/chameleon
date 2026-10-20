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

import java.math.BigDecimal;

/**
 * 出勤汇总DTO
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
public class AcademicAttendanceDTO {

    /** 到课人次 */
    @Schema(description = "到课人次")
    private Long attendCount = 0L;

    /** 请假人次 */
    @Schema(description = "请假人次")
    private Long leaveCount = 0L;

    /** 缺勤人次 */
    @Schema(description = "缺勤人次")
    private Long absentCount = 0L;

    /** 应到人次（排课 × 在学学员） */
    @Schema(description = "应到人次")
    private Long expectedCount = 0L;

    /** 到课率（百分比，到课/已点名） */
    @Schema(description = "到课率")
    private BigDecimal attendRate = BigDecimal.ZERO;

    /** 出勤率（百分比，到课/应到） */
    @Schema(description = "出勤率")
    private BigDecimal attendanceRate = BigDecimal.ZERO;
}
