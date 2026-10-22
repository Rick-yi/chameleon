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
package vip.xiaonuo.platform.modular.roletemplate.service;

import vip.xiaonuo.platform.modular.roletemplate.PlatformRoleTemplate;

import java.util.List;

/**
 * 角色模板Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
public interface PlatformRoleTemplateService {

    /** 角色模板列表 */
    List<PlatformRoleTemplate> listTemplates();

    /**
     * 为租户补齐标准业务角色（幂等：已存在则补授权，不覆盖管理员手工调整的其它授权）
     *
     * @param tenantId 租户id
     * @return 创建/更新的角色数量
     */
    int ensureTenantRoles(String tenantId);

    /**
     * 为租户重新套用模板（覆盖式：先清掉模板角色的授权再按模板重建）
     *
     * @param tenantId 租户id
     * @return 套用的角色数量
     */
    int applyTemplates(String tenantId);
}
