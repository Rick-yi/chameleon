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
package vip.xiaonuo.platform.modular.report.service;

import jakarta.servlet.http.HttpServletResponse;
import vip.xiaonuo.platform.modular.report.dto.PlatformExpiringTenantDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformOverviewDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformPackageDistributionDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantRankDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantTrendDTO;

import java.io.IOException;
import java.util.List;

/**
 * 平台报表Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface PlatformReportService {

    /** 平台概览 */
    PlatformOverviewDTO overview();

    /** 租户增长趋势 */
    List<PlatformTenantTrendDTO> tenantTrend(Integer months);

    /** 套餐分布 */
    List<PlatformPackageDistributionDTO> packageDistribution();

    /** 到期租户 */
    List<PlatformExpiringTenantDTO> expiring(Integer days);

    /** 租户活跃度排行 */
    List<PlatformTenantRankDTO> tenantRank(Integer limit);

    /**
     * 导出平台报表
     *
     * @param type     导出类型：OVERVIEW/TREND/PACKAGE/EXPIRING/RANK
     * @param response 响应
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    void export(String type, HttpServletResponse response) throws IOException;
}
