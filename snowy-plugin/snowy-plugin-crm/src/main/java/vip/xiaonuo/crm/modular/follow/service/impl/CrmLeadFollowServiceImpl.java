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
package vip.xiaonuo.crm.modular.follow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.crm.modular.follow.entity.CrmLeadFollow;
import vip.xiaonuo.crm.modular.follow.mapper.CrmLeadFollowMapper;
import vip.xiaonuo.crm.modular.follow.param.CrmLeadFollowAddParam;
import vip.xiaonuo.crm.modular.follow.param.CrmLeadFollowPageParam;
import vip.xiaonuo.crm.modular.follow.service.CrmLeadFollowService;
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadStatusEnum;
import vip.xiaonuo.crm.modular.lead.service.CrmLeadService;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import java.util.Map;

/**
 * 线索跟进记录Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CrmLeadFollowServiceImpl extends ServiceImpl<CrmLeadFollowMapper, CrmLeadFollow> implements CrmLeadFollowService {

    @Resource
    private CrmLeadService crmLeadService;

    @Resource
    private BizEventApi bizEventApi;

    @Override
    public Page<CrmLeadFollow> page(CrmLeadFollowPageParam crmLeadFollowPageParam) {
        QueryWrapper<CrmLeadFollow> queryWrapper = new QueryWrapper<CrmLeadFollow>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(CrmLeadFollow::getLeadId, crmLeadFollowPageParam.getLeadId())
                .eq(StrUtil.isNotBlank(crmLeadFollowPageParam.getFollowType()),
                        CrmLeadFollow::getFollowType, crmLeadFollowPageParam.getFollowType())
                .orderByDesc(CrmLeadFollow::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CrmLeadFollowAddParam crmLeadFollowAddParam) {
        bizEventApi.log(BizEventCodeEnum.LEAD_FOLLOW.getValue(), "lead", crmLeadFollowAddParam.getLeadId(), null,
                Map.of("方式", String.valueOf(crmLeadFollowAddParam.getFollowType())));
        // 校验线索存在
        CrmLead crmLead = crmLeadService.queryEntity(crmLeadFollowAddParam.getLeadId());
        CrmLeadFollow follow = BeanUtil.toBean(crmLeadFollowAddParam, CrmLeadFollow.class);
        follow.setFollowUserId(StpUtil.getLoginIdAsString());
        this.save(follow);
        // 刷新线索：状态（默认已跟进）、下次跟进时间、跟进人
        crmLead.setFollowUserId(follow.getFollowUserId());
        if (ObjectUtil.isNotEmpty(crmLeadFollowAddParam.getNextFollowTime())) {
            crmLead.setNextFollowTime(crmLeadFollowAddParam.getNextFollowTime());
        }
        if (ObjectUtil.isNotEmpty(crmLeadFollowAddParam.getStatus())) {
            crmLead.setStatus(crmLeadFollowAddParam.getStatus());
        } else if (CrmLeadStatusEnum.NEW.getValue().equals(crmLead.getStatus())) {
            crmLead.setStatus(CrmLeadStatusEnum.FOLLOWED.getValue());
        }
        crmLeadService.updateLead(crmLead);
    }
}
