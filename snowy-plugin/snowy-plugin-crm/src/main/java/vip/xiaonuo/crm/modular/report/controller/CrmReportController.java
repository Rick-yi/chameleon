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
package vip.xiaonuo.crm.modular.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.crm.api.dto.CrmFollowRankDTO;
import vip.xiaonuo.crm.api.dto.CrmFunnelStageDTO;
import vip.xiaonuo.crm.api.dto.CrmSourceDTO;
import vip.xiaonuo.crm.api.dto.CrmTrialReportDTO;
import vip.xiaonuo.crm.api.param.CrmReportQueryParam;
import vip.xiaonuo.crm.modular.report.service.CrmReportService;

import java.util.List;

/**
 * 招生报表控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "招生报表控制器")
@RestController
@Validated
public class CrmReportController {

    @Resource
    private CrmReportService crmReportService;

    /**
     * 线索漏斗
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "线索漏斗")
    @GetMapping("/crm/report/funnel")
    public CommonResult<List<CrmFunnelStageDTO>> funnel(CrmReportQueryParam param) {
        return CommonResult.data(crmReportService.funnel(param));
    }

    /**
     * 来源渠道
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "来源渠道")
    @GetMapping("/crm/report/source")
    public CommonResult<List<CrmSourceDTO>> source(CrmReportQueryParam param) {
        return CommonResult.data(crmReportService.source(param));
    }

    /**
     * 跟进排行
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "跟进排行")
    @GetMapping("/crm/report/followRank")
    public CommonResult<List<CrmFollowRankDTO>> followRank(CrmReportQueryParam param) {
        return CommonResult.data(crmReportService.followRank(param));
    }

    /**
     * 试听转化
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "试听转化")
    @GetMapping("/crm/report/trial")
    public CommonResult<CrmTrialReportDTO> trial(CrmReportQueryParam param) {
        return CommonResult.data(crmReportService.trial(param));
    }
}
