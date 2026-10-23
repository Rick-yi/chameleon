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
package vip.xiaonuo.crm.modular.report.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import vip.xiaonuo.biz.api.export.BizExportSheet;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.crm.api.dto.CrmFollowRankDTO;
import vip.xiaonuo.crm.api.dto.CrmFunnelStageDTO;
import vip.xiaonuo.crm.api.dto.CrmSourceDTO;
import vip.xiaonuo.crm.api.dto.CrmTrialReportDTO;
import vip.xiaonuo.crm.api.param.CrmReportQueryParam;
import vip.xiaonuo.crm.modular.follow.entity.CrmLeadFollow;
import vip.xiaonuo.crm.modular.follow.mapper.CrmLeadFollowMapper;
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadSourceEnum;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadStatusEnum;
import vip.xiaonuo.crm.modular.lead.mapper.CrmLeadMapper;
import vip.xiaonuo.crm.modular.report.service.CrmReportService;
import vip.xiaonuo.crm.modular.trial.entity.CrmTrialAppointment;
import vip.xiaonuo.crm.modular.trial.enums.CrmTrialStatusEnum;
import vip.xiaonuo.crm.modular.trial.mapper.CrmTrialAppointmentMapper;
import vip.xiaonuo.sys.api.SysUserApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 招生报表Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CrmReportServiceImpl implements CrmReportService {

    /** 默认返回条数 */
    private static final int DEFAULT_LIMIT = 20;

    /** 未知来源占位 */
    private static final String UNKNOWN_SOURCE = "UNKNOWN";

    @Resource
    private CrmLeadMapper crmLeadMapper;

    @Resource
    private CrmLeadFollowMapper crmLeadFollowMapper;

    @Resource
    private CrmTrialAppointmentMapper crmTrialAppointmentMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Override
    public List<CrmFunnelStageDTO> funnel(CrmReportQueryParam param) {
        Date start = resolveStart(param);
        Date end = resolveEnd(param);
        // 只需 id 与状态，避免把线索全部字段读进内存
        List<CrmLead> leadList = crmLeadMapper.selectList(leadWrapper(start, end)
                .select(CrmLead::getId, CrmLead::getStatus));
        long total = leadList.size();
        Set<String> leadIdSet = leadList.stream().map(CrmLead::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> followedIdSet = leadList.stream()
                .filter(lead -> CrmLeadStatusEnum.FOLLOWED.getValue().equals(lead.getStatus())
                        || CrmLeadStatusEnum.TRIALED.getValue().equals(lead.getStatus())
                        || CrmLeadStatusEnum.ENROLLED.getValue().equals(lead.getStatus()))
                .map(CrmLead::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ObjectUtil.isNotEmpty(leadIdSet)) {
            for (CrmLeadFollow follow : crmLeadFollowMapper.selectList(new LambdaQueryWrapper<CrmLeadFollow>()
                    .select(CrmLeadFollow::getLeadId)
                    .in(CrmLeadFollow::getLeadId, leadIdSet)
                    .ge(CrmLeadFollow::getCreateTime, start)
                    .le(CrmLeadFollow::getCreateTime, end))) {
                if (StrUtil.isNotBlank(follow.getLeadId())) {
                    followedIdSet.add(follow.getLeadId());
                }
            }
        }

        Set<String> trialIdSet = leadList.stream()
                .filter(lead -> CrmLeadStatusEnum.TRIALED.getValue().equals(lead.getStatus())
                        || CrmLeadStatusEnum.ENROLLED.getValue().equals(lead.getStatus()))
                .map(CrmLead::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ObjectUtil.isNotEmpty(leadIdSet)) {
            for (CrmTrialAppointment trial : crmTrialAppointmentMapper.selectList(new LambdaQueryWrapper<CrmTrialAppointment>()
                    .select(CrmTrialAppointment::getLeadId)
                    .in(CrmTrialAppointment::getLeadId, leadIdSet)
                    .ge(CrmTrialAppointment::getCreateTime, start)
                    .le(CrmTrialAppointment::getCreateTime, end))) {
                if (StrUtil.isNotBlank(trial.getLeadId())) {
                    trialIdSet.add(trial.getLeadId());
                }
            }
        }

        Set<String> enrolledIdSet = leadList.stream()
                .filter(lead -> CrmLeadStatusEnum.ENROLLED.getValue().equals(lead.getStatus()))
                .map(CrmLead::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        // 漏斗必须逐级收窄：已报名 ⊆ 已试听 ⊆ 已跟进 ⊆ 新线索
        trialIdSet.addAll(enrolledIdSet);
        followedIdSet.addAll(trialIdSet);

        List<CrmFunnelStageDTO> resultList = new ArrayList<>();
        resultList.add(stage("NEW_LEAD", "新线索", total, total));
        resultList.add(stage("FOLLOWED", "已跟进", followedIdSet.size(), total));
        resultList.add(stage("TRIALED", "已试听", trialIdSet.size(), total));
        resultList.add(stage("ENROLLED", "已报名", enrolledIdSet.size(), total));
        return resultList;
    }

    @Override
    public List<CrmSourceDTO> source(CrmReportQueryParam param) {
        // 只需来源与状态
        List<CrmLead> leadList = crmLeadMapper.selectList(leadWrapper(resolveStart(param), resolveEnd(param))
                .select(CrmLead::getId, CrmLead::getSource, CrmLead::getStatus));
        Map<String, CrmSourceDTO> sourceMap = new LinkedHashMap<>();
        for (CrmLead lead : leadList) {
            String source = StrUtil.blankToDefault(lead.getSource(), UNKNOWN_SOURCE);
            CrmSourceDTO dto = sourceMap.computeIfAbsent(source, key -> {
                CrmSourceDTO item = new CrmSourceDTO();
                item.setSource(key);
                item.setSourceName(sourceName(key));
                return item;
            });
            dto.setLeadCount(dto.getLeadCount() + 1);
            if (CrmLeadStatusEnum.ENROLLED.getValue().equals(lead.getStatus())) {
                dto.setEnrolledCount(dto.getEnrolledCount() + 1);
            }
        }
        List<CrmSourceDTO> resultList = new ArrayList<>(sourceMap.values());
        resultList.forEach(dto -> dto.setConversionRate(percent(dto.getEnrolledCount(), dto.getLeadCount())));
        resultList.sort((a, b) -> b.getLeadCount().compareTo(a.getLeadCount()));
        return limit(resultList, param.getLimit());
    }

    @Override
    public List<CrmFollowRankDTO> followRank(CrmReportQueryParam param) {
        Date start = resolveStart(param);
        Date end = resolveEnd(param);
        List<CrmLeadFollow> followList = crmLeadFollowMapper.selectList(new LambdaQueryWrapper<CrmLeadFollow>()
                .ge(CrmLeadFollow::getCreateTime, start)
                .le(CrmLeadFollow::getCreateTime, end)
                .isNotNull(CrmLeadFollow::getFollowUserId)
                .ne(CrmLeadFollow::getFollowUserId, ""));
        Map<String, CrmFollowRankDTO> rankMap = new LinkedHashMap<>();
        Map<String, Set<String>> userLeadMap = new HashMap<>();
        for (CrmLeadFollow follow : followList) {
            String userId = follow.getFollowUserId();
            CrmFollowRankDTO dto = rankMap.computeIfAbsent(userId, key -> {
                CrmFollowRankDTO item = new CrmFollowRankDTO();
                item.setUserId(key);
                return item;
            });
            dto.setFollowCount(dto.getFollowCount() + 1);
            userLeadMap.computeIfAbsent(userId, key -> new LinkedHashSet<>()).add(follow.getLeadId());
        }
        if (rankMap.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> allLeadIdSet = userLeadMap.values().stream().flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, CrmLead> leadMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(allLeadIdSet)) {
            crmLeadMapper.selectBatchIds(allLeadIdSet).forEach(lead -> leadMap.put(lead.getId(), lead));
        }
        Map<String, String> userNameMap = loadUserNameMap(new ArrayList<>(rankMap.keySet()));
        List<CrmFollowRankDTO> resultList = new ArrayList<>(rankMap.values());
        for (CrmFollowRankDTO dto : resultList) {
            Set<String> leadIdSet = userLeadMap.getOrDefault(dto.getUserId(), new LinkedHashSet<>());
            dto.setLeadCount((long) leadIdSet.size());
            dto.setEnrolledCount(leadIdSet.stream()
                    .map(leadMap::get)
                    .filter(lead -> lead != null && CrmLeadStatusEnum.ENROLLED.getValue().equals(lead.getStatus()))
                    .count());
            dto.setUserName(userNameMap.get(dto.getUserId()));
        }
        resultList.sort((a, b) -> b.getFollowCount().compareTo(a.getFollowCount()));
        return limit(resultList, param.getLimit());
    }

    @Override
    public CrmTrialReportDTO trial(CrmReportQueryParam param) {
        Date start = resolveStart(param);
        Date end = resolveEnd(param);
        List<CrmTrialAppointment> trialList = crmTrialAppointmentMapper.selectList(
                new LambdaQueryWrapper<CrmTrialAppointment>()
                        .apply("COALESCE(TRIAL_TIME, CREATE_TIME) >= {0}", start)
                        .apply("COALESCE(TRIAL_TIME, CREATE_TIME) <= {0}", end));
        CrmTrialReportDTO result = new CrmTrialReportDTO();
        result.setTrialCount((long) trialList.size());
        result.setDoneCount(trialList.stream()
                .filter(item -> CrmTrialStatusEnum.DONE.equals(item.getStatus())).count());
        result.setInterestedCount(trialList.stream()
                .filter(item -> CrmTrialStatusEnum.RESULT_INTERESTED.equals(item.getResult())).count());
        result.setNotInterestedCount(trialList.stream()
                .filter(item -> CrmTrialStatusEnum.RESULT_NOT_INTERESTED.equals(item.getResult())).count());
        List<String> leadIdList = trialList.stream().map(CrmTrialAppointment::getLeadId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(leadIdList)) {
            result.setEnrolledCount(crmLeadMapper.selectBatchIds(leadIdList).stream()
                    .filter(lead -> CrmLeadStatusEnum.ENROLLED.getValue().equals(lead.getStatus())).count());
        }
        result.setConversionRate(percent(result.getEnrolledCount(), result.getTrialCount()));
        return result;
    }

    @Override
    public void export(CrmReportQueryParam param, String type, HttpServletResponse response) throws IOException {
        BizExportSheet sheet = buildSheet(param, type);
        File file = FileUtil.file(FileUtil.getTmpDir(), "招生报表_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
        try {
            CommonExcelUtil.write(file, sheet.getSheetName(), sheet.getHeadList(), sheet.getRowList());
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    @Override
    public BizExportSheet buildSheet(CrmReportQueryParam param, String type) {
        String exportType = StrUtil.blankToDefault(type, "FUNNEL").toUpperCase();
        String sheetName;
        List<List<String>> headList = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        switch (exportType) {
            case "FUNNEL" -> {
                sheetName = "线索漏斗";
                headList.add(List.of("阶段"));
                headList.add(List.of("数量"));
                headList.add(List.of("转化率(%)"));
                for (CrmFunnelStageDTO dto : funnel(param)) {
                    rowList.add(List.of(text(dto.getStageName()), String.valueOf(dto.getCount()), num(dto.getConversionRate())));
                }
            }
            case "SOURCE" -> {
                sheetName = "来源渠道";
                headList.add(List.of("来源"));
                headList.add(List.of("线索数"));
                headList.add(List.of("报名数"));
                headList.add(List.of("转化率(%)"));
                for (CrmSourceDTO dto : source(param)) {
                    rowList.add(List.of(text(dto.getSourceName()), String.valueOf(dto.getLeadCount()),
                            String.valueOf(dto.getEnrolledCount()), num(dto.getConversionRate())));
                }
            }
            case "FOLLOW_RANK" -> {
                sheetName = "跟进排行";
                headList.add(List.of("跟进人"));
                headList.add(List.of("跟进次数"));
                headList.add(List.of("跟进线索数"));
                headList.add(List.of("报名数"));
                for (CrmFollowRankDTO dto : followRank(param)) {
                    rowList.add(List.of(text(dto.getUserName()), String.valueOf(dto.getFollowCount()),
                            String.valueOf(dto.getLeadCount()), String.valueOf(dto.getEnrolledCount())));
                }
            }
            case "TRIAL" -> {
                sheetName = "试听转化";
                headList.add(List.of("试听总数"));
                headList.add(List.of("已完成"));
                headList.add(List.of("有意向"));
                headList.add(List.of("无意向"));
                headList.add(List.of("报名数"));
                headList.add(List.of("转化率(%)"));
                CrmTrialReportDTO dto = trial(param);
                rowList.add(List.of(String.valueOf(dto.getTrialCount()), String.valueOf(dto.getDoneCount()),
                        String.valueOf(dto.getInterestedCount()), String.valueOf(dto.getNotInterestedCount()),
                        String.valueOf(dto.getEnrolledCount()), num(dto.getConversionRate())));
            }
            default -> throw new CommonException("不支持的导出类型：{}", exportType);
        }
        if (rowList.isEmpty()) {
            throw new CommonException("无数据可导出");
        }
        return new BizExportSheet(sheetName, headList, rowList);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String num(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 线索查询条件（按创建时间）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private LambdaQueryWrapper<CrmLead> leadWrapper(Date start, Date end) {        return new LambdaQueryWrapper<CrmLead>()
                .ge(CrmLead::getCreateTime, start)
                .le(CrmLead::getCreateTime, end);
    }

    /**
     * 漏斗阶段
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private CrmFunnelStageDTO stage(String stage, String stageName, long count, long total) {
        CrmFunnelStageDTO dto = new CrmFunnelStageDTO();
        dto.setStage(stage);
        dto.setStageName(stageName);
        dto.setCount(count);
        dto.setConversionRate(percent(count, total));
        return dto;
    }

    /**
     * 来源名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String sourceName(String source) {
        if (CrmLeadSourceEnum.GROUND.getValue().equals(source)) {
            return CrmLeadSourceEnum.GROUND.getMessage();
        }
        if (CrmLeadSourceEnum.REFERRAL.getValue().equals(source)) {
            return CrmLeadSourceEnum.REFERRAL.getMessage();
        }
        if (CrmLeadSourceEnum.ONLINE.getValue().equals(source)) {
            return CrmLeadSourceEnum.ONLINE.getMessage();
        }
        if (CrmLeadSourceEnum.VISIT.getValue().equals(source)) {
            return CrmLeadSourceEnum.VISIT.getMessage();
        }
        return "未填写";
    }

    /**
     * 用户名称
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Map<String, String> loadUserNameMap(List<String> userIdList) {
        Map<String, String> nameMap = new HashMap<>();
        List<String> distinctIdList = userIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return nameMap;
        }
        for (JSONObject user : sysUserApi.getUserListByIdListWithoutException(distinctIdList)) {
            nameMap.put(user.getStr("id"), user.getStr("name"));
        }
        return nameMap;
    }

    private Date resolveStart(CrmReportQueryParam param) {
        if (StrUtil.isNotBlank(param.getStartTime())) {
            return DateUtil.parse(param.getStartTime());
        }
        return DateUtil.beginOfMonth(new Date());
    }

    private Date resolveEnd(CrmReportQueryParam param) {
        if (StrUtil.isNotBlank(param.getEndTime())) {
            return DateUtil.parse(param.getEndTime());
        }
        return new Date();
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private <T> List<T> limit(List<T> list, Integer limit) {
        int size = ObjectUtil.isNotEmpty(limit) ? limit : DEFAULT_LIMIT;
        return list.stream().limit(Math.max(size, 1)).collect(Collectors.toList());
    }
}
