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
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.enums.TenantStatusEnum;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantEditParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantIdParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantPageParam;
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

    @Override
    public Page<Tenant> page(TenantPageParam tenantPageParam) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<Tenant>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(tenantPageParam.getSearchKey())) {
            queryWrapper.lambda().like(Tenant::getName, tenantPageParam.getSearchKey())
                    .or().like(Tenant::getContactPhone, tenantPageParam.getSearchKey());
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
        boolean repeatName = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getName, tenant.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的机构名称：{}", tenant.getName());
        }
        // 新开通租户默认试用状态
        tenant.setStatus(TenantStatusEnum.TRIAL.getValue());
        this.save(tenant);
        // 租户模板数据初始化（角色/字典/配置等），待底座表租户化后实现
        this.initTenantTemplate(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(TenantEditParam tenantEditParam) {
        Tenant tenant = this.queryEntity(tenantEditParam.getId());
        BeanUtil.copyProperties(tenantEditParam, tenant);
        boolean repeatName = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getName, tenant.getName())
                .ne(Tenant::getId, tenant.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的机构名称：{}", tenant.getName());
        }
        this.updateById(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<TenantIdParam> tenantIdParamList) {
        List<String> tenantIdList = CollStreamUtil.toList(tenantIdParamList, TenantIdParam::getId);
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

    /**
     * 初始化租户模板数据
     * <p>
     * 待 M1（底座表租户化完成后）实现：复制默认角色、菜单授权、字典、系统配置到该租户，
     * 并生成机构管理员账号。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private void initTenantTemplate(Tenant tenant) {
        log.info(">>> 租户[{}]开通完成，模板数据初始化待M1实现", tenant.getId());
    }
}
