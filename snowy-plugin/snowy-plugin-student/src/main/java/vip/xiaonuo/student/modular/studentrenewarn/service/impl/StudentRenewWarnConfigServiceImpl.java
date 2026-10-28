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
package vip.xiaonuo.student.modular.studentrenewarn.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.dev.api.DevConfigApi;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentrenewarn.dto.ResolvedWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;
import vip.xiaonuo.student.modular.studentrenewarn.mapper.StudentRenewWarnConfigMapper;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnConfigParam;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnConfigService;
import vip.xiaonuo.student.modular.studentrenewarn.util.RenewWarnTenantUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 续费预警阈值配置Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/26 10:00
 **/
@Slf4j
@Service
public class StudentRenewWarnConfigServiceImpl
        extends ServiceImpl<StudentRenewWarnConfigMapper, StudentRenewWarnConfig>
        implements StudentRenewWarnConfigService {

    /** 机构默认课时阈值配置键（兼容旧配置） */
    private static final String WARN_LESSON_THRESHOLD_KEY = "SNOWY_RENEW_WARN_LESSON_THRESHOLD";

    /** 机构默认到期提前天数配置键（兼容旧配置） */
    private static final String WARN_EXPIRE_DAYS_KEY = "SNOWY_RENEW_WARN_EXPIRE_DAYS";

    /** 系统默认重复提醒间隔（天） */
    private static final int DEFAULT_REPEAT_DAYS = 7;

    /** 系统默认课时阈值 */
    private static final BigDecimal SYSTEM_DEFAULT_THRESHOLD = BigDecimal.valueOf(3);

    /** 系统默认到期提前天数 */
    private static final int SYSTEM_DEFAULT_EXPIRE_DAYS = 7;

    @Resource
    private DevConfigApi devConfigApi;

    @Resource
    private CourseApi courseApi;

    @Override
    public List<StudentRenewWarnConfig> listWithDefault() {
        List<StudentRenewWarnConfig> configList = new ArrayList<>(this.list(new LambdaQueryWrapper<StudentRenewWarnConfig>()
                .eq(StudentRenewWarnConfig::getTenantId, RenewWarnTenantUtil.currentTenantId())
                .orderByAsc(StudentRenewWarnConfig::getCreateTime)));
        boolean hasDefault = configList.stream()
                .anyMatch(config -> StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(config.getScopeType()));
        if (!hasDefault) {
            configList.add(buildSystemDefaultRow());
        }
        configList.forEach(this::fillScopeName);
        configList.sort(Comparator
                .comparingInt((StudentRenewWarnConfig config) -> scopeOrder(config.getScopeType()))
                .thenComparing(config -> StrUtil.blankToDefault(config.getScopeName(), "")));
        return configList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveConfig(StudentRenewWarnConfigParam param) {
        String scopeType = StrUtil.blankToDefault(param.getScopeType(), "").trim().toUpperCase();
        if (!StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(scopeType)
                && !StudentRenewWarnStatusEnum.SCOPE_COURSE.equals(scopeType)
                && !StudentRenewWarnStatusEnum.SCOPE_CLASS.equals(scopeType)) {
            throw new CommonException("配置范围不合法，仅支持 DEFAULT/COURSE/CLASS");
        }
        String scopeId = StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(scopeType)
                ? "0" : StrUtil.trim(param.getScopeId());
        if (!StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(scopeType) && StrUtil.isBlank(scopeId)) {
            throw new CommonException("请选择课程或班级");
        }
        validateScopeExists(scopeType, scopeId);
        String tenantId = RenewWarnTenantUtil.currentTenantId();

        StudentRenewWarnConfig config = null;
        if (StrUtil.isNotBlank(param.getId())) {
            config = this.getById(param.getId());
            // 表已纳入租户拦截器，跨租户 id 查不到（返回空），此处显式报错避免静默新建
            if (ObjectUtil.isEmpty(config)) {
                throw new CommonException("预警配置不存在或无权修改，id值为：{}", param.getId());
            }
        }
        if (ObjectUtil.isEmpty(config)) {
            config = this.getOne(new LambdaQueryWrapper<StudentRenewWarnConfig>()
                    .eq(StudentRenewWarnConfig::getTenantId, tenantId)
                    .eq(StudentRenewWarnConfig::getScopeType, scopeType)
                    .eq(StudentRenewWarnConfig::getScopeId, scopeId)
                    .last("limit 1"));
        }
        if (ObjectUtil.isEmpty(config)) {
            config = new StudentRenewWarnConfig();
            config.setTenantId(tenantId);
        }
        config.setScopeType(scopeType);
        config.setScopeId(scopeId);
        config.setLessonThreshold(param.getLessonThreshold());
        config.setExpireDays(param.getExpireDays());
        config.setRepeatDays(param.getRepeatDays());
        config.setEnabled(StrUtil.blankToDefault(param.getEnabled(), "YES").toUpperCase());
        this.saveOrUpdate(config);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteConfig(StudentRenewWarnConfigIdParam param) {
        StudentRenewWarnConfig config = this.getById(param.getId());
        if (ObjectUtil.isEmpty(config)) {
            throw new CommonException("预警配置不存在，id值为：{}", param.getId());
        }
        if (!StrUtil.equals(RenewWarnTenantUtil.currentTenantId(), config.getTenantId())) {
            throw new CommonException("无权删除其他机构的预警配置");
        }
        this.removeById(config.getId());
    }

    @Override
    public List<StudentRenewWarnConfig> listEnabledForCurrentTenant() {
        return this.list(new LambdaQueryWrapper<StudentRenewWarnConfig>()
                .eq(StudentRenewWarnConfig::getTenantId, RenewWarnTenantUtil.currentTenantId())
                .eq(StudentRenewWarnConfig::getEnabled, "YES")
                .orderByAsc(StudentRenewWarnConfig::getCreateTime));
    }

    @Override
    public Map<String, StudentRenewWarnConfig> buildClassConfigMap(List<StudentRenewWarnConfig> configList) {
        Map<String, StudentRenewWarnConfig> classConfigMap = new HashMap<>();
        if (ObjectUtil.isEmpty(configList)) {
            return classConfigMap;
        }
        for (StudentRenewWarnConfig config : configList) {
            if (!StudentRenewWarnStatusEnum.SCOPE_CLASS.equals(config.getScopeType())
                    || !isEnabled(config) || StrUtil.isBlank(config.getScopeId())) {
                continue;
            }
            JSONObject classInfo;
            try {
                classInfo = courseApi.getCourseClassInfo(config.getScopeId());
            } catch (Exception e) {
                log.warn(">>> 读取班级[{}]配置失败：{}", config.getScopeId(), e.getMessage());
                continue;
            }
            if (ObjectUtil.isEmpty(classInfo) || StrUtil.isBlank(classInfo.getStr("courseId"))) {
                continue;
            }
            String courseId = classInfo.getStr("courseId");
            List<String> studentIdList;
            try {
                studentIdList = courseApi.getStudyingStudentIdList(config.getScopeId());
            } catch (Exception e) {
                log.warn(">>> 读取班级[{}]学员失败：{}", config.getScopeId(), e.getMessage());
                continue;
            }
            if (ObjectUtil.isEmpty(studentIdList)) {
                continue;
            }
            for (String studentId : studentIdList) {
                classConfigMap.putIfAbsent(studentId + "|" + courseId, config);
            }
        }
        return classConfigMap;
    }

    @Override
    public ResolvedWarnConfig resolve(StudentLessonAccount account, List<StudentRenewWarnConfig> configList,
                                      Map<String, StudentRenewWarnConfig> classConfigMap) {
        // 优先级：班级 > 课程 > 默认；都未命中时回落到系统参数
        StudentRenewWarnConfig selected = pickByPriority(account.getStudentId(), account.getCourseId(),
                configList, classConfigMap);
        ResolvedWarnConfig resolved = new ResolvedWarnConfig();
        if (ObjectUtil.isNotEmpty(selected)) {
            resolved.setLessonThreshold(ObjectUtil.defaultIfNull(selected.getLessonThreshold(),
                    SYSTEM_DEFAULT_THRESHOLD));
            resolved.setExpireDays(ObjectUtil.defaultIfNull(selected.getExpireDays(),
                    SYSTEM_DEFAULT_EXPIRE_DAYS));
            resolved.setRepeatDays(ObjectUtil.defaultIfNull(selected.getRepeatDays(), DEFAULT_REPEAT_DAYS));
            resolved.setSource(selected.getScopeType());
            resolved.setSourceId(selected.getScopeId());
            return resolved;
        }
        resolved.setLessonThreshold(Convert.toBigDecimal(
                devConfigApi.getValueByKey(WARN_LESSON_THRESHOLD_KEY), SYSTEM_DEFAULT_THRESHOLD));
        resolved.setExpireDays(Convert.toInt(devConfigApi.getValueByKey(WARN_EXPIRE_DAYS_KEY),
                SYSTEM_DEFAULT_EXPIRE_DAYS));
        resolved.setRepeatDays(DEFAULT_REPEAT_DAYS);
        resolved.setSource("SYSTEM");
        resolved.setSourceId("0");
        return resolved;
    }

    /**
     * 按「班级 > 课程 > 默认」优先级挑选生效配置
     * <p>
     * 抽成静态方法便于单测直接覆盖优先级规则：这是续费预警的核心口径，
     * 一旦顺序或启用判断被改错，会让整个机构的预警阈值静默失效。
     * </p>
     *
     * @param studentId      学员id
     * @param courseId       课程id
     * @param configList     当前租户启用中的配置
     * @param classConfigMap 班级配置映射（key 为 "学员id|课程id"）
     * @return 命中的配置，均未命中返回 null
     */
    static StudentRenewWarnConfig pickByPriority(String studentId, String courseId,
                                                List<StudentRenewWarnConfig> configList,
                                                Map<String, StudentRenewWarnConfig> classConfigMap) {
        // 1) 班级级：按学员+课程定位
        if (ObjectUtil.isNotEmpty(classConfigMap)) {
            StudentRenewWarnConfig classConfig = classConfigMap.get(studentId + "|" + courseId);
            if (ObjectUtil.isNotEmpty(classConfig) && isEnabled(classConfig)) {
                return classConfig;
            }
        }
        if (ObjectUtil.isEmpty(configList)) {
            return null;
        }
        // 2) 课程级
        for (StudentRenewWarnConfig config : configList) {
            if (StudentRenewWarnStatusEnum.SCOPE_COURSE.equals(config.getScopeType())
                    && StrUtil.equals(config.getScopeId(), courseId)
                    && isEnabled(config)) {
                return config;
            }
        }
        // 3) 默认级
        for (StudentRenewWarnConfig config : configList) {
            if (StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(config.getScopeType())
                    && isEnabled(config)) {
                return config;
            }
        }
        return null;
    }

    /**
     * 校验课程/班级是否存在
     */
    private void validateScopeExists(String scopeType, String scopeId) {
        if (StudentRenewWarnStatusEnum.SCOPE_COURSE.equals(scopeType)) {
            if (StrUtil.isBlank(courseApi.getCourseName(scopeId))) {
                throw new CommonException("课程不存在，id值为：{}", scopeId);
            }
        } else if (StudentRenewWarnStatusEnum.SCOPE_CLASS.equals(scopeType)) {
            if (ObjectUtil.isEmpty(courseApi.getCourseClassInfo(scopeId))) {
                throw new CommonException("班级不存在，id值为：{}", scopeId);
            }
        }
    }

    /**
     * 系统默认行（只读展示，保存后会落为机构默认配置）
     */
    private StudentRenewWarnConfig buildSystemDefaultRow() {
        StudentRenewWarnConfig config = new StudentRenewWarnConfig();
        config.setTenantId(RenewWarnTenantUtil.currentTenantId());
        config.setScopeType(StudentRenewWarnStatusEnum.SCOPE_DEFAULT);
        config.setScopeId("0");
        config.setLessonThreshold(Convert.toBigDecimal(
                devConfigApi.getValueByKey(WARN_LESSON_THRESHOLD_KEY), SYSTEM_DEFAULT_THRESHOLD));
        config.setExpireDays(Convert.toInt(devConfigApi.getValueByKey(WARN_EXPIRE_DAYS_KEY),
                SYSTEM_DEFAULT_EXPIRE_DAYS));
        config.setRepeatDays(DEFAULT_REPEAT_DAYS);
        config.setEnabled("YES");
        return config;
    }

    /**
     * 填充范围名称
     */
    private void fillScopeName(StudentRenewWarnConfig config) {
        if (StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(config.getScopeType())) {
            config.setScopeName(StrUtil.isBlank(config.getId()) ? "系统默认（沿用旧阈值）" : "机构默认");
            return;
        }
        if (StudentRenewWarnStatusEnum.SCOPE_COURSE.equals(config.getScopeType())) {
            String courseName = courseApi.getCourseName(config.getScopeId());
            config.setScopeName(StrUtil.blankToDefault(courseName, config.getScopeId()));
            return;
        }
        JSONObject classInfo = null;
        try {
            classInfo = courseApi.getCourseClassInfo(config.getScopeId());
        } catch (Exception e) {
            log.warn(">>> 读取班级[{}]名称失败：{}", config.getScopeId(), e.getMessage());
        }
        config.setScopeName(classInfo == null ? config.getScopeId() : classInfo.getStr("name"));
    }

    private static boolean isEnabled(StudentRenewWarnConfig config) {
        return !"NO".equalsIgnoreCase(config.getEnabled());
    }

    private int scopeOrder(String scopeType) {
        if (StudentRenewWarnStatusEnum.SCOPE_DEFAULT.equals(scopeType)) {
            return 0;
        }
        if (StudentRenewWarnStatusEnum.SCOPE_COURSE.equals(scopeType)) {
            return 1;
        }
        return 2;
    }
}
