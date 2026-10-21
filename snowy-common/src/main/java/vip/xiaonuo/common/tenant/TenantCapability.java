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
package vip.xiaonuo.common.tenant;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.cache.CommonCacheOperator;
import vip.xiaonuo.common.context.TenantContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 租户能力（套餐限额与功能开关）
 * <p>
 * 平台运营在租户开通 / 套餐变更时把租户的能力写入 Redis，
 * 业务模块（学员、校区、菜单下发）从这里读取限额与开关，避免跨插件反向依赖。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 11:00
 **/
@Component
public class TenantCapability {

    /** 缓存键前缀 */
    private static final String CACHE_PREFIX = "tenant:capability:";

    /** 缓存有效期（秒），兜底刷新 */
    private static final long CACHE_TTL_SECONDS = 3600L;

    /** 静态实例（供静态工具类使用） */
    private static TenantCapability instance;

    @Resource
    private CommonCacheOperator cacheOperator;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 获取当前实例
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public static TenantCapability getInstance() {
        return instance;
    }

    /**
     * 读取当前租户能力（无缓存时返回空能力，即不限制）
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public Capability current() {
        return get(TenantContext.getTenantId());
    }

    /**
     * 读取指定租户能力
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public Capability get(String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            return new Capability();
        }
        Object cached = cacheOperator.get(CACHE_PREFIX + tenantId);
        if (cached instanceof String json) {
            try {
                return JSONUtil.toBean(json, Capability.class);
            } catch (Exception ignored) {
                // 缓存解析失败时按不限制处理
            }
        }
        return new Capability();
    }

    /**
     * 写入租户能力缓存
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public void cache(Capability capability) {
        if (capability == null || StrUtil.isBlank(capability.getTenantId())) {
            return;
        }
        cacheOperator.put(CACHE_PREFIX + capability.getTenantId(), JSONUtil.toJsonStr(capability), CACHE_TTL_SECONDS);
    }

    /**
     * 功能是否开启（未配置视为开启）
     *
     * @param funcKey 功能键
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public boolean enabled(String funcKey) {
        Map<String, Boolean> config = current().getFuncConfig();
        if (config == null || config.isEmpty()) {
            return true;
        }
        Boolean value = config.get(funcKey);
        return value == null || value;
    }

    /**
     * 学员数上限（null 表示不限制）
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public Integer studentLimit() {
        return current().getStudentLimit();
    }

    /**
     * 校区数上限（null 表示不限制）
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public Integer campusLimit() {
        return current().getCampusLimit();
    }

    /**
     * 租户能力数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public static class Capability {

        /** 租户id */
        private String tenantId;

        /** 套餐id */
        private String packageId;

        /** 套餐名称 */
        private String packageName;

        /** 学员数上限 */
        private Integer studentLimit;

        /** 校区数上限 */
        private Integer campusLimit;

        /** 功能开关 */
        private Map<String, Boolean> funcConfig = new HashMap<>();

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getPackageId() {
            return packageId;
        }

        public void setPackageId(String packageId) {
            this.packageId = packageId;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public Integer getStudentLimit() {
            return studentLimit;
        }

        public void setStudentLimit(Integer studentLimit) {
            this.studentLimit = studentLimit;
        }

        public Integer getCampusLimit() {
            return campusLimit;
        }

        public void setCampusLimit(Integer campusLimit) {
            this.campusLimit = campusLimit;
        }

        public Map<String, Boolean> getFuncConfig() {
            return funcConfig == null ? Collections.emptyMap() : funcConfig;
        }

        public void setFuncConfig(Map<String, Boolean> funcConfig) {
            this.funcConfig = funcConfig == null ? new HashMap<>() : funcConfig;
        }
    }
}
