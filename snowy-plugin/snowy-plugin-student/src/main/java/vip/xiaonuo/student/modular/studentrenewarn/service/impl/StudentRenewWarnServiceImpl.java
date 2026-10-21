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
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
import vip.xiaonuo.dev.api.DevConfigApi;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.enums.LessonAccountStatusEnum;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.notify.service.NotifyLogService;
import vip.xiaonuo.student.modular.studentrenewarn.entity.StudentRenewWarn;
import vip.xiaonuo.student.modular.studentrenewarn.enums.StudentRenewWarnStatusEnum;
import vip.xiaonuo.student.modular.studentrenewarn.mapper.StudentRenewWarnMapper;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnIdParam;
import vip.xiaonuo.student.modular.studentrenewarn.param.StudentRenewWarnPageParam;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 续费预警Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Slf4j
@Service
public class StudentRenewWarnServiceImpl extends ServiceImpl<StudentRenewWarnMapper, StudentRenewWarn> implements StudentRenewWarnService {

    /** 课时阈值配置键（机构可在系统配置调整） */
    private static final String WARN_LESSON_THRESHOLD_KEY = "SNOWY_RENEW_WARN_LESSON_THRESHOLD";

    /** 到期提前天数配置键 */
    private static final String WARN_EXPIRE_DAYS_KEY = "SNOWY_RENEW_WARN_EXPIRE_DAYS";

    /** 去重周期（天） */
    private static final int DEDUP_DAYS = 7;

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    @Resource
    private DevConfigApi devConfigApi;

    @Resource
    private vip.xiaonuo.student.modular.student.service.StudentService studentService;

    @Resource
    private vip.xiaonuo.student.modular.student.mapper.StudentMapper studentMapper;

    @Resource
    private vip.xiaonuo.course.api.CourseApi courseApi;

    @Resource
    private NotifyLogService notifyLogService;

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
        fillNameInfo(page.getRecords());
        return page;
    }

    /**
     * 批量回填学员姓名与课程名称（列表展示用）
     */
    private void fillNameInfo(List<StudentRenewWarn> warnList) {
        if (ObjectUtil.isEmpty(warnList)) {
            return;
        }
        List<String> studentIdList = warnList.stream().map(StudentRenewWarn::getStudentId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, String> studentNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            studentMapper.selectBatchIds(studentIdList).forEach(student -> studentNameMap.put(student.getId(), student.getName()));
        }
        List<String> courseIdList = warnList.stream().map(StudentRenewWarn::getCourseId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, String> courseNameMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(courseIdList)) {
            courseApi.listCourseNameMap(courseIdList).forEach((id, name) -> courseNameMap.put(id, name));
        }
        for (StudentRenewWarn warn : warnList) {
            warn.setStudentName(studentNameMap.get(warn.getStudentId()));
            warn.setCourseName(courseNameMap.get(warn.getCourseId()));
        }
    }

    @Override
    public void scan() {
        // 阈值默认：课时≤3 或 7天内到期
        int lessonThreshold = Convert.toInt(devConfigApi.getValueByKey(WARN_LESSON_THRESHOLD_KEY), 3);
        int expireDays = Convert.toInt(devConfigApi.getValueByKey(WARN_EXPIRE_DAYS_KEY), 7);
        DateTime now = DateUtil.date();
        DateTime expireDeadline = DateUtil.offsetDay(now, expireDays);
        // 扫描当前租户全部正常账户
        List<StudentLessonAccount> accountList = studentLessonAccountService.listNormalAccounts();
        for (StudentLessonAccount account : accountList) {
            String warnReason = null;
            if (account.getRemainLessons() != null
                    && account.getRemainLessons().compareTo(BigDecimal.valueOf(lessonThreshold)) <= 0) {
                warnReason = StudentRenewWarnStatusEnum.REASON_LESSON_LOW;
            } else if (account.getExpireTime() != null && account.getExpireTime().before(expireDeadline)
                    && account.getExpireTime().after(now)) {
                warnReason = StudentRenewWarnStatusEnum.REASON_EXPIRE_SOON;
            }
            if (warnReason == null) {
                continue;
            }
            // 7 天去重：最近一条预警创建于 7 天内则跳过
            StudentRenewWarn latest = this.getOne(new LambdaQueryWrapper<StudentRenewWarn>()
                    .eq(StudentRenewWarn::getStudentId, account.getStudentId())
                    .eq(StudentRenewWarn::getCourseId, account.getCourseId())
                    .orderByDesc(StudentRenewWarn::getCreateTime)
                    .last("limit 1"));
            if (ObjectUtil.isNotEmpty(latest) && latest.getCreateTime() != null
                    && latest.getCreateTime().after(DateUtil.offsetDay(now, -DEDUP_DAYS))) {
                continue;
            }
            StudentRenewWarn warn = new StudentRenewWarn();
            warn.setStudentId(account.getStudentId());
            warn.setCourseId(account.getCourseId());
            warn.setRemainLessons(account.getRemainLessons());
            warn.setWarnReason(warnReason);
            warn.setStatus(StudentRenewWarnStatusEnum.PENDING);
            warn.setLastNotifyTime(now);
            this.save(warn);
            log.info(">>> 续费预警生成：学员{} 课程{} 剩余{} 原因{}", warn.getStudentId(), warn.getCourseId(), warn.getRemainLessons(), warnReason);
            // 机构端待办 + 家长通知落库（v1 站内记录，M4 接订阅消息/短信）
            try {
                notifyLogService.log("RENEW_WARN", "STAFF", null, warn.getStudentId(),
                        "续费预警", "学员课程剩余" + warn.getRemainLessons().stripTrailingZeros().toPlainString()
                                + "课时，请及时跟进续费", "IN_APP");
                notifyLogService.log("RENEW_WARN", "PARENT", warn.getStudentId(), warn.getStudentId(),
                        "续费提醒", "课程剩余" + warn.getRemainLessons().stripTrailingZeros().toPlainString()
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
        warn.setStatus(StudentRenewWarnStatusEnum.IGNORED);
        this.updateById(warn);
    }

    @Override
    public void markRenewed(String studentId, String courseId) {
        this.update(new LambdaUpdateWrapper<StudentRenewWarn>()
                .eq(StudentRenewWarn::getStudentId, studentId)
                .eq(StudentRenewWarn::getCourseId, courseId)
                .eq(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.PENDING)
                .set(StudentRenewWarn::getStatus, StudentRenewWarnStatusEnum.RENEWED));
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
