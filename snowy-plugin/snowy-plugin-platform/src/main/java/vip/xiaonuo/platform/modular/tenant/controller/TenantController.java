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
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantEditParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantIdParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantPageParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantRenewParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantStatusParam;
import vip.xiaonuo.platform.modular.tenant.service.TenantService;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.List;

/**
 * 租户控制器（SaaS平台端）
 * <p>
 * 仅平台运营角色 platformAdmin 可访问。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "租户控制器")
@RestController
@Validated
@SaCheckRole("platformAdmin")
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

    /**
     * 到期扫描（将过期租户置为停用，返回处理数量；后续可接入定时任务）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "到期扫描")
    @CommonLog("租户到期扫描")
    @PostMapping("/platform/tenant/checkExpired")
    public CommonResult<Integer> checkExpired() {
        return CommonResult.data(tenantService.checkExpired());
    }

    /**
     * 租户续费/延期
     *
     * @author 会飞的猪
     * @date 2026/10/26 09:00
     */
    @Operation(summary = "租户续费")
    @CommonLog("租户续费")
    @PostMapping("/platform/tenant/renew")
    public CommonResult<Map<String, Object>> renew(@RequestBody @Valid TenantRenewParam tenantRenewParam) {
        return CommonResult.data(tenantService.renew(tenantRenewParam));
    }

    /**
     * 停用/启用租户
     *
     * @author 会飞的猪
     * @date 2026/10/26 09:00
     */
    @Operation(summary = "停用/启用租户")
    @CommonLog("变更租户状态")
    @PostMapping("/platform/tenant/status")
    public CommonResult<String> changeStatus(@RequestBody @Valid TenantStatusParam tenantStatusParam) {
        tenantService.changeStatus(tenantStatusParam);
        return CommonResult.ok();
    }

    /**
     * 重置租户管理员密码（一次性密码只在本次响应返回）
     *
     * @author 会飞的猪
     * @date 2026/10/26 09:00
     */
    @Operation(summary = "重置租户管理员密码")
    @CommonLog("重置租户管理员密码")
    @PostMapping("/platform/tenant/resetAdminPassword")
    public CommonResult<Map<String, Object>> resetAdminPassword(@RequestBody @Valid TenantIdParam tenantIdParam) {
        return CommonResult.data(tenantService.resetAdminPassword(tenantIdParam));
    }

    /**
     * 最近登录记录
     *
     * @author 会飞的猪
     * @date 2026/10/26 09:00
     */
    @Operation(summary = "租户最近登录记录")
    @GetMapping("/platform/tenant/loginRecords")
    public CommonResult<List<Map<String, Object>>> loginRecords(@Valid TenantIdParam tenantIdParam) {
        return CommonResult.data(tenantService.loginRecords(tenantIdParam));
    }

    /**
     * 续费记录
     *
     * @author 会飞的猪
     * @date 2026/10/26 09:00
     */
    @Operation(summary = "租户续费记录")
    @GetMapping("/platform/tenant/renewRecords")
    public CommonResult<List<Map<String, Object>>> renewRecords(@Valid TenantIdParam tenantIdParam) {
        return CommonResult.data(tenantService.renewRecords(tenantIdParam));
    }
}
