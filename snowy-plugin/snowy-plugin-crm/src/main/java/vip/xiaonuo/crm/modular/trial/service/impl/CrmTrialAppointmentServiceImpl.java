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
package vip.xiaonuo.crm.modular.trial.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadStatusEnum;
import vip.xiaonuo.crm.modular.lead.mapper.CrmLeadMapper;
import vip.xiaonuo.crm.modular.lead.service.CrmLeadService;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.crm.modular.trial.entity.CrmTrialAppointment;
import vip.xiaonuo.crm.modular.trial.enums.CrmTrialStatusEnum;
import vip.xiaonuo.crm.modular.trial.mapper.CrmTrialAppointmentMapper;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentAddParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentIdParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentPageParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentUpdateParam;
import vip.xiaonuo.crm.modular.trial.service.CrmTrialAppointmentService;
import vip.xiaonuo.sys.api.SysUserApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 试听预约Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CrmTrialAppointmentServiceImpl extends ServiceImpl<CrmTrialAppointmentMapper, CrmTrialAppointment> implements CrmTrialAppointmentService {

    @Resource
    private CrmLeadService crmLeadService;

    @Resource
    private BizEventApi bizEventApi;

    @Resource
    private CrmLeadMapper crmLeadMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Override
    public Page<CrmTrialAppointment> page(CrmTrialAppointmentPageParam crmTrialAppointmentPageParam) {
        QueryWrapper<CrmTrialAppointment> queryWrapper = new QueryWrapper<CrmTrialAppointment>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(crmTrialAppointmentPageParam.getLeadId()), CrmTrialAppointment::getLeadId, crmTrialAppointmentPageParam.getLeadId())
                .eq(ObjectUtil.isNotEmpty(crmTrialAppointmentPageParam.getStatus()), CrmTrialAppointment::getStatus, crmTrialAppointmentPageParam.getStatus())
                .orderByDesc(CrmTrialAppointment::getTrialTime);
        Page<CrmTrialAppointment> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillLeadInfo(page.getRecords());
        return page;
    }

    /**
     * 批量回填学员姓名/手机/顾问姓名（试听列表展示用）
     */
    private void fillLeadInfo(List<CrmTrialAppointment> appointmentList) {
        if (ObjectUtil.isEmpty(appointmentList)) {
            return;
        }
        List<String> leadIdList = appointmentList.stream().map(CrmTrialAppointment::getLeadId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(leadIdList)) {
            return;
        }
        Map<String, CrmLead> leadMap = new HashMap<>();
        crmLeadMapper.selectBatchIds(leadIdList).forEach(lead -> leadMap.put(lead.getId(), lead));
        List<String> userIdList = leadMap.values().stream().map(CrmLead::getFollowUserId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, String> userNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(userIdList)) {
            for (JSONObject user : sysUserApi.getUserListByIdListWithoutException(userIdList)) {
                userNameMap.put(user.getStr("id"), user.getStr("name"));
            }
        }
        for (CrmTrialAppointment appointment : appointmentList) {
            CrmLead lead = leadMap.get(appointment.getLeadId());
            if (ObjectUtil.isEmpty(lead)) {
                continue;
            }
            appointment.setLeadName(lead.getName());
            appointment.setParentPhone(lead.getParentPhone());
            appointment.setFollowUserName(userNameMap.get(lead.getFollowUserId()));
            appointment.setLeadStatus(lead.getStatus());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CrmTrialAppointmentAddParam crmTrialAppointmentAddParam) {
        bizEventApi.log(BizEventCodeEnum.TRIAL_BOOK.getValue(), "lead", crmTrialAppointmentAddParam.getLeadId(), null, null);
        // 校验线索存在
        CrmLead crmLead = crmLeadService.queryEntity(crmTrialAppointmentAddParam.getLeadId());
        CrmTrialAppointment appointment = BeanUtil.toBean(crmTrialAppointmentAddParam, CrmTrialAppointment.class);
        appointment.setStatus(CrmTrialStatusEnum.PENDING);
        appointment.setResult(CrmTrialStatusEnum.RESULT_PENDING);
        this.save(appointment);
        // 线索置为已试听（预约即进入试听流程）
        if (!CrmLeadStatusEnum.ENROLLED.getValue().equals(crmLead.getStatus())) {
            crmLead.setStatus(CrmLeadStatusEnum.TRIALED.getValue());
            crmLeadService.updateLead(crmLead);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(CrmTrialAppointmentIdParam crmTrialAppointmentIdParam) {
        CrmTrialAppointment appointment = this.queryEntity(crmTrialAppointmentIdParam.getId());
        if (!CrmTrialStatusEnum.PENDING.equals(appointment.getStatus())) {
            throw new CommonException("仅待试听预约可取消");
        }
        appointment.setStatus(CrmTrialStatusEnum.CANCELED);
        this.updateById(appointment);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateResult(CrmTrialAppointmentUpdateParam crmTrialAppointmentUpdateParam) {
        CrmTrialAppointment appointment = this.queryEntity(crmTrialAppointmentUpdateParam.getId());
        if (StrUtil.isNotBlank(crmTrialAppointmentUpdateParam.getStatus())) {
            appointment.setStatus(crmTrialAppointmentUpdateParam.getStatus());
        }
        if (StrUtil.isNotBlank(crmTrialAppointmentUpdateParam.getResult())) {
            appointment.setResult(crmTrialAppointmentUpdateParam.getResult());
        }
        if (crmTrialAppointmentUpdateParam.getRemark() != null) {
            appointment.setRemark(crmTrialAppointmentUpdateParam.getRemark());
        }
        this.updateById(appointment);
    }

    @Override
    public CrmTrialAppointment queryEntity(String id) {
        CrmTrialAppointment appointment = this.getById(id);
        if (ObjectUtil.isEmpty(appointment)) {
            throw new CommonException("试听预约不存在，id值为：{}", id);
        }
        return appointment;
    }
}
