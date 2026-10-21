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

import java.util.ArrayList;
import java.util.List;

/**
 * 批量生成排课结果DTO
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class CourseScheduleGenerateResultDTO {

    /** 计划生成条数 */
    @Schema(description = "计划生成条数")
    private Integer plannedCount = 0;

    /** 实际新增条数 */
    @Schema(description = "实际新增条数")
    private Integer createdCount = 0;

    /** 跳过条数（与已有排课完全重复） */
    @Schema(description = "跳过条数")
    private Integer skippedCount = 0;

    /** 冲突说明 */
    @Schema(description = "冲突说明")
    private List<String> conflicts = new ArrayList<>();

    /** 是否仅预览（未落库） */
    @Schema(description = "是否仅预览")
    private Boolean preview = Boolean.FALSE;
}
