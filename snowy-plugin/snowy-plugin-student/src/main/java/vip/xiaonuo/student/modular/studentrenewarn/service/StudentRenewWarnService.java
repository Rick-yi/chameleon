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
package vip.xiaonuo.student.modular.studentrenewarn.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.xiaonuo.student.modular.studentrenewarn.dto.StudentRenewWarnRiskVO;
import vip.xiaonuo.student.modular.studentrenewarn.dto.StudentRenewWarnStatsVO;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentFollowRecord;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentFollowAddParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentFollowPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnAssignParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnNotifyParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnProcessParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnRiskPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnRiskTodoParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnStatsParam;

import java.util.List;

/**
 * 续费预警Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
public interface StudentRenewWarnService {

    /** 获取预警分页 */
    Page<StudentRenewWarn> page(StudentRenewWarnPageParam studentRenewWarnPageParam);

    /** 预警扫描（按生效阈值判断剩余课时/到期时间，重复间隔内去重） */
    void scan();

    /** 忽略预警 */
    void ignore(StudentRenewWarnIdParam studentRenewWarnIdParam);

    /** 标记已续费（支付入账后调用） */
    void markRenewed(String studentId, String courseId);

    /** 预警阈值配置列表 */
    List<StudentRenewWarnConfig> configList();

    /** 保存预警阈值配置 */
    void saveConfig(StudentRenewWarnConfigParam param);

    /** 删除预警阈值配置 */
    void deleteConfig(StudentRenewWarnConfigIdParam param);

    /** 发送续费提醒（仅落库通知记录，不接真实短信/微信） */
    void notifyWarn(StudentRenewWarnNotifyParam param);

    /** 转顾问（指派跟进人） */
    String assign(StudentRenewWarnAssignParam param);

    /** 标记已处理 */
    void process(StudentRenewWarnProcessParam param);

    /** 风险学员分页 */
    Page<StudentRenewWarnRiskVO> riskPage(StudentRenewWarnRiskPageParam param);

    /** 风险学员生成续费待办，返回待办（预警）id */
    String createRiskTodo(StudentRenewWarnRiskTodoParam param);

    /** 新增回访/跟进记录（含退学/结课流失原因） */
    String addFollow(StudentFollowAddParam param);

    /** 回访/跟进记录分页 */
    Page<StudentFollowRecord> followPage(StudentFollowPageParam param);

    /** 可指派顾问选项 */
    List<JSONObject> assigneeOptions(String searchKey);

    /** 续费率/流失率统计（轻量口径） */
    StudentRenewWarnStatsVO stats(StudentRenewWarnStatsParam param);
}
