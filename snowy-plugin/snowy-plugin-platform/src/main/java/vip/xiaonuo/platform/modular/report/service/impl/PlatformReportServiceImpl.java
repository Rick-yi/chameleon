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
package vip.xiaonuo.platform.modular.report.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.platform.core.service.TenantHealthService;
import vip.xiaonuo.platform.modular.report.dto.PlatformExpiringTenantDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformExpiryBucketDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformMetricsDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformMetricTrendDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformOverviewDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformPackageDistributionDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantHealthDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantRankDTO;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantTrendDTO;
import vip.xiaonuo.platform.modular.report.service.PlatformReportService;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 平台报表Service接口实现类
 * <p>
 * 平台看板需要跨租户聚合，使用 JdbcTemplate 直接查询并显式排除平台租户（id=0），
 * 不复用业务租户的 MyBatis-Plus 租户拦截查询。接口由 @SaCheckRole("platformAdmin") 限制。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class PlatformReportServiceImpl implements PlatformReportService {

    private static final String NOT_DELETE = "NOT_DELETE";

    private static final String PLATFORM_TENANT_ID = "0";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TenantHealthService tenantHealthService;

    @Override
    public PlatformOverviewDTO overview() {
        PlatformOverviewDTO overview = new PlatformOverviewDTO();
        overview.setTenantTotal(queryLong("SELECT COUNT(*) FROM T_TENANT WHERE ID <> ? AND DELETE_FLAG = ?",
                PLATFORM_TENANT_ID, NOT_DELETE));
        overview.setNewTenantCount(queryLong("SELECT COUNT(*) FROM T_TENANT "
                        + "WHERE ID <> ? AND DELETE_FLAG = ? AND CREATE_TIME >= ?",
                PLATFORM_TENANT_ID, NOT_DELETE, DateUtil.beginOfMonth(new Date())));
        // 活跃租户：近 30 天有登录记录
        overview.setActiveTenantCount(queryLong("SELECT COUNT(DISTINCT TENANT_ID) FROM T_TENANT_LOGIN "
                        + "WHERE TENANT_ID <> ? AND DELETE_FLAG = ? AND LOGIN_TIME >= ?",
                PLATFORM_TENANT_ID, NOT_DELETE, DateUtil.offsetDay(new Date(), -30)));
        // 订单活跃租户：近 30 天有已支付订单
        overview.setOrderActiveTenantCount(queryLong("SELECT COUNT(DISTINCT TENANT_ID) FROM STUDENT_ORDER "
                        + "WHERE TENANT_ID <> ? AND DELETE_FLAG = ? AND STATUS IN ('PAID','REFUNDED') "
                        + "AND COALESCE(PAY_TIME, CREATE_TIME) >= ?",
                PLATFORM_TENANT_ID, NOT_DELETE, DateUtil.offsetDay(new Date(), -30)));
        overview.setExpiringTenantCount(queryLong("SELECT COUNT(*) FROM T_TENANT "
                        + "WHERE ID <> ? AND DELETE_FLAG = ? AND EXPIRE_TIME IS NOT NULL "
                        + "AND EXPIRE_TIME <= DATE_ADD(NOW(), INTERVAL ? DAY)",
                PLATFORM_TENANT_ID, NOT_DELETE, 30));
        overview.setStudentTotal(queryLong("SELECT COUNT(*) FROM STUDENT "
                + "WHERE TENANT_ID <> ? AND DELETE_FLAG = ?", PLATFORM_TENANT_ID, NOT_DELETE));
        overview.setOrderCount(queryLong("SELECT COUNT(*) FROM STUDENT_ORDER "
                        + "WHERE TENANT_ID <> ? AND DELETE_FLAG = ? AND STATUS IN ('PAID','REFUNDED')",
                PLATFORM_TENANT_ID, NOT_DELETE));
        overview.setPaidAmount(queryBigDecimal("SELECT COALESCE(SUM(PAID_AMOUNT),0) FROM STUDENT_ORDER "
                        + "WHERE TENANT_ID <> ? AND DELETE_FLAG = ? AND STATUS IN ('PAID','REFUNDED')",
                PLATFORM_TENANT_ID, NOT_DELETE));
        overview.setEstimatedArr(queryBigDecimal("SELECT COALESCE(SUM(p.PRICE),0) FROM T_TENANT t "
                        + "JOIN T_TENANT_PACKAGE p ON p.ID = t.PACKAGE_ID AND p.DELETE_FLAG = ? "
                        + "WHERE t.ID <> ? AND t.DELETE_FLAG = ?",
                NOT_DELETE, PLATFORM_TENANT_ID, NOT_DELETE));
        overview.setPackageCount(queryLong("SELECT COUNT(*) FROM T_TENANT_PACKAGE WHERE DELETE_FLAG = ?",
                NOT_DELETE));
        return overview;
    }

    @Override
    public List<PlatformTenantTrendDTO> tenantTrend(Integer months) {
        int monthCount = ObjectUtil.defaultIfNull(months, 12);
        monthCount = Math.min(Math.max(monthCount, 1), 24);
        Date startMonth = DateUtil.beginOfMonth(DateUtil.offsetMonth(new Date(), -(monthCount - 1)));
        long baseline = queryLong("SELECT COUNT(*) FROM T_TENANT "
                        + "WHERE ID <> ? AND DELETE_FLAG = ? AND CREATE_TIME < ?",
                PLATFORM_TENANT_ID, NOT_DELETE, startMonth);

        Map<String, Long> newCountMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(CREATE_TIME, '%Y-%m') AS STAT_PERIOD, COUNT(*) AS CNT FROM T_TENANT "
                        + "WHERE ID <> ? AND DELETE_FLAG = ? AND CREATE_TIME >= ? GROUP BY STAT_PERIOD",
                PLATFORM_TENANT_ID, NOT_DELETE, startMonth)) {
            newCountMap.put(String.valueOf(row.get("STAT_PERIOD")), toLong(row.get("CNT")));
        }

        List<PlatformTenantTrendDTO> resultList = new ArrayList<>();
        long cumulative = baseline;
        for (int i = 0; i < monthCount; i++) {
            Date month = DateUtil.offsetMonth(startMonth, i);
            String period = DateUtil.format(month, "yyyy-MM");
            long newCount = newCountMap.getOrDefault(period, 0L);
            cumulative += newCount;
            PlatformTenantTrendDTO dto = new PlatformTenantTrendDTO();
            dto.setPeriod(period);
            dto.setNewCount(newCount);
            dto.setTotalCount(cumulative);
            resultList.add(dto);
        }
        return resultList;
    }

    @Override
    public List<PlatformPackageDistributionDTO> packageDistribution() {
        List<Map<String, Object>> rowList = jdbcTemplate.queryForList(
                "SELECT t.PACKAGE_ID AS PACKAGE_ID, COALESCE(p.NAME,'未分配套餐') AS PACKAGE_NAME, "
                        + "COUNT(*) AS TENANT_COUNT FROM T_TENANT t "
                        + "LEFT JOIN T_TENANT_PACKAGE p ON p.ID = t.PACKAGE_ID AND p.DELETE_FLAG = ? "
                        + "WHERE t.ID <> ? AND t.DELETE_FLAG = ? GROUP BY t.PACKAGE_ID, p.NAME",
                NOT_DELETE, PLATFORM_TENANT_ID, NOT_DELETE);
        long total = rowList.stream().mapToLong(row -> toLong(row.get("TENANT_COUNT"))).sum();
        List<PlatformPackageDistributionDTO> resultList = new ArrayList<>();
        for (Map<String, Object> row : rowList) {
            PlatformPackageDistributionDTO dto = new PlatformPackageDistributionDTO();
            dto.setPackageId(row.get("PACKAGE_ID") == null ? null : row.get("PACKAGE_ID").toString());
            dto.setPackageName(row.get("PACKAGE_NAME") == null ? "未分配套餐" : row.get("PACKAGE_NAME").toString());
            dto.setTenantCount(toLong(row.get("TENANT_COUNT")));
            if (total > 0) {
                dto.setRatio(BigDecimal.valueOf(dto.getTenantCount()).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            }
            resultList.add(dto);
        }
        resultList.sort((a, b) -> b.getTenantCount().compareTo(a.getTenantCount()));
        return resultList;
    }

    @Override
    public List<PlatformExpiringTenantDTO> expiring(Integer days) {
        int dayCount = ObjectUtil.defaultIfNull(days, 30);
        dayCount = Math.min(Math.max(dayCount, 1), 365);
        List<Map<String, Object>> rowList = jdbcTemplate.queryForList(
                "SELECT t.ID AS TENANT_ID, t.CODE, t.NAME, t.EXPIRE_TIME, t.STATUS, "
                        + "p.NAME AS PACKAGE_NAME FROM T_TENANT t "
                        + "LEFT JOIN T_TENANT_PACKAGE p ON p.ID = t.PACKAGE_ID AND p.DELETE_FLAG = ? "
                        + "WHERE t.ID <> ? AND t.DELETE_FLAG = ? AND t.EXPIRE_TIME IS NOT NULL "
                        + "AND t.EXPIRE_TIME <= DATE_ADD(NOW(), INTERVAL ? DAY) "
                        + "ORDER BY t.EXPIRE_TIME ASC LIMIT 100",
                NOT_DELETE, PLATFORM_TENANT_ID, NOT_DELETE, dayCount);
        List<PlatformExpiringTenantDTO> resultList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Map<String, Object> row : rowList) {
            PlatformExpiringTenantDTO dto = new PlatformExpiringTenantDTO();
            dto.setTenantId(row.get("TENANT_ID") == null ? null : row.get("TENANT_ID").toString());
            dto.setCode(row.get("CODE") == null ? null : row.get("CODE").toString());
            dto.setName(row.get("NAME") == null ? null : row.get("NAME").toString());
            dto.setPackageName(row.get("PACKAGE_NAME") == null ? null : row.get("PACKAGE_NAME").toString());
            dto.setStatus(row.get("STATUS") == null ? null : row.get("STATUS").toString());
            Date expireTime = toDate(row.get("EXPIRE_TIME"));
            dto.setExpireTime(expireTime);
            if (expireTime != null) {
                LocalDate expireDate = expireTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                dto.setDaysLeft(ChronoUnit.DAYS.between(today, expireDate));
            }
            resultList.add(dto);
        }
        return resultList;
    }

    @Override
    public List<PlatformTenantRankDTO> tenantRank(Integer limit) {
        int size = ObjectUtil.defaultIfNull(limit, 20);
        size = Math.min(Math.max(size, 1), 100);
        Map<String, PlatformTenantRankDTO> rankMap = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT ID, NAME FROM T_TENANT WHERE ID <> ? AND DELETE_FLAG = ? ORDER BY CREATE_TIME ASC",
                PLATFORM_TENANT_ID, NOT_DELETE)) {
            String tenantId = row.get("ID").toString();
            PlatformTenantRankDTO dto = new PlatformTenantRankDTO();
            dto.setTenantId(tenantId);
            dto.setTenantName(row.get("NAME") == null ? null : row.get("NAME").toString());
            rankMap.put(tenantId, dto);
        }
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, COUNT(*) AS CNT FROM STUDENT WHERE DELETE_FLAG = ? GROUP BY TENANT_ID",
                NOT_DELETE)) {
            PlatformTenantRankDTO dto = rankMap.get(String.valueOf(row.get("TENANT_ID")));
            if (dto != null) {
                dto.setStudentCount(toLong(row.get("CNT")));
            }
        }
        // 各租户最近登录时间
        Map<String, Date> lastLoginMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, MAX(LOGIN_TIME) AS LAST_LOGIN_TIME FROM T_TENANT_LOGIN "
                        + "WHERE DELETE_FLAG = ? AND TENANT_ID <> ? GROUP BY TENANT_ID",
                NOT_DELETE, PLATFORM_TENANT_ID)) {
            lastLoginMap.put(String.valueOf(row.get("TENANT_ID")), toDate(row.get("LAST_LOGIN_TIME")));
        }
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, COUNT(*) AS CNT, COALESCE(SUM(PAID_AMOUNT),0) AS AMT, "
                        + "MAX(PAY_TIME) AS LAST_PAY_TIME FROM STUDENT_ORDER "
                        + "WHERE DELETE_FLAG = ? AND STATUS IN ('PAID','REFUNDED') GROUP BY TENANT_ID",
                NOT_DELETE)) {
            PlatformTenantRankDTO dto = rankMap.get(String.valueOf(row.get("TENANT_ID")));
            if (dto != null) {
                dto.setOrderCount(toLong(row.get("CNT")));
                dto.setPaidAmount(toBigDecimal(row.get("AMT")));
                dto.setLastPayTime(toDate(row.get("LAST_PAY_TIME")));
                dto.setLastLoginTime(lastLoginMap.get(dto.getTenantId()));
            }
        }
        List<PlatformTenantRankDTO> resultList = new ArrayList<>(rankMap.values());
        resultList.sort((a, b) -> b.getPaidAmount().compareTo(a.getPaidAmount()));
        return resultList.stream().limit(size).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public PlatformMetricsDTO metrics(String packageId, String month) {
        YearMonth statMonth = resolveMonth(month);
        Date start = monthStart(statMonth);
        Date end = monthStart(statMonth.plusMonths(1));
        Date trendStart = monthStart(statMonth.minusMonths(11));
        Date now = new Date();
        Date in30 = DateUtil.offsetDay(now, 30);

        List<TenantSnapshot> tenantList = loadTenantSnapshots(packageId);
        Map<String, TenantPackage> packageMap = loadPackageMap();
        List<RenewalSnapshot> renewalList = loadRenewals(trendStart, packageId);
        Set<String> allRenewalTenantIds = loadRenewalTenantIds(packageId);

        PlatformMetricsDTO dto = new PlatformMetricsDTO();
        dto.setMonth(statMonth.toString());
        dto.setPackageId(StrUtil.emptyToNull(packageId));
        TenantPackage selectedPackage = StrUtil.isBlank(packageId) ? null : packageMap.get(packageId);
        if (selectedPackage != null) {
            dto.setPackageName(selectedPackage.getName());
        }

        long activeTenantCount = 0L;
        long payingTenantCount = 0L;
        long activeAtStart = 0L;
        long newTenantCount = 0L;
        long trialConvertedCount = 0L;
        long trialTenantCount = 0L;
        long expiring30Count = 0L;
        long expiredCount = 0L;
        BigDecimal mrr = BigDecimal.ZERO;
        for (TenantSnapshot tenant : tenantList) {
            boolean createdBeforeEnd = tenant.createTime() != null && tenant.createTime().before(end);
            boolean notExpiredAtStart = tenant.expireTime() == null || !tenant.expireTime().before(start);
            boolean nonStopped = !"STOP".equals(tenant.status());
            if (createdBeforeEnd && notExpiredAtStart && nonStopped) {
                activeTenantCount++;
            }
            if ("NORMAL".equals(tenant.status()) && StrUtil.isNotBlank(tenant.packageId())
                    && createdBeforeEnd && notExpiredAtStart) {
                payingTenantCount++;
                mrr = mrr.add(monthlyValue(packageMap.get(tenant.packageId())));
            }
            if (tenant.createTime() != null && tenant.createTime().before(start) && notExpiredAtStart) {
                activeAtStart++;
            }
            if (inMonth(tenant.createTime(), start, end)) {
                newTenantCount++;
                if (allRenewalTenantIds.contains(tenant.id())) {
                    trialConvertedCount++;
                }
            }
            if ("TRIAL".equals(tenant.status())) {
                trialTenantCount++;
            }
            if (tenant.expireTime() != null) {
                if (tenant.expireTime().before(now)) {
                    expiredCount++;
                } else if (!tenant.expireTime().after(in30)) {
                    expiring30Count++;
                }
            }
        }

        BigDecimal renewalAmount = BigDecimal.ZERO;
        long renewalTenantCount = 0L;
        Set<String> renewedTenantIds = new HashSet<>();
        for (RenewalSnapshot renewal : renewalList) {
            if (inMonth(renewal.createTime(), start, end)) {
                renewalAmount = renewalAmount.add(renewal.amount());
                if (renewedTenantIds.add(renewal.tenantId())) {
                    renewalTenantCount++;
                }
            }
        }
        long expiredUnrenewedCount = 0L;
        long churnTenantCount = 0L;
        for (TenantSnapshot tenant : tenantList) {
            if (inMonth(tenant.expireTime(), start, end) && !renewedTenantIds.contains(tenant.id())) {
                expiredUnrenewedCount++;
                if ("STOP".equals(tenant.status())) {
                    churnTenantCount++;
                }
            }
        }

        dto.setActiveTenantCount(activeTenantCount);
        dto.setPayingTenantCount(payingTenantCount);
        dto.setMrr(moneyValue(mrr));
        dto.setArr(moneyValue(mrr.multiply(BigDecimal.valueOf(12))));
        dto.setRenewalAmount(moneyValue(renewalAmount));
        dto.setRenewalTenantCount(renewalTenantCount);
        dto.setExpiredUnrenewedCount(expiredUnrenewedCount);
        dto.setRenewalRate(percent(renewalTenantCount, renewalTenantCount + expiredUnrenewedCount));
        dto.setChurnTenantCount(churnTenantCount);
        dto.setChurnRate(percent(churnTenantCount, activeAtStart));
        dto.setNewTenantCount(newTenantCount);
        dto.setTrialTenantCount(trialTenantCount);
        dto.setTrialConvertedCount(trialConvertedCount);
        dto.setTrialConversionRate(percent(trialConvertedCount, newTenantCount));
        dto.setExpiring30Count(expiring30Count);
        dto.setExpiredCount(expiredCount);
        dto.setTrend(buildMetricTrend(statMonth, tenantList, packageMap, renewalList));
        dto.setExpiryDistribution(buildExpiryDistribution(tenantList, packageMap));
        return dto;
    }

    @Override
    public List<PlatformTenantHealthDTO> tenantHealth(String packageId, Integer limit) {
        return tenantHealthService.rank(packageId, limit);
    }

    /**
     * 近 12 个月经营趋势（以筛选月份为最后一个月）
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     **/
    private List<PlatformMetricTrendDTO> buildMetricTrend(YearMonth statMonth, List<TenantSnapshot> tenantList,
                                                          Map<String, TenantPackage> packageMap,
                                                          List<RenewalSnapshot> renewalList) {
        List<PlatformMetricTrendDTO> trendList = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth current = statMonth.minusMonths(i);
            Date start = monthStart(current);
            Date end = monthStart(current.plusMonths(1));
            BigDecimal mrr = BigDecimal.ZERO;
            long newTenantCount = 0L;
            long activeAtStart = 0L;
            for (TenantSnapshot tenant : tenantList) {
                boolean createdBeforeEnd = tenant.createTime() != null && tenant.createTime().before(end);
                boolean notExpiredAtStart = tenant.expireTime() == null || !tenant.expireTime().before(start);
                if ("NORMAL".equals(tenant.status()) && createdBeforeEnd && notExpiredAtStart) {
                    mrr = mrr.add(monthlyValue(packageMap.get(tenant.packageId())));
                }
                if (inMonth(tenant.createTime(), start, end)) {
                    newTenantCount++;
                }
                if (tenant.createTime() != null && tenant.createTime().before(start) && notExpiredAtStart) {
                    activeAtStart++;
                }
            }
            BigDecimal renewalAmount = BigDecimal.ZERO;
            Set<String> renewedIds = new HashSet<>();
            for (RenewalSnapshot renewal : renewalList) {
                if (inMonth(renewal.createTime(), start, end)) {
                    renewalAmount = renewalAmount.add(renewal.amount());
                    renewedIds.add(renewal.tenantId());
                }
            }
            long expiredUnrenewed = 0L;
            long churn = 0L;
            for (TenantSnapshot tenant : tenantList) {
                if (inMonth(tenant.expireTime(), start, end) && !renewedIds.contains(tenant.id())) {
                    expiredUnrenewed++;
                    if ("STOP".equals(tenant.status())) {
                        churn++;
                    }
                }
            }
            PlatformMetricTrendDTO trend = new PlatformMetricTrendDTO();
            trend.setMonth(current.toString());
            trend.setMrr(moneyValue(mrr));
            trend.setRenewalAmount(moneyValue(renewalAmount));
            trend.setRenewalTenantCount((long) renewedIds.size());
            trend.setNewTenantCount(newTenantCount);
            trend.setChurnTenantCount(churn);
            trend.setRenewalRate(percent(renewedIds.size(), renewedIds.size() + expiredUnrenewed));
            trendList.add(trend);
        }
        return trendList;
    }

    /**
     * 当前租户到期分布（含已过期档）
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     **/
    private List<PlatformExpiryBucketDTO> buildExpiryDistribution(List<TenantSnapshot> tenantList,
                                                                  Map<String, TenantPackage> packageMap) {
        Map<String, PlatformExpiryBucketDTO> bucketMap = new LinkedHashMap<>();
        addBucket(bucketMap, "EXPIRED", "已过期", -999999, -1);
        addBucket(bucketMap, "D0_7", "0-7天", 0, 7);
        addBucket(bucketMap, "D8_15", "8-15天", 8, 15);
        addBucket(bucketMap, "D16_30", "16-30天", 16, 30);
        addBucket(bucketMap, "D31_60", "31-60天", 31, 60);
        addBucket(bucketMap, "D61_90", "61-90天", 61, 90);
        addBucket(bucketMap, "D90_PLUS", "90天以上", 91, null);
        LocalDate today = LocalDate.now();
        for (TenantSnapshot tenant : tenantList) {
            if (tenant.expireTime() == null) {
                continue;
            }
            LocalDate expireDate = tenant.expireTime().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            long daysLeft = ChronoUnit.DAYS.between(today, expireDate);
            String bucketKey;
            if (daysLeft < 0) {
                bucketKey = "EXPIRED";
            } else if (daysLeft <= 7) {
                bucketKey = "D0_7";
            } else if (daysLeft <= 15) {
                bucketKey = "D8_15";
            } else if (daysLeft <= 30) {
                bucketKey = "D16_30";
            } else if (daysLeft <= 60) {
                bucketKey = "D31_60";
            } else if (daysLeft <= 90) {
                bucketKey = "D61_90";
            } else {
                bucketKey = "D90_PLUS";
            }
            PlatformExpiryBucketDTO bucket = bucketMap.get(bucketKey);
            bucket.setTenantCount(bucket.getTenantCount() + 1);
            if (!"STOP".equals(tenant.status())) {
                bucket.setMrr(moneyValue(bucket.getMrr().add(monthlyValue(packageMap.get(tenant.packageId())))));
            }
        }
        return new ArrayList<>(bucketMap.values());
    }

    private void addBucket(Map<String, PlatformExpiryBucketDTO> bucketMap, String bucket, String label,
                           Integer minDays, Integer maxDays) {
        PlatformExpiryBucketDTO dto = new PlatformExpiryBucketDTO();
        dto.setBucket(bucket);
        dto.setLabel(label);
        dto.setMinDays(minDays);
        dto.setMaxDays(maxDays);
        bucketMap.put(bucket, dto);
    }

    private List<TenantSnapshot> loadTenantSnapshots(String packageId) {
        StringBuilder sql = new StringBuilder("SELECT t.ID, t.PACKAGE_ID, t.STATUS, t.CREATE_TIME, t.EXPIRE_TIME "
                + "FROM T_TENANT t WHERE t.ID <> ? AND t.DELETE_FLAG = ?");
        List<Object> args = new ArrayList<>();
        args.add(PLATFORM_TENANT_ID);
        args.add(NOT_DELETE);
        if (StrUtil.isNotBlank(packageId)) {
            sql.append(" AND t.PACKAGE_ID = ?");
            args.add(packageId);
        }
        List<TenantSnapshot> resultList = new ArrayList<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql.toString(), args.toArray())) {
            resultList.add(new TenantSnapshot(
                    String.valueOf(row.get("ID")),
                    row.get("PACKAGE_ID") == null ? null : row.get("PACKAGE_ID").toString(),
                    row.get("STATUS") == null ? null : row.get("STATUS").toString(),
                    toDate(row.get("CREATE_TIME")),
                    toDate(row.get("EXPIRE_TIME"))));
        }
        return resultList;
    }

    private Map<String, TenantPackage> loadPackageMap() {
        Map<String, TenantPackage> packageMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT ID, NAME, PRICE, VALIDITY_DAYS FROM T_TENANT_PACKAGE WHERE DELETE_FLAG = ?",
                NOT_DELETE)) {
            TenantPackage tenantPackage = new TenantPackage();
            tenantPackage.setId(String.valueOf(row.get("ID")));
            tenantPackage.setName(row.get("NAME") == null ? null : row.get("NAME").toString());
            tenantPackage.setPrice(toBigDecimal(row.get("PRICE")));
            Object validityDays = row.get("VALIDITY_DAYS");
            tenantPackage.setValidityDays(validityDays instanceof Number number ? number.intValue() : null);
            packageMap.put(tenantPackage.getId(), tenantPackage);
        }
        return packageMap;
    }

    private List<RenewalSnapshot> loadRenewals(Date since, String packageId) {
        StringBuilder sql = new StringBuilder("SELECT r.TENANT_ID, r.AMOUNT, r.CREATE_TIME FROM T_TENANT_RENEW r "
                + "JOIN T_TENANT t ON t.ID = r.TENANT_ID AND t.DELETE_FLAG = ? "
                + "WHERE r.DELETE_FLAG = ?");
        List<Object> args = new ArrayList<>();
        args.add(NOT_DELETE);
        args.add(NOT_DELETE);
        if (since != null) {
            sql.append(" AND r.CREATE_TIME >= ?");
            args.add(since);
        }
        if (StrUtil.isNotBlank(packageId)) {
            sql.append(" AND t.PACKAGE_ID = ?");
            args.add(packageId);
        }
        List<RenewalSnapshot> resultList = new ArrayList<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql.toString(), args.toArray())) {
            resultList.add(new RenewalSnapshot(
                    String.valueOf(row.get("TENANT_ID")),
                    toBigDecimal(row.get("AMOUNT")),
                    toDate(row.get("CREATE_TIME"))));
        }
        return resultList;
    }

    private Set<String> loadRenewalTenantIds(String packageId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT r.TENANT_ID FROM T_TENANT_RENEW r "
                + "JOIN T_TENANT t ON t.ID = r.TENANT_ID AND t.DELETE_FLAG = ? WHERE r.DELETE_FLAG = ?");
        List<Object> args = new ArrayList<>();
        args.add(NOT_DELETE);
        args.add(NOT_DELETE);
        if (StrUtil.isNotBlank(packageId)) {
            sql.append(" AND t.PACKAGE_ID = ?");
            args.add(packageId);
        }
        Set<String> tenantIds = new HashSet<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql.toString(), args.toArray())) {
            tenantIds.add(String.valueOf(row.get("TENANT_ID")));
        }
        return tenantIds;
    }

    private BigDecimal monthlyValue(TenantPackage tenantPackage) {
        if (tenantPackage == null || tenantPackage.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        int validityDays = ObjectUtil.defaultIfNull(tenantPackage.getValidityDays(), 0);
        if (validityDays <= 0) {
            return tenantPackage.getPrice();
        }
        return tenantPackage.getPrice().multiply(BigDecimal.valueOf(30))
                .divide(BigDecimal.valueOf(validityDays), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyValue(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean inMonth(Date date, Date start, Date end) {
        return date != null && !date.before(start) && date.before(end);
    }

    private YearMonth resolveMonth(String month) {
        if (StrUtil.isBlank(month)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new CommonException("月份格式应为 yyyy-MM：{}", month);
        }
    }

    private Date monthStart(YearMonth yearMonth) {
        return Date.from(yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private record TenantSnapshot(String id, String packageId, String status, Date createTime, Date expireTime) {
    }

    private record RenewalSnapshot(String tenantId, BigDecimal amount, Date createTime) {
    }

    @Override
    public void export(String type, HttpServletResponse response) throws IOException {
        String exportType = StrUtil.blankToDefault(type, "OVERVIEW").toUpperCase();
        String sheetName;
        List<List<String>> headList = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        switch (exportType) {
            case "OVERVIEW" -> {
                sheetName = "平台概览";
                headList.add(List.of("指标"));
                headList.add(List.of("数值"));
                PlatformOverviewDTO dto = overview();
                rowList.add(List.of("租户总数", String.valueOf(dto.getTenantTotal())));
                rowList.add(List.of("本月新增租户", String.valueOf(dto.getNewTenantCount())));
                rowList.add(List.of("活跃租户(30天,登录口径)", String.valueOf(dto.getActiveTenantCount())));
                rowList.add(List.of("订单活跃租户(30天)", String.valueOf(dto.getOrderActiveTenantCount())));
                rowList.add(List.of("30天内到期租户", String.valueOf(dto.getExpiringTenantCount())));
                rowList.add(List.of("学员总数", String.valueOf(dto.getStudentTotal())));
                rowList.add(List.of("订单总数", String.valueOf(dto.getOrderCount())));
                rowList.add(List.of("收款总额", money(dto.getPaidAmount())));
                rowList.add(List.of("预估ARR", money(dto.getEstimatedArr())));
                rowList.add(List.of("套餐数", String.valueOf(dto.getPackageCount())));
            }
            case "TREND" -> {
                sheetName = "租户增长趋势";
                headList.add(List.of("月份"));
                headList.add(List.of("当月新增"));
                headList.add(List.of("累计租户"));
                for (PlatformTenantTrendDTO dto : tenantTrend(12)) {
                    rowList.add(List.of(dto.getPeriod(), String.valueOf(dto.getNewCount()), String.valueOf(dto.getTotalCount())));
                }
            }
            case "PACKAGE" -> {
                sheetName = "套餐分布";
                headList.add(List.of("套餐"));
                headList.add(List.of("租户数"));
                headList.add(List.of("占比(%)"));
                for (PlatformPackageDistributionDTO dto : packageDistribution()) {
                    rowList.add(List.of(text(dto.getPackageName()), String.valueOf(dto.getTenantCount()),
                            money(dto.getRatio())));
                }
            }
            case "EXPIRING" -> {
                sheetName = "到期租户";
                headList.add(List.of("机构编码"));
                headList.add(List.of("机构名称"));
                headList.add(List.of("套餐"));
                headList.add(List.of("到期时间"));
                headList.add(List.of("剩余天数"));
                headList.add(List.of("状态"));
                for (PlatformExpiringTenantDTO dto : expiring(30)) {
                    rowList.add(List.of(text(dto.getCode()), text(dto.getName()), text(dto.getPackageName()),
                            dto.getExpireTime() == null ? "" : DateUtil.formatDateTime(dto.getExpireTime()),
                            dto.getDaysLeft() == null ? "" : String.valueOf(dto.getDaysLeft()), text(dto.getStatus())));
                }
            }
            case "RANK" -> {
                sheetName = "租户活跃度排行";
                headList.add(List.of("机构名称"));
                headList.add(List.of("学员数"));
                headList.add(List.of("订单数"));
                headList.add(List.of("收款金额"));
                headList.add(List.of("最近支付时间"));
                for (PlatformTenantRankDTO dto : tenantRank(20)) {
                    rowList.add(List.of(text(dto.getTenantName()), String.valueOf(dto.getStudentCount()),
                            String.valueOf(dto.getOrderCount()), money(dto.getPaidAmount()),
                            dto.getLastPayTime() == null ? "" : DateUtil.formatDateTime(dto.getLastPayTime())));
                }
            }
            default -> throw new CommonException("不支持的导出类型：{}", exportType);
        }
        if (rowList.isEmpty()) {
            throw new CommonException("无数据可导出");
        }
        File file = FileUtil.file(FileUtil.getTmpDir(), "平台报表_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
        try {
            CommonExcelUtil.write(file, sheetName, headList, rowList);
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private BigDecimal queryBigDecimal(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private Date toDate(Object value) {
        if (value instanceof Date date) {
            return date;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return new Date(timestamp.getTime());
        }
        // MySQL Connector/J 8 在部分配置下 getObject 返回 LocalDateTime/LocalDate，不能直接丢掉
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof java.time.LocalDate localDate) {
            return Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof String text && cn.hutool.core.util.StrUtil.isNotBlank(text)) {
            try {
                return DateUtil.parse(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
