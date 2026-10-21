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
package vip.xiaonuo.core.filter;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.xiaonuo.auth.core.util.StpClientUtil;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.dev.modular.dict.service.DevDictService;
import vip.xiaonuo.platform.api.TenantApi;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * <p>
 * 已登录请求的租户只从登录会话读取，禁止通过请求头切换租户，避免越权访问其他机构数据；
 * 未登录的登录/配置请求可通过 X-Tenant-Code 指定租户编码，用于识别登录机构。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    /** 租户编码请求头名称（仅登录前使用） */
    public static final String TENANT_CODE_HEADER = "X-Tenant-Code";

    /** 字典缓存当前所属租户（底座 easy-trans 字典缓存为全局共享，按租户切换时刷新） */
    private static final AtomicReference<String> DICT_CACHE_TENANT = new AtomicReference<>();

    /** 字典缓存刷新节流（毫秒） */
    private static final long DICT_CACHE_REFRESH_INTERVAL = 10_000L;

    /** 上次字典缓存刷新时间戳 */
    private static final AtomicLong DICT_CACHE_REFRESH_TIME = new AtomicLong();

    @Resource
    private SaTokenConfig saTokenConfig;

    @Autowired
    private ObjectProvider<TenantApi> tenantApiProvider;

    @Resource
    private ObjectProvider<DevDictService> devDictServiceProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            String tokenValue = request.getHeader(saTokenConfig.getTokenName());
            if (ObjectUtil.isNotEmpty(tokenValue)) {
                // 已登录：租户只认会话，忽略任何请求头
                String tenantId = getTenantIdFromSession(tokenValue);
                if (ObjectUtil.isNotEmpty(tenantId)) {
                    TenantContext.setTenantId(tenantId);
                }
            } else {
                // 未登录：允许通过租户编码识别登录机构
                String tenantCode = request.getHeader(TENANT_CODE_HEADER);
                if (ObjectUtil.isNotEmpty(tenantCode)) {
                    TenantContext.setTenantCode(tenantCode);
                    String tenantId = tenantApiProvider.getObject().getTenantIdByCode(tenantCode);
                    if (ObjectUtil.isNotEmpty(tenantId)) {
                        TenantContext.setTenantId(tenantId);
                    }
                }
            }
            // 底座字典缓存是全局共享的，切换租户时按需刷新，避免读到其他租户的字典
            refreshDictCacheIfTenantChanged(TenantContext.getTenantId());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 租户切换时刷新字典缓存（带节流，避免高频请求反复刷新）
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    private void refreshDictCacheIfTenantChanged(String tenantId) {
        if (StrUtil.isBlank(tenantId) || tenantId.equals(DICT_CACHE_TENANT.get())) {
            return;
        }
        long now = System.currentTimeMillis();
        long lastTime = DICT_CACHE_REFRESH_TIME.get();
        if (now - lastTime < DICT_CACHE_REFRESH_INTERVAL) {
            return;
        }
        if (!DICT_CACHE_REFRESH_TIME.compareAndSet(lastTime, now)) {
            return;
        }
        DICT_CACHE_TENANT.set(tenantId);
        try {
            DevDictService devDictService = devDictServiceProvider.getIfAvailable();
            if (devDictService != null) {
                devDictService.refreshTransCache();
            }
        } catch (Exception e) {
            log.error(">>> 刷新租户字典缓存失败，租户id：{}", tenantId, e);
        }
    }

    /**
     * 从登录会话读取租户id（兼容B端与C端会话）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String getTenantIdFromSession(String tokenValue) {
        // B端会话
        try {
            Object tenantId = StpUtil.getTokenSessionByToken(tokenValue).get("tenantId");
            if (ObjectUtil.isNotEmpty(tenantId)) {
                return tenantId.toString();
            }
        } catch (Exception e) {
            log.debug(">>> 从B端登录会话读取租户id失败：{}", e.getMessage());
        }
        // C端会话
        try {
            Object tenantId = StpClientUtil.getTokenSessionByToken(tokenValue).get("tenantId");
            if (ObjectUtil.isNotEmpty(tenantId)) {
                return tenantId.toString();
            }
        } catch (Exception e) {
            log.debug(">>> 从C端登录会话读取租户id失败：{}", e.getMessage());
        }
        return null;
    }
}
