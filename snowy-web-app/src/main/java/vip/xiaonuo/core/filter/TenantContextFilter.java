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
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.xiaonuo.auth.core.util.StpClientUtil;
import vip.xiaonuo.common.context.TenantContext;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * <p>
 * 从请求头 X-Tenant-Id 解析租户id并写入请求级租户上下文，请求结束后清除，
 * 供租户行级隔离拦截器使用。请求头未携带时，从登录会话（B端/C端）回退租户id。
 * 平台端/未登录请求无租户时，上下文为空。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    /** 租户id请求头名称 */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Resource
    private SaTokenConfig saTokenConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(TENANT_ID_HEADER);
            if (ObjectUtil.isEmpty(tenantId)) {
                // 未带租户请求头时，从登录会话回退租户id
                tenantId = getTenantIdFromSession(request);
            }
            if (ObjectUtil.isNotEmpty(tenantId)) {
                TenantContext.setTenantId(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 从登录会话回退租户id（兼容B端与C端会话）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private String getTenantIdFromSession(HttpServletRequest request) {
        String tokenValue = request.getHeader(saTokenConfig.getTokenName());
        if (ObjectUtil.isEmpty(tokenValue)) {
            return null;
        }
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
