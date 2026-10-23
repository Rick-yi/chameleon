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
package vip.xiaonuo.crm.modular.report.service;

import jakarta.servlet.http.HttpServletResponse;
import vip.xiaonuo.crm.api.dto.CrmFollowRankDTO;
import vip.xiaonuo.crm.api.dto.CrmFunnelStageDTO;
import vip.xiaonuo.crm.api.dto.CrmSourceDTO;
import vip.xiaonuo.crm.api.dto.CrmTrialReportDTO;
import vip.xiaonuo.crm.api.param.CrmReportQueryParam;

import java.io.IOException;
import java.util.List;

/**
 * 招生报表Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface CrmReportService {

    /** 线索漏斗 */
    List<CrmFunnelStageDTO> funnel(CrmReportQueryParam param);

    /** 来源渠道 */
    List<CrmSourceDTO> source(CrmReportQueryParam param);

    /** 跟进排行 */
    List<CrmFollowRankDTO> followRank(CrmReportQueryParam param);

    /** 试听转化 */
    CrmTrialReportDTO trial(CrmReportQueryParam param);

    /**
     * 导出招生报表
     *
     * @param param    查询参数
     * @param type     导出类型：FUNNEL/SOURCE/FOLLOW_RANK/TRIAL
     * @param response 响应
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    void export(CrmReportQueryParam param, String type, HttpServletResponse response) throws IOException;

    /**
     * 构建导出内容（同步导出与异步导出共用）
     *
     * @param param 查询参数
     * @param type  导出类型：FUNNEL/SOURCE/FOLLOW_RANK
     *
     * @author 会飞的猪
     * @date 2026/10/23 11:00
     **/
    vip.xiaonuo.biz.api.export.BizExportSheet buildSheet(CrmReportQueryParam param, String type);
}
