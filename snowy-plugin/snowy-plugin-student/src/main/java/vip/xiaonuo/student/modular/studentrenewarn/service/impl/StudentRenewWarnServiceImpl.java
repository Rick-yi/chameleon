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

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.api.StudentReportApi;
import vip.xiaonuo.student.api.dto.AcademicRiskStudentDTO;
import vip.xiaonuo.student.api.param.AcademicReportQueryParam;
import vip.xiaonuo.student.modular.notify.service.NotifyLogService;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.studentrenewarn.dto.ResolvedWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.dto.StudentRenewWarnRiskVO;
import vip.xiaonuo.student.modular.studentrenewarn.dto.StudentRenewWarnStatsVO;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentFollowRecord;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarnConfig;
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;
import vip.xiaonuo.student.modular.studentrenewarn.mapper.StudentFollowRecordMapper;
import vip.xiaonuo.student.modular.studentrenewarn.mapper.StudentRenewWarnMapper;
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
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnConfigService;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnService;
import vip.xiaonuo.student.modular.studentrenewarn.util.RenewWarnTenantUtil;
import vip.xiaonuo.sys.api.SysUserApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 续费预警Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class StudentRenewWarnServiceImpl extends ServiceImpl<StudentRenewWarnMapper, StudentRenewWarn>
        implements StudentRenewWarnService {

    /** 默认返回条数 */
    private static final int DEFAULT_RISK_LIMIT = 300;

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    @Resource
    private StudentLessonAccountMapper studentLessonAccountMapper;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private CourseApi courseApi;

    @Resource
    private NotifyLogService notifyLogService;

    @Resource
    private StudentRenewWarnConfigService studentRenewWarnConfigService;

    @Resource
    private StudentFollowRecordMapper studentFollowRecordMapper;

    @Resource
    private StudentReportApi studentReportApi;

    @Resource
    private SysUserApi sysUserApi;

    @Override
    public Page<StudentRenewWarn> page(StudentRenewWarnPageParam studentRenewWarnPageParam) {
        QueryWrapper<StudentRenewWarn> queryWrapper = new QueryWrapper<StudentRenewWarn>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentRenewWarnPageParam.getStudentId()), StudentRenewWarn::getStudentId, studentRenewWarnPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentRenewWarnPageParam.getCourseId()), StudentRenewWarn::getCourseId, studentRenewWarnPageParam.getCourseId())
                .eq(ObjectUtil.isNotEmpty(studentRenewWarnPageParam.getStatus()), StudentRenewWarn::getStatus, studentRenewWarnPageParam.getStatus());
        // 关键词：按学员姓名/家长手机过滤
        if (ObjectUtil.isNotEmpty(studentRenewWarnPageParam.getSearchKey())) {
            queryWrapper.apply("STUDENT_ID IN (SELECT ID FROM STUDENT WHERE NAME LIKE CONCAT('%', {0}, '%') "
                    + "OR PARENT_PHONE LIKE CONCAT('%', {0}, '%'))", studentRenewWarnPageParam.getSearchKey());
        }
        if (ObjectUtil.isAllNotEmpty(studentRenewWarnPageParam.getSortField(), studentRenewWarnPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(studentRenewWarnPageParam.getSortOrder());
            queryWrapper.orderBy(true, studentRenewWarnPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(studentRenewWarnPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(StudentRenewWarn::getCreateTime);
        }
        Page<StudentRenewWarn> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillWarnDetailInfo(page.getRecords());
        return page;
    }

    @Override
    public void scan() {
        List<StudentRenewWarnConfig> configList = studentRenewWarnConfigService.listEnabledForCurrentTenant();
        Map<String, StudentRenewWarnConfig> classConfigMap = studentRenewWarnConfigService.buildClassConfigMap(configList);
        DateTime now = DateUtil.date();
        // 扫描当前租户全部正常账户
        List<StudentLessonAccount> accountList = studentLessonAccountService.listNormalAccounts();
        for (StudentLessonAccount account : accountList) {
            ResolvedWarnConfig config = studentRenewWarnConfigService.resolve(account, configList, classConfigMap);
            String warnReason = null;
            BigDecimal lessonThreshold = ObjectUtil.defaultIfNull(config.getLessonThreshold(), BigDecimal.ZERO);
            int expireDays = ObjectUtil.defaultIfNull(config.getExpireDays(), 7);
            DateTime expireDeadline = DateUtil.offsetDay(now, expireDays);
            if (account.getRemainLessons() != null
                    && account.getRemainLessons().compareTo(lessonThreshold) <= 0) {
                warnReason = StudentRenewWarnStatusEnum.REASON_LESSON_LOW;
            } else if (account.getExpireTime() != null && account.getExpireTime().before(expireDeadline)
                    && account.getExpireTime().after(now)) {
                warnReason = StudentRenewWarnStatusEnum.REASON_EXPIRE_SOON;
            }
            if (warnReason == null) {
                continue;
            }
            // 重复间隔去重：待处理预警不重复生成；已处理/已忽略/已续费在间隔内不重复提醒
            int repeatDays = ObjectUtil.defaultIfNull(config.getRepeatDays(), 7);
            StudentRenewWarn latest = this.getOne(new LambdaQueryWrapper<StudentRenewWarn>()
                    .eq(StudentRenewWarn::getStudentId, account.getStudentId())
                    .eq(StudentRenewWarn::getCourseId, account.getCourseId())
                    .orderByDesc(StudentRenewWarn::getCreateTime)
                    .last("limit 1"));
            if (ObjectUtil.isNotEmpty(latest)) {
                if (StudentRenewWarnStatusEnum.PENDING.equals(latest.getStatus())) {
                    continue;
                }
                if (latest.getCreateTime() != null
                        && latest.getCreateTime().after(DateUtil.offsetDay(now, -repeatDays))) {
                    continue;
                }
            }
            StudentRenewWarn warn = new StudentRenewWarn();
            warn.setStudentId(account.getStudentId());
            warn.setCourseId(account.getCourseId());
            warn.setRemainLessons(account.getRemainLessons());
            warn.setWarnReason(warnReason);
            warn.setStatus(StudentRenewWarnStatusEnum.PENDING);
            warn.setSnapshotTime(now);
            warn.setLastNotifyTime(now);
            this.save(warn);
            log.info(">>> 续费预警生成：学员{} 课程{} 剩余{} 原因{} 阈值来源{}",
                    warn.getStudentId(), warn.getCourseId(), warn.getRemainLessons(), warnReason, config.getSource());
            // 机构端待办 + 家长通知落库（v1 站内记录，M4 接订阅消息/短信）
            try {
                notifyLogService.log("RENEW_WARN", "STAFF", null, warn.getStudentId(),
                        "续费预警", "学员课程剩余" + nvl(warn.getRemainLessons()).stripTrailingZeros().toPlainString()
                                + "课时，请及时跟进续费", "IN_APP");
                notifyLogService.log("RENEW_WARN", "PARENT", warn.getStudentId(), warn.getStudentId(),
                        "续费提醒", "课程剩余" + nvl(warn.getRemainLessons()).stripTrailingZeros().toPlainString()
                                + "课时，建议尽快续费", "IN_APP");
            } catch (Exception e) {
                log.warn(">>> 续费预警通知落库失败：学员{}", warn.getStudentId(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void ignore(StudentRenewWarnIdParam studentRenewWarnIdParam) {
        StudentRenewWarn warn = this.queryEntity(studentRenewWarnIdParam.getId());
        if (StudentRenewWarnStatusEnum.RENEWED.equals(warn.getStatus())) {
            throw new CommonException("该预警已续费，无需忽略");
        }
        warn.setStatus(StudentRenewWarnStatusEnum.IGNORED);
        warn.setProcessTime(DateUtil.date());
        this.updateById(warn);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void process(StudentRenewWarnProcessParam param) {
        StudentRenewWarn warn = this.queryEntity(param.getId());
        if (StudentRenewWarnStatusEnum.RENEWED.equals(warn.getStatus())) {
            throw new CommonException("该预警已续费，无需标记处理");
        }
        warn.setStatus(StudentRenewWarnStatusEnum.PROCESSED);
        warn.setProcessTime(DateUtil.date());
        warn.setProcessRemark(param.getRemark());
        this.updateById(warn);
    }

    @Override
    public void markRenewed(String studentId, String courseId) {
        this.update(new LambdaUpdateWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStudentId, studentId)
                .eq(StudentRenewWarn::getCourseId, courseId)
                .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING)
                .set(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.RENEWED)
                .set(StudentRenewWarn::getProcessTime, DateUtil.date()));
    }

    @Override
    public List<StudentRenewWarnConfig> configList() {
        return studentRenewWarnConfigService.listWithDefault();
    }

    @Override
    public void saveConfig(StudentRenewWarnConfigParam param) {
        studentRenewWarnConfigService.saveConfig(param);
    }

    @Override
    public void deleteConfig(StudentRenewWarnConfigIdParam param) {
        studentRenewWarnConfigService.deleteConfig(param);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notifyWarn(StudentRenewWarnNotifyParam param) {
        StudentRenewWarn warn = findWarn(param.getId(), param.getStudentId(), param.getCourseId());
        String studentId = StrUtil.isNotBlank(param.getStudentId()) ? param.getStudentId()
                : (warn != null ? warn.getStudentId() : null);
        if (StrUtil.isBlank(studentId)) {
            throw new CommonException("学员id不能为空");
        }
        String channel = StrUtil.blankToDefault(param.getChannel(), "IN_APP");
        if (warn != null && warn.getCurrentRemainLessons() == null && StrUtil.isNotBlank(warn.getCourseId())) {
            StudentLessonAccount account = studentLessonAccountMapper.selectOne(
                    new LambdaQueryWrapper<StudentLessonAccount>()
                            .eq(StudentLessonAccount::getStudentId, warn.getStudentId())
                            .eq(StudentLessonAccount::getCourseId, warn.getCourseId())
                            .last("limit 1"));
            if (account != null) {
                warn.setCurrentRemainLessons(account.getRemainLessons());
            }
        }
        String content = StrUtil.isNotBlank(param.getContent()) ? param.getContent()
                : buildNotifyContent(warn, studentId, param.getCourseId());
        notifyLogService.log("RENEW_WARN", "PARENT", studentId, studentId, "续费提醒", content, channel);
        notifyLogService.log("RENEW_WARN", "STAFF", null, studentId, "续费预警",
                "请跟进续费：" + content, channel);
        if (warn != null) {
            StudentRenewWarn update = new StudentRenewWarn();
            update.setId(warn.getId());
            update.setLastNotifyTime(DateUtil.date());
            this.updateById(update);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String assign(StudentRenewWarnAssignParam param) {
        if (StrUtil.isBlank(param.getFollowUserId())) {
            throw new CommonException("请选择跟进顾问");
        }
        JSONObject user = sysUserApi.getUserByIdWithoutException(param.getFollowUserId());
        if (ObjectUtil.isEmpty(user)) {
            throw new CommonException("跟进顾问不存在，id值为：{}", param.getFollowUserId());
        }
        StudentRenewWarn warn = findWarn(param.getId(), param.getStudentId(), param.getCourseId());
        if (warn == null) {
            if (StrUtil.isBlank(param.getStudentId())) {
                throw new CommonException("学员id不能为空");
            }
            warn = createRiskTodoEntity(param.getStudentId(), param.getCourseId(), param.getRiskType(), null);
        }
        warn.setFollowUserId(param.getFollowUserId());
        warn.setFollowTime(DateUtil.date());
        this.updateById(warn);
        String studentName = loadStudentName(warn.getStudentId());
        String remark = StrUtil.isBlank(param.getRemark()) ? "" : "，备注：" + param.getRemark();
        notifyLogService.log("RENEW_WARN", "STAFF", param.getFollowUserId(), warn.getStudentId(),
                "续费跟进指派", "学员" + studentName + "的续费跟进已指派给你，请及时联系家长" + remark, "IN_APP");
        return warn.getId();
    }

    @Override
    public Page<StudentRenewWarnRiskVO> riskPage(StudentRenewWarnRiskPageParam param) {
        AcademicReportQueryParam reportParam = new AcademicReportQueryParam();
        reportParam.setType(StrUtil.blankToDefault(param.getType(), "LOW_LESSON"));
        reportParam.setCampusId(param.getCampusId());
        reportParam.setCourseId(param.getCourseId());
        reportParam.setClassId(param.getClassId());
        int limit = ObjectUtil.defaultIfNull(param.getLimit(), DEFAULT_RISK_LIMIT);
        reportParam.setLimit(Math.max(1, Math.min(limit, 1000)));
        List<AcademicRiskStudentDTO> riskList = studentReportApi.riskStudents(reportParam);
        Page<StudentRenewWarnRiskVO> page = new Page<>(ObjectUtil.defaultIfNull(param.getCurrent(), 1),
                ObjectUtil.defaultIfNull(param.getSize(), 20));
        if (ObjectUtil.isEmpty(riskList)) {
            page.setRecords(new ArrayList<>());
            page.setTotal(0);
            return page;
        }
        List<String> studentIdList = riskList.stream().map(AcademicRiskStudentDTO::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, Student> studentMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            studentMapper.selectBatchIds(studentIdList).forEach(student -> studentMap.put(student.getId(), student));
        }
        Map<String, String> classNamesMap = courseApi.listStudentClassNameMap(studentIdList);
        Set<String> pendingStudentIds = new HashSet<>();
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            for (StudentRenewWarn warn : this.list(new LambdaQueryWrapper<StudentRenewWarn>()
                    .in(StudentRenewWarn::getStudentId, studentIdList)
                    .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING))) {
                pendingStudentIds.add(warn.getStudentId());
            }
        }
        Map<String, StudentFollowRecord> latestFollowMap = loadLatestFollowByStudent(studentIdList);
        Map<String, String> userNameMap = loadUserNameMap(latestFollowMap.values().stream()
                .map(StudentFollowRecord::getFollowUserId).collect(Collectors.toList()));
        List<StudentRenewWarnRiskVO> voList = new ArrayList<>();
        for (AcademicRiskStudentDTO dto : riskList) {
            StudentRenewWarnRiskVO vo = new StudentRenewWarnRiskVO();
            vo.setStudentId(dto.getStudentId());
            vo.setStudentName(dto.getStudentName());
            vo.setCampusId(dto.getCampusId());
            vo.setCourseId(dto.getCourseId());
            vo.setCourseName(dto.getCourseName());
            vo.setRemainLessons(dto.getRemainLessons());
            vo.setExpireTime(dto.getExpireTime());
            vo.setLastAttendTime(dto.getLastAttendTime());
            vo.setDaysWithoutAttend(dto.getDaysWithoutAttend());
            vo.setRiskType(dto.getRiskType());
            vo.setRiskDesc(dto.getRiskDesc());
            Student student = studentMap.get(dto.getStudentId());
            if (student != null) {
                vo.setParentPhone(student.getParentPhone());
                if (StrUtil.isBlank(vo.getCampusId())) {
                    vo.setCampusId(student.getCampusId());
                }
            }
            vo.setClassNames(classNamesMap.get(dto.getStudentId()));
            vo.setHasPendingWarn(pendingStudentIds.contains(dto.getStudentId()));
            StudentFollowRecord latestFollow = latestFollowMap.get(dto.getStudentId());
            if (latestFollow != null) {
                vo.setLastFollowContent(latestFollow.getContent());
                vo.setLastFollowTime(latestFollow.getCreateTime());
                vo.setFollowUserName(userNameMap.get(latestFollow.getFollowUserId()));
            }
            voList.add(vo);
        }
        // 课时不足按剩余升序；长期未消课按未到课天数降序；停课按姓名
        String riskType = reportParam.getType();
        if ("LOW_LESSON".equalsIgnoreCase(riskType)) {
            voList.sort((a, b) -> nvl(a.getRemainLessons()).compareTo(nvl(b.getRemainLessons())));
        } else if ("NO_ATTEND".equalsIgnoreCase(riskType)) {
            voList.sort((a, b) -> Long.compare(ObjectUtil.defaultIfNull(b.getDaysWithoutAttend(), 0L),
                    ObjectUtil.defaultIfNull(a.getDaysWithoutAttend(), 0L)));
        } else {
            voList.sort((a, b) -> StrUtil.compare(StrUtil.blankToDefault(a.getStudentName(), ""),
                    StrUtil.blankToDefault(b.getStudentName(), ""), true));
        }
        long total = voList.size();
        int current = (int) page.getCurrent();
        int size = (int) page.getSize();
        int fromIndex = Math.min((current - 1) * size, voList.size());
        int toIndex = Math.min(fromIndex + size, voList.size());
        page.setRecords(new ArrayList<>(voList.subList(fromIndex, toIndex)));
        page.setTotal(total);
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String createRiskTodo(StudentRenewWarnRiskTodoParam param) {
        StudentRenewWarn existing = this.getOne(new LambdaQueryWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStudentId, param.getStudentId())
                .eq(StrUtil.isNotBlank(param.getCourseId()), StudentRenewWarn::getCourseId, param.getCourseId())
                .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING)
                .orderByDesc(StudentRenewWarn::getCreateTime)
                .last("limit 1"));
        if (ObjectUtil.isNotEmpty(existing)) {
            return existing.getId();
        }
        StudentRenewWarn warn = createRiskTodoEntity(param.getStudentId(), param.getCourseId(),
                param.getRiskType(), param.getRemainLessons());
        notifyLogService.log("RENEW_WARN", "STAFF", null, warn.getStudentId(), "续费待办",
                "风险学员" + loadStudentName(warn.getStudentId()) + "已生成续费待办，请及时跟进", "IN_APP");
        return warn.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String addFollow(StudentFollowAddParam param) {
        Student student = studentMapper.selectById(param.getStudentId());
        if (ObjectUtil.isEmpty(student)) {
            throw new CommonException("学员不存在，id值为：{}", param.getStudentId());
        }
        StudentRenewWarn warn = null;
        if (StrUtil.isNotBlank(param.getWarnId())) {
            warn = this.getById(param.getWarnId());
            if (ObjectUtil.isEmpty(warn)) {
                throw new CommonException("续费预警不存在，id值为：{}", param.getWarnId());
            }
            if (!StrUtil.equals(warn.getStudentId(), param.getStudentId())) {
                throw new CommonException("跟进学员与预警学员不一致");
            }
        }
        String bizType = StrUtil.blankToDefault(param.getBizType(), "");
        if (StrUtil.isBlank(bizType)) {
            if (StrUtil.isNotBlank(param.getLossType())) {
                bizType = StudentRenewWarnStatusEnum.FOLLOW_BIZ_LOSS;
            } else if (warn != null) {
                bizType = StudentRenewWarnStatusEnum.FOLLOW_BIZ_RENEW_WARN;
            } else {
                bizType = StudentRenewWarnStatusEnum.FOLLOW_BIZ_RISK;
            }
        }
        StudentFollowRecord record = new StudentFollowRecord();
        record.setTenantId(RenewWarnTenantUtil.currentTenantId());
        record.setStudentId(param.getStudentId());
        record.setWarnId(param.getWarnId());
        record.setBizType(bizType);
        record.setFollowType(StrUtil.blankToDefault(param.getFollowType(), "PHONE"));
        record.setContent(param.getContent());
        record.setNextFollowTime(param.getNextFollowTime());
        record.setFollowUserId(currentUserId());
        record.setLossType(param.getLossType());
        record.setLossReason(param.getLossReason());
        studentFollowRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public Page<StudentFollowRecord> followPage(StudentFollowPageParam param) {
        LambdaQueryWrapper<StudentFollowRecord> wrapper = new LambdaQueryWrapper<StudentFollowRecord>()
                .eq(StudentFollowRecord::getTenantId, RenewWarnTenantUtil.currentTenantId())
                .eq(StrUtil.isNotBlank(param.getStudentId()), StudentFollowRecord::getStudentId, param.getStudentId())
                .eq(StrUtil.isNotBlank(param.getWarnId()), StudentFollowRecord::getWarnId, param.getWarnId())
                .eq(StrUtil.isNotBlank(param.getBizType()), StudentFollowRecord::getBizType, param.getBizType())
                .orderByDesc(StudentFollowRecord::getCreateTime);
        Page<StudentFollowRecord> page = studentFollowRecordMapper.selectPage(CommonPageRequest.defaultPage(), wrapper);
        List<StudentFollowRecord> records = page.getRecords();
        if (ObjectUtil.isNotEmpty(records)) {
            List<String> studentIdList = records.stream().map(StudentFollowRecord::getStudentId)
                    .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
            Map<String, String> studentNameMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(studentIdList)) {
                studentMapper.selectBatchIds(studentIdList)
                        .forEach(student -> studentNameMap.put(student.getId(), student.getName()));
            }
            Map<String, String> userNameMap = loadUserNameMap(records.stream()
                    .map(StudentFollowRecord::getFollowUserId).collect(Collectors.toList()));
            for (StudentFollowRecord record : records) {
                record.setStudentName(studentNameMap.get(record.getStudentId()));
                record.setFollowUserName(userNameMap.get(record.getFollowUserId()));
            }
        }
        return page;
    }

    @Override
    public List<JSONObject> assigneeOptions(String searchKey) {
        List<JSONObject> userList = sysUserApi.listUserOptions(searchKey);
        if (ObjectUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        List<JSONObject> advisorList = new ArrayList<>();
        for (JSONObject user : userList) {
            String userId = user.getStr("id");
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            List<String> roleCodeList;
            try {
                roleCodeList = sysUserApi.ownRoleCode(userId);
            } catch (Exception e) {
                roleCodeList = new ArrayList<>();
            }
            if (roleCodeList != null && roleCodeList.contains("sales")) {
                advisorList.add(JSONUtil.createObj().set("id", userId).set("name", user.getStr("name")));
            }
        }
        if (ObjectUtil.isNotEmpty(advisorList)) {
            return advisorList;
        }
        // 没有招生顾问角色时退回全部启用用户，避免无法指派
        List<JSONObject> allUserList = new ArrayList<>();
        for (JSONObject user : userList) {
            if (StrUtil.isNotBlank(user.getStr("id"))) {
                allUserList.add(JSONUtil.createObj().set("id", user.getStr("id")).set("name", user.getStr("name")));
            }
        }
        return allUserList;
    }

    @Override
    public StudentRenewWarnStatsVO stats(StudentRenewWarnStatsParam param) {
        Date end = StrUtil.isBlank(param.getEndTime()) ? new Date() : DateUtil.parse(param.getEndTime());
        Date start = StrUtil.isBlank(param.getStartTime()) ? DateUtil.offsetDay(end, -90) : DateUtil.parse(param.getStartTime());
        StudentRenewWarnStatsVO vo = new StudentRenewWarnStatsVO();
        vo.setStartTime(DateUtil.formatDateTime(start));
        vo.setEndTime(DateUtil.formatDateTime(end));
        List<StudentRenewWarn> warnList = this.list(new LambdaQueryWrapper<StudentRenewWarn>()
                .ge(StudentRenewWarn::getCreateTime, start)
                .le(StudentRenewWarn::getCreateTime, end));
        Set<String> warnStudentIds = new HashSet<>();
        for (StudentRenewWarn warn : warnList) {
            if (StrUtil.isNotBlank(warn.getStudentId())) {
                warnStudentIds.add(warn.getStudentId());
            }
            if (StudentRenewWarnStatusEnum.PENDING.equals(warn.getStatus())) {
                vo.setPendingCount(vo.getPendingCount() + 1);
            } else if (StudentRenewWarnStatusEnum.PROCESSED.equals(warn.getStatus())) {
                vo.setProcessedCount(vo.getProcessedCount() + 1);
            } else if (StudentRenewWarnStatusEnum.RENEWED.equals(warn.getStatus())) {
                vo.setRenewedCount(vo.getRenewedCount() + 1);
            } else if (StudentRenewWarnStatusEnum.IGNORED.equals(warn.getStatus())) {
                vo.setIgnoredCount(vo.getIgnoredCount() + 1);
            }
        }
        vo.setWarnTotal((long) warnList.size());
        vo.setWarnStudentCount((long) warnStudentIds.size());
        vo.setRenewedRate(percent(vo.getRenewedCount(), vo.getWarnTotal()));

        List<StudentFollowRecord> lossList = studentFollowRecordMapper.selectList(
                new LambdaQueryWrapper<StudentFollowRecord>()
                        .eq(StudentFollowRecord::getTenantId, RenewWarnTenantUtil.currentTenantId())
                        .eq(StudentFollowRecord::getBizType, StudentRenewWarnStatusEnum.FOLLOW_BIZ_LOSS)
                        .ge(StudentFollowRecord::getCreateTime, start)
                        .le(StudentFollowRecord::getCreateTime, end));
        Set<String> lossStudentIds = new HashSet<>();
        for (StudentFollowRecord loss : lossList) {
            if (StrUtil.isNotBlank(loss.getStudentId())) {
                lossStudentIds.add(loss.getStudentId());
            }
        }
        vo.setLossCount((long) lossList.size());
        vo.setLossStudentCount((long) lossStudentIds.size());
        long warnedLossStudentCount = lossStudentIds.stream().filter(warnStudentIds::contains).count();
        vo.setChurnRate(percent(warnedLossStudentCount, vo.getWarnStudentCount()));
        return vo;
    }

    /**
     * 批量回填列表展示信息：姓名、当前课时、班级、跟进人、最近跟进
     */
    private void fillWarnDetailInfo(List<StudentRenewWarn> warnList) {
        if (ObjectUtil.isEmpty(warnList)) {
            return;
        }
        List<String> studentIdList = warnList.stream().map(StudentRenewWarn::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        List<String> courseIdList = warnList.stream().map(StudentRenewWarn::getCourseId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, Student> studentMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            studentMapper.selectBatchIds(studentIdList).forEach(student -> studentMap.put(student.getId(), student));
        }
        Map<String, String> courseNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(courseIdList)) {
            courseApi.listCourseNameMap(courseIdList).forEach(courseNameMap::put);
        }
        Map<String, String> classNamesMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            classNamesMap.putAll(courseApi.listStudentClassNameMap(studentIdList));
        }
        // 当前课时账户：列表按学员+课程批量取，避免逐条查询
        Map<String, StudentLessonAccount> accountMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(studentIdList) && ObjectUtil.isNotEmpty(courseIdList)) {
            for (StudentLessonAccount account : studentLessonAccountMapper.selectList(
                    new LambdaQueryWrapper<StudentLessonAccount>()
                            .in(StudentLessonAccount::getStudentId, studentIdList)
                            .in(StudentLessonAccount::getCourseId, courseIdList))) {
                accountMap.put(account.getStudentId() + "|" + account.getCourseId(), account);
            }
        }
        // 每条预警最近一次跟进
        List<String> warnIdList = warnList.stream().map(StudentRenewWarn::getId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, StudentFollowRecord> latestFollowMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(warnIdList)) {
            List<StudentFollowRecord> followList = studentFollowRecordMapper.selectList(
                    new LambdaQueryWrapper<StudentFollowRecord>()
                            .eq(StudentFollowRecord::getTenantId, RenewWarnTenantUtil.currentTenantId())
                            .in(StudentFollowRecord::getWarnId, warnIdList)
                            .orderByDesc(StudentFollowRecord::getCreateTime));
            for (StudentFollowRecord follow : followList) {
                latestFollowMap.putIfAbsent(follow.getWarnId(), follow);
            }
        }
        // 风险学员跟进没有 warnId，按学员兜底展示最近一次跟进
        Map<String, StudentFollowRecord> studentFollowMap = loadLatestFollowByStudent(studentIdList);
        Set<String> userIdSet = new HashSet<>();
        warnList.forEach(warn -> {
            if (StrUtil.isNotBlank(warn.getFollowUserId())) {
                userIdSet.add(warn.getFollowUserId());
            }
            if (StrUtil.isNotBlank(warn.getUpdateUser())) {
                userIdSet.add(warn.getUpdateUser());
            }
        });
        latestFollowMap.values().forEach(follow -> {
            if (StrUtil.isNotBlank(follow.getFollowUserId())) {
                userIdSet.add(follow.getFollowUserId());
            }
        });
        studentFollowMap.values().forEach(follow -> {
            if (StrUtil.isNotBlank(follow.getFollowUserId())) {
                userIdSet.add(follow.getFollowUserId());
            }
        });
        Map<String, String> userNameMap = loadUserNameMap(new ArrayList<>(userIdSet));
        for (StudentRenewWarn warn : warnList) {
            Student student = studentMap.get(warn.getStudentId());
            warn.setStudentName(student == null ? null : student.getName());
            warn.setParentPhone(student == null ? null : student.getParentPhone());
            warn.setCourseName(courseNameMap.get(warn.getCourseId()));
            warn.setClassNames(classNamesMap.get(warn.getStudentId()));
            if (warn.getSnapshotTime() == null) {
                warn.setSnapshotTime(warn.getCreateTime());
            }
            StudentLessonAccount account = accountMap.get(warn.getStudentId() + "|" + warn.getCourseId());
            boolean currentRenewed = StudentRenewWarnStatusEnum.RENEWED.equals(warn.getStatus());
            if (account != null) {
                warn.setCurrentRemainLessons(account.getRemainLessons());
                warn.setCurrentExpireTime(account.getExpireTime());
                warn.setCurrentAccountStatus(account.getStatus());
                if (account.getRemainLessons() != null && warn.getRemainLessons() != null
                        && account.getRemainLessons().compareTo(warn.getRemainLessons()) > 0) {
                    currentRenewed = true;
                }
            }
            warn.setCurrentRenewed(currentRenewed);
            warn.setFollowUserName(userNameMap.get(warn.getFollowUserId()));
            if (StudentRenewWarnStatusEnum.PROCESSED.equals(warn.getStatus())
                    || StudentRenewWarnStatusEnum.IGNORED.equals(warn.getStatus())) {
                warn.setProcessUserName(userNameMap.get(warn.getUpdateUser()));
            }
            StudentFollowRecord latestFollow = latestFollowMap.get(warn.getId());
            if (latestFollow == null) {
                latestFollow = studentFollowMap.get(warn.getStudentId());
            }
            if (latestFollow != null) {
                warn.setLastFollowContent(latestFollow.getContent());
                warn.setLastFollowTime(latestFollow.getCreateTime());
            }
        }
    }

    /**
     * 查找预警：优先按id，其次按学员+课程取最近一条
     */
    private StudentRenewWarn findWarn(String id, String studentId, String courseId) {
        if (StrUtil.isNotBlank(id)) {
            return this.queryEntity(id);
        }
        if (StrUtil.isBlank(studentId)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStudentId, studentId)
                .eq(StrUtil.isNotBlank(courseId), StudentRenewWarn::getCourseId, courseId)
                .orderByDesc(StudentRenewWarn::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 风险学员生成待办（待处理预警），不重复生成
     */
    private StudentRenewWarn createRiskTodoEntity(String studentId, String courseId, String riskType,
                                                  BigDecimal remainLessons) {
        BigDecimal remain = remainLessons;
        if (remain == null && StrUtil.isNotBlank(courseId)) {
            StudentLessonAccount account = studentLessonAccountMapper.selectOne(
                    new LambdaQueryWrapper<StudentLessonAccount>()
                            .eq(StudentLessonAccount::getStudentId, studentId)
                            .eq(StudentLessonAccount::getCourseId, courseId)
                            .last("limit 1"));
            if (account != null) {
                remain = account.getRemainLessons();
            }
        }
        StudentRenewWarn warn = new StudentRenewWarn();
        warn.setStudentId(studentId);
        warn.setCourseId(courseId);
        warn.setRemainLessons(remain);
        warn.setWarnReason(mapRiskReason(riskType));
        warn.setStatus(StudentRenewWarnStatusEnum.PENDING);
        warn.setSnapshotTime(DateUtil.date());
        this.save(warn);
        return warn;
    }

    private String mapRiskReason(String riskType) {
        if ("NO_ATTEND".equalsIgnoreCase(riskType)) {
            return StudentRenewWarnStatusEnum.REASON_NO_ATTEND;
        }
        if ("STOPPED".equalsIgnoreCase(riskType)) {
            return StudentRenewWarnStatusEnum.REASON_LOSS_RISK;
        }
        return StudentRenewWarnStatusEnum.REASON_LESSON_LOW;
    }

    private String buildNotifyContent(StudentRenewWarn warn, String studentId, String courseId) {
        String studentName = loadStudentName(studentId);
        String courseName = StrUtil.isBlank(courseId) ? "" : StrUtil.blankToDefault(courseApi.getCourseName(courseId), "");
        if (warn == null) {
            return "学员" + studentName + (StrUtil.isBlank(courseName) ? "" : "的" + courseName)
                    + "课时/到课情况需要关注，请尽快联系家长沟通续费";
        }
        String snapshot = nvl(warn.getRemainLessons()).stripTrailingZeros().toPlainString();
        String current = nvl(warn.getCurrentRemainLessons()).stripTrailingZeros().toPlainString();
        return "学员" + studentName + (StrUtil.isBlank(courseName) ? "" : "的" + courseName)
                + "预警时剩余" + snapshot + "课时，当前剩余" + current + "课时，请尽快跟进续费";
    }

    /**
     * 批量查询学员最近一次跟进
     */
    private Map<String, StudentFollowRecord> loadLatestFollowByStudent(List<String> studentIdList) {
        Map<String, StudentFollowRecord> latestFollowMap = new HashMap<>();
        if (ObjectUtil.isEmpty(studentIdList)) {
            return latestFollowMap;
        }
        List<StudentFollowRecord> followList = studentFollowRecordMapper.selectList(
                new LambdaQueryWrapper<StudentFollowRecord>()
                        .eq(StudentFollowRecord::getTenantId, RenewWarnTenantUtil.currentTenantId())
                        .in(StudentFollowRecord::getStudentId, studentIdList)
                        .orderByDesc(StudentFollowRecord::getCreateTime));
        for (StudentFollowRecord follow : followList) {
            latestFollowMap.putIfAbsent(follow.getStudentId(), follow);
        }
        return latestFollowMap;
    }

    /**
     * 用户id → 姓名
     */
    private Map<String, String> loadUserNameMap(List<String> userIdList) {
        Map<String, String> userNameMap = new HashMap<>();
        List<String> distinctIdList = userIdList.stream().filter(StrUtil::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (ObjectUtil.isEmpty(distinctIdList)) {
            return userNameMap;
        }
        try {
            List<JSONObject> userList = sysUserApi.getUserListByIdListWithoutException(distinctIdList);
            if (ObjectUtil.isNotEmpty(userList)) {
                for (JSONObject user : userList) {
                    if (StrUtil.isNotBlank(user.getStr("id"))) {
                        userNameMap.put(user.getStr("id"), user.getStr("name"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn(">>> 续费闭环加载用户名称失败：{}", e.getMessage());
        }
        return userNameMap;
    }

    private String loadStudentName(String studentId) {
        if (StrUtil.isBlank(studentId)) {
            return "学员";
        }
        Student student = studentMapper.selectById(studentId);
        return student == null || StrUtil.isBlank(student.getName()) ? "学员" : student.getName();
    }

    private String currentUserId() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal percent(Long numerator, Long denominator) {
        long den = ObjectUtil.defaultIfNull(denominator, 0L);
        if (den <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(ObjectUtil.defaultIfNull(numerator, 0L))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(den), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 获取预警详情（内部用）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private StudentRenewWarn queryEntity(String id) {
        StudentRenewWarn warn = this.getById(id);
        if (ObjectUtil.isEmpty(warn)) {
            throw new CommonException("续费预警不存在，id值为：{}", id);
        }
        return warn;
    }
}
