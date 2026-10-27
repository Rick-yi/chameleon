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
package vip.xiaonuo.finance.modular.util;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 财务对比期计算
 * <p>
 * 只做纯日期区间计算，不依赖数据库与租户上下文，便于单元测试覆盖：
 * 环比 = 与当前区间等长、紧邻其前的上一周期；同比 = 起止日期各减一年。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26
 **/
public final class FinanceCompareSupport {

    /** 对比方式：环比 */
    public static final String MODE_MOM = "MOM";

    /** 对比方式：同比 */
    public static final String MODE_YOY = "YOY";

    /** 对比方式：自定义 */
    public static final String MODE_CUSTOM = "CUSTOM";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FinanceCompareSupport() {
    }

    /**
     * 解析对比方式，缺省按环比处理
     */
    public static String normalizeMode(String mode) {
        if (StrUtil.equalsIgnoreCase(MODE_YOY, mode)) {
            return MODE_YOY;
        }
        if (StrUtil.equalsIgnoreCase(MODE_CUSTOM, mode)) {
            return MODE_CUSTOM;
        }
        return MODE_MOM;
    }

    /**
     * 当前统计区间：缺省为本月 1 日至今天，并统一补齐为整天边界
     */
    public static Range currentRange(String startTime, String endTime) {
        LocalDate today = LocalDate.now();
        LocalDate start = parseDate(startTime, today.withDayOfMonth(1));
        LocalDate end = parseDate(endTime, today);
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        return toRange(start, end);
    }

    /**
     * 环比区间：与当前区间天数相同、紧邻其前
     */
    public static Range previousRange(Range current) {
        LocalDate start = LocalDate.parse(current.startDate(), DATE_FORMATTER);
        LocalDate end = LocalDate.parse(current.endDate(), DATE_FORMATTER);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);
        return toRange(previousStart, previousEnd);
    }

    /**
     * 同比区间：起止日期各减一年（2 月 29 日自动落到 2 月 28 日）
     */
    public static Range yoyRange(Range current) {
        LocalDate start = LocalDate.parse(current.startDate(), DATE_FORMATTER);
        LocalDate end = LocalDate.parse(current.endDate(), DATE_FORMATTER);
        return toRange(start.minusYears(1), end.minusYears(1));
    }

    /**
     * 自定义对比区间，缺少任一端返回 null
     */
    public static Range customRange(String startTime, String endTime) {
        if (StrUtil.isBlank(startTime) || StrUtil.isBlank(endTime)) {
            return null;
        }
        try {
            LocalDate start = LocalDate.parse(StrUtil.subPre(startTime, 10), DATE_FORMATTER);
            LocalDate end = LocalDate.parse(StrUtil.subPre(endTime, 10), DATE_FORMATTER);
            if (start.isAfter(end)) {
                LocalDate swap = start;
                start = end;
                end = swap;
            }
            return toRange(start, end);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value, LocalDate defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(StrUtil.subPre(value, 10), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }

    private static Range toRange(LocalDate start, LocalDate end) {
        return new Range(start.format(DATE_FORMATTER) + " 00:00:00", end.format(DATE_FORMATTER) + " 23:59:59",
                start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
    }

    /**
     * 区间（同时保留日期部分，便于纯逻辑测试）
     *
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间 yyyy-MM-dd HH:mm:ss
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     */
    public record Range(String startTime, String endTime, String startDate, String endDate) {

        public String label() {
            return startDate + " ~ " + endDate;
        }

        @Override
        public String toString() {
            return DATETIME_FORMATTER.format(java.time.LocalDateTime.parse(startTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) + " ~ " + endTime;
        }
    }
}
