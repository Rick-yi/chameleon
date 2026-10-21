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
     */
    record AutomationParam(String key, String label, String type, String defaultValue, String remark) {
    }

    /**
     * 执行上下文
     *
     * @param params      规则参数
     * @param triggerType 触发方式：TIMER 定时 / MANUAL 手动 / TRIGGER 实时
     * @param operatorId  操作人（系统任务为 -1）
     */
    record AutomationContext(Map<String, String> params, String triggerType, String operatorId) {

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
