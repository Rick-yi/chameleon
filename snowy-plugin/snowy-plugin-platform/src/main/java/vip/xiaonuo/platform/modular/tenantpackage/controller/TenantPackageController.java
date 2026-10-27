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
package vip.xiaonuo.platform.modular.tenantpackage.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageAddParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageCopyParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageEditParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageIdParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackagePageParam;
import vip.xiaonuo.platform.modular.tenantpackage.param.TenantPackageStatusParam;
import vip.xiaonuo.platform.modular.tenantpackage.service.TenantPackageService;

import javax.validation.Valid;
import java.util.List;

/**
 * 租户套餐控制器（SaaS平台端）
 * <p>
 * 仅平台运营角色 platformAdmin 可访问。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "租户套餐控制器")
@RestController
@Validated
@SaCheckRole("platformAdmin")
public class TenantPackageController {

    @Resource
    private TenantPackageService tenantPackageService;

    /**
     * 获取套餐分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取套餐分页")
    @GetMapping("/platform/package/page")
    public CommonResult<Page<TenantPackage>> page(TenantPackagePageParam tenantPackagePageParam) {
        return CommonResult.data(tenantPackageService.page(tenantPackagePageParam));
    }

    /**
     * 添加套餐
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加套餐")
    @CommonLog("添加套餐")
    @PostMapping("/platform/package/add")
    public CommonResult<String> add(@RequestBody @Valid TenantPackageAddParam tenantPackageAddParam) {
        tenantPackageService.add(tenantPackageAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑套餐
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "编辑套餐")
    @CommonLog("编辑套餐")
    @PostMapping("/platform/package/edit")
    public CommonResult<String> edit(@RequestBody @Valid TenantPackageEditParam tenantPackageEditParam) {
        tenantPackageService.edit(tenantPackageEditParam);
        return CommonResult.ok();
    }

    /**
     * 上架/下架套餐
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "上架/下架套餐")
    @CommonLog("套餐上下架")
    @PostMapping("/platform/package/status")
    public CommonResult<String> changeStatus(@RequestBody @Valid TenantPackageStatusParam tenantPackageStatusParam) {
        tenantPackageService.changeStatus(tenantPackageStatusParam);
        return CommonResult.ok();
    }

    /**
     * 复制套餐（副本默认下架，便于先调整再上架）
     *
     * @author 会飞的猪
     * @date 2026/10/26 10:00
     */
    @Operation(summary = "复制套餐")
    @CommonLog("复制套餐")
    @PostMapping("/platform/package/copy")
    public CommonResult<String> copy(@RequestBody @Valid TenantPackageCopyParam tenantPackageCopyParam) {
        return CommonResult.data(tenantPackageService.copy(tenantPackageCopyParam));
    }

    /**
     * 删除套餐
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "删除套餐")
    @CommonLog("删除套餐")
    @PostMapping("/platform/package/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<TenantPackageIdParam> tenantPackageIdParamList) {
        tenantPackageService.delete(tenantPackageIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取套餐详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取套餐详情")
    @GetMapping("/platform/package/detail")
    public CommonResult<TenantPackage> detail(@Valid TenantPackageIdParam tenantPackageIdParam) {
        return CommonResult.data(tenantPackageService.detail(tenantPackageIdParam));
    }
}
