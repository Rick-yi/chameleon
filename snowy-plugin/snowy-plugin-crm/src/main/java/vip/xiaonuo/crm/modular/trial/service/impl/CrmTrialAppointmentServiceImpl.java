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
import vip.xiaonuo.crm.modular.lead.service.CrmLeadService;
import vip.xiaonuo.crm.modular.trial.entity.CrmTrialAppointment;
import vip.xiaonuo.crm.modular.trial.enums.CrmTrialStatusEnum;
import vip.xiaonuo.crm.modular.trial.mapper.CrmTrialAppointmentMapper;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentAddParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentIdParam;
import vip.xiaonuo.crm.modular.trial.param.CrmTrialAppointmentPageParam;
import vip.xiaonuo.crm.modular.trial.service.CrmTrialAppointmentService;

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

    @Override
    public Page<CrmTrialAppointment> page(CrmTrialAppointmentPageParam crmTrialAppointmentPageParam) {
        QueryWrapper<CrmTrialAppointment> queryWrapper = new QueryWrapper<CrmTrialAppointment>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(crmTrialAppointmentPageParam.getLeadId()), CrmTrialAppointment::getLeadId, crmTrialAppointmentPageParam.getLeadId())
                .eq(ObjectUtil.isNotEmpty(crmTrialAppointmentPageParam.getStatus()), CrmTrialAppointment::getStatus, crmTrialAppointmentPageParam.getStatus())
                .orderByDesc(CrmTrialAppointment::getTrialTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CrmTrialAppointmentAddParam crmTrialAppointmentAddParam) {
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

    @Override
    public CrmTrialAppointment queryEntity(String id) {
        CrmTrialAppointment appointment = this.getById(id);
        if (ObjectUtil.isEmpty(appointment)) {
            throw new CommonException("试听预约不存在，id值为：{}", id);
        }
        return appointment;
    }
}
