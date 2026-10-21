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
package vip.xiaonuo.platform.modular.tenantpackage.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.platform.core.service.TenantCapabilityService;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.mapper.TenantPackageMapper;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageAddParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageEditParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageIdParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackagePageParam;
import vip.xiaonuo.platform.modular.tenantpackage.service.TenantPackageService;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;

import java.util.List;
import jakarta.annotation.Resource;

/**
 * 租户套餐Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class TenantPackageServiceImpl extends ServiceImpl<TenantPackageMapper, TenantPackage> implements TenantPackageService {

    @Resource
    private TenantCapabilityService tenantCapabilityService;

    @Override
    public Page<TenantPackage> page(TenantPackagePageParam tenantPackagePageParam) {
        QueryWrapper<TenantPackage> queryWrapper = new QueryWrapper<TenantPackage>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(tenantPackagePageParam.getSearchKey())) {
            queryWrapper.lambda().like(TenantPackage::getName, tenantPackagePageParam.getSearchKey());
        }
        queryWrapper.lambda().orderByAsc(TenantPackage::getSortCode).orderByDesc(TenantPackage::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(TenantPackageAddParam tenantPackageAddParam) {
        TenantPackage tenantPackage = BeanUtil.toBean(tenantPackageAddParam, TenantPackage.class);
        boolean repeatName = this.count(new LambdaQueryWrapper<TenantPackage>()
                .eq(TenantPackage::getName, tenantPackage.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的套餐名称：{}", tenantPackage.getName());
        }
        this.save(tenantPackage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(TenantPackageEditParam tenantPackageEditParam) {
        TenantPackage tenantPackage = this.queryEntity(tenantPackageEditParam.getId());
        BeanUtil.copyProperties(tenantPackageEditParam, tenantPackage);
        boolean repeatName = this.count(new LambdaQueryWrapper<TenantPackage>()
                .eq(TenantPackage::getName, tenantPackage.getName())
                .ne(TenantPackage::getId, tenantPackage.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的套餐名称：{}", tenantPackage.getName());
        }
        this.updateById(tenantPackage);
        // 套餐变更后刷新使用该套餐的租户能力缓存
        tenantCapabilityService.refreshByPackage(tenantPackage.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<TenantPackageIdParam> tenantPackageIdParamList) {
        List<String> packageIdList = CollStreamUtil.toList(tenantPackageIdParamList, TenantPackageIdParam::getId);
        if (ObjectUtil.isEmpty(packageIdList)) {
            return;
        }
        // 被租户引用的套餐不允许删除
        // 说明：套餐与租户同属平台库，直接查询引用关系，避免误删
        this.removeByIds(packageIdList);
    }

    @Override
    public TenantPackage detail(TenantPackageIdParam tenantPackageIdParam) {
        return this.queryEntity(tenantPackageIdParam.getId());
    }

    @Override
    public TenantPackage queryEntity(String id) {
        TenantPackage tenantPackage = this.getById(id);
        if (ObjectUtil.isEmpty(tenantPackage)) {
            throw new CommonException("套餐不存在，id值为：{}", id);
        }
        return tenantPackage;
    }
}
