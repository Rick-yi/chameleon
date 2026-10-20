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
package vip.xiaonuo.student.api;

/**
 * 通知记录API接口（跨插件调用）
 * <p>
 * 家校通知统一落库；v1 暂不接微信订阅消息，后续由定时任务/发送器消费 NOTIFY_LOG。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentNotifyApi {

    /**
     * 记录待发送通知（失败不抛出，避免影响主业务）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    void log(String bizType, String receiverType, String receiverId, String studentId,
             String title, String content, String channel);
}