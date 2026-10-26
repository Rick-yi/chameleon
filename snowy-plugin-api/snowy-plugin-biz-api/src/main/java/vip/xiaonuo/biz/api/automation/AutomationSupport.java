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
package vip.xiaonuo.biz.api.automation;

import cn.hutool.crypto.digest.DigestUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 自动化公共工具：稳定分组、幂等键、静默时段判断
 *
 * @author 会飞的猪
 * @date 2026/10/26 18:00
 **/
public final class AutomationSupport {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private AutomationSupport() {
    }

    /**
     * 稳定实验分组桶：同一租户/规则/目标永远落到同一个 0-99 桶
     */
    public static int holdoutBucket(String tenantId, String ruleCode, String targetId) {
        String hash = DigestUtil.sha256Hex(String.join("|",
                Objects.toString(tenantId, ""),
                Objects.toString(ruleCode, ""),
                Objects.toString(targetId, "")));
        return (int) (Long.parseLong(hash.substring(0, 8), 16) % 100);
    }

    /**
     * 是否命中对照组
     */
    public static boolean isHoldout(String tenantId, String ruleCode, String targetId, int holdoutPercent) {
        if (holdoutPercent <= 0) {
            return false;
        }
        if (holdoutPercent >= 100) {
            return true;
        }
        return holdoutBucket(tenantId, ruleCode, targetId) < holdoutPercent;
    }

    /**
     * 幂等键：同一规则、动作、目标和窗口只允许一次有效动作
     */
    public static String idempotencyKey(String ruleCode, String action, String targetId, String windowKey) {
        return String.join("|", Objects.toString(ruleCode, ""), Objects.toString(action, ""),
                Objects.toString(targetId, ""), Objects.toString(windowKey, ""));
    }

    /**
     * 判断当前时间是否在静默时段内，支持跨午夜（如 22:00-08:00）
     */
    public static boolean inQuietHours(LocalTime now, String quietStart, String quietEnd) {
        if (now == null || quietStart == null || quietEnd == null) {
            return false;
        }
        try {
            LocalTime start = LocalTime.parse(quietStart, HH_MM);
            LocalTime end = LocalTime.parse(quietEnd, HH_MM);
            if (start.equals(end)) {
                return false;
            }
            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }
            return !now.isBefore(start) || now.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    /** 校验 HH:mm 格式（00:00-23:59） */
    public static boolean isValidHhMm(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (!value.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            return false;
        }
        try {
            LocalTime.parse(value, HH_MM);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
