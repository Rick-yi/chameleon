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
package vip.xiaonuo.finance.modular.stat.service;

import vip.xiaonuo.finance.modular.stat.entity.FinDailyStat;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务日汇总Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public interface FinDailyStatService {

    /**
     * 汇总指定日期（当前租户上下文）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    void aggregate(String statDate);

    /**
     * 按日查询汇总数据
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    List<FinDailyStat> listByDate(String startDate, String endDate);

    /**
     * 汇总区间收款金额（用于与实时报表对账）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    BigDecimal sumPaidAmount(String startDate, String endDate);

    /**
     * 判断区间是否已被日汇总完整覆盖（每天都有汇总行）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    boolean covered(String startDate, String endDate);
}
