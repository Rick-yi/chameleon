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

import java.util.List;
import java.util.Map;

/**
 * 自动化规则执行器（SPI）
 * <p>
 * 各业务插件实现本接口并注册为 Spring Bean，自动化中心即可统一管理：
 * 开关、参数、立即执行、定时执行与执行记录。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
public interface BizAutomationHandler {

    /** 规则编码（全局唯一） */
    String getRuleCode();

    /** 规则名称 */
    String getRuleName();

    /** 规则说明：告诉用户这条规则替他省掉了什么操作 */
    String getRuleDesc();

    /** 执行模式：TIMER 定时执行 / TRIGGER 业务触发时实时执行 */
    String getMode();

    /** 默认是否开启 */
    default boolean defaultEnabled() {
        return false;
    }

    /** 规则版本：决策日志用它定位当时执行的规则定义 */
    default int getRuleVersion() {
        return 1;
    }

    /** 是否是提醒类规则（受静默时段约束） */
    default boolean isNotificationRule() {
        return false;
    }

    /** 是否支持 holdout 实验分组 */
    default boolean supportsHoldout() {
        return isNotificationRule();
    }

    /**
     * 参数定义（前端据此渲染配置表单）
     *
     * @return 参数定义列表，无参数返回空列表
     */
    default List<AutomationParam> getParamDef() {
        return List.of();
    }

    /**
     * 执行规则
     *
     * @param context 执行上下文（参数、触发方式）
     * @return 执行结果
     */
    AutomationResult execute(AutomationContext context);

    /**
     * 参数定义
     *
     * @param key      参数键
     * @param label    参数名称
     * @param type     参数类型：NUMBER 数字 / TEXT 文本 / SWITCH 开关
     * @param defaultValue 默认值
     * @param remark   参数说明
     * @param min      数字最小值（可空）
     * @param max      数字最大值（可空）
     */
    record AutomationParam(String key, String label, String type, String defaultValue, String remark,
                           Integer min, Integer max) {

        public AutomationParam(String key, String label, String type, String defaultValue, String remark) {
            this(key, label, type, defaultValue, remark, null, null);
        }
    }

    /**
     * 目标级结果记录器：由引擎实现，规则通过上下文调用
     */
    interface TargetRecorder {

        /** 占位成功返回 true；已执行过、超出配额或并发冲突返回 false */
        boolean claim(String targetType, String targetId, String action, String windowKey);

        /** 动作成功 */
        void success(String targetType, String targetId, String action, String messageId, String detail);

        /** 动作失败：保留失败记录，允许下一轮按重试次数重试 */
        void fail(String targetType, String targetId, String action, String reason);

        /** 跳过：如数据不满足、实验对照组、无接收人等 */
        void skip(String targetType, String targetId, String action, String reason, String experimentGroup);

        /** 是否命中 holdout 对照组 */
        boolean isHoldout(String targetId);

        /** 实验分组：TREATMENT / HOLDOUT / NONE */
        String experimentGroup(String targetId);

        TargetRecorder NOOP = new TargetRecorder() {
            @Override
            public boolean claim(String targetType, String targetId, String action, String windowKey) {
                return true;
            }

            @Override
            public void success(String targetType, String targetId, String action, String messageId, String detail) {
            }

            @Override
            public void fail(String targetType, String targetId, String action, String reason) {
            }

            @Override
            public void skip(String targetType, String targetId, String action, String reason, String experimentGroup) {
            }

            @Override
            public boolean isHoldout(String targetId) {
                return false;
            }

            @Override
            public String experimentGroup(String targetId) {
                return "NONE";
            }
        };
    }

    /**
     * 执行上下文
     *
     * @param params      规则参数
     * @param triggerType 触发方式：TIMER 定时 / MANUAL 手动 / TRIGGER 实时
     * @param operatorId  操作人（系统任务为 -1）
     * @param dryRun      预览模式：只计算影响面，不产生副作用
     * @param remainingQuota 本轮剩余可执行条数
     * @param tenantId    租户id
     * @param ruleCode    规则编码
     * @param recorder    目标级结果记录器
     */
    record AutomationContext(Map<String, String> params, String triggerType, String operatorId,
                             boolean dryRun, int remainingQuota, String tenantId, String ruleCode,
                             TargetRecorder recorder) {

        /** 兼容旧构造：非预览、不限制、无记录器 */
        public AutomationContext(Map<String, String> params, String triggerType, String operatorId) {
            this(params, triggerType, operatorId, false, Integer.MAX_VALUE, null, null, TargetRecorder.NOOP);
        }

        /** 取参数（带默认值兜底） */
        public String getParam(String key, String defaultValue) {
            String value = params == null ? null : params.get(key);
            return value == null || value.isBlank() ? defaultValue : value;
        }

        /** 取整数参数 */
        public int getIntParam(String key, int defaultValue) {
            try {
                return Integer.parseInt(getParam(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public boolean isHoldout(String targetId) {
            return recorder != null && recorder.isHoldout(targetId);
        }

        public String experimentGroup(String targetId) {
            return recorder == null ? "NONE" : recorder.experimentGroup(targetId);
        }

        public boolean claim(String targetType, String targetId, String action, String windowKey) {
            return dryRun || (recorder != null && recorder.claim(targetType, targetId, action, windowKey));
        }

        public void success(String targetType, String targetId, String action, String messageId, String detail) {
            if (!dryRun && recorder != null) {
                recorder.success(targetType, targetId, action, messageId, detail);
            }
        }

        public void fail(String targetType, String targetId, String action, String reason) {
            if (!dryRun && recorder != null) {
                recorder.fail(targetType, targetId, action, reason);
            }
        }

        public void skip(String targetType, String targetId, String action, String reason) {
            skip(targetType, targetId, action, reason, experimentGroup(targetId));
        }

        public void skip(String targetType, String targetId, String action, String reason, String experimentGroup) {
            if (!dryRun && recorder != null) {
                recorder.skip(targetType, targetId, action, reason, experimentGroup);
            }
        }
    }

    /**
     * 执行结果
     *
     * @param success       是否成功
     * @param message       结果描述（展示给用户）
     * @param affectedCount 影响条数
     * @param detail        明细（JSON 字符串，可空）
     */
    record AutomationResult(boolean success, String message, int affectedCount, String detail) {

        /** 成功结果 */
        public static AutomationResult ok(String message, int affectedCount) {
            return new AutomationResult(true, message, affectedCount, null);
        }

        /** 成功结果（带明细） */
        public static AutomationResult ok(String message, int affectedCount, String detail) {
            return new AutomationResult(true, message, affectedCount, detail);
        }

        /** 失败结果 */
        public static AutomationResult fail(String message) {
            return new AutomationResult(false, message, 0, null);
        }
    }
}
