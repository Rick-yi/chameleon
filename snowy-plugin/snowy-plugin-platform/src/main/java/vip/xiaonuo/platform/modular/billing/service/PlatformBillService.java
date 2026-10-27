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
package vip.xiaonuo.platform.modular.billing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import vip.xiaonuo.platform.modular.billing.dto.PlatformBillSummaryDTO;
import vip.xiaonuo.platform.modular.billing.entity.PlatformBill;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillAddParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillEditParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillIdParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillPageParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillPayParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillVoidParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 平台账单Service接口
 * <p>
 * 内部对账能力：只登记账单与人工收款，不接真实支付/发票平台。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/26 12:00
 **/
public interface PlatformBillService {

    /** 分页 */
    Page<PlatformBill> page(PlatformBillPageParam pageParam);

    /** 汇总 */
    PlatformBillSummaryDTO summary(PlatformBillPageParam pageParam);

    /** 添加 */
    void add(PlatformBillAddParam addParam);

    /** 编辑 */
    void edit(PlatformBillEditParam editParam);

    /** 删除 */
    void delete(List<PlatformBillIdParam> idParamList);

    /** 登记收款 */
    void markPaid(PlatformBillPayParam payParam);

    /** 作废账单 */
    void voidBill(PlatformBillVoidParam voidParam);

    /** 导出对账 Excel */
    void export(PlatformBillPageParam pageParam, HttpServletResponse response) throws IOException;

    /**
     * 续费时自动生成一条已收款账单
     *
     * @param tenant     租户
     * @param packageId  套餐id
     * @param amount     金额
     * @param periodStart 周期开始
     * @param periodEnd   周期结束
     * @param remark     备注
     * @param operatorId 操作人id
     * @param operatorName 操作人
     *
     * @author 会飞的猪
     * @date 2026/10/26 12:00
     **/
    void recordRenewBill(vip.xiaonuo.platform.modular.tenant.entity.Tenant tenant, String packageId,
                         BigDecimal amount, Date periodStart, Date periodEnd, String remark,
                         String operatorId, String operatorName);
}
