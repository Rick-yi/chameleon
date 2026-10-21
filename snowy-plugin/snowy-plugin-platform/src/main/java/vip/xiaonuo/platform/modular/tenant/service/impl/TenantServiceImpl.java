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
package vip.xiaonuo.platform.modular.tenant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.platform.core.service.TenantCapabilityService;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.service.TenantPackageService;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.enums.TenantStatusEnum;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantEditParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantIdParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantPageParam;
import vip.xiaonuo.platform.modular.tenant.service.TenantInitService;
import vip.xiaonuo.platform.modular.tenant.service.TenantService;

import java.util.List;

/**
 * 租户Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantPackageService tenantPackageService;

    @Resource
    private TenantCapabilityService tenantCapabilityService;

    @Resource
    private TenantInitService tenantInitService;

    @Override
    public Page<Tenant> page(TenantPageParam tenantPageParam) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<Tenant>().checkSqlInjection();
        // 平台运营租户（id=0）仅用于平台账号归属，不在租户列表中展示
        queryWrapper.lambda().ne(Tenant::getId, "0");
        if (ObjectUtil.isNotEmpty(tenantPageParam.getSearchKey())) {
            queryWrapper.lambda().and(q -> q.like(Tenant::getName, tenantPageParam.getSearchKey())
                    .or().like(Tenant::getCode, tenantPageParam.getSearchKey())
                    .or().like(Tenant::getContactPhone, tenantPageParam.getSearchKey()));
        }
        if (ObjectUtil.isAllNotEmpty(tenantPageParam.getSortField(), tenantPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(tenantPageParam.getSortOrder());
            queryWrapper.orderBy(true, tenantPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(tenantPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(Tenant::getCreateTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(TenantAddParam tenantAddParam) {
        Tenant tenant = BeanUtil.toBean(tenantAddParam, Tenant.class);
        if ("platform".equalsIgnoreCase(tenant.getCode())) {
            throw new CommonException("租户编码 platform 为平台运营保留编码");
        }
        boolean repeatCode = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, tenant.getCode())) > 0;
        if (repeatCode) {
            throw new CommonException("存在重复的租户编码：{}", tenant.getCode());
        }
        boolean repeatName = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getName, tenant.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的机构名称：{}", tenant.getName());
        }
        // 新开通租户默认试用状态
        tenant.setStatus(TenantStatusEnum.TRIAL.getValue());
        // 套餐联动：校验套餐并按套餐有效期自动计算到期时间
        if (ObjectUtil.isNotEmpty(tenant.getPackageId())) {
            TenantPackage tenantPackage = tenantPackageService.queryEntity(tenant.getPackageId());
            if (ObjectUtil.isEmpty(tenant.getExpireTime())) {
                int validityDays = ObjectUtil.isNotEmpty(tenantPackage.getValidityDays())
                        ? tenantPackage.getValidityDays() : 365;
                tenant.setExpireTime(DateUtil.offsetDay(new java.util.Date(), validityDays));
            }
        }
        this.save(tenant);
        // 租户模板数据初始化：校区、管理员、角色、菜单、配置、字典
        tenantInitService.initTenant(tenant, tenantAddParam);
        // 刷新租户能力缓存（套餐限额与功能开关）
        tenantCapabilityService.refresh(tenant.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(TenantEditParam tenantEditParam) {
        if ("0".equals(tenantEditParam.getId())) {
            throw new CommonException("平台运营租户不允许编辑");
        }
        Tenant tenant = this.queryEntity(tenantEditParam.getId());
        BeanUtil.copyProperties(tenantEditParam, tenant);
        if ("platform".equalsIgnoreCase(tenant.getCode())) {
            throw new CommonException("租户编码 platform 为平台运营保留编码");
        }
        boolean repeatCode = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, tenant.getCode())
                .ne(Tenant::getId, tenant.getId())) > 0;
        if (repeatCode) {
            throw new CommonException("存在重复的租户编码：{}", tenant.getCode());
        }
        boolean repeatName = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getName, tenant.getName())
                .ne(Tenant::getId, tenant.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的机构名称：{}", tenant.getName());
        }
        this.updateById(tenant);
        // 刷新租户能力缓存（套餐限额与功能开关）
        tenantCapabilityService.refresh(tenant.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<TenantIdParam> tenantIdParamList) {
        List<String> tenantIdList = CollStreamUtil.toList(tenantIdParamList, TenantIdParam::getId);
        if (tenantIdList.contains("0")) {
            throw new CommonException("平台运营租户不允许删除");
        }
        if (ObjectUtil.isNotEmpty(tenantIdList)) {
            this.removeByIds(tenantIdList);
        }
    }

    @Override
    public Tenant detail(TenantIdParam tenantIdParam) {
        return this.queryEntity(tenantIdParam.getId());
    }

    @Override
    public Tenant queryEntity(String id) {
        Tenant tenant = this.getById(id);
        if (ObjectUtil.isEmpty(tenant)) {
            throw new CommonException("租户不存在，id值为：{}", id);
        }
        return tenant;
    }

    @Override
    public Tenant getTenantById(String id) {
        return this.getById(id);
    }

    @Override
    public Tenant getTenantByCode(String code) {
        if (ObjectUtil.isEmpty(code)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code));
    }

    @Override
    public List<Tenant> listActiveTenants() {
        return this.list(new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, "0")
                .ne(Tenant::getStatus, TenantStatusEnum.STOP.getValue()));
    }

    @Override
    public int checkExpired() {
        java.util.List<Tenant> expiredList = this.list(new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, "0")
                .ne(Tenant::getStatus, TenantStatusEnum.STOP.getValue())
                .isNotNull(Tenant::getExpireTime)
                .lt(Tenant::getExpireTime, new java.util.Date()));
        for (Tenant tenant : expiredList) {
            tenant.setStatus(TenantStatusEnum.STOP.getValue());
        }
        if (ObjectUtil.isNotEmpty(expiredList)) {
            this.updateBatchById(expiredList);
            expiredList.forEach(tenant -> log.info(">>> 租户已到期，自动停用：{}（{}）", tenant.getName(), tenant.getId()));
        }
        return expiredList.size();
    }
}
