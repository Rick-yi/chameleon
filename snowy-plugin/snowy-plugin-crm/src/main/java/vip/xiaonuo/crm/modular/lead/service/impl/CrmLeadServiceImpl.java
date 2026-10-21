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
package vip.xiaonuo.crm.modular.lead.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadStatusEnum;
import vip.xiaonuo.crm.modular.lead.mapper.CrmLeadMapper;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadAddParam;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadEditParam;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadIdParam;
import vip.xiaonuo.crm.modular.lead.param.CrmLeadPageParam;
import vip.xiaonuo.crm.modular.lead.service.CrmLeadService;
import vip.xiaonuo.sys.api.SysUserApi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 招生线索Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CrmLeadServiceImpl extends ServiceImpl<CrmLeadMapper, CrmLead> implements CrmLeadService {

    @Resource
    private SysUserApi sysUserApi;

    @Override
    public Page<CrmLead> page(CrmLeadPageParam crmLeadPageParam) {
        QueryWrapper<CrmLead> queryWrapper = new QueryWrapper<CrmLead>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(crmLeadPageParam.getSearchKey())) {
            queryWrapper.lambda().and(q -> q.like(CrmLead::getName, crmLeadPageParam.getSearchKey())
                    .or().like(CrmLead::getParentPhone, crmLeadPageParam.getSearchKey()));
        }
        if (ObjectUtil.isNotEmpty(crmLeadPageParam.getStatus())) {
            queryWrapper.lambda().eq(CrmLead::getStatus, crmLeadPageParam.getStatus());
        }
        if (ObjectUtil.isNotEmpty(crmLeadPageParam.getSource())) {
            queryWrapper.lambda().eq(CrmLead::getSource, crmLeadPageParam.getSource());
        }
        // 待跟进筛选：下次跟进时间早于指定时间
        if (StrUtil.isNotBlank(crmLeadPageParam.getNextFollowBefore())) {
            queryWrapper.lambda().le(CrmLead::getNextFollowTime,
                    cn.hutool.core.date.DateUtil.parse(crmLeadPageParam.getNextFollowBefore()));
        }
        if (StrUtil.isNotBlank(crmLeadPageParam.getExcludeStatus())) {
            List<String> excludeStatusList = Arrays.stream(crmLeadPageParam.getExcludeStatus().split(","))
                    .map(String::trim).filter(StrUtil::isNotBlank).collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(excludeStatusList)) {
                queryWrapper.lambda().notIn(CrmLead::getStatus, excludeStatusList);
            }
        }
        if (ObjectUtil.isAllNotEmpty(crmLeadPageParam.getSortField(), crmLeadPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(crmLeadPageParam.getSortOrder());
            queryWrapper.orderBy(true, crmLeadPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(crmLeadPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(CrmLead::getCreateTime);
        }
        Page<CrmLead> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillFollowUserName(page.getRecords());
        return page;
    }

    /**
     * 批量回填跟进人姓名（列表展示用，避免暴露用户id）
     */
    private void fillFollowUserName(List<CrmLead> leadList) {
        if (ObjectUtil.isEmpty(leadList)) {
            return;
        }
        List<String> userIdList = leadList.stream().map(CrmLead::getFollowUserId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(userIdList)) {
            return;
        }
        Map<String, String> userNameMap = new HashMap<>();
        sysUserApi.getUserListByIdListWithoutException(userIdList)
                .forEach(user -> userNameMap.put(user.getStr("id"), user.getStr("name")));
        for (CrmLead lead : leadList) {
            lead.setFollowUserName(userNameMap.get(lead.getFollowUserId()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CrmLeadAddParam crmLeadAddParam) {
        CrmLead crmLead = BeanUtil.toBean(crmLeadAddParam, CrmLead.class);
        crmLead.setStatus(CrmLeadStatusEnum.NEW.getValue());
        this.save(crmLead);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CrmLeadEditParam crmLeadEditParam) {
        CrmLead crmLead = this.queryEntity(crmLeadEditParam.getId());
        BeanUtil.copyProperties(crmLeadEditParam, crmLead);
        this.updateById(crmLead);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CrmLeadIdParam> crmLeadIdParamList) {
        List<String> crmLeadIdList = CollStreamUtil.toList(crmLeadIdParamList, CrmLeadIdParam::getId);
        if (ObjectUtil.isNotEmpty(crmLeadIdList)) {
            this.removeByIds(crmLeadIdList);
        }
    }

    @Override
    public CrmLead detail(CrmLeadIdParam crmLeadIdParam) {
        return this.queryEntity(crmLeadIdParam.getId());
    }

    @Override
    public CrmLead queryEntity(String id) {
        CrmLead crmLead = this.getById(id);
        if (ObjectUtil.isEmpty(crmLead)) {
            throw new CommonException("线索不存在，id值为：{}", id);
        }
        return crmLead;
    }

    @Override
    public void updateLead(CrmLead crmLead) {
        this.updateById(crmLead);
    }
}
