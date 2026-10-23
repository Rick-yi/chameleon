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
package vip.xiaonuo.sys.modular.audit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.xiaonuo.auth.core.util.StpLoginUserUtil;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.sys.modular.audit.entity.SysAuthAudit;
import vip.xiaonuo.sys.modular.audit.mapper.SysAuthAuditMapper;
import vip.xiaonuo.sys.modular.audit.param.SysAuthAuditPageParam;
import vip.xiaonuo.sys.modular.audit.service.SysAuthAuditService;

/**
 * 权限变更审计Service实现类
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Service
public class SysAuthAuditServiceImpl extends ServiceImpl<SysAuthAuditMapper, SysAuthAudit> implements SysAuthAuditService {

    @Override
    public void log(String action, String targetType, String targetId, String targetName, String summary, String detail) {
        try {
            SysAuthAudit audit = new SysAuthAudit();
            audit.setTenantId(TenantContext.getTenantId());
            try {
                if (StpLoginUserUtil.getLoginUser() != null) {
                    audit.setOperatorId(StpLoginUserUtil.getLoginUser().getId());
                    audit.setOperatorName(StpLoginUserUtil.getLoginUser().getName());
                }
            } catch (Exception ignore) {
                // 无登录态（脚本/迁移）时不记录操作人
            }
            audit.setAction(action);
            audit.setTargetType(targetType);
            audit.setTargetId(targetId);
            audit.setTargetName(StrUtil.maxLength(targetName, 120));
            audit.setSummary(StrUtil.maxLength(summary, 480));
            audit.setDetail(detail);
            this.save(audit);
        } catch (Exception e) {
            // 审计失败不能影响授权本身
            log.warn(">>> 权限变更审计写入失败：{}", e.getMessage());
        }
    }

    @Override
    public Page<SysAuthAudit> page(SysAuthAuditPageParam param) {
        LambdaQueryWrapper<SysAuthAudit> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(param.getAction()), SysAuthAudit::getAction, param.getAction())
                .eq(StrUtil.isNotBlank(param.getTargetType()), SysAuthAudit::getTargetType, param.getTargetType())
                .and(StrUtil.isNotBlank(param.getSearchKey()), q -> q
                        .like(SysAuthAudit::getTargetName, param.getSearchKey())
                        .or().like(SysAuthAudit::getOperatorName, param.getSearchKey()))
                .orderByDesc(SysAuthAudit::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }
}
