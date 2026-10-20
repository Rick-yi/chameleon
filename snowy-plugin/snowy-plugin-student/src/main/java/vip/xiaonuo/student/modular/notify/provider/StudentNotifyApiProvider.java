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
package vip.xiaonuo.student.modular.notify.provider;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.student.api.StudentNotifyApi;
import vip.xiaonuo.student.modular.notify.service.NotifyLogService;

/**
 * 通知记录API接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class StudentNotifyApiProvider implements StudentNotifyApi {

    @Resource
    private NotifyLogService notifyLogService;

    @Override
    public void log(String bizType, String receiverType, String receiverId, String studentId,
                    String title, String content, String channel) {
        try {
            notifyLogService.log(bizType, receiverType, receiverId, studentId, title, content, channel);
        } catch (Exception e) {
            // 通知记录失败不影响点名/调课等主业务
            log.warn(">>> 记录通知流水失败：bizType={}，studentId={}", bizType, studentId, e);
        }
    }
}