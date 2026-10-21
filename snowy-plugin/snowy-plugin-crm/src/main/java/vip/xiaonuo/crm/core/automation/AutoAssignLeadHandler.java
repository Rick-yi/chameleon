/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.crm.core.automation;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.crm.modular.lead.entity.CrmLead;
import vip.xiaonuo.crm.modular.lead.enums.CrmLeadStatusEnum;
import vip.xiaonuo.crm.modular.lead.mapper.CrmLeadMapper;
import vip.xiaonuo.sys.api.SysUserApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化规则：线索自动分配
 * <p>
 * 新录入的线索若没有指定跟进人，系统按「谁手上的在跟线索最少」自动分配给招生顾问，
 * 避免线索躺在「无人认领」状态。规则为实时触发型：线索新增的瞬间生效。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Component
public class AutoAssignLeadHandler implements BizAutomationHandler {

    /** 规则编码 */
    public static final String RULE_CODE = "AUTO_ASSIGN_LEAD";
    /** 未结束的线索状态（在跟线索） */
    private static final List<String> ACTIVE_STATUS_LIST = List.of(
            CrmLeadStatusEnum.NEW.getValue(), CrmLeadStatusEnum.FOLLOWED.getValue(), CrmLeadStatusEnum.TRIALED.getValue());

    @Resource
    private CrmLeadMapper crmLeadMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return "线索自动分配";
    }

    @Override
    public String getRuleDesc() {
        return "新线索没有指定跟进人时，自动分配给当前在跟线索最少的招生顾问，不用再手工分人";
    }

    @Override
    public String getMode() {
        return "TRIGGER";
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public List<AutomationParam> getParamDef() {
        return List.of(
                new AutomationParam("excludeUserName", "排除的跟进人", "TEXT", "",
                        "填写后该用户名（多个用逗号分隔）不会被分配到线索")
        );
    }

    @Override
    public AutomationResult execute(AutomationContext context) {
        // 立即执行：把历史上没有跟进人的线索补分配
        List<CrmLead> noOwnerList = crmLeadMapper.selectList(new LambdaQueryWrapper<CrmLead>()
                .isNull(CrmLead::getFollowUserId)
                .in(CrmLead::getStatus, ACTIVE_STATUS_LIST)
                .orderByAsc(CrmLead::getCreateTime));
        int count = 0;
        for (CrmLead lead : noOwnerList) {
            if (assign(lead, context.getParam("excludeUserName", ""))) {
                count++;
            }
        }
        return AutomationResult.ok(count > 0 ? "已补分配 " + count + " 条线索" : "没有待分配的线索", count);
    }

    /**
     * 给线索分配跟进人（调用方需先判断规则是否开启）
     *
     * @param crmLead          线索
     * @param excludeUserName  排除的用户名（逗号分隔）
     * @return 是否分配成功
     */
    public boolean assign(CrmLead crmLead, String excludeUserName) {
        if (crmLead == null || StrUtil.isNotBlank(crmLead.getFollowUserId())) {
            return false;
        }
        String assignUserId = pickUserId(excludeUserName);
        if (StrUtil.isBlank(assignUserId)) {
            return false;
        }
        crmLead.setFollowUserId(assignUserId);
        crmLeadMapper.updateById(crmLead);
        return true;
    }

    /** 选出在跟线索最少的用户 */
    private String pickUserId(String excludeUserName) {
        List<JSONObject> userList = sysUserApi.listUserOptions(null);
        if (userList == null || userList.isEmpty()) {
            return null;
        }
        // 各用户在跟线索数量
        Map<String, Long> loadMap = new HashMap<>();
        for (CrmLead lead : crmLeadMapper.selectList(new LambdaQueryWrapper<CrmLead>()
                .isNotNull(CrmLead::getFollowUserId)
                .in(CrmLead::getStatus, ACTIVE_STATUS_LIST))) {
            loadMap.merge(lead.getFollowUserId(), 1L, Long::sum);
        }
        List<String> excludeNameList = new ArrayList<>();
        if (StrUtil.isNotBlank(excludeUserName)) {
            for (String name : excludeUserName.split(",")) {
                if (StrUtil.isNotBlank(name)) {
                    excludeNameList.add(name.trim());
                }
            }
        }
        String bestUserId = null;
        String bestUserName = null;
        long bestLoad = Long.MAX_VALUE;
        for (JSONObject user : userList) {
            String userId = user.getStr("id");
            String userName = user.getStr("name");
            if (StrUtil.isBlank(userId) || excludeNameList.contains(userName)) {
                continue;
            }
            long load = loadMap.getOrDefault(userId, 0L);
            // 负载相同时按用户 id 排序，保证结果稳定
            if (load < bestLoad || (load == bestLoad && bestUserId != null && userId.compareTo(bestUserId) < 0)) {
                bestLoad = load;
                bestUserId = userId;
                bestUserName = userName;
            }
        }
        log.info(">>> 线索自动分配：选择 {}（在跟 {} 条）", bestUserName, bestLoad == Long.MAX_VALUE ? 0 : bestLoad);
        return bestUserId;
    }
}
