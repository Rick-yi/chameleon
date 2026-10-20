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
package vip.xiaonuo.platform.modular.tenant.controller;

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
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantEditParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantIdParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantPageParam;
import vip.xiaonuo.platform.modular.tenant.service.TenantService;

import javax.validation.Valid;
import java.util.List;

/**
 * 租户控制器（SaaS平台端）
 * <p>
 * TODO(M1)：接入平台端菜单与超管权限校验
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "租户控制器")
@RestController
@Validated
public class TenantController {

    @Resource
    private TenantService tenantService;

    /**
     * 获取租户分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取租户分页")
    @GetMapping("/platform/tenant/page")
    public CommonResult<Page<Tenant>> page(TenantPageParam tenantPageParam) {
        return CommonResult.data(tenantService.page(tenantPageParam));
    }

    /**
     * 添加租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加租户")
    @CommonLog("添加租户")
    @PostMapping("/platform/tenant/add")
    public CommonResult<String> add(@RequestBody @Valid TenantAddParam tenantAddParam) {
        tenantService.add(tenantAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "编辑租户")
    @CommonLog("编辑租户")
    @PostMapping("/platform/tenant/edit")
    public CommonResult<String> edit(@RequestBody @Valid TenantEditParam tenantEditParam) {
        tenantService.edit(tenantEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除租户
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "删除租户")
    @CommonLog("删除租户")
    @PostMapping("/platform/tenant/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<TenantIdParam> tenantIdParamList) {
        tenantService.delete(tenantIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取租户详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取租户详情")
    @GetMapping("/platform/tenant/detail")
    public CommonResult<Tenant> detail(@Valid TenantIdParam tenantIdParam) {
        return CommonResult.data(tenantService.detail(tenantIdParam));
    }
}
