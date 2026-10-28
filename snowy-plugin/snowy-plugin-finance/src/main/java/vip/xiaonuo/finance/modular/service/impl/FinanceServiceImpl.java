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
package vip.xiaonuo.finance.modular.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vip.xiaonuo.auth.core.util.StpDataScopeUtil;
import vip.xiaonuo.common.cache.CommonCacheOperator;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.finance.modular.dto.FinanceCompareDTO;
import vip.xiaonuo.finance.modular.dto.FinanceReportMetaDTO;
import vip.xiaonuo.finance.modular.export.FinanceExportRowBuilder;
import vip.xiaonuo.finance.modular.service.FinanceService;
import vip.xiaonuo.finance.modular.util.FinanceCompareSupport;
import vip.xiaonuo.finance.modular.stat.entity.FinDailyStat;
import vip.xiaonuo.finance.modular.stat.service.FinDailyStatService;
import vip.xiaonuo.student.api.StudentFinanceApi;
import vip.xiaonuo.student.api.dto.DashboardOverviewDTO;
import vip.xiaonuo.student.api.dto.FinanceAttendTrendDTO;
import vip.xiaonuo.student.api.dto.FinanceCampusRankDTO;
import vip.xiaonuo.student.api.dto.FinanceChannelDTO;
import vip.xiaonuo.student.api.dto.FinanceConsumeDTO;
import vip.xiaonuo.student.api.dto.FinanceCourseRankDTO;
import vip.xiaonuo.student.api.dto.FinancePaymentDTO;
import vip.xiaonuo.student.api.dto.FinanceReconcileDTO;
import vip.xiaonuo.student.api.dto.FinanceRefundDTO;
import vip.xiaonuo.student.api.dto.FinanceStatDTO;
import vip.xiaonuo.student.api.dto.FinanceSummaryDTO;
import vip.xiaonuo.student.api.dto.FinanceTrendDTO;
import vip.xiaonuo.student.api.dto.ReportOptionDTO;
import vip.xiaonuo.student.api.param.FinanceQueryParam;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 财务Service接口实现类
 * <p>
 * 财务不维护独立流水表，直接基于报名订单/退费单/课时流水聚合，
 * 保证报表与业务数据天然一致；看板类接口带 5 分钟缓存（按租户 + 参数维度）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class FinanceServiceImpl implements FinanceService {

    /** 报表缓存有效期（秒）：业务写入会主动失效，这里只做兜底 */
    private static final long CACHE_TTL_SECONDS = 60L;

    /** 单次导出最大行数（超过建议异步导出） */
    private static final int MAX_EXPORT_ROWS = 50000;

    @Resource
    private StudentFinanceApi studentFinanceApi;

    @Resource
    private FinDailyStatService finDailyStatService;

    @Resource
    private CommonCacheOperator cacheOperator;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private FinanceExportRowBuilder financeExportRowBuilder;

    @Override
    public Page<FinancePaymentDTO> paymentPage(FinanceQueryParam financeQueryParam) {
        String key = cacheKey("paymentPage", financeQueryParam);
        Object cached = cacheOperator.get(key);
        if (cached instanceof String json) {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(json);
                Page<FinancePaymentDTO> page = new Page<>(ObjectUtil.defaultIfNull(financeQueryParam.getCurrent(), 1),
                        ObjectUtil.defaultIfNull(financeQueryParam.getSize(), 10), jsonObject.getInt("total"));
                page.setRecords(JSONUtil.toList(jsonObject.getJSONArray("records"), FinancePaymentDTO.class));
                return page;
            } catch (Exception e) {
                log.warn(">>> 收款明细缓存解析失败，重新查询：{}", key);
            }
        }
        Page<FinancePaymentDTO> page = studentFinanceApi.paymentPage(financeQueryParam);
        cacheOperator.put(key, JSONUtil.toJsonStr(Map.of("total", page.getTotal(), "records", page.getRecords())),
                CACHE_TTL_SECONDS);
        return page;
    }

    @Override
    public Page<FinanceRefundDTO> refundPage(FinanceQueryParam financeQueryParam) {
        String key = cacheKey("refundPage", financeQueryParam);
        Object cached = cacheOperator.get(key);
        if (cached instanceof String json) {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(json);
                Page<FinanceRefundDTO> page = new Page<>(ObjectUtil.defaultIfNull(financeQueryParam.getCurrent(), 1),
                        ObjectUtil.defaultIfNull(financeQueryParam.getSize(), 10), jsonObject.getInt("total"));
                page.setRecords(JSONUtil.toList(jsonObject.getJSONArray("records"), FinanceRefundDTO.class));
                return page;
            } catch (Exception e) {
                log.warn(">>> 退费明细缓存解析失败，重新查询：{}", key);
            }
        }
        Page<FinanceRefundDTO> page = studentFinanceApi.refundPage(financeQueryParam);
        cacheOperator.put(key, JSONUtil.toJsonStr(Map.of("total", page.getTotal(), "records", page.getRecords())),
                CACHE_TTL_SECONDS);
        return page;
    }

    @Override
    public List<FinanceStatDTO> courseStat(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.courseStat(financeQueryParam);
    }

    @Override
    public FinanceSummaryDTO summary(FinanceQueryParam financeQueryParam) {
        return cachedObject("summary", FinanceSummaryDTO.class, financeQueryParam,
                () -> {
                    FinanceSummaryDTO fastSummary = fastSummary(financeQueryParam);
                    return fastSummary != null ? fastSummary : studentFinanceApi.summary(financeQueryParam);
                });
    }

    @Override
    public FinanceCompareDTO compare(FinanceQueryParam financeQueryParam, String compareMode,
                                     String compareStartTime, String compareEndTime) {
        String mode = FinanceCompareSupport.normalizeMode(compareMode);
        FinanceCompareSupport.Range currentRange = FinanceCompareSupport.currentRange(
                financeQueryParam.getStartTime(), financeQueryParam.getEndTime());
        FinanceCompareSupport.Range previousRange = FinanceCompareSupport.previousRange(currentRange);
        FinanceCompareSupport.Range yoyRange = FinanceCompareSupport.yoyRange(currentRange);

        FinanceCompareDTO result = new FinanceCompareDTO();
        result.setMode(mode);
        result.setStartTime(currentRange.startTime());
        result.setEndTime(currentRange.endTime());

        // 当前区间沿用原有汇总口径，固定周期接口行为不变
        result.setPreviousSummary(ObjectUtil.defaultIfNull(
                summary(withRange(financeQueryParam, previousRange)), new FinanceSummaryDTO()));
        result.setYoySummary(ObjectUtil.defaultIfNull(
                summary(withRange(financeQueryParam, yoyRange)), new FinanceSummaryDTO()));

        if (FinanceCompareSupport.MODE_CUSTOM.equals(mode)) {
            FinanceCompareSupport.Range customRange = FinanceCompareSupport.customRange(compareStartTime, compareEndTime);
            if (customRange == null) {
                throw new CommonException("自定义对比期需要同时提供正确的开始日期与结束日期");
            }
            result.setCompareStartTime(customRange.startTime());
            result.setCompareEndTime(customRange.endTime());
            result.setCompareSummary(ObjectUtil.defaultIfNull(
                    summary(withRange(financeQueryParam, customRange)), new FinanceSummaryDTO()));
        } else if (FinanceCompareSupport.MODE_YOY.equals(mode)) {
            result.setCompareStartTime(yoyRange.startTime());
            result.setCompareEndTime(yoyRange.endTime());
            result.setCompareSummary(result.getYoySummary());
        } else {
            result.setCompareStartTime(previousRange.startTime());
            result.setCompareEndTime(previousRange.endTime());
            result.setCompareSummary(result.getPreviousSummary());
        }
        result.setGeneratedAt(DateUtil.formatDateTime(new Date()));
        return result;
    }

    @Override
    public FinanceReportMetaDTO meta() {
        FinanceReportMetaDTO meta = new FinanceReportMetaDTO();
        meta.setGeneratedAt(DateUtil.formatDateTime(new Date()));
        meta.setNote("实时聚合即时生效；已完整覆盖日汇总的区间每日 00:30 更新，未覆盖时自动回落实时聚合。");
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT MAX(`STAT_DATE`) AS `LATEST_DATE`, MAX(`UPDATE_TIME`) AS `LATEST_UPDATE` "
                            + "FROM `FIN_DAILY_STAT` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'",
                    currentTenantId());
            Object latestDate = row.get("LATEST_DATE");
            Object latestUpdate = row.get("LATEST_UPDATE");
            if (latestDate instanceof Date date) {
                meta.setDailyStatLatestDate(DateUtil.formatDate(date));
            } else if (latestDate != null) {
                meta.setDailyStatLatestDate(StrUtil.subPre(latestDate.toString(), 10));
            }
            if (latestUpdate instanceof Date date) {
                meta.setDailyStatUpdatedAt(DateUtil.formatDateTime(date));
            } else if (latestUpdate != null) {
                meta.setDailyStatUpdatedAt(latestUpdate.toString());
            }
        } catch (Exception e) {
            log.warn(">>> 查询报表日汇总更新时间失败：{}", e.getMessage());
        }
        return meta;
    }

    /**
     * 复制查询参数并替换时间区间，避免修改调用方参数
     *
     * @author 会飞的猪
     * @date 2026/10/26
     **/
    private FinanceQueryParam withRange(FinanceQueryParam source, FinanceCompareSupport.Range range) {
        FinanceQueryParam target = new FinanceQueryParam();
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setSearchKey(source.getSearchKey());
        target.setChannel(source.getChannel());
        target.setCampusId(source.getCampusId());
        target.setCourseId(source.getCourseId());
        target.setDimension(source.getDimension());
        target.setLimit(source.getLimit());
        target.setStatus(source.getStatus());
        target.setSource(source.getSource());
        target.setStartTime(range.startTime());
        target.setEndTime(range.endTime());
        return target;
    }

    @Override
    public List<FinanceTrendDTO> trend(FinanceQueryParam financeQueryParam) {
        return cachedList("trend", FinanceTrendDTO.class, financeQueryParam,
                () -> {
                    List<FinanceTrendDTO> fastTrend = fastTrend(financeQueryParam);
                    return fastTrend != null ? fastTrend : studentFinanceApi.trend(financeQueryParam);
                });
    }

    @Override
    public List<FinanceChannelDTO> channelStat(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.channelStat(financeQueryParam);
    }

    @Override
    public List<FinanceCourseRankDTO> courseRank(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.courseRank(financeQueryParam);
    }

    @Override
    public List<FinanceCampusRankDTO> campusRank(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.campusRank(financeQueryParam);
    }

    @Override
    public List<FinanceAttendTrendDTO> attendTrend(FinanceQueryParam financeQueryParam) {
        return studentFinanceApi.attendTrend(financeQueryParam);
    }

    @Override
    public List<ReportOptionDTO> campusOptions() {
        return studentFinanceApi.campusOptions();
    }

    @Override
    public DashboardOverviewDTO dashboard(FinanceQueryParam financeQueryParam) {
        return cachedObject("dashboard", DashboardOverviewDTO.class, financeQueryParam,
                () -> studentFinanceApi.dashboard(financeQueryParam));
    }

    @Override
    public void export(FinanceQueryParam financeQueryParam, String type, HttpServletResponse response) throws IOException {
        FinanceExportRowBuilder.ExportPayload payload = financeExportRowBuilder.build(type, financeQueryParam);
        if (payload.rowList().isEmpty()) {
            throw new CommonException("无数据可导出");
        }
        if (payload.rowList().size() > MAX_EXPORT_ROWS) {
            throw new CommonException("导出数据量超过 {} 行，请缩小时间范围或改用异步导出", MAX_EXPORT_ROWS);
        }
        String fileName = payload.sheetName() + "_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
        File file = FileUtil.file(FileUtil.getTmpDir(), fileName);
        try {
            CommonExcelUtil.write(file, payload.sheetName(), payload.headList(), payload.rowList());
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    @Override
    public List<FinDailyStat> dailyStat(String startDate, String endDate) {
        String start = StrUtil.blankToDefault(startDate, DateUtil.formatDate(DateUtil.offsetDay(new Date(), -29)));
        String end = StrUtil.blankToDefault(endDate, DateUtil.formatDate(new Date()));
        return finDailyStatService.listByDate(start, end);
    }

    @Override
    public void rebuildDailyStat(String statDate) {
        finDailyStatService.aggregate(StrUtil.blankToDefault(statDate, DateUtil.formatDate(new Date())));
        clearCache();
    }

    @Override
    public List<FinanceConsumeDTO> consume(FinanceQueryParam financeQueryParam) {
        return cachedList("consume", FinanceConsumeDTO.class, financeQueryParam,
                () -> studentFinanceApi.consume(financeQueryParam));
    }

    @Override
    public FinanceReconcileDTO reconcile(FinanceQueryParam financeQueryParam) {        FinanceReconcileDTO reconcile = studentFinanceApi.reconcile(financeQueryParam);
        String startDate = StrUtil.blankToDefault(financeQueryParam.getStartTime(),
                DateUtil.formatDateTime(DateUtil.beginOfMonth(new Date()))).substring(0, 10);
        String endDate = StrUtil.blankToDefault(financeQueryParam.getEndTime(),
                DateUtil.formatDateTime(new Date())).substring(0, 10);
        BigDecimal dailyPaid = finDailyStatService.sumPaidAmount(startDate, endDate);
        if (dailyPaid.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = dailyPaid.subtract(reconcile.getOrderPaidAmount());
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                reconcile.getMessages().add("日汇总与实时收款差异：" + diff.toPlainString() + " 元（可重算日汇总后复核）");
                reconcile.setConsistent(Boolean.FALSE);
            }
        }
        return reconcile;
    }

    @Override
    public void clearCache() {
        String prefix = "report:" + StrUtil.blankToDefault(TenantContext.getTenantId(), "0") + ":";
        Collection<String> keyList = cacheOperator.getAllKeys().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(java.util.stream.Collectors.toList());
        if (keyList.isEmpty()) {
            return;
        }
        cacheOperator.remove(keyList.toArray(new String[0]));
    }

    /**
     * 对象缓存（JSON 字符串存储，按租户 + 参数隔离）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private <T> T cachedObject(String name, Class<T> type, Object param, Supplier<T> supplier) {
        String key = cacheKey(name, param);
        Object cached = cacheOperator.get(key);
        if (cached instanceof String json) {
            try {
                return JSONUtil.toBean(json, type);
            } catch (Exception e) {
                log.warn(">>> 报表缓存解析失败，重新计算：{}", key);
            }
        }
        T value = supplier.get();
        cacheOperator.put(key, JSONUtil.toJsonStr(value), CACHE_TTL_SECONDS);
        return value;
    }

    /**
     * 列表缓存（JSON 字符串存储，按租户 + 参数隔离）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private <T> List<T> cachedList(String name, Class<T> elementType, Object param, Supplier<List<T>> supplier) {
        String key = cacheKey(name, param);
        Object cached = cacheOperator.get(key);
        if (cached instanceof String json) {
            try {
                return JSONUtil.toList(JSONUtil.parseArray(json), elementType);
            } catch (Exception e) {
                log.warn(">>> 报表缓存解析失败，重新计算：{}", key);
            }
        }
        List<T> value = supplier.get();
        cacheOperator.put(key, JSONUtil.toJsonStr(value), CACHE_TTL_SECONDS);
        return value;
    }

    /**
     * 缓存 key（租户 + 报表名 + 参数指纹）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private String cacheKey(String name, Object param) {
        String paramJson = param == null ? "" : JSONUtil.toJsonStr(param);
        String hash = DigestUtil.md5Hex(paramJson).substring(0, 16);
        return "report:" + StrUtil.blankToDefault(TenantContext.getTenantId(), "0") + ":" + name + ":" + hash;
    }

    /**
     * 日汇总快路径：区间已被日汇总完整覆盖、且没有校区/课程筛选时，直接用日汇总表出数
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private FinanceSummaryDTO fastSummary(FinanceQueryParam param) {
        if ("REAL".equalsIgnoreCase(param.getSource())) {
            return null;
        }
        // 日汇总是租户级数据，存在机构数据范围限制时必须走实时查询
        if (StpDataScopeUtil.visibleOrgIds() != null) {
            return null;
        }
        String startDate = fullDayDate(param.getStartTime(), false);
        String endDate = fullDayDate(param.getEndTime(), true);
        if (startDate == null || endDate == null) {
            return null;
        }
        if (StrUtil.isNotBlank(param.getCampusId()) || StrUtil.isNotBlank(param.getCourseId())) {
            return null;
        }
        if (!finDailyStatService.covered(startDate, endDate)) {
            return null;
        }
        List<FinDailyStat> statList = finDailyStatService.listByDate(startDate, endDate);
        if (ObjectUtil.isEmpty(statList)) {
            return null;
        }
        FinanceSummaryDTO summary = new FinanceSummaryDTO();
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal lessons = BigDecimal.ZERO;
        BigDecimal recognized = BigDecimal.ZERO;
        long orderCount = 0L;
        long newStudentCount = 0L;
        for (FinDailyStat stat : statList) {
            paid = paid.add(nvl(stat.getPaidAmount()));
            refund = refund.add(nvl(stat.getRefundAmount()));
            lessons = lessons.add(nvl(stat.getNetLessons()));
            recognized = recognized.add(nvl(stat.getRecognizedAmount()));
            orderCount += ObjectUtil.defaultIfNull(stat.getOrderCount(), 0L);
            newStudentCount += ObjectUtil.defaultIfNull(stat.getNewStudentCount(), 0L);
        }
        summary.setPaidAmount(paid);
        summary.setRefundAmount(refund);
        summary.setNetAmount(paid.subtract(refund));
        summary.setOrderCount(orderCount);
        summary.setNewStudentCount(newStudentCount);
        summary.setAttendLessons(lessons);
        summary.setRecognizedAmount(recognized);
        summary.setDataSource("DAILY_STAT");

        String tenantId = currentTenantId();
        Date start = DateUtil.beginOfDay(DateUtil.parse(startDate));
        Date end = DateUtil.endOfDay(DateUtil.parse(endDate));
        long payingStudentCount = queryLong("SELECT COUNT(DISTINCT `STUDENT_ID`) FROM `STUDENT_ORDER` "
                + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` IN ('PAID','REFUNDED') "
                + "AND COALESCE(`PAY_TIME`, `CREATE_TIME`) >= ? AND COALESCE(`PAY_TIME`, `CREATE_TIME`) <= ?",
                tenantId, start, end);
        summary.setPayingStudentCount(payingStudentCount);
        BigDecimal unpaidAmount = queryBigDecimal("SELECT COALESCE(SUM(`AMOUNT`),0) FROM `STUDENT_ORDER` "
                + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` = 'UNPAID' "
                + "AND `CREATE_TIME` >= ? AND `CREATE_TIME` <= ?", tenantId, start, end);
        long unpaidOrderCount = queryLong("SELECT COUNT(*) FROM `STUDENT_ORDER` "
                + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` = 'UNPAID' "
                + "AND `CREATE_TIME` >= ? AND `CREATE_TIME` <= ?", tenantId, start, end);
        summary.setUnpaidAmount(unpaidAmount);
        summary.setUnpaidOrderCount(unpaidOrderCount);
        summary.setStudyingStudentCount(queryLong("SELECT COUNT(*) FROM `STUDENT` "
                + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` = 'STUDYING'", tenantId));
        summary.setRenewWarnCount(queryLong("SELECT COUNT(*) FROM `STUDENT_RENEW_WARN` "
                + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` = 'PENDING'", tenantId));
        if (orderCount > 0) {
            summary.setAvgOrderAmount(paid.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP));
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            summary.setRefundRate(refund.multiply(BigDecimal.valueOf(100)).divide(paid, 2, RoundingMode.HALF_UP));
        }
        if (payingStudentCount > 0) {
            summary.setPerStudentRevenue(paid.divide(BigDecimal.valueOf(payingStudentCount), 2, RoundingMode.HALF_UP));
        }
        BigDecimal receivable = paid.add(unpaidAmount);
        if (receivable.compareTo(BigDecimal.ZERO) > 0) {
            summary.setCollectionRate(paid.multiply(BigDecimal.valueOf(100)).divide(receivable, 2, RoundingMode.HALF_UP));
        }
        return summary;
    }

    /**
     * 日汇总快路径趋势
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private List<FinanceTrendDTO> fastTrend(FinanceQueryParam param) {
        if ("REAL".equalsIgnoreCase(param.getSource())) {
            return null;
        }
        // 日汇总是租户级数据，存在机构数据范围限制时必须走实时查询
        if (StpDataScopeUtil.visibleOrgIds() != null) {
            return null;
        }
        String startDate = fullDayDate(param.getStartTime(), false);
        String endDate = fullDayDate(param.getEndTime(), true);
        if (startDate == null || endDate == null) {
            return null;
        }
        if (StrUtil.isNotBlank(param.getCampusId()) || StrUtil.isNotBlank(param.getCourseId())) {
            return null;
        }
        if (!finDailyStatService.covered(startDate, endDate)) {
            return null;
        }
        List<FinDailyStat> statList = finDailyStatService.listByDate(startDate, endDate);
        if (ObjectUtil.isEmpty(statList)) {
            return null;
        }
        String dimension = StrUtil.blankToDefault(param.getDimension(), "MONTH").toUpperCase();
        boolean yearly = "YEAR".equals(dimension);
        boolean monthly = !yearly && !"DAY".equals(dimension);
        Map<String, FinanceTrendDTO> trendMap = new java.util.LinkedHashMap<>();
        for (FinDailyStat stat : statList) {
            String period = yearly ? DateUtil.format(stat.getStatDate(), "yyyy")
                    : monthly ? DateUtil.format(stat.getStatDate(), "yyyy-MM")
                    : DateUtil.formatDate(stat.getStatDate());
            FinanceTrendDTO dto = trendMap.computeIfAbsent(period, key -> {
                FinanceTrendDTO item = new FinanceTrendDTO();
                item.setPeriod(key);
                return item;
            });
            dto.setPaidAmount(dto.getPaidAmount().add(nvl(stat.getPaidAmount())));
            dto.setRefundAmount(dto.getRefundAmount().add(nvl(stat.getRefundAmount())));
            dto.setAttendLessons(dto.getAttendLessons().add(nvl(stat.getNetLessons())));
        }
        List<FinanceTrendDTO> resultList = new ArrayList<>(trendMap.values());
        resultList.forEach(item -> item.setNetAmount(item.getPaidAmount().subtract(item.getRefundAmount())));
        return resultList;
    }

    /**
     * 解析整天起止时间（yyyy-MM-dd），非整天返回 null
     * <p>
     * 起始时间需为 00:00:00，结束时间需为 23:59:59（当天末尾），才允许走日汇总快路径。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private String fullDayDate(String time, boolean isEnd) {
        if (StrUtil.isBlank(time)) {
            return null;
        }
        Date parsed = DateUtil.parse(time);
        if (isEnd) {
            long gap = DateUtil.endOfDay(parsed).getTime() - parsed.getTime();
            if (gap > 1000) {
                return null;
            }
        } else if (!DateUtil.beginOfDay(parsed).equals(parsed)) {
            return null;
        }
        return DateUtil.formatDate(parsed);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String currentTenantId() {
        return StrUtil.blankToDefault(TenantContext.getTenantId(), "0");
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private BigDecimal queryBigDecimal(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
