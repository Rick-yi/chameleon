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
package vip.xiaonuo.platform.modular.report.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.platform.modular.report.dto.PlatformExpiringTenantDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformOverviewDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformPackageDistributionDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantRankDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantTrendDTO;
import vip.xiaonuo.platform.modular.report.service.PlatformReportService;

import java.io.IOException;
import java.util.List;

/**
 * 平台报表控制器
 * <p>
 * 仅平台运营角色 platformAdmin 可访问；跨租户聚合查询在 Service 内显式排除平台租户。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "平台报表控制器")
@RestController
@Validated
@SaCheckRole("platformAdmin")
public class PlatformReportController {

    @Resource
    private PlatformReportService platformReportService;

    /**
     * 平台概览
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "平台概览")
    @GetMapping("/platform/report/overview")
    public CommonResult<PlatformOverviewDTO> overview() {
        return CommonResult.data(platformReportService.overview());
    }

    /**
     * 租户增长趋势
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "租户增长趋势")
    @GetMapping("/platform/report/tenantTrend")
    public CommonResult<List<PlatformTenantTrendDTO>> tenantTrend(
            @RequestParam(required = false, defaultValue = "12") Integer months) {
        return CommonResult.data(platformReportService.tenantTrend(months));
    }

    /**
     * 套餐分布
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "套餐分布")
    @GetMapping("/platform/report/packageDistribution")
    public CommonResult<List<PlatformPackageDistributionDTO>> packageDistribution() {
        return CommonResult.data(platformReportService.packageDistribution());
    }

    /**
     * 到期租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "到期租户")
    @GetMapping("/platform/report/expiring")
    public CommonResult<List<PlatformExpiringTenantDTO>> expiring(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return CommonResult.data(platformReportService.expiring(days));
    }

    /**
     * 租户活跃度排行
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "租户活跃度排行")
    @GetMapping("/platform/report/tenantRank")
    public CommonResult<List<PlatformTenantRankDTO>> tenantRank(
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return CommonResult.data(platformReportService.tenantRank(limit));
    }

    /**
     * 导出平台报表
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "导出平台报表")
    @GetMapping("/platform/report/export")
    public void export(@RequestParam(required = false, defaultValue = "OVERVIEW") String type,
                       HttpServletResponse response) throws IOException {
        platformReportService.export(type, response);
    }
}
