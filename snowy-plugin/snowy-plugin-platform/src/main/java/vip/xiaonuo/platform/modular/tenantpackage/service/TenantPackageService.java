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
package vip.xiaonuo.platform.modular.tenantpackage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageAddParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageEditParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageIdParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackagePageParam;

import java.util.List;

/**
 * 租户套餐Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface TenantPackageService {

    /** 获取套餐分页 */
    Page<TenantPackage> page(TenantPackagePageParam tenantPackagePageParam);

    /** 添加套餐 */
    void add(TenantPackageAddParam tenantPackageAddParam);

    /** 编辑套餐 */
    void edit(TenantPackageEditParam tenantPackageEditParam);

    /** 删除套餐（被租户引用时不允许删除） */
    void delete(List<TenantPackageIdParam> tenantPackageIdParamList);

    /** 获取套餐详情 */
    TenantPackage detail(TenantPackageIdParam tenantPackageIdParam);

    /** 获取套餐详情（内部用） */
    TenantPackage queryEntity(String id);
}
