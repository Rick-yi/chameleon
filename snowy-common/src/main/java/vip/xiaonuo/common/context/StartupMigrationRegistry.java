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
package vip.xiaonuo.common.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动期结构/权限迁移失败登记
 * <p>
 * 各迁移监听器原先各自 catch 后只打一行日志：应用照常启动对外服务，
 * 但表结构或权限没有升级，故障会推迟到业务调用时才暴露，且堆栈指向业务代码而非根因。
 * </p>
 * <p>
 * 本登记器把这些失败集中起来：迁移监听器失败时调用 {@link #record}，
 * 由启动期检查统一输出汇总，运维只需看一处即可知道是否需要手工补跑 SQL。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/28
 **/
public final class StartupMigrationRegistry {

    /** 迁移名称 -> 失败原因（保持登记顺序，便于排查） */
    private static final Map<String, String> FAILURES = Collections.synchronizedMap(new LinkedHashMap<>());

    private StartupMigrationRegistry() {
    }

    /**
     * 登记一次迁移失败
     *
     * @param migrationName 迁移名称（如「P0 结构升级」）
     * @param reason        失败原因
     */
    public static void record(String migrationName, String reason) {
        FAILURES.put(migrationName, reason == null ? "未知原因" : reason);
    }

    /**
     * 是否发生过迁移失败
     */
    public static boolean hasFailure() {
        return !FAILURES.isEmpty();
    }

    /**
     * 获取全部失败项（副本，避免外部修改）
     */
    public static Map<String, String> failures() {
        synchronized (FAILURES) {
            return new LinkedHashMap<>(FAILURES);
        }
    }

    /**
     * 获取失败项名称列表
     */
    public static List<String> failedNames() {
        return List.copyOf(failures().keySet());
    }

    /**
     * 清空登记（测试用或重启后重置）
     */
    public static void clear() {
        FAILURES.clear();
    }
}
