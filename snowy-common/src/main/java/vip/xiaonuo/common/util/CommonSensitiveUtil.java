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
package vip.xiaonuo.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 敏感字段脱敏工具
 * <p>
 * 用于操作日志、审计日志等场景：字段名包含 password 的值统一替换为 ******。
 * 支持嵌套 JSON 对象以及“JSON 字符串里再包一层 JSON”的情况。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 16:00
 **/
public class CommonSensitiveUtil {

    private static final String MASK = "******";

    public static String maskSensitive(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        try {
            Object parsed = JSONUtil.parse(json);
            maskNode(parsed);
            return JSONUtil.toJsonStr(parsed);
        } catch (Exception e) {
            return json;
        }
    }

    private static void maskNode(Object node) {
        if (node instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);
                if (key != null && key.toLowerCase().contains("password")) {
                    jsonObject.set(key, MASK);
                } else if (value instanceof CharSequence text && looksLikeJson(text.toString())) {
                    // 参数常被序列化成字符串再包一层，如 {"param":"{\"password\":\"x\"}"}
                    try {
                        Object inner = JSONUtil.parse(text.toString());
                        maskNode(inner);
                        jsonObject.set(key, JSONUtil.toJsonStr(inner));
                    } catch (Exception ignore) {
                        maskNode(value);
                    }
                } else {
                    maskNode(value);
                }
            }
        } else if (node instanceof JSONArray jsonArray) {
            jsonArray.forEach(CommonSensitiveUtil::maskNode);
        }
    }

    private static boolean looksLikeJson(String text) {
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}
