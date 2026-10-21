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
package vip.xiaonuo.biz.api.automation;

import java.util.Map;

/**
 * 自动化规则API
 * <p>
 * 供各业务插件在业务动作发生时判断「某条自动化规则是否开启、参数是什么」。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
public interface BizAutomationApi {

    /**
     * 规则是否开启
     *
     * @param ruleCode 规则编码
     */
    boolean isEnabled(String ruleCode);

    /**
     * 规则参数（未配置返回空 Map）
     *
     * @param ruleCode 规则编码
     */
    Map<String, String> getParams(String ruleCode);

    /**
     * 规则参数（带默认值兜底）
     *
     * @param ruleCode    规则编码
     * @param key         参数键
     * @param defaultValue 默认值
     */
    String getParam(String ruleCode, String key, String defaultValue);
}
