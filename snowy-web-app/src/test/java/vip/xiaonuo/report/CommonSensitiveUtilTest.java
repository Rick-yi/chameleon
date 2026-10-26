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
package vip.xiaonuo.report;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.common.util.CommonSensitiveUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 敏感字段脱敏单测
 *
 * @author 会飞的猪
 * @date 2026/10/26 16:00
 **/
class CommonSensitiveUtilTest {

    @Test
    @DisplayName("顶层和嵌套对象的密码字段都会被脱敏")
    void maskTopLevelAndNestedPassword() {
        String raw = "{\"account\":\"admin\",\"password\":\"secret\",\"tenant\":{\"adminPassword\":\"secret2\"}}";
        JSONObject masked = JSONUtil.parseObj(CommonSensitiveUtil.maskSensitive(raw));
        assertEquals("******", masked.getStr("password"));
        assertEquals("******", masked.getJSONObject("tenant").getStr("adminPassword"));
        assertEquals("admin", masked.getStr("account"));
    }

    @Test
    @DisplayName("JSON 字符串里再包一层 JSON 也能脱敏")
    void maskPasswordInsideJsonString() {
        String raw = "{\"tenantAddParam\":\"{\\\"code\\\":\\\"aiglobe\\\",\\\"adminPassword\\\":\\\"secret\\\"}\"}";
        String result = CommonSensitiveUtil.maskSensitive(raw);
        assertFalse(result.contains("secret"));
        assertTrue(result.contains("******"));
        assertTrue(result.contains("aiglobe"));
    }

    @Test
    @DisplayName("没有敏感字段时保持原值")
    void keepNormalJson() {
        String raw = "{\"name\":\"张三\",\"amount\":3600}";
        assertEquals(raw, CommonSensitiveUtil.maskSensitive(raw));
    }
}
