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
package vip.xiaonuo.finance.modular.cache;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.event.ReportCacheEvictEvent;
import vip.xiaonuo.finance.modular.service.FinanceService;

/**
 * 报表缓存失效监听：收款/退费/消课提交后清掉该租户的报表缓存
 *
 * @author 会飞的猪
 * @date 2026/10/23 12:00
 **/
@Slf4j
@Component
public class ReportCacheEvictListener {

    @Resource
    private FinanceService financeService;

    /**
     * 事务提交后执行；非事务场景（如定时任务）直接执行
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReportDataChanged(ReportCacheEvictEvent event) {
        String originalTenantId = TenantContext.getTenantId();
        try {
            if (event.getTenantId() != null) {
                TenantContext.setTenantId(event.getTenantId());
            }
            financeService.clearCache();
        } catch (Exception e) {
            // 缓存清理失败不影响业务，最多等 TTL 自然过期
            log.warn(">>> 报表缓存清理失败，来源：{}", event.getSource(), e);
        } finally {
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }
}
