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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 财务日汇总Service接口实现类
 * <p>
 * 汇总使用 JdbcTemplate 直接查询业务表并显式带上 TENANT_ID 条件，避免绕过租户隔离；
 * v1 按「租户 + 日期」单行汇总（校区/课程维度留空，维度下钻仍走实时聚合），
 * 每一天都会落一行（含零值行），用于判断区间是否已被完整汇总覆盖。
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

        FinDailyStat stat = new FinDailyStat();
        stat.setStatDate(start);

        // 收款
        MapWrapper orderStat = queryOne(
                "SELECT COUNT(*) AS `ORDER_COUNT`, COUNT(DISTINCT `STUDENT_ID`) AS `PAYING_STUDENT_COUNT`, "
                        + "COALESCE(SUM(`PAID_AMOUNT`),0) AS `PAID_AMOUNT` FROM `STUDENT_ORDER` "
                        + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` IN ('PAID','REFUNDED') "
                        + "AND COALESCE(`PAY_TIME`, `CREATE_TIME`) >= ? AND COALESCE(`PAY_TIME`, `CREATE_TIME`) < ?",
                tenantId, start, end);
        stat.setOrderCount(orderStat.getLong("ORDER_COUNT"));
        stat.setPayingStudentCount(orderStat.getLong("PAYING_STUDENT_COUNT"));
        stat.setPaidAmount(orderStat.getBigDecimal("PAID_AMOUNT"));

        // 退费
        MapWrapper refundStat = queryOne(
                "SELECT COALESCE(SUM(`REFUND_AMOUNT`),0) AS `REFUND_AMOUNT` FROM `STUDENT_REFUND` "
                        + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STATUS` = 'REFUNDED' "
                        + "AND COALESCE(`REFUND_TIME`, `APPROVE_TIME`, `UPDATE_TIME`, `CREATE_TIME`) >= ? "
                        + "AND COALESCE(`REFUND_TIME`, `APPROVE_TIME`, `UPDATE_TIME`, `CREATE_TIME`) < ?",
                tenantId, start, end);
        stat.setRefundAmount(refundStat.getBigDecimal("REFUND_AMOUNT"));

        // 课消课时
        MapWrapper lessonStat = queryOne(
                "SELECT COALESCE(SUM(CASE WHEN `FLOW_TYPE` = 'ATTEND' THEN ABS(`LESSONS`) ELSE 0 END),0) AS `ATTEND_LESSONS`, "
                        + "COALESCE(SUM(CASE WHEN `FLOW_TYPE` = 'RESTORE' THEN ABS(`LESSONS`) ELSE 0 END),0) AS `RESTORE_LESSONS` "
                        + "FROM `STUDENT_LESSON_FLOW` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' "
                        + "AND `FLOW_TYPE` IN ('ATTEND','RESTORE') AND `CREATE_TIME` >= ? AND `CREATE_TIME` < ?",
                tenantId, start, end);
        stat.setAttendLessons(lessonStat.getBigDecimal("ATTEND_LESSONS"));
        stat.setRestoreLessons(lessonStat.getBigDecimal("RESTORE_LESSONS"));

        // 课消金额（权责口径）：消课 × 订单购买课时单价，退费销课与回补为负
        MapWrapper recognizedStat = queryOne(
                "SELECT COALESCE(SUM(CASE WHEN f.`FLOW_TYPE` = 'ATTEND' THEN ABS(f.`LESSONS`) * (o.`PAID_AMOUNT` / NULLIF(o.`PACKAGE_LESSONS`,0)) "
                        + "WHEN f.`FLOW_TYPE` = 'RESTORE' THEN -ABS(f.`LESSONS`) * (o.`PAID_AMOUNT` / NULLIF(o.`PACKAGE_LESSONS`,0)) "
                        + "ELSE 0 END),0) AS `RECOGNIZED_AMOUNT` "
                        + "FROM `STUDENT_LESSON_FLOW` f "
                        + "JOIN `STUDENT_ORDER` o ON o.`ID` = f.`REF_ORDER_ID` AND o.`TENANT_ID` = f.`TENANT_ID` AND o.`DELETE_FLAG` = 'NOT_DELETE' "
                        + "WHERE f.`TENANT_ID` = ? AND f.`DELETE_FLAG` = 'NOT_DELETE' AND f.`CREATE_TIME` >= ? AND f.`CREATE_TIME` < ?",
                tenantId, start, end);
        stat.setRecognizedAmount(recognizedStat.getBigDecimal("RECOGNIZED_AMOUNT"));

        // 新增学员
        Long newStudentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `STUDENT` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' "
                        + "AND `CREATE_TIME` >= ? AND `CREATE_TIME` < ?",
                Long.class, tenantId, start, end);
        stat.setNewStudentCount(ObjectUtil.defaultIfNull(newStudentCount, 0L));

        stat.setNetAmount(stat.getPaidAmount().subtract(stat.getRefundAmount()));
        stat.setNetLessons(stat.getAttendLessons().subtract(stat.getRestoreLessons()));

        // 覆盖写入当日汇总
        jdbcTemplate.update("DELETE FROM `FIN_DAILY_STAT` WHERE `TENANT_ID` = ? AND `STAT_DATE` = ?", tenantId, start);
        stat.setId(IdUtil.getSnowflakeNextIdStr());
        stat.setTenantId(tenantId);
        finDailyStatMapper.insert(stat);
    }

    @Override
    public List<FinDailyStat> listByDate(String startDate, String endDate) {
        String tenantId = currentTenantId();
        Date start = DateUtil.beginOfDay(DateUtil.parse(startDate));
        Date end = DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parse(endDate)), 1);
        return finDailyStatMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinDailyStat>()
                .eq(FinDailyStat::getTenantId, tenantId)
                .ge(FinDailyStat::getStatDate, start)
                .lt(FinDailyStat::getStatDate, end)
                .orderByAsc(FinDailyStat::getStatDate));
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

    @Override
    public boolean covered(String startDate, String endDate) {
        String tenantId = currentTenantId();
        Date start = DateUtil.beginOfDay(DateUtil.parse(startDate));
        Date end = DateUtil.offsetDay(DateUtil.beginOfDay(DateUtil.parse(endDate)), 1);
        // 结束时间为开区间上界：等于今天零点说明只覆盖到昨天，未闭环的区间不走日汇总快路径
        if (end.after(DateUtil.beginOfDay(new Date()))) {
            return false;
        }
        Long dayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT `STAT_DATE`) FROM `FIN_DAILY_STAT` "
                        + "WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE' AND `STAT_DATE` >= ? AND `STAT_DATE` < ?",
                Long.class, tenantId, start, end);
        long expectDays = DateUtil.betweenDay(start, end, true);
        return dayCount != null && dayCount == expectDays;
    }

    /**
     * 单行查询结果
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private MapWrapper queryOne(String sql, Object... args) {
        List<Map<String, Object>> rowList = jdbcTemplate.queryForList(sql, args);
        if (rowList.isEmpty()) {
            return new MapWrapper();
        }
        return new MapWrapper(rowList.get(0));
    }

    private String currentTenantId() {
        String tenantId = TenantContext.getTenantId();
        return StrUtil.blankToDefault(tenantId, "0");
    }

    /**
     * 查询结果行读取包装
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private static class MapWrapper {

        private final java.util.Map<String, Object> data;

        MapWrapper() {
            this.data = new java.util.HashMap<>();
        }

        MapWrapper(java.util.Map<String, Object> data) {
            this.data = data;
        }

        long getLong(String key) {
            Object value = data.get(key);
            return value instanceof Number number ? number.longValue() : 0L;
        }

        BigDecimal getBigDecimal(String key) {
            Object value = data.get(key);
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return BigDecimal.ZERO;
        }
    }
}
