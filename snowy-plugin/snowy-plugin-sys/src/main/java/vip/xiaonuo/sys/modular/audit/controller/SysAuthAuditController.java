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
package vip.xiaonuo.sys.modular.audit.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.sys.modular.audit.entity.SysAuthAudit;
import vip.xiaonuo.sys.modular.audit.param.SysAuthAuditPageParam;
import vip.xiaonuo.sys.modular.audit.service.SysAuthAuditService;

/**
 * 权限变更审计控制器
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Tag(name = "权限变更审计")
@RestController
@Validated
@SaCheckRole(value = {"superAdmin", "platformAdmin"}, mode = SaMode.OR)
public class SysAuthAuditController {

    @Resource
    private SysAuthAuditService sysAuthAuditService;

    /**
     * 权限变更记录分页
     */
    @Operation(summary = "权限变更记录分页")
    @GetMapping("/sys/authAudit/page")
    public CommonResult<Page<SysAuthAudit>> page(SysAuthAuditPageParam param) {
        return CommonResult.data(sysAuthAuditService.page(param));
    }
}
