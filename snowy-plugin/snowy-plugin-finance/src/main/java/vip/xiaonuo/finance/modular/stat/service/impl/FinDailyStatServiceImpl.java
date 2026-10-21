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
package vip.xiaonuo.finance.modular.stat.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.finance.modular.stat.entity.FinDailyStat;
import vip.xiaonuo.finance.modular.stat.mapper.FinDailyStatMapper;
import vip.xiaonuo.finance.modular.stat.service.FinDailyStatService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务日汇总Service接口实现类
 * <p>
 * 汇总使用 JdbcTemplate 直接查询业务表并显式带上 TENANT_ID 条件，避免绕过租户隔离；
 * 结果写入 FIN_DAILY_STAT（该表已登记进租户拦截表清单）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Service
public class FinDailyStatServiceImpl implements FinDailyStatService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private FinDailyStatMapper finDailyStatMapper;

    @Override
    public void aggregate(String statDate) {
        String tenantId = currentTenantId();
        Date start = DateUtil.beginOfDay(DateUtil.parse(statDate));
        Date end = DateUtil.offsetDay(start, 1);

        Map<String, FinDailyStat> statMap = new LinkedHashMap<>();

        // 收款
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT COALESCE(`CAMPUS_ID`,'') AS `CAMPUS_ID`, COALESCE(`COURSE_ID`,'') AS `COURSE_ID`, "
                        + "COUNT(*) AS `ORDER_COUNT`, COUNT(DISTINCT `STUDENT_ID`) AS `PAYING_STUDENT_COUNT`, "
                        + "COALESCE(SUM(`PAID_AMOUNT`),0) AS `PAID_AMOUNT` FROM `STUDENT_ORDER` "
                        + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` IN ('PAID','REFUNDED') "
                        + "AND COALESCE(`PAY_TIME`, `CREATE_TIME`) >= ? AND COALESCE(`PAY_TIME`, `CREATE_TIME`) < ? "
                        + "GROUP BY `CAMPUS_ID`, `COURSE_ID`",
                tenantId, start, end)) {
            FinDailyStat stat = statOf(statMap, string(row.get("CAMPUS_ID")), string(row.get("COURSE_ID")));
            stat.setOrderCount(toLong(row.get("ORDER_COUNT")));
            stat.setPayingStudentCount(toLong(row.get("PAYING_STUDENT_COUNT")));
            stat.setPaidAmount(toBigDecimal(row.get("PAID_AMOUNT")));
        }

        // 退费
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT COALESCE(o.`CAMPUS_ID`,'') AS `CAMPUS_ID`, COALESCE(r.`COURSE_ID`,'') AS `COURSE_ID`, "
                        + "COALESCE(SUM(r.`REFUND_AMOUNT`),0) AS `REFUND_AMOUNT` FROM `STUDENT_REFUND` r "
                        + "LEFT JOIN `STUDENT_ORDER` o ON o.`ID` = r.`ORDER_ID` AND o.`TENANT_ID` = r.`TENANT_ID` "
                        + "AND o.`DELETE_FLAG` = 'NOT_DELETE' "
                        + "WHERE r.`TENANT_ID` = ? AND r.`DELETE_FLAG` = 'NOT_DELETE' AND r.`STATUS` = 'REFUNDED' "
                        + "AND COALESCE(r.`REFUND_TIME`, r.`APPROVE_TIME`, r.`UPDATE_TIME`, r.`CREATE_TIME`) >= ? "
                        + "AND COALESCE(r.`REFUND_TIME`, r.`APPROVE_TIME`, r.`UPDATE_TIME`, r.`CREATE_TIME`) < ? "
                        + "GROUP BY o.`CAMPUS_ID`, r.`COURSE_ID`",
                tenantId, start, end)) {
            FinDailyStat stat = statOf(statMap, string(row.get("CAMPUS_ID")), string(row.get("COURSE_ID")));
            stat.setRefundAmount(toBigDecimal(row.get("REFUND_AMOUNT")));
        }

        // 课消
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT COALESCE(`COURSE_ID`,'') AS `COURSE_ID`, "
                        + "COALESCE(SUM(CASE WHEN `FLOW_TYPE` = 'ATTEND' THEN ABS(`LESSONS`) ELSE 0 END),0) AS `ATTEND_LESSONS`, "
                        + "COALESCE(SUM(CASE WHEN `FLOW_TYPE` = 'RESTORE' THEN ABS(`LESSONS`) ELSE 0 END),0) AS `RESTORE_LESSONS` "
                        + "FROM `STUDENT_LESSON_FLOW` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' "
                        + "AND `FLOW_TYPE` IN ('ATTEND','RESTORE') AND `CREATE_TIME` >= ? AND `CREATE_TIME` < ? "
                        + "GROUP BY `COURSE_ID`",
                tenantId, start, end)) {
            FinDailyStat stat = statOf(statMap, "", string(row.get("COURSE_ID")));
            stat.setAttendLessons(toBigDecimal(row.get("ATTEND_LESSONS")));
            stat.setRestoreLessons(toBigDecimal(row.get("RESTORE_LESSONS")));
        }

        // 新增学员
        Long newStudentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `STUDENT` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' "
                        + "AND `CREATE_TIME` >= ? AND `CREATE_TIME` < ?",
                Long.class, tenantId, start, end);
        if (ObjectUtil.isNotEmpty(newStudentCount) && newStudentCount > 0) {
            FinDailyStat stat = statOf(statMap, "", "");
            stat.setNewStudentCount(newStudentCount);
        }

        // 覆盖写入当日汇总
        jdbcTemplate.update("DELETE FROM `FIN_DAILY_STAT` WHERE `TENANT_ID` = ? AND `STAT_DATE` = ?", tenantId, start);
        for (FinDailyStat stat : statMap.values()) {
            stat.setId(IdUtil.getSnowflakeNextIdStr());
            stat.setTenantId(tenantId);
            stat.setStatDate(start);
            stat.setNetAmount(stat.getPaidAmount().subtract(stat.getRefundAmount()));
            stat.setNetLessons(stat.getAttendLessons().subtract(stat.getRestoreLessons()));
            finDailyStatMapper.insert(stat);
        }
    }

    @Override
    public List<FinDailyStat> listByDate(String startDate, String endDate) {
        String tenantId = currentTenantId();
        Date start = DateUtil.beginOfDay(DateUtil.parse(startDate));
        Date end = DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parse(endDate)), 1);
        List<Map<String, Object>> rowList = jdbcTemplate.queryForList(
                "SELECT `STAT_DATE`, COALESCE(SUM(`PAID_AMOUNT`),0) AS `PAID_AMOUNT`, "
                        + "COALESCE(SUM(`REFUND_AMOUNT`),0) AS `REFUND_AMOUNT`, "
                        + "COALESCE(SUM(`NET_AMOUNT`),0) AS `NET_AMOUNT`, "
                        + "COALESCE(SUM(`ORDER_COUNT`),0) AS `ORDER_COUNT`, "
                        + "COALESCE(SUM(`NET_LESSONS`),0) AS `NET_LESSONS`, "
                        + "COALESCE(SUM(`NEW_STUDENT_COUNT`),0) AS `NEW_STUDENT_COUNT` "
                        + "FROM `FIN_DAILY_STAT` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' "
                        + "AND `STAT_DATE` >= ? AND `STAT_DATE` < ? GROUP BY `STAT_DATE` ORDER BY `STAT_DATE`",
                tenantId, start, end);
        List<FinDailyStat> resultList = new ArrayList<>();
        for (Map<String, Object> row : rowList) {
            FinDailyStat stat = new FinDailyStat();
            stat.setStatDate(toDate(row.get("STAT_DATE")));
            stat.setPaidAmount(toBigDecimal(row.get("PAID_AMOUNT")));
            stat.setRefundAmount(toBigDecimal(row.get("REFUND_AMOUNT")));
            stat.setNetAmount(toBigDecimal(row.get("NET_AMOUNT")));
            stat.setOrderCount(toLong(row.get("ORDER_COUNT")));
            stat.setNetLessons(toBigDecimal(row.get("NET_LESSONS")));
            stat.setNewStudentCount(toLong(row.get("NEW_STUDENT_COUNT")));
            resultList.add(stat);
        }
        return resultList;
    }

    @Override
    public BigDecimal sumPaidAmount(String startDate, String endDate) {
        String tenantId = currentTenantId();
        Date start = DateUtil.beginOfDay(DateUtil.parse(startDate));
        Date end = DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parse(endDate)), 1);
        BigDecimal value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(`PAID_AMOUNT`),0) FROM `FIN_DAILY_STAT` "
                        + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STAT_DATE` >= ? AND `STAT_DATE` < ?",
                BigDecimal.class, tenantId, start, end);
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 取或创建汇总项
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private FinDailyStat statOf(Map<String, FinDailyStat> statMap, String campusId, String courseId) {
        String key = campusId + "|" + courseId;
        return statMap.computeIfAbsent(key, k -> {
            FinDailyStat stat = new FinDailyStat();
            stat.setCampusId(StrUtil.emptyToNull(campusId));
            stat.setCourseId(StrUtil.emptyToNull(courseId));
            return stat;
        });
    }

    private String currentTenantId() {
        String tenantId = TenantContext.getTenantId();
        return StrUtil.blankToDefault(tenantId, "0");
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private Long toLong(Object value) {
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

    private Date toDate(Object value) {
        if (value instanceof Date date) {
            return date;
        }
        return null;
    }
}
