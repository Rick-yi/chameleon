/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.platform.modular.roletemplate.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.platform.modular.roletemplate.PlatformRoleTemplate;
import vip.xiaonuo.platform.modular.roletemplate.service.PlatformRoleTemplateService;

import java.util.List;

/**
 * 角色模板控制器（平台运营侧）
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@RestController
@RequestMapping("/platform/roleTemplate")
public class PlatformRoleTemplateController {

    @Resource
    private PlatformRoleTemplateService platformRoleTemplateService;

    /**
     * 角色模板列表
     */
    @SaCheckRole("platformAdmin")
    @GetMapping("/list")
    public CommonResult<List<PlatformRoleTemplate>> list() {
        return CommonResult.data(platformRoleTemplateService.listTemplates());
    }

    /**
     * 为租户套用角色模板（覆盖式重建模板角色的授权）
     */
    @SaCheckRole("platformAdmin")
    @PostMapping("/apply")
    public CommonResult<Integer> apply(@RequestBody @Valid PlatformRoleTemplateApplyParam param) {
        return CommonResult.data(platformRoleTemplateService.applyTemplates(param.getTenantId()));
    }

    /**
     * 租户id参数
     */
    public static class PlatformRoleTemplateApplyParam {

        /** 租户id */
        @jakarta.validation.constraints.NotBlank(message = "租户id不能为空")
        private String tenantId;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
