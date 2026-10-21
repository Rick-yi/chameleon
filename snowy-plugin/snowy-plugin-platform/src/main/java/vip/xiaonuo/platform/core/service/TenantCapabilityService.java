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
package vip.xiaonuo.platform.core.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.tenant.TenantCapability;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.mapper.TenantPackageMapper;

import java.util.List;
import java.util.Map;

/**
 * 租户能力刷新服务
 * <p>
 * 套餐里的「学员数上限 / 校区数上限 / 功能开关」在这里写入 Redis 缓存，
 * 业务模块（学员、班级、菜单下发）从缓存读取，避免跨插件反向依赖。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 11:00
 **/
@Service
public class TenantCapabilityService {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @Resource
    private TenantCapability tenantCapability;

    /**
     * 刷新单个租户的能力缓存
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public void refresh(String tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (ObjectUtil.isEmpty(tenant)) {
            return;
        }
        TenantCapability.Capability capability = new TenantCapability.Capability();
        capability.setTenantId(tenant.getId());
        capability.setPackageId(tenant.getPackageId());
        if (StrUtil.isNotBlank(tenant.getPackageId())) {
            TenantPackage tenantPackage = tenantPackageMapper.selectById(tenant.getPackageId());
            if (ObjectUtil.isNotEmpty(tenantPackage)) {
                capability.setPackageName(tenantPackage.getName());
                capability.setStudentLimit(tenantPackage.getStudentLimit());
                capability.setCampusLimit(tenantPackage.getCampusLimit());
                capability.setFuncConfig(parseFuncConfig(tenantPackage.getFuncConfig()));
            }
        }
        tenantCapability.cache(capability);
    }

    /**
     * 刷新使用指定套餐的所有租户
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public void refreshByPackage(String packageId) {
        List<Tenant> tenantList = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getPackageId, packageId));
        for (Tenant tenant : tenantList) {
            refresh(tenant.getId());
        }
    }

    /**
     * 预热全部租户的能力缓存
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    public void refreshAll() {
        for (Tenant tenant : tenantMapper.selectList(new LambdaQueryWrapper<Tenant>())) {
            refresh(tenant.getId());
        }
    }

    /**
     * 解析功能开关 JSON
     *
     * @author 会飞的猪
     * @date 2026/10/21 11:00
     **/
    private Map<String, Boolean> parseFuncConfig(String funcConfig) {
        if (StrUtil.isBlank(funcConfig)) {
            return new java.util.HashMap<>();
        }
        try {
            Map<String, Boolean> map = JSONUtil.toBean(funcConfig, new cn.hutool.core.lang.TypeReference<Map<String, Boolean>>() {
            }, true);
            return map == null ? new java.util.HashMap<>() : map;
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }
}
