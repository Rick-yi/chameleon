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
package vip.xiaonuo.student.modular.studentlessonaccount.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.student.api.pojo.LessonDeductParam;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.enums.LessonAccountStatusEnum;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentlessonaccount.param.StudentLessonAccountPageParam;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.service.StudentLessonFlowService;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentrenewarn.service.StudentRenewWarnService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 课时账户Service接口实现类
 * <p>
 * 课时账务核心：入账（支付后）/扣课（点名联动）/回补（撤销点名）。
 * 并发控制：扣课/回补使用条件更新（剩余充足才扣、已用足够才回补），配合唯一索引防重复。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentLessonAccountServiceImpl extends ServiceImpl<StudentLessonAccountMapper, StudentLessonAccount> implements StudentLessonAccountService {

    @Resource
    private StudentLessonFlowService studentLessonFlowService;

    @Resource
    private StudentRenewWarnService studentRenewWarnService;

    @Resource
    private StudentService studentService;

    @Resource
    private CourseApi courseApi;

    @Override
    public Page<StudentLessonAccount> page(StudentLessonAccountPageParam studentLessonAccountPageParam) {
        QueryWrapper<StudentLessonAccount> queryWrapper = new QueryWrapper<StudentLessonAccount>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentLessonAccountPageParam.getStudentId()), StudentLessonAccount::getStudentId, studentLessonAccountPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentLessonAccountPageParam.getCourseId()), StudentLessonAccount::getCourseId, studentLessonAccountPageParam.getCourseId())
                .orderByDesc(StudentLessonAccount::getUpdateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Override
    public StudentLessonAccount getAccount(String studentId, String courseId) {
        StudentLessonAccount account = this.getOne(new LambdaQueryWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getStudentId, studentId)
                .eq(StudentLessonAccount::getCourseId, courseId));
        if (ObjectUtil.isEmpty(account)) {
            throw new CommonException("课时账户不存在（请先报名购课）");
        }
        return account;
    }

    @Override
    public void updateAccount(StudentLessonAccount studentLessonAccount) {
        this.updateById(studentLessonAccount);
    }

    @Override
    public List<StudentLessonAccount> listNormalAccounts() {
        return this.list(new LambdaQueryWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getStatus, LessonAccountStatusEnum.NORMAL.getValue()));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void recharge(StudentOrder studentOrder) {        JSONObject packageInfo = courseApi.getCoursePackageInfo(studentOrder.getCoursePackageId());
        if (ObjectUtil.isEmpty(packageInfo)) {
            throw new CommonException("课包不存在，id值为：{}", studentOrder.getCoursePackageId());
        }
        String courseId = packageInfo.getStr("courseId");
        BigDecimal totalAdded = studentOrder.getPackageLessons().add(studentOrder.getPackageGiftLessons());
        StudentLessonAccount account = this.getOne(new LambdaQueryWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getStudentId, studentOrder.getStudentId())
                .eq(StudentLessonAccount::getCourseId, courseId));
        LocalDateTime now = LocalDateTime.now();
        if (ObjectUtil.isEmpty(account)) {
            // 首次购课：创建账户
            account = new StudentLessonAccount();
            account.setStudentId(studentOrder.getStudentId());
            account.setCourseId(courseId);
            account.setTotalLessons(totalAdded);
            account.setGiftLessons(studentOrder.getPackageGiftLessons());
            account.setUsedLessons(BigDecimal.ZERO);
            account.setRemainLessons(totalAdded);
            account.setExpireTime(calcExpireTime(null, studentOrder.getPackageValidityDays()));
            account.setStatus(LessonAccountStatusEnum.NORMAL.getValue());
            this.save(account);
        } else {
            // 续费：追加课时 + 有效期顺延
            account.setTotalLessons(account.getTotalLessons().add(totalAdded));
            account.setGiftLessons(account.getGiftLessons().add(studentOrder.getPackageGiftLessons()));
            account.setRemainLessons(account.getRemainLessons().add(totalAdded));
            account.setExpireTime(calcExpireTime(account.getExpireTime(), studentOrder.getPackageValidityDays()));
            account.setStatus(LessonAccountStatusEnum.NORMAL.getValue());
            this.updateById(account);
        }
        // 入账流水
        StudentLessonFlow flow = new StudentLessonFlow();
        flow.setStudentId(studentOrder.getStudentId());
        flow.setCourseId(courseId);
        flow.setFlowType(LessonFlowTypeEnum.SIGNUP.getValue());
        flow.setLessons(totalAdded);
        flow.setRefOrderId(studentOrder.getId());
        flow.setRemark(studentOrder.getPackageName());
        studentLessonFlowService.add(flow);
        // 续费预警置已续费
        studentRenewWarnService.markRenewed(studentOrder.getStudentId(), courseId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deductLessons(List<LessonDeductParam> deductParamList) {
        for (LessonDeductParam param : deductParamList) {
            StudentLessonAccount account = this.getAccount(param.getStudentId(), param.getCourseId());
            Student student = studentService.queryEntity(param.getStudentId());
            if (!LessonAccountStatusEnum.NORMAL.getValue().equals(account.getStatus())) {
                throw new CommonException("学员【{}】课时账户状态异常（{}），无法扣课", student.getName(), account.getStatus());
            }
            if (account.getRemainLessons().compareTo(param.getLessons()) < 0) {
                throw new CommonException("学员【{}】课时不足：剩余{}，本次需扣{}", student.getName(),
                        account.getRemainLessons(), param.getLessons());
            }
            // 条件更新防并发：剩余充足才扣
            boolean deducted = this.update(new LambdaUpdateWrapper<StudentLessonAccount>()
                    .eq(StudentLessonAccount::getId, account.getId())
                    .ge(StudentLessonAccount::getRemainLessons, param.getLessons())
                    .setSql("USED_LESSONS = USED_LESSONS + " + param.getLessons())
                    .setSql("REMAIN_LESSONS = REMAIN_LESSONS - " + param.getLessons()));
            if (!deducted) {
                throw new CommonException("学员【{}】课时不足或账户状态异常，扣课失败", student.getName());
            }
            // 消课流水
            StudentLessonFlow flow = new StudentLessonFlow();
            flow.setStudentId(param.getStudentId());
            flow.setCourseId(param.getCourseId());
            flow.setScheduleId(param.getScheduleId());
            flow.setFlowType(LessonFlowTypeEnum.ATTEND.getValue());
            flow.setLessons(param.getLessons().negate());
            flow.setRemark("点名" + param.getAttendStatus());
            studentLessonFlowService.add(flow);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void restoreLessons(String scheduleId, String studentId) {
        // 定位原消课流水
        StudentLessonFlow deductFlow = studentLessonFlowService.getDeductFlow(scheduleId, studentId);
        if (ObjectUtil.isEmpty(deductFlow)) {
            throw new CommonException("未找到该学员的原消课流水，无法回补");
        }
        BigDecimal restoreLessons = deductFlow.getLessons().negate();
        StudentLessonAccount account = this.getAccount(studentId, deductFlow.getCourseId());
        // 条件更新防并发：已用足够才回补
        boolean restored = this.update(new LambdaUpdateWrapper<StudentLessonAccount>()
                .eq(StudentLessonAccount::getId, account.getId())
                .ge(StudentLessonAccount::getUsedLessons, restoreLessons)
                .setSql("USED_LESSONS = USED_LESSONS - " + restoreLessons)
                .setSql("REMAIN_LESSONS = REMAIN_LESSONS + " + restoreLessons));
        if (!restored) {
            throw new CommonException("账户数据异常，回补失败");
        }
        // 回补流水（关联原流水）
        StudentLessonFlow flow = new StudentLessonFlow();
        flow.setStudentId(studentId);
        flow.setCourseId(deductFlow.getCourseId());
        flow.setScheduleId(scheduleId);
        flow.setFlowType(LessonFlowTypeEnum.RESTORE.getValue());
        flow.setLessons(restoreLessons);
        flow.setRefFlowId(deductFlow.getId());
        flow.setRemark("撤销点名回补");
        studentLessonFlowService.add(flow);
    }

    /**
     * 计算课包到期时间：新购=now+有效期；续费=顺延（max(now,原到期)+有效期）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     **/
    private Date calcExpireTime(Date oldExpireTime, Integer validityDays) {
        if (validityDays == null || validityDays <= 0) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = oldExpireTime == null ? now
                : oldExpireTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (base.isBefore(now)) {
            base = now;
        }
        return Date.from(base.plusDays(validityDays).atZone(ZoneId.systemDefault()).toInstant());
    }
}
