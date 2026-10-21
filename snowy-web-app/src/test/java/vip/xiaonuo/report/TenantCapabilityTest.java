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

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.common.tenant.TenantCapability;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 租户能力（限额与功能开关）单测
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
class TenantCapabilityTest {

    @Test
    @DisplayName("能力缓存可完整序列化与还原")
    void capabilitySerialize() {
        TenantCapability.Capability capability = new TenantCapability.Capability();
        capability.setTenantId("t1");
        capability.setPackageId("pkg1");
        capability.setPackageName("专业版");
        capability.setStudentLimit(50);
        capability.setCampusLimit(3);
        capability.setFuncConfig(Map.of("crm", true, "report", false));

        String json = JSONUtil.toJsonStr(capability);
        TenantCapability.Capability restored = JSONUtil.toBean(json, TenantCapability.Capability.class);

        assertNotNull(restored);
        assertEquals("t1", restored.getTenantId());
        assertEquals("pkg1", restored.getPackageId());
        assertEquals(50, restored.getStudentLimit());
        assertEquals(3, restored.getCampusLimit());
        assertTrue(restored.getFuncConfig().get("crm"));
        assertFalse(restored.getFuncConfig().get("report"));
    }

    @Test
    @DisplayName("未配置的功能开关视为开启")
    void emptyConfigMeansEnabled() {
        TenantCapability.Capability capability = new TenantCapability.Capability();
        assertTrue(capability.getFuncConfig().isEmpty());
    }
}
