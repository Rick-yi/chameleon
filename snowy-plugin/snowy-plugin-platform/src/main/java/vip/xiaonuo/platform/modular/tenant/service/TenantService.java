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
package vip.xiaonuo.platform.modular.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.platform.modular.tenant.entity.Tenant;
import vip.xiaonuo.platform.modular.tenant.param.TenantAddParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantEditParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantIdParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantPageParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantRenewParam;
import vip.xiaonuo.platform.modular.tenant.param.TenantStatusParam;

import java.util.List;
import java.util.Map;

/**
 * 租户Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface TenantService {

    /** 获取租户分页 */
    Page<Tenant> page(TenantPageParam tenantPageParam);

    /** 添加租户 */
    void add(TenantAddParam tenantAddParam);

    /** 编辑租户 */
    void edit(TenantEditParam tenantEditParam);

    /** 删除租户 */
    void delete(List<TenantIdParam> tenantIdParamList);

    /** 获取租户详情 */
    Tenant detail(TenantIdParam tenantIdParam);

    /** 获取租户详情（内部用） */
    Tenant queryEntity(String id);

    /** 按id获取租户，不存在返回null（不抛异常，供跨插件调用） */
    Tenant getTenantById(String id);

    /** 按租户编码获取租户，不存在返回null */
    Tenant getTenantByCode(String code);

    /** 获取所有未停用租户（不含平台运营租户） */
    List<Tenant> listActiveTenants();

    /** 到期扫描：将已过期租户置为停用，返回处理数量 */
    int checkExpired();

    /** 续费/延期：更新到期时间并恢复状态，返回新的到期信息 */
    Map<String, Object> renew(TenantRenewParam tenantRenewParam);

    /** 停用/启用租户；停用时踢出该租户全部会话 */
    void changeStatus(TenantStatusParam tenantStatusParam);

    /** 重置租户管理员密码，返回一次性密码（仅本次返回） */
    Map<String, Object> resetAdminPassword(TenantIdParam tenantIdParam);

    /** 最近登录记录 */
    List<Map<String, Object>> loginRecords(TenantIdParam tenantIdParam);

    /** 续费记录 */
    List<Map<String, Object>> renewRecords(TenantIdParam tenantIdParam);

    /** 租户不可用原因；可用时返回 null */
    String getTenantUnavailableReason(String tenantId);
}
