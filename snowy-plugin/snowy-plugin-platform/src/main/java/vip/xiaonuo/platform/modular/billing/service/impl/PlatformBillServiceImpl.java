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
package vip.xiaonuo.platform.modular.billing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.auth.core.pojo.SaBaseLoginUser;
import vip.xiaonuo.auth.core.util.StpLoginUserUtil;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.platform.modular.billing.dto.PlatformBillSummaryDTO;
import vip.xiaonuo.platform.modular.billing.entity.PlatformBill;
import vip.xiaonuo.platform.modular.billing.mapper.PlatformBillMapper;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillAddParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillEditParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillIdParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillPageParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillPayParam;
import vip.xiaonuo.platform.modular.billing.param.PlatformBillVoidParam;
import vip.xiaonuo.platform.modular.billing.service.PlatformBillService;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.mapper.TenantPackageMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台账单Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/26 12:00
 **/
@Service
public class PlatformBillServiceImpl extends ServiceImpl<PlatformBillMapper, PlatformBill>
        implements PlatformBillService {

    private static final String STATUS_UNPAID = "UNPAID";

    private static final String STATUS_PAID = "PAID";

    private static final String STATUS_VOID = "VOID";

    private static final String SOURCE_MANUAL = "MANUAL";

    private static final String SOURCE_RENEW = "RENEW";

    private static final String INVOICE_NONE = "NONE";

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @Override
    public Page<PlatformBill> page(PlatformBillPageParam pageParam) {
        Page<PlatformBill> billPage = this.page(CommonPageRequest.defaultPage(), buildQuery(pageParam));
        fillDisplayFields(billPage.getRecords());
        return billPage;
    }

    @Override
    public PlatformBillSummaryDTO summary(PlatformBillPageParam pageParam) {
        List<PlatformBill> billList = this.list(buildQuery(pageParam));
        PlatformBillSummaryDTO summary = new PlatformBillSummaryDTO();
        summary.setBillCount((long) billList.size());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal unpaid = BigDecimal.ZERO;
        BigDecimal voidAmount = BigDecimal.ZERO;
        long paidCount = 0L;
        long unpaidCount = 0L;
        long voidCount = 0L;
        for (PlatformBill bill : billList) {
            BigDecimal amount = ObjectUtil.defaultIfNull(bill.getAmount(), BigDecimal.ZERO);
            total = total.add(amount);
            if (STATUS_PAID.equals(bill.getStatus())) {
                paid = paid.add(amount);
                paidCount++;
            } else if (STATUS_VOID.equals(bill.getStatus())) {
                voidAmount = voidAmount.add(amount);
                voidCount++;
            } else {
                unpaid = unpaid.add(amount);
                unpaidCount++;
            }
        }
        summary.setPaidCount(paidCount);
        summary.setUnpaidCount(unpaidCount);
        summary.setVoidCount(voidCount);
        summary.setTotalAmount(money(total));
        summary.setPaidAmount(money(paid));
        summary.setUnpaidAmount(money(unpaid));
        summary.setVoidAmount(money(voidAmount));
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(PlatformBillAddParam addParam) {
        Tenant tenant = tenantMapper.selectById(addParam.getTenantId());
        if (ObjectUtil.isEmpty(tenant)) {
            throw new CommonException("租户不存在，id值为：{}", addParam.getTenantId());
        }
        PlatformBill bill = BeanUtil.toBean(addParam, PlatformBill.class);
        bill.setBillNo(generateBillNo());
        bill.setStatus(normalizeStatus(addParam.getStatus(), STATUS_UNPAID));
        bill.setInvoiceStatus(normalizeInvoiceStatus(addParam.getInvoiceStatus()));
        bill.setSource(SOURCE_MANUAL);
        bill.setAmount(money(ObjectUtil.defaultIfNull(addParam.getAmount(), BigDecimal.ZERO)));
        if (STATUS_PAID.equals(bill.getStatus()) && bill.getPayTime() == null) {
            bill.setPayTime(new Date());
        }
        fillOperator(bill);
        this.save(bill);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(PlatformBillEditParam editParam) {
        PlatformBill bill = queryEntity(editParam.getId());
        String billNo = bill.getBillNo();
        String source = bill.getSource();
        BeanUtil.copyProperties(editParam, bill);
        bill.setBillNo(billNo);
        bill.setSource(source);
        bill.setStatus(normalizeStatus(editParam.getStatus(), STATUS_UNPAID));
        bill.setInvoiceStatus(normalizeInvoiceStatus(editParam.getInvoiceStatus()));
        bill.setAmount(money(ObjectUtil.defaultIfNull(editParam.getAmount(), BigDecimal.ZERO)));
        if (STATUS_PAID.equals(bill.getStatus()) && bill.getPayTime() == null) {
            bill.setPayTime(new Date());
        }
        fillOperator(bill);
        this.updateById(bill);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<PlatformBillIdParam> idParamList) {
        List<String> idList = CollStreamUtil.toList(idParamList, PlatformBillIdParam::getId);
        if (ObjectUtil.isNotEmpty(idList)) {
            this.removeByIds(idList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markPaid(PlatformBillPayParam payParam) {
        PlatformBill bill = queryEntity(payParam.getId());
        if (STATUS_VOID.equals(bill.getStatus())) {
            throw new CommonException("已作废账单不能登记收款");
        }
        bill.setStatus(STATUS_PAID);
        bill.setPayTime(payParam.getPayTime() == null ? new Date() : payParam.getPayTime());
        bill.setPayMethod(StrUtil.blankToDefault(payParam.getPayMethod(), "OFFLINE"));
        if (StrUtil.isNotBlank(payParam.getRemark())) {
            bill.setRemark(payParam.getRemark());
        }
        fillOperator(bill);
        this.updateById(bill);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void voidBill(PlatformBillVoidParam voidParam) {
        PlatformBill bill = queryEntity(voidParam.getId());
        bill.setStatus(STATUS_VOID);
        if (StrUtil.isNotBlank(voidParam.getReason())) {
            String remark = StrUtil.blankToDefault(bill.getRemark(), "");
            bill.setRemark(StrUtil.blankToDefault(remark + "；作废原因：" + voidParam.getReason(),
                    "作废原因：" + voidParam.getReason()));
        }
        fillOperator(bill);
        this.updateById(bill);
    }

    @Override
    public void export(PlatformBillPageParam pageParam, HttpServletResponse response) throws IOException {
        List<PlatformBill> billList = this.list(buildQuery(pageParam).last("LIMIT 5000"));
        if (ObjectUtil.isEmpty(billList)) {
            throw new CommonException("无数据可导出");
        }
        fillDisplayFields(billList);
        List<List<String>> headList = new ArrayList<>();
        headList.add(List.of("账单号"));
        headList.add(List.of("机构编码"));
        headList.add(List.of("机构名称"));
        headList.add(List.of("套餐"));
        headList.add(List.of("周期开始"));
        headList.add(List.of("周期结束"));
        headList.add(List.of("金额(元)"));
        headList.add(List.of("状态"));
        headList.add(List.of("收款方式"));
        headList.add(List.of("收款时间"));
        headList.add(List.of("发票状态"));
        headList.add(List.of("来源"));
        headList.add(List.of("备注"));
        headList.add(List.of("操作人"));
        headList.add(List.of("创建时间"));
        List<List<String>> rowList = new ArrayList<>();
        for (PlatformBill bill : billList) {
            rowList.add(List.of(
                    text(bill.getBillNo()),
                    text(bill.getTenantCode()),
                    text(bill.getTenantName()),
                    text(bill.getPackageName()),
                    formatDate(bill.getPeriodStart()),
                    formatDate(bill.getPeriodEnd()),
                    money(bill.getAmount()).toPlainString(),
                    statusText(bill.getStatus()),
                    payMethodText(bill.getPayMethod()),
                    formatDate(bill.getPayTime()),
                    invoiceText(bill.getInvoiceStatus()),
                    SOURCE_RENEW.equals(bill.getSource()) ? "续费自动生成" : "手工登记",
                    text(bill.getRemark()),
                    text(bill.getOperatorName()),
                    formatDate(bill.getCreateTime())));
        }
        File file = FileUtil.file(FileUtil.getTmpDir(),
                "平台账单对账_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
        try {
            CommonExcelUtil.write(file, "账单对账", headList, rowList);
            CommonDownloadUtil.download(file, response);
        } finally {
            FileUtil.del(file);
        }
    }

    @Override
    public void recordRenewBill(Tenant tenant, String packageId, BigDecimal amount, Date periodStart,
                                Date periodEnd, String remark, String operatorId, String operatorName) {
        PlatformBill bill = new PlatformBill();
        bill.setBillNo(generateBillNo());
        bill.setTenantId(tenant.getId());
        bill.setPackageId(packageId);
        bill.setPeriodStart(periodStart);
        bill.setPeriodEnd(periodEnd);
        bill.setAmount(money(ObjectUtil.defaultIfNull(amount, BigDecimal.ZERO)));
        bill.setStatus(STATUS_PAID);
        bill.setPayMethod("OFFLINE");
        bill.setPayTime(new Date());
        bill.setInvoiceStatus(INVOICE_NONE);
        bill.setSource(SOURCE_RENEW);
        bill.setRemark(remark);
        bill.setOperatorId(operatorId);
        bill.setOperatorName(operatorName);
        this.save(bill);
    }

    private LambdaQueryWrapper<PlatformBill> buildQuery(PlatformBillPageParam pageParam) {
        LambdaQueryWrapper<PlatformBill> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageParam.getSearchKey())) {
            String key = pageParam.getSearchKey().trim();
            List<String> tenantIds = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                            .like(Tenant::getName, key)
                            .or().like(Tenant::getCode, key))
                    .stream().map(Tenant::getId).collect(Collectors.toList());
            queryWrapper.and(q -> {
                q.like(PlatformBill::getBillNo, key);
                if (ObjectUtil.isNotEmpty(tenantIds)) {
                    q.or().in(PlatformBill::getTenantId, tenantIds);
                }
            });
        }
        queryWrapper.eq(StrUtil.isNotBlank(pageParam.getTenantId()), PlatformBill::getTenantId,
                pageParam.getTenantId());
        queryWrapper.eq(StrUtil.isNotBlank(pageParam.getPackageId()), PlatformBill::getPackageId,
                pageParam.getPackageId());
        queryWrapper.eq(StrUtil.isNotBlank(pageParam.getStatus()), PlatformBill::getStatus,
                pageParam.getStatus());
        queryWrapper.eq(StrUtil.isNotBlank(pageParam.getInvoiceStatus()), PlatformBill::getInvoiceStatus,
                pageParam.getInvoiceStatus());
        if (StrUtil.isNotBlank(pageParam.getStartMonth())) {
            YearMonth start = parseMonth(pageParam.getStartMonth());
            queryWrapper.ge(PlatformBill::getPeriodStart, monthStart(start));
        }
        if (StrUtil.isNotBlank(pageParam.getEndMonth())) {
            YearMonth end = parseMonth(pageParam.getEndMonth());
            queryWrapper.lt(PlatformBill::getPeriodStart, monthStart(end.plusMonths(1)));
        }
        queryWrapper.orderByDesc(PlatformBill::getCreateTime);
        return queryWrapper;
    }

    private void fillDisplayFields(List<PlatformBill> billList) {
        if (ObjectUtil.isEmpty(billList)) {
            return;
        }
        List<String> tenantIds = billList.stream().map(PlatformBill::getTenantId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, Tenant> tenantMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(tenantIds)) {
            tenantMapper.selectBatchIds(tenantIds).forEach(tenant -> tenantMap.put(tenant.getId(), tenant));
        }
        List<String> packageIds = billList.stream().map(PlatformBill::getPackageId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, TenantPackage> packageMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(packageIds)) {
            tenantPackageMapper.selectBatchIds(packageIds)
                    .forEach(item -> packageMap.put(item.getId(), item));
        }
        for (PlatformBill bill : billList) {
            Tenant tenant = tenantMap.get(bill.getTenantId());
            if (tenant != null) {
                bill.setTenantName(tenant.getName());
                bill.setTenantCode(tenant.getCode());
            }
            TenantPackage tenantPackage = packageMap.get(bill.getPackageId());
            if (tenantPackage != null) {
                bill.setPackageName(tenantPackage.getName());
            }
        }
    }

    private PlatformBill queryEntity(String id) {
        PlatformBill bill = this.getById(id);
        if (ObjectUtil.isEmpty(bill)) {
            throw new CommonException("账单不存在，id值为：{}", id);
        }
        return bill;
    }

    private String generateBillNo() {
        return "B" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(3);
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String value = StrUtil.blankToDefault(status, defaultStatus).toUpperCase();
        if (!STATUS_UNPAID.equals(value) && !STATUS_PAID.equals(value) && !STATUS_VOID.equals(value)) {
            throw new CommonException("不支持的账单状态：{}", status);
        }
        return value;
    }

    private String normalizeInvoiceStatus(String invoiceStatus) {
        String value = StrUtil.blankToDefault(invoiceStatus, INVOICE_NONE).toUpperCase();
        if (!"NONE".equals(value) && !"REQUESTED".equals(value) && !"ISSUED".equals(value)) {
            throw new CommonException("不支持的发票状态：{}", invoiceStatus);
        }
        return value;
    }

    private void fillOperator(PlatformBill bill) {
        SaBaseLoginUser loginUser = StpLoginUserUtil.getLoginUser();
        if (loginUser != null) {
            bill.setOperatorId(loginUser.getId());
            bill.setOperatorName(loginUser.getName());
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new CommonException("月份格式应为 yyyy-MM：{}", month);
        }
    }

    private Date monthStart(YearMonth yearMonth) {
        return Date.from(yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Date date) {
        return date == null ? "" : DateUtil.formatDateTime(date);
    }

    private String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, "")) {
            case STATUS_UNPAID -> "待收款";
            case STATUS_PAID -> "已收款";
            case STATUS_VOID -> "已作废";
            default -> text(status);
        };
    }

    private String payMethodText(String payMethod) {
        return switch (StrUtil.blankToDefault(payMethod, "")) {
            case "OFFLINE" -> "线下";
            case "BANK" -> "银行转账";
            case "OTHER" -> "其他";
            default -> text(payMethod);
        };
    }

    private String invoiceText(String invoiceStatus) {
        return switch (StrUtil.blankToDefault(invoiceStatus, "")) {
            case "NONE" -> "未开票";
            case "REQUESTED" -> "已申请";
            case "ISSUED" -> "已开票";
            default -> text(invoiceStatus);
        };
    }
}
