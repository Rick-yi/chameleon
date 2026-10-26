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
package vip.xiaonuo.dev.modular.log.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.util.CommonSensitiveUtil;
import vip.xiaonuo.dev.modular.log.entity.DevLog;
import vip.xiaonuo.dev.modular.log.service.DevLogService;
import vip.xiaonuo.dev.modular.log.util.DevLogUtil;

import java.util.List;
import java.util.Objects;

/**
 * 历史操作日志敏感字段清理
 * <p>
 * 启动时扫描包含 password 的历史日志，把密码值替换为 ******。
 * 只处理已存在的记录，不影响审计日志的其他内容。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 16:00
 **/
@Slf4j
@Component
public class SensitiveLogRedactor implements ApplicationRunner {

    /** 单次启动最多处理的记录数，避免日志表过大时拖慢启动 */
    private static final int BATCH_LIMIT = 1000;

    /** 脱敏后在日志里留下可追溯标记 */
    private static final String REDACT_MARK = "（历史敏感字段已脱敏）";

    @Resource
    private DevLogService devLogService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<DevLog> logList = devLogService.list(new LambdaQueryWrapper<DevLog>()
                    .and(wrapper -> wrapper.like(DevLog::getParamJson, "password")
                            .or().like(DevLog::getResultJson, "password"))
                    .last("limit " + BATCH_LIMIT));
            int cleaned = 0;
            for (DevLog devLog : logList) {
                String paramJson = CommonSensitiveUtil.maskSensitive(devLog.getParamJson());
                String resultJson = CommonSensitiveUtil.maskSensitive(devLog.getResultJson());
                if (Objects.equals(paramJson, devLog.getParamJson())
                        && Objects.equals(resultJson, devLog.getResultJson())) {
                    continue;
                }
                devLog.setParamJson(paramJson);
                devLog.setResultJson(resultJson);
                String exeMessage = devLog.getExeMessage() == null ? "" : devLog.getExeMessage();
                if (!exeMessage.contains(REDACT_MARK)) {
                    devLog.setExeMessage(exeMessage + REDACT_MARK);
                }
                // 内容已变更，重算签名，避免审计校验误报
                DevLogUtil.refreshSignData(devLog);
                devLogService.updateById(devLog);
                cleaned++;
            }
            if (cleaned > 0) {
                log.info(">>> 历史操作日志脱敏完成，共处理 {} 条", cleaned);
            }
        } catch (Exception e) {
            // 脱敏失败不能阻断应用启动
            log.warn(">>> 历史操作日志脱敏失败：{}", e.getMessage());
        }
    }
}
