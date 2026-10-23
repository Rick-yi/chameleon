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
package vip.xiaonuo.platform.modular.event.timer;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.timer.CommonTimerTaskRunner;
import vip.xiaonuo.platform.api.TenantApi;
import vip.xiaonuo.platform.modular.event.service.BizEventService;

import java.util.List;

/**
 * 埋点流水归档任务：按保留天数清理历史事件
 * <p>
 * 埋点按「页面访问」级别写入，单个机构一天几千到几万条，不清理会无限膨胀。
 * 该任务每天凌晨按租户清理保留期之前的数据（分批删除，避免长事务与锁表）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Component
public class BizEventArchiveTimerTaskRunner implements CommonTimerTaskRunner {

    /** 默认保留天数 */
    public static final int DEFAULT_RETAIN_DAYS = 90;
    /** 保留天数配置键（DEV_CONFIG，可按租户覆盖） */
    public static final String RETAIN_DAYS_CONFIG_KEY = "BIZ_EVENT_RETAIN_DAYS";
    /** 保留天数可选范围 */
    private static final int MIN_RETAIN_DAYS = 7;
    private static final int MAX_RETAIN_DAYS = 3650;
    /** 单批删除条数 */
    private static final int BATCH_SIZE = 5000;
    /** 单次任务最多删除批次（防止一次跑太久） */
    private static final int MAX_BATCH = 200;

    @Resource
    private TenantApi tenantApi;

    @Resource
    private BizEventService bizEventService;

    @Resource
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void action(String extJson) {
        String originalTenantId = TenantContext.getTenantId();
        List<String> tenantIdList = tenantApi.listActiveTenantIds();
        int totalDeleted = 0;
        for (String tenantId : tenantIdList) {
            try {
                TenantContext.setTenantId(tenantId);
                int retainDays = resolveRetainDays(tenantId);
                int deleted = archiveTenant(retainDays);
                totalDeleted += deleted;
                log.info(">>> 租户[{}]埋点归档：保留 {} 天，清理 {} 条", tenantId, retainDays, deleted);
            } catch (Exception e) {
                log.error(">>> 租户[{}]埋点归档失败", tenantId, e);
            }
        }
        if (ObjectUtil.isNotEmpty(originalTenantId)) {
            TenantContext.setTenantId(originalTenantId);
        } else {
            TenantContext.clear();
        }
        log.info(">>> 埋点归档完成，共清理 {} 条", totalDeleted);
    }

    /**
     * 读取当前租户的保留天数；未配置或配置非法时用默认值
     * <p>
     * 直查 DEV_CONFIG 而不是走配置服务：配置服务带全局缓存且依赖租户上下文，
     * 定时任务里需要按租户精确取各自的配置。
     * </p>
     */
    private int resolveRetainDays(String tenantId) {
        try {
            List<String> valueList = jdbcTemplate.queryForList(
                    "SELECT CONFIG_VALUE FROM DEV_CONFIG WHERE CONFIG_KEY = ? AND TENANT_ID = ? "
                            + "AND (DELETE_FLAG IS NULL OR DELETE_FLAG = 'NOT_DELETE') LIMIT 1",
                    String.class, RETAIN_DAYS_CONFIG_KEY, tenantId);
            if (valueList.isEmpty() || ObjectUtil.isEmpty(valueList.get(0))) {
                return DEFAULT_RETAIN_DAYS;
            }
            String value = valueList.get(0);
            int days = Integer.parseInt(value.trim());
            if (days < MIN_RETAIN_DAYS || days > MAX_RETAIN_DAYS) {
                log.warn(">>> 租户[{}]埋点保留天数配置超出范围（{}），按默认 {} 天处理", tenantId, value, DEFAULT_RETAIN_DAYS);
                return DEFAULT_RETAIN_DAYS;
            }
            return days;
        } catch (Exception e) {
            log.warn(">>> 租户[{}]埋点保留天数读取失败，按默认 {} 天处理", tenantId, DEFAULT_RETAIN_DAYS);
            return DEFAULT_RETAIN_DAYS;
        }
    }

    /**
     * 清理单个租户保留期之前的事件
     *
     * @param retainDays 保留天数
     * @return 清理条数
     */
    private int archiveTenant(int retainDays) {
        int deleted = 0;
        for (int i = 0; i < MAX_BATCH; i++) {
            int rows = bizEventService.deleteBefore(retainDays, BATCH_SIZE);
            deleted += rows;
            if (rows < BATCH_SIZE) {
                break;
            }
        }
        return deleted;
    }
}
