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
package vip.xiaonuo.platform.modular.tenant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.auth.api.AuthApi;
import vip.xiaonuo.auth.core.pojo.SaBaseLoginUser;
import vip.xiaonuo.auth.core.util.StpLoginUserUtil;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.common.util.CommonCryptogramUtil;
import vip.xiaonuo.platform.core.service.TenantCapabilityService;
import vip.xiaonuo.platform.modular.tenantpackage.entity.TenantPackage;
import vip.xiaonuo.platform.modular.tenantpackage.mapper.TenantPackageMapper;
import vip.xiaonuo.platform.modular.tenantpackage.service.TenantPackageService;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.enums.TenantStatusEnum;
import vip.xiaonuo.platform.modular.tenant.mapper.TenantMapper;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantEditParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantIdParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantPageParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantRenewParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantStatusParam;
import vip.xiaonuo.platform.modular.tenant.service.TenantInitService;
import vip.xiaonuo.platform.modular.tenant.service.TenantService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

/**
 * 租户Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantPackageService tenantPackageService;

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TenantCapabilityService tenantCapabilityService;

    @Resource
    private TenantInitService tenantInitService;

    @Resource
    private AuthApi authApi;

    @Override
    public Page<Tenant> page(TenantPageParam tenantPageParam) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<Tenant>().checkSqlInjection();
        // 平台运营租户（id=0）仅用于平台账号归属，不在租户列表中展示
        queryWrapper.lambda().ne(Tenant::getId, "0");
        if (ObjectUtil.isNotEmpty(tenantPageParam.getSearchKey())) {
            queryWrapper.lambda().and(q -> q.like(Tenant::getName, tenantPageParam.getSearchKey())
                    .or().like(Tenant::getCode, tenantPageParam.getSearchKey())
                    .or().like(Tenant::getContactPhone, tenantPageParam.getSearchKey()));
        }
        if (ObjectUtil.isAllNotEmpty(tenantPageParam.getSortField(), tenantPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(tenantPageParam.getSortOrder());
            queryWrapper.orderBy(true, tenantPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(tenantPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(Tenant::getCreateTime);
        }
        Page<Tenant> tenantPage = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillDisplayFields(tenantPage.getRecords());
        return tenantPage;
    }

    /**
     * 填充租户列表的展示字段（套餐限额 / 到期倒计时 / 最近登录 / 经营数据）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private void fillDisplayFields(List<Tenant> tenantList) {
        if (ObjectUtil.isEmpty(tenantList)) {
            return;
        }
        // 套餐信息
        List<String> packageIdList = tenantList.stream().map(Tenant::getPackageId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, TenantPackage> packageMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(packageIdList)) {
            tenantPackageMapper.selectBatchIds(packageIdList)
                    .forEach(item -> packageMap.put(item.getId(), item));
        }
        // 最近登录
        Map<String, Date> lastLoginMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, MAX(LOGIN_TIME) AS LAST_LOGIN_TIME FROM T_TENANT_LOGIN "
                        + "WHERE DELETE_FLAG = 'NOT_DELETE' GROUP BY TENANT_ID")) {
            Object time = row.get("LAST_LOGIN_TIME");
            if (time instanceof Date date) {
                lastLoginMap.put(String.valueOf(row.get("TENANT_ID")), date);
            }
        }
        // 经营数据
        Map<String, Long> studentCountMap = countByTenant(
                "SELECT TENANT_ID, COUNT(*) AS CNT FROM STUDENT WHERE DELETE_FLAG = 'NOT_DELETE' GROUP BY TENANT_ID");
        Map<String, Long> orderCountMap = countByTenant(
                "SELECT TENANT_ID, COUNT(*) AS CNT FROM STUDENT_ORDER WHERE DELETE_FLAG = 'NOT_DELETE' "
                        + "AND STATUS IN ('PAID','REFUNDED') GROUP BY TENANT_ID");
        Map<String, BigDecimal> paidAmountMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT TENANT_ID, COALESCE(SUM(PAID_AMOUNT),0) AS AMT FROM STUDENT_ORDER "
                        + "WHERE DELETE_FLAG = 'NOT_DELETE' AND STATUS IN ('PAID','REFUNDED') GROUP BY TENANT_ID")) {
            Object amount = row.get("AMT");
            paidAmountMap.put(String.valueOf(row.get("TENANT_ID")),
                    amount instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO);
        }
        long today = System.currentTimeMillis();
        for (Tenant tenant : tenantList) {
            TenantPackage tenantPackage = packageMap.get(tenant.getPackageId());
            if (ObjectUtil.isNotEmpty(tenantPackage)) {
                tenant.setPackageName(tenantPackage.getName());
                tenant.setStudentLimit(tenantPackage.getStudentLimit());
                tenant.setCampusLimit(tenantPackage.getCampusLimit());
            }
            if (tenant.getExpireTime() != null) {
                long days = (tenant.getExpireTime().getTime() - today) / (24 * 60 * 60 * 1000L);
                tenant.setExpireDays(days);
            }
            tenant.setLastLoginTime(lastLoginMap.get(tenant.getId()));
            tenant.setStudentCount(studentCountMap.getOrDefault(tenant.getId(), 0L));
            tenant.setOrderCount(orderCountMap.getOrDefault(tenant.getId(), 0L));
            tenant.setPaidAmount(paidAmountMap.getOrDefault(tenant.getId(), BigDecimal.ZERO));
        }
    }

    /**
     * 按租户统计数量
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private Map<String, Long> countByTenant(String sql) {
        Map<String, Long> resultMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
            Object count = row.get("CNT");
            if (count instanceof Number number) {
                resultMap.put(String.valueOf(row.get("TENANT_ID")), number.longValue());
            }
        }
        return resultMap;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(TenantAddParam tenantAddParam) {
        Tenant tenant = BeanUtil.toBean(tenantAddParam, Tenant.class);
        if ("platform".equalsIgnoreCase(tenant.getCode())) {
            throw new CommonException("租户编码 platform 为平台运营保留编码");
        }
        boolean repeatCode = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, tenant.getCode())) > 0;
        if (repeatCode) {
            throw new CommonException("存在重复的租户编码：{}", tenant.getCode());
        }
        boolean repeatName = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getName, tenant.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的机构名称：{}", tenant.getName());
        }
        // 新开通租户默认试用状态
        tenant.setStatus(TenantStatusEnum.TRIAL.getValue());
        // 套餐联动：校验套餐并按套餐有效期自动计算到期时间
        if (ObjectUtil.isNotEmpty(tenant.getPackageId())) {
            TenantPackage tenantPackage = tenantPackageService.queryEntity(tenant.getPackageId());
            if (ObjectUtil.isEmpty(tenant.getExpireTime())) {
                int validityDays = ObjectUtil.isNotEmpty(tenantPackage.getValidityDays())
                        ? tenantPackage.getValidityDays() : 365;
                tenant.setExpireTime(DateUtil.offsetDay(new java.util.Date(), validityDays));
            }
        }
        this.save(tenant);
        // 租户模板数据初始化：校区、管理员、角色、菜单、配置、字典
        tenantInitService.initTenant(tenant, tenantAddParam);
        // 刷新租户能力缓存（套餐限额与功能开关）
        tenantCapabilityService.refresh(tenant.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(TenantEditParam tenantEditParam) {
        if ("0".equals(tenantEditParam.getId())) {
            throw new CommonException("平台运营租户不允许编辑");
        }
        Tenant tenant = this.queryEntity(tenantEditParam.getId());
        BeanUtil.copyProperties(tenantEditParam, tenant);
        if ("platform".equalsIgnoreCase(tenant.getCode())) {
            throw new CommonException("租户编码 platform 为平台运营保留编码");
        }
        boolean repeatCode = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, tenant.getCode())
                .ne(Tenant::getId, tenant.getId())) > 0;
        if (repeatCode) {
            throw new CommonException("存在重复的租户编码：{}", tenant.getCode());
        }
        boolean repeatName = this.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getName, tenant.getName())
                .ne(Tenant::getId, tenant.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的机构名称：{}", tenant.getName());
        }
        this.updateById(tenant);
        // 刷新租户能力缓存（套餐限额与功能开关）
        tenantCapabilityService.refresh(tenant.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<TenantIdParam> tenantIdParamList) {
        List<String> tenantIdList = CollStreamUtil.toList(tenantIdParamList, TenantIdParam::getId);
        if (tenantIdList.contains("0")) {
            throw new CommonException("平台运营租户不允许删除");
        }
        if (ObjectUtil.isNotEmpty(tenantIdList)) {
            this.removeByIds(tenantIdList);
        }
    }

    @Override
    public Tenant detail(TenantIdParam tenantIdParam) {
        return this.queryEntity(tenantIdParam.getId());
    }

    @Override
    public Tenant queryEntity(String id) {
        Tenant tenant = this.getById(id);
        if (ObjectUtil.isEmpty(tenant)) {
            throw new CommonException("租户不存在，id值为：{}", id);
        }
        return tenant;
    }

    @Override
    public Tenant getTenantById(String id) {
        return this.getById(id);
    }

    @Override
    public Tenant getTenantByCode(String code) {
        if (ObjectUtil.isEmpty(code)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code));
    }

    @Override
    public List<Tenant> listActiveTenants() {
        return this.list(new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, "0")
                .ne(Tenant::getStatus, TenantStatusEnum.STOP.getValue()));
    }

    @Override
    public int checkExpired() {
        java.util.List<Tenant> expiredList = this.list(new LambdaQueryWrapper<Tenant>()
                .ne(Tenant::getId, "0")
                .ne(Tenant::getStatus, TenantStatusEnum.STOP.getValue())
                .isNotNull(Tenant::getExpireTime)
                .lt(Tenant::getExpireTime, new java.util.Date()));
        for (Tenant tenant : expiredList) {
            tenant.setStatus(TenantStatusEnum.STOP.getValue());
        }
        if (ObjectUtil.isNotEmpty(expiredList)) {
            this.updateBatchById(expiredList);
            expiredList.forEach(tenant -> log.info(">>> 租户已到期，自动停用：{}（{}）", tenant.getName(), tenant.getId()));
        }
        return expiredList.size();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> renew(TenantRenewParam tenantRenewParam) {
        if ("0".equals(tenantRenewParam.getId())) {
            throw new CommonException("平台运营租户不需要续费");
        }
        Tenant tenant = this.queryEntity(tenantRenewParam.getId());
        Date oldExpireTime = tenant.getExpireTime();
        Date now = new Date();
        // 已过期从今天续，未过期从原到期日续
        Date baseTime = ObjectUtil.isEmpty(oldExpireTime) || oldExpireTime.before(now) ? now : oldExpireTime;
        Date newExpireTime = DateUtil.offsetDay(baseTime, tenantRenewParam.getDays());
        if (StrUtil.isNotBlank(tenantRenewParam.getPackageId())) {
            TenantPackage tenantPackage = tenantPackageService.queryEntity(tenantRenewParam.getPackageId());
            tenant.setPackageId(tenantPackage.getId());
        }
        tenant.setExpireTime(newExpireTime);
        tenant.setStatus(TenantStatusEnum.NORMAL.getValue());
        this.updateById(tenant);
        tenantCapabilityService.refresh(tenant.getId());

        SaBaseLoginUser loginUser = StpLoginUserUtil.getLoginUser();
        jdbcTemplate.update("""
                        INSERT INTO `T_TENANT_RENEW` (`ID`, `TENANT_ID`, `PACKAGE_ID`, `DAYS`, `AMOUNT`,
                        `OLD_EXPIRE_TIME`, `NEW_EXPIRE_TIME`, `REMARK`, `OPERATOR_ID`, `OPERATOR_NAME`,
                        `DELETE_FLAG`, `CREATE_TIME`)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'NOT_DELETE', ?)
                        """,
                IdUtil.getSnowflakeNextIdStr(), tenant.getId(), tenant.getPackageId(), tenantRenewParam.getDays(),
                ObjectUtil.defaultIfNull(tenantRenewParam.getAmount(), BigDecimal.ZERO),
                oldExpireTime, newExpireTime, tenantRenewParam.getRemark(),
                loginUser == null ? null : loginUser.getId(), loginUser == null ? null : loginUser.getName(), now);

        Map<String, Object> result = new HashMap<>();
        result.put("id", tenant.getId());
        result.put("expireTime", newExpireTime);
        result.put("status", tenant.getStatus());
        result.put("days", tenantRenewParam.getDays());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void changeStatus(TenantStatusParam tenantStatusParam) {
        if ("0".equals(tenantStatusParam.getId())) {
            throw new CommonException("平台运营租户不允许停用");
        }
        String targetStatus = StrUtil.blankToDefault(tenantStatusParam.getStatus(), "").toUpperCase();
        if (!TenantStatusEnum.STOP.getValue().equals(targetStatus)
                && !TenantStatusEnum.NORMAL.getValue().equals(targetStatus)) {
            throw new CommonException("不支持的目标状态：{}", tenantStatusParam.getStatus());
        }
        if (TenantStatusEnum.STOP.getValue().equals(targetStatus) && StrUtil.isBlank(tenantStatusParam.getReason())) {
            throw new CommonException("停用租户必须填写原因");
        }
        Tenant tenant = this.queryEntity(tenantStatusParam.getId());
        SaBaseLoginUser loginUser = StpLoginUserUtil.getLoginUser();
        JSONObject extJson = StrUtil.isBlank(tenant.getExtJson()) ? new JSONObject() : JSONUtil.parseObj(tenant.getExtJson());
        tenant.setStatus(targetStatus);
        if (TenantStatusEnum.STOP.getValue().equals(targetStatus)) {
            extJson.set("stopReason", tenantStatusParam.getReason());
            extJson.set("stopTime", DateUtil.formatDateTime(new Date()));
            extJson.set("stopBy", loginUser == null ? null : loginUser.getName());
        } else {
            extJson.set("resumeTime", DateUtil.formatDateTime(new Date()));
            extJson.set("resumeBy", loginUser == null ? null : loginUser.getName());
        }
        tenant.setExtJson(JSONUtil.toJsonStr(extJson));
        this.updateById(tenant);
        if (TenantStatusEnum.STOP.getValue().equals(targetStatus)) {
            // 停用后立即失效该租户全部登录态
            List<String> userIdList = jdbcTemplate.queryForList(
                    "SELECT `ID` FROM `SYS_USER` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'",
                    String.class, tenant.getId());
            authApi.logoutByUserIdList(userIdList);
            log.info(">>> 租户已停用：{}（{}），原因：{}，踢出会话 {} 个", tenant.getName(), tenant.getId(),
                    tenantStatusParam.getReason(), userIdList.size());
        } else {
            log.info(">>> 租户已启用：{}（{}）", tenant.getName(), tenant.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> resetAdminPassword(TenantIdParam tenantIdParam) {
        if ("0".equals(tenantIdParam.getId())) {
            throw new CommonException("平台运营租户不支持重置");
        }
        Tenant tenant = this.queryEntity(tenantIdParam.getId());
        Map<String, Object> adminUser;
        try {
            adminUser = jdbcTemplate.queryForMap("""
                    SELECT `ID` AS id, `ACCOUNT` AS account FROM `SYS_USER`
                    WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'
                    ORDER BY CASE WHEN `ACCOUNT` = 'admin' THEN 0 ELSE 1 END, `CREATE_TIME` ASC
                    LIMIT 1
                    """, tenant.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new CommonException("该租户还没有可重置的管理员账号");
        }
        String userId = String.valueOf(adminUser.get("id"));
        String account = String.valueOf(adminUser.get("account"));
        String newPassword = RandomUtil.randomString(
                "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789", 12);
        jdbcTemplate.update("UPDATE `SYS_USER` SET `PASSWORD` = ?, `UPDATE_TIME` = NOW() WHERE `ID` = ?",
                CommonCryptogramUtil.doHashValue(newPassword), userId);
        // 记录重置痕迹（不保存密码本身）
        SaBaseLoginUser operator = StpLoginUserUtil.getLoginUser();
        JSONObject tenantExt = StrUtil.isBlank(tenant.getExtJson())
                ? new JSONObject() : JSONUtil.parseObj(tenant.getExtJson());
        tenantExt.set("passwordResetTime", DateUtil.formatDateTime(new Date()));
        tenantExt.set("passwordResetBy", operator == null ? null : operator.getName());
        tenant.setExtJson(JSONUtil.toJsonStr(tenantExt));
        this.updateById(tenant);
        // 旧密码已失效，立即踢出该管理员的所有会话
        authApi.logoutByUserIdList(List.of(userId));
        log.info(">>> 已重置租户管理员密码：{} / {}（{}）", tenant.getName(), account, tenant.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenant.getId());
        result.put("tenantCode", tenant.getCode());
        result.put("tenantName", tenant.getName());
        result.put("account", account);
        result.put("password", newPassword);
        return result;
    }

    @Override
    public List<Map<String, Object>> loginRecords(TenantIdParam tenantIdParam) {
        this.queryEntity(tenantIdParam.getId());
        return jdbcTemplate.queryForList("""
                SELECT `ACCOUNT` AS account,
                       DATE_FORMAT(`LOGIN_TIME`, '%Y-%m-%d %H:%i:%s') AS loginTime
                FROM `T_TENANT_LOGIN`
                WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'
                ORDER BY `LOGIN_TIME` DESC LIMIT 20
                """, tenantIdParam.getId());
    }

    @Override
    public List<Map<String, Object>> renewRecords(TenantIdParam tenantIdParam) {
        this.queryEntity(tenantIdParam.getId());
        return jdbcTemplate.queryForList("""
                SELECT `DAYS` AS days, `AMOUNT` AS amount,
                       DATE_FORMAT(`OLD_EXPIRE_TIME`, '%Y-%m-%d %H:%i:%s') AS oldExpireTime,
                       DATE_FORMAT(`NEW_EXPIRE_TIME`, '%Y-%m-%d %H:%i:%s') AS newExpireTime,
                       `REMARK` AS remark, `OPERATOR_NAME` AS operatorName,
                       DATE_FORMAT(`CREATE_TIME`, '%Y-%m-%d %H:%i:%s') AS createTime
                FROM `T_TENANT_RENEW` WHERE `TENANT_ID` = ? AND `DELETE_FLAG` = 'NOT_DELETE'
                ORDER BY `CREATE_TIME` DESC LIMIT 20
                """, tenantIdParam.getId());
    }

    @Override
    public String getTenantUnavailableReason(String tenantId) {
        // 平台运营租户不参与到期/停用校验，避免平台自己把自己锁在门外
        if ("0".equals(tenantId)) {
            return null;
        }
        Tenant tenant = this.getTenantById(tenantId);
        if (ObjectUtil.isEmpty(tenant)) {
            return "机构不存在或已删除，请联系平台运营";
        }
        if (TenantStatusEnum.STOP.getValue().equals(tenant.getStatus())) {
            return "机构已停用，请联系平台运营";
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date())) {
            return "机构已于 " + DateUtil.formatDate(tenant.getExpireTime()) + " 到期，请联系平台运营续费";
        }
        return null;
    }
}
