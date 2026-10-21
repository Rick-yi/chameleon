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
package vip.xiaonuo.finance.modular.export;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.student.api.StudentFinanceApi;
import vip.xiaonuo.student.api.dto.FinancePaymentDTO;
import vip.xiaonuo.student.api.dto.FinanceRefundDTO;
import vip.xiaonuo.student.api.dto.FinanceStatDTO;
import vip.xiaonuo.student.api.param.FinanceQueryParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务导出内容构建器
 * <p>
 * 同步导出与异步导出共用同一套行数据构建逻辑，保证两种方式产出一致。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Component
public class FinanceExportRowBuilder {

    @Resource
    private StudentFinanceApi studentFinanceApi;

    @Resource
    private vip.xiaonuo.common.cache.CommonCacheOperator cacheOperator;

    /** 导出数据缓存有效期（秒） */
    private static final long CACHE_TTL_SECONDS = 300L;

    /**
     * 构建导出内容
     *
     * @param type  导出类型：PAYMENT/REFUND/COURSE_SUMMARY
     * @param param 查询参数
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public ExportPayload build(String type, FinanceQueryParam param) {
        String exportType = StrUtil.blankToDefault(type, "PAYMENT").toUpperCase();
        List<List<String>> headList = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        String sheetName;
        if ("PAYMENT".equals(exportType)) {
            sheetName = "收款明细";
            headList.add(List.of("学员"));
            headList.add(List.of("课包"));
            headList.add(List.of("应收金额"));
            headList.add(List.of("实收金额"));
            headList.add(List.of("收款渠道"));
            headList.add(List.of("支付时间"));
            headList.add(List.of("状态"));
            headList.add(List.of("订单号"));
            for (FinancePaymentDTO dto : cachedList("paymentList", FinancePaymentDTO.class, param,
                    () -> studentFinanceApi.paymentList(param))) {
                rowList.add(List.of(text(dto.getStudentName()), text(dto.getPackageName()),
                        money(dto.getAmount()), money(dto.getPaidAmount()), channelName(dto.getPayChannel()),
                        time(dto.getPayTime() == null ? dto.getCreateTime() : dto.getPayTime()),
                        orderStatusName(dto.getStatus()), text(dto.getId())));
            }
        } else if ("REFUND".equals(exportType)) {
            sheetName = "退费明细";
            headList.add(List.of("学员"));
            headList.add(List.of("退费金额"));
            headList.add(List.of("退费课时"));
            headList.add(List.of("状态"));
            headList.add(List.of("审批时间"));
            headList.add(List.of("退费单号"));
            for (FinanceRefundDTO dto : cachedList("refundList", FinanceRefundDTO.class, param,
                    () -> studentFinanceApi.refundList(param))) {
                rowList.add(List.of(text(dto.getStudentName()), money(dto.getRefundAmount()),
                        money(dto.getRefundLessons()), refundStatusName(dto.getStatus()),
                        time(dto.getApproveTime()), text(dto.getId())));
            }
        } else if ("COURSE_SUMMARY".equals(exportType)) {
            sheetName = "课程汇总";
            headList.add(List.of("课程"));
            headList.add(List.of("收款金额"));
            headList.add(List.of("退费金额"));
            headList.add(List.of("净营收"));
            headList.add(List.of("课消课时"));
            headList.add(List.of("订单数"));
            for (FinanceStatDTO dto : cachedList("courseStat", FinanceStatDTO.class, param,
                    () -> studentFinanceApi.courseStat(param))) {
                rowList.add(List.of(text(dto.getCourseName()), money(dto.getPaidAmount()),
                        money(dto.getRefundAmount()), money(dto.getNetAmount()),
                        money(dto.getAttendLessons()),
                        String.valueOf(dto.getOrderCount() == null ? 0L : dto.getOrderCount())));
            }
        } else {
            throw new CommonException("不支持的导出类型：{}", exportType);
        }
        return new ExportPayload(sheetName, headList, rowList);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    /**
     * 列表缓存（JSON 字符串存储，按租户 + 参数隔离）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private <T> List<T> cachedList(String name, Class<T> elementType, Object param,
                                    java.util.function.Supplier<List<T>> supplier) {
        String paramJson = param == null ? "" : cn.hutool.json.JSONUtil.toJsonStr(param);
        String hash = cn.hutool.crypto.digest.DigestUtil.md5Hex(paramJson).substring(0, 16);
        String key = "report:" + StrUtil.blankToDefault(vip.xiaonuo.common.context.TenantContext.getTenantId(), "0")
                + ":" + name + ":" + hash;
        Object cached = cacheOperator.get(key);
        if (cached instanceof String json) {
            try {
                return cn.hutool.json.JSONUtil.toList(cn.hutool.json.JSONUtil.parseArray(json), elementType);
            } catch (Exception ignored) {
                // 缓存解析失败时回落到实时查询
            }
        }
        List<T> value = supplier.get();
        cacheOperator.put(key, cn.hutool.json.JSONUtil.toJsonStr(value), CACHE_TTL_SECONDS);
        return value;
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String time(java.util.Date value) {
        return value == null ? "" : cn.hutool.core.date.DateUtil.formatDateTime(value);
    }

    private String channelName(String channel) {
        if ("OFFLINE".equals(channel)) {
            return "线下转账";
        }
        if ("CASH".equals(channel)) {
            return "现金";
        }
        if ("WECHAT".equals(channel)) {
            return "微信";
        }
        if ("ALIPAY".equals(channel)) {
            return "支付宝";
        }
        return text(channel);
    }

    private String orderStatusName(String status) {
        if ("PAID".equals(status)) {
            return "已支付";
        }
        if ("REFUNDED".equals(status)) {
            return "已退费";
        }
        return text(status);
    }

    private String refundStatusName(String status) {
        return switch (status == null ? "" : status) {
            case "PENDING" -> "待审批";
            case "TO_REFUND" -> "待退款";
            case "REFUNDED" -> "已退款";
            case "REJECTED" -> "已驳回";
            case "CANCELLED" -> "已撤销";
            default -> text(status);
        };
    }

    /**
     * 导出内容
     *
     * @param sheetName 工作表名
     * @param headList  表头
     * @param rowList   行数据
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public record ExportPayload(String sheetName, List<List<String>> headList, List<List<String>> rowList) {
    }
}
