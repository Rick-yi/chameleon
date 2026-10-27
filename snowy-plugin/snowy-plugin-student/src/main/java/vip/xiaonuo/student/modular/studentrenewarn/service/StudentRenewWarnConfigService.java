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

import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentrenewarn.dto.ResolvedWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigParam;

import java.util.List;
import java.util.Map;

/**
 * 续费预警阈值配置Service接口
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
public interface StudentRenewWarnConfigService {

    /**
     * 当前租户配置列表（无机构默认配置时返回系统默认的虚拟行，id为空）
     */
    List<StudentRenewWarnConfig> listWithDefault();

    /**
     * 保存配置（按 租户+范围+范围id 新增或更新）
     */
    void saveConfig(StudentRenewWarnConfigParam param);

    /**
     * 删除配置（逻辑删除，删除后回落到上一级配置）
     */
    void deleteConfig(StudentRenewWarnConfigIdParam param);

    /**
     * 当前租户启用中的配置（扫描时一次加载）
     */
    List<StudentRenewWarnConfig> listEnabledForCurrentTenant();

    /**
     * 构建班级配置映射：key = studentId + "|" + courseId
     */
    Map<String, StudentRenewWarnConfig> buildClassConfigMap(List<StudentRenewWarnConfig> configList);

    /**
     * 解析某个课时账户的生效配置（班级 &gt; 课程 &gt; 机构默认 &gt; 系统默认）
     */
    ResolvedWarnConfig resolve(StudentLessonAccount account, List<StudentRenewWarnConfig> configList,
                               Map<String, StudentRenewWarnConfig> classConfigMap);
}
