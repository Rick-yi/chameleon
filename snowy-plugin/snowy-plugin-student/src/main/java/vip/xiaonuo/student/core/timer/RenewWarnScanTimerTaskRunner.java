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
package vip.xiaonuo.student.core.timer;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.timer.CommonTimerTaskRunner;
import vip.xiaonuo.platform.api.TenantApi;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnService;

import java.util.List;

/**
 * 续费预警扫描任务：按租户逐个扫描正常课时账户
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Component
public class RenewWarnScanTimerTaskRunner implements CommonTimerTaskRunner {

    @Resource
    private TenantApi tenantApi;

    @Resource
    private StudentRenewWarnService studentRenewWarnService;

    @Override
    public void action(String extJson) {
        String originalTenantId = TenantContext.getTenantId();
        List<String> tenantIdList = tenantApi.listActiveTenantIds();
        int success = 0;
        for (String tenantId : tenantIdList) {
            try {
                TenantContext.setTenantId(tenantId);
                studentRenewWarnService.scan();
                success++;
            } catch (Exception e) {
                log.error(">>> 租户[{}]续费预警扫描失败", tenantId, e);
            }
        }
        if (ObjectUtil.isNotEmpty(originalTenantId)) {
            TenantContext.setTenantId(originalTenantId);
        } else {
            TenantContext.clear();
        }
        log.info(">>> 续费预警扫描完成，成功 {}/{} 个租户", success, tenantIdList.size());
    }
}