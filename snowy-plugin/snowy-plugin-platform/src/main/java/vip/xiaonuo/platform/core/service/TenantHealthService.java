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
package vip.xiaonuo.platform.core.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vip.xiaonuo.platform.modular.report.dto.PlatformTenantHealthDTO;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.mapper.TenantPackageMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户健康度评分
 * <p>
 * 只做可解释的规则评分：登录频次 30 + 课消 25 + 收款 25 + 到期风险 20。
 * 数据全部来自现有登录、点名、订单、租户到期字段，不接外部模型，也不落重复统计表。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 11:00
 **/
@Service
public class TenantHealthService {

    private static final String NOT_DELETE = "NOT_DELETE";

    private static final String PLATFORM_TENANT_ID = "0";

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    /**
     * 对给定租户批量评分
     *
     * @author 会飞的猪
     * @date 2026/10/26 11:00
     **/
    public Map<String, PlatformTenantHealthDTO> evaluate(List<Tenant> tenantList) {
        Map<String, PlatformTenantHealthDTO> resultMap = new LinkedHashMap<>();
        if (ObjectUtil.isEmpty(tenantList)) {
            return resultMap;
        }
        List<String> packageIdList = tenantList.stream().map(Tenant::getPackageId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, TenantPackage> packageMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(packageIdList)) {
            tenantPackageMapper.selectBatchIds(packageIdList)
                    .forEach(item -> packageMap.put(item.getId(), item));
        }
        Date loginSince = new Date(System.currentTimeMillis() - 30 * DAY_MILLIS);
        Date consumeSince = new Date(System.currentTimeMillis() - 30 * DAY_MILLIS);
        Date paySince = new Date(System.currentTimeMillis() - 90 * DAY_MILLIS);

        Map<String, Date> lastLoginMap = new HashMap<>();
        Map<String, Long> loginCountMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, MAX(LOGIN_TIME) AS LAST_LOGIN_TIME, "
                        + "SUM(CASE WHEN LOGIN_TIME >= ? THEN 1 ELSE 0 END) AS LOGIN_CNT "
                        + "FROM T_TENANT_LOGIN WHERE DELETE_FLAG = ? GROUP BY TENANT_ID",
                loginSince, NOT_DELETE)) {
            String tenantId = String.valueOf(row.get("TENANT_ID"));
            lastLoginMap.put(tenantId, toDate(row.get("LAST_LOGIN_TIME")));
            loginCountMap.put(tenantId, toLong(row.get("LOGIN_CNT")));
        }
        Map<String, Long> consumeCountMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, COUNT(*) AS CNT FROM COURSE_CLASS_ATTEND "
                        + "WHERE DELETE_FLAG = ? AND ATTEND_STATUS = 'ATTEND' "
                        + "AND COALESCE(ATTEND_TIME, CREATE_TIME) >= ? GROUP BY TENANT_ID",
                NOT_DELETE, consumeSince)) {
            consumeCountMap.put(String.valueOf(row.get("TENANT_ID")), toLong(row.get("CNT")));
        }
        Map<String, BigDecimal> paidAmountMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, COALESCE(SUM(PAID_AMOUNT),0) AS AMT FROM STUDENT_ORDER "
                        + "WHERE DELETE_FLAG = ? AND STATUS IN ('PAID','REFUNDED') "
                        + "AND COALESCE(PAY_TIME, CREATE_TIME) >= ? GROUP BY TENANT_ID",
                NOT_DELETE, paySince)) {
            paidAmountMap.put(String.valueOf(row.get("TENANT_ID")), toBigDecimal(row.get("AMT")));
        }

        for (Tenant tenant : tenantList) {
            PlatformTenantHealthDTO dto = new PlatformTenantHealthDTO();
            dto.setTenantId(tenant.getId());
            dto.setTenantCode(tenant.getCode());
            dto.setTenantName(tenant.getName());
            dto.setStatus(tenant.getStatus());
            dto.setPackageId(tenant.getPackageId());
            dto.setExpireTime(tenant.getExpireTime());
            TenantPackage tenantPackage = packageMap.get(tenant.getPackageId());
            if (ObjectUtil.isNotEmpty(tenantPackage)) {
                dto.setPackageName(tenantPackage.getName());
            }
            dto.setLastLoginTime(lastLoginMap.get(tenant.getId()));
            dto.setLoginCount30d(loginCountMap.getOrDefault(tenant.getId(), 0L));
            dto.setConsumeCount30d(consumeCountMap.getOrDefault(tenant.getId(), 0L));
            dto.setPaidAmount90d(paidAmountMap.getOrDefault(tenant.getId(), BigDecimal.ZERO));
            if (tenant.getExpireTime() != null) {
                LocalDate expireDate = tenant.getExpireTime().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                dto.setDaysLeft(ChronoUnit.DAYS.between(LocalDate.now(), expireDate));
            }
            score(dto);
            resultMap.put(tenant.getId(), dto);
        }
        return resultMap;
    }

    /**
     * 按健康度从低到高返回（风险租户优先），用于流失预警
     *
     * @author 会飞的猪
     * @date 2026/10/26 11:00
     **/
    public List<PlatformTenantHealthDTO> rank(String packageId, Integer limit) {
        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, PLATFORM_TENANT_ID);
        if (StrUtil.isNotBlank(packageId)) {
            queryWrapper.eq(Tenant::getPackageId, packageId);
        }
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        List<PlatformTenantHealthDTO> resultList = new ArrayList<>(evaluate(tenantList).values());
        resultList.sort(Comparator.comparing(PlatformTenantHealthDTO::getScore)
                .thenComparing(PlatformTenantHealthDTO::getChurnWarning, Comparator.reverseOrder()));
        int size = ObjectUtil.defaultIfNull(limit, 20);
        size = Math.min(Math.max(size, 1), 200);
        return resultList.stream().limit(size).collect(Collectors.toList());
    }

    /**
     * 规则评分：0-30 登录、0-25 课消、0-25 收款、0-20 到期
     *
     * @author 会飞的猪
     * @date 2026/10/26 11:00
     **/
    private void score(PlatformTenantHealthDTO dto) {
        List<String> reasons = new ArrayList<>();

        int loginScore;
        if (dto.getLastLoginTime() == null) {
            loginScore = 0;
            reasons.add("近30天无登录");
        } else {
            long days = Math.max(0,
                    (System.currentTimeMillis() - dto.getLastLoginTime().getTime()) / DAY_MILLIS);
            if (days <= 3) {
                loginScore = 30;
            } else if (days <= 7) {
                loginScore = 26;
            } else if (days <= 14) {
                loginScore = 20;
            } else if (days <= 30) {
                loginScore = 14;
            } else if (days <= 60) {
                loginScore = 7;
                reasons.add("超过30天未登录");
            } else {
                loginScore = 0;
                reasons.add("超过60天未登录");
            }
        }

        long consume = ObjectUtil.defaultIfNull(dto.getConsumeCount30d(), 0L);
        int consumeScore;
        if (consume >= 50) {
            consumeScore = 25;
        } else if (consume >= 20) {
            consumeScore = 20;
        } else if (consume >= 5) {
            consumeScore = 12;
        } else if (consume >= 1) {
            consumeScore = 6;
        } else {
            consumeScore = 0;
            reasons.add("近30天无课消");
        }

        BigDecimal paid = ObjectUtil.defaultIfNull(dto.getPaidAmount90d(), BigDecimal.ZERO);
        int paymentScore;
        if (paid.compareTo(BigDecimal.valueOf(50000)) >= 0) {
            paymentScore = 25;
        } else if (paid.compareTo(BigDecimal.valueOf(20000)) >= 0) {
            paymentScore = 20;
        } else if (paid.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            paymentScore = 15;
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            paymentScore = 8;
        } else {
            paymentScore = 0;
            reasons.add("近90天无收款");
        }

        int expiryScore;
        Long daysLeft = dto.getDaysLeft();
        if (daysLeft == null) {
            expiryScore = 10;
        } else if (daysLeft <= 0) {
            expiryScore = 0;
            reasons.add("租户已到期");
        } else if (daysLeft <= 7) {
            expiryScore = 4;
            reasons.add("7天内到期");
        } else if (daysLeft <= 15) {
            expiryScore = 8;
            reasons.add("15天内到期");
        } else if (daysLeft <= 30) {
            expiryScore = 12;
            reasons.add("30天内到期");
        } else if (daysLeft <= 60) {
            expiryScore = 16;
        } else {
            expiryScore = 20;
        }

        int score = loginScore + consumeScore + paymentScore + expiryScore;
        boolean stopped = "STOP".equalsIgnoreCase(dto.getStatus());
        if (stopped) {
            reasons.add("租户已停用");
            score = Math.min(score, 20);
        }
        String level;
        if (score >= 70) {
            level = "HEALTHY";
        } else if (score >= 40) {
            level = "WARNING";
        } else {
            level = "RISK";
        }
        dto.setLoginScore(loginScore);
        dto.setConsumeScore(consumeScore);
        dto.setPaymentScore(paymentScore);
        dto.setExpiryScore(expiryScore);
        dto.setScore(score);
        dto.setLevel(level);
        dto.setChurnWarning("RISK".equals(level) || stopped
                || (daysLeft != null && daysLeft <= 7));
        dto.setReasons(reasons);
    }

    private Date toDate(Object value) {
        if (value instanceof Date date) {
            return date;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return new Date(timestamp.getTime());
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof java.time.LocalDate localDate) {
            return Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof String text && cn.hutool.core.util.StrUtil.isNotBlank(text)) {
            try {
                return cn.hutool.core.date.DateUtil.parse(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
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
}
