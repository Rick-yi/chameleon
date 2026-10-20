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
package vip.xiaonuo.crm.modular.lead.controller;

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
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadAddParam;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadEditParam;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadIdParam;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadPageParam;
import vip.xiaonuo.crm.modular.lead.service.CrmLeadService;

import javax.validation.Valid;
import java.util.List;

/**
 * 招生线索控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "招生线索控制器")
@RestController
@Validated
public class CrmLeadController {

    @Resource
    private CrmLeadService crmLeadService;

    /**
     * 获取线索分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取线索分页")
    @GetMapping("/crm/lead/page")
    public CommonResult<Page<CrmLead>> page(CrmLeadPageParam crmLeadPageParam) {
        return CommonResult.data(crmLeadService.page(crmLeadPageParam));
    }

    /**
     * 添加线索
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加线索")
    @CommonLog("添加线索")
    @PostMapping("/crm/lead/add")
    public CommonResult<String> add(@RequestBody @Valid CrmLeadAddParam crmLeadAddParam) {
        crmLeadService.add(crmLeadAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑线索
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "编辑线索")
    @CommonLog("编辑线索")
    @PostMapping("/crm/lead/edit")
    public CommonResult<String> edit(@RequestBody @Valid CrmLeadEditParam crmLeadEditParam) {
        crmLeadService.edit(crmLeadEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除线索
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "删除线索")
    @CommonLog("删除线索")
    @PostMapping("/crm/lead/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<CrmLeadIdParam> crmLeadIdParamList) {
        crmLeadService.delete(crmLeadIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取线索详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取线索详情")
    @GetMapping("/crm/lead/detail")
    public CommonResult<CrmLead> detail(@Valid CrmLeadIdParam crmLeadIdParam) {
        return CommonResult.data(crmLeadService.detail(crmLeadIdParam));
    }
}
