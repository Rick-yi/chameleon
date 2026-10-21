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
package vip.xiaonuo.student.core.automation;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.biz.api.automation.BizAutomationHandler;
import vip.xiaonuo.dev.api.DevMessageApi;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.sys.api.SysUserApi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化规则：未支付订单提醒
 * <p>
 * 报名开单后家长迟迟不付款，钱就悬在「待收款」里。这里主动提醒机构管理员去收款，
 * 避免靠人天天去翻收款台。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Slf4j
@Component
public class AutoNotifyUnpaidOrderHandler implements BizAutomationHandler {

    /** 规则编码 */
    public static final String RULE_CODE = "AUTO_NOTIFY_UNPAID_ORDER";
    /** 机构管理员角色码 */
    private static final String ADMIN_ROLE_CODE = "superAdmin";
    /** 待支付状态 */
    private static final String ORDER_STATUS_UNPAID = "UNPAID";

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private SysUserApi sysUserApi;

    @Resource
    private DevMessageApi devMessageApi;

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return "未支付订单提醒";
    }

    @Override
    public String getRuleDesc() {
        return "报名开单后长时间未付款的订单，自动提醒管理员去收款，不用天天翻收款台";
    }

    @Override
    public String getMode() {
        return "TIMER";
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public List<AutomationParam> getParamDef() {
        return List.of(
                new AutomationParam("overdueHours", "超时未付小时数", "NUMBER", "24",
                        "订单创建超过多少小时仍未支付才提醒"),
                new AutomationParam("repeatHours", "重复提醒间隔（小时）", "NUMBER", "24",
                        "同一笔订单提醒过一次后，至少间隔这么久才再次提醒")
        );
    }

    @Override
    public AutomationResult execute(AutomationContext context) {
        int overdueHours = context.getIntParam("overdueHours", 24);
        int repeatHours = context.getIntParam("repeatHours", 24);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime overdueBefore = now.minusHours(overdueHours);
        LocalDateTime repeatBefore = now.minusHours(repeatHours);

        List<StudentOrder> orderList = studentOrderMapper.selectList(new LambdaQueryWrapper<StudentOrder>()
                .eq(StudentOrder::getStatus, ORDER_STATUS_UNPAID)
                .lt(StudentOrder::getCreateTime, Date.from(overdueBefore.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .and(wrapper -> wrapper.isNull(StudentOrder::getLastNotifyTime)
                        .or().lt(StudentOrder::getLastNotifyTime, Date.from(repeatBefore.atZone(java.time.ZoneId.systemDefault()).toInstant()))));
        if (orderList.isEmpty()) {
            return AutomationResult.ok("没有需要提醒的未支付订单", 0);
        }
        List<String> receiverIdList = adminUserIdList();
        if (receiverIdList.isEmpty()) {
            return AutomationResult.ok("没有找到接收提醒的管理员", 0);
        }
        Map<String, String> studentNameMap = new HashMap<>();
        List<String> studentIdList = orderList.stream().map(StudentOrder::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        for (Student student : studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .in(Student::getId, studentIdList))) {
            studentNameMap.put(student.getId(), student.getName());
        }
        List<String> contentList = new ArrayList<>();
        for (StudentOrder order : orderList) {
            contentList.add("学员 " + StrUtil.blankToDefault(studentNameMap.get(order.getStudentId()), order.getStudentId())
                    + "，" + StrUtil.blankToDefault(order.getPackageName(), "课包")
                    + "，待收 ¥" + (order.getAmount() == null ? "0" : order.getAmount().toPlainString())
                    + "（开单 " + (order.getCreateTime() == null ? "-" : cn.hutool.core.date.DateUtil.formatDateTime(order.getCreateTime())) + "）");
        }
        devMessageApi.sendMessageWithContent(receiverIdList, "未支付订单提醒",
                "共 " + contentList.size() + " 笔订单超时未支付：\n" + String.join("\n", contentList));
        for (StudentOrder order : orderList) {
            StudentOrder update = new StudentOrder();
            update.setId(order.getId());
            update.setLastNotifyTime(new Date());
            studentOrderMapper.updateById(update);
        }
        return AutomationResult.ok("已提醒 " + contentList.size() + " 笔未支付订单", contentList.size());
    }

    /** 机构管理员用户id列表 */
    private List<String> adminUserIdList() {
        List<String> resultList = new ArrayList<>();
        List<JSONObject> userList = sysUserApi.listUserOptions(null);
        if (userList == null) {
            return resultList;
        }
        for (JSONObject user : userList) {
            String userId = user.getStr("id");
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            List<String> roleList = sysUserApi.ownRole(userId);
            if (roleList != null && roleList.contains(ADMIN_ROLE_CODE)) {
                resultList.add(userId);
            }
        }
        return resultList;
    }
}
