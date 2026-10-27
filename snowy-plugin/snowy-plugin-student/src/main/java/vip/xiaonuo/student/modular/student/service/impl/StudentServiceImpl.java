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
package vip.xiaonuo.student.modular.student.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.auth.core.util.StpRoleUtil;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.api.CourseApi;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.student.core.enums.StudentStatusMachineEnum;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentEditParam;
import vip.xiaonuo.student.modular.student.param.StudentIdParam;
import vip.xiaonuo.student.modular.student.param.StudentPageParam;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.mapper.StudentLessonAccountMapper;
import vip.xiaonuo.student.modular.studentorder.entity.StudentOrder;
import vip.xiaonuo.student.modular.studentorder.enums.StudentOrderStatusEnum;
import vip.xiaonuo.student.modular.studentorder.mapper.StudentOrderMapper;
import vip.xiaonuo.student.modular.studentstatus.entity.StudentStatusLog;
import vip.xiaonuo.student.modular.studentstatus.mapper.StudentStatusLogMapper;
import vip.xiaonuo.student.modular.studentstatus.param.StudentStatusChangeParam;
import vip.xiaonuo.student.modular.studenttag.entity.StudentTag;
import vip.xiaonuo.student.modular.studenttag.service.StudentTagService;
import vip.xiaonuo.sys.api.SysOrgApi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vip.xiaonuo.common.tenant.TenantCapability;
import vip.xiaonuo.student.core.service.StudentDataScopeService;
import jakarta.annotation.Resource;

/**
 * 学员Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Resource
    private StudentDataScopeService studentDataScopeService;

    @Resource
    private TenantCapability tenantCapability;

    @Resource
    private BizEventApi bizEventApi;

    @Resource
    private StudentLessonAccountMapper lessonAccountMapper;

    @Resource
    private StudentOrderMapper studentOrderMapper;

    @Resource
    private CourseApi courseApi;

    @Resource
    private SysOrgApi sysOrgApi;

    @Resource
    private StudentTagService studentTagService;

    @Resource
    private StudentStatusLogMapper studentStatusLogMapper;

    @Override
    public Page<Student> page(StudentPageParam studentPageParam) {
        QueryWrapper<Student> queryWrapper = buildQueryWrapper(studentPageParam);
        if (ObjectUtil.isAllNotEmpty(studentPageParam.getSortField(), studentPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(studentPageParam.getSortOrder());
            queryWrapper.orderBy(true, studentPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(studentPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(Student::getCreateTime);
        }
        Page<Student> studentPage = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillBusinessSummary(studentPage.getRecords());
        fillTags(studentPage.getRecords());
        return studentPage;
    }

    /**
     * 组装学员列表筛选条件（分页与导出共用）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private QueryWrapper<Student> buildQueryWrapper(StudentPageParam studentPageParam) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<Student>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(studentPageParam.getSearchKey())) {
            queryWrapper.lambda().and(q -> q.like(Student::getName, studentPageParam.getSearchKey())
                    .or().like(Student::getParentPhone, studentPageParam.getSearchKey()));
        }
        if (ObjectUtil.isNotEmpty(studentPageParam.getCampusId())) {
            queryWrapper.lambda().eq(Student::getCampusId, studentPageParam.getCampusId());
        }
        if (ObjectUtil.isNotEmpty(studentPageParam.getParentPhone())) {
            queryWrapper.lambda().eq(Student::getParentPhone, studentPageParam.getParentPhone());
        }
        // 数据范围：仅可见校区的学员
        List<String> visibleCampusIds = studentDataScopeService.visibleCampusIds();
        if (visibleCampusIds != null) {
            if (visibleCampusIds.isEmpty()) {
                queryWrapper.lambda().eq(Student::getId, StudentDataScopeService.NO_DATA_ID);
            } else {
                queryWrapper.lambda().in(Student::getCampusId, visibleCampusIds);
            }
        }
        if (ObjectUtil.isNotEmpty(studentPageParam.getStatus())) {
            queryWrapper.lambda().eq(Student::getStatus, studentPageParam.getStatus());
        }
        // 标签筛选：标签表未登记租户表清单，子查询里显式带租户
        if (StrUtil.isNotBlank(studentPageParam.getTagId())) {
            queryWrapper.lambda().apply(
                    "ID IN (SELECT R.STUDENT_ID FROM `STUDENT_TAG_REL` R"
                            + " JOIN `STUDENT_TAG` T ON T.ID = R.TAG_ID"
                            + " WHERE R.TENANT_ID = {0} AND R.TAG_ID = {1}"
                            + " AND R.DELETE_FLAG = 'NOT_DELETE' AND T.DELETE_FLAG = 'NOT_DELETE')",
                    currentTenantId(), studentPageParam.getTagId());
        }
        // 岗位数据范围：授课老师只看自己班里的学员
        String selfTeacherId = StpRoleUtil.hasRole(StpRoleUtil.ROLE_TEACHER) ? StpRoleUtil.currentUserId() : null;
        if (StrUtil.isNotBlank(selfTeacherId)) {
            queryWrapper.lambda().apply(
                    "id in (select cs.student_id from COURSE_CLASS_STUDENT cs join COURSE_CLASS c on c.id = cs.class_id"
                            + " where c.teacher_id = {0} and cs.delete_flag = 'NOT_DELETE')", selfTeacherId);
        }
        return queryWrapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String add(StudentAddParam studentAddParam) {
        return doAdd(studentAddParam, null);
    }

    @Override
    public String addForImport(StudentAddParam studentAddParam, String status) {
        return doAdd(studentAddParam, status);
    }

    /**
     * 新增学员（普通新增与批量导入共用）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private String doAdd(StudentAddParam studentAddParam, String status) {
        Student student = BeanUtil.toBean(studentAddParam, Student.class);
        if (StrUtil.isNotBlank(status)) {
            student.setStatus(status);
        }
        if (ObjectUtil.isEmpty(student.getStatus())) {
            student.setStatus(StudentStatusMachineEnum.STUDYING.getValue());
        }
        // 校区缺省时按“当前可见校区 → 机构首个校区”兜底，保证订单/报表有校区维度
        if (StrUtil.isBlank(student.getCampusId())) {
            student.setCampusId(resolveDefaultCampusId());
        }
        if (StrUtil.isBlank(student.getCampusId())) {
            throw new CommonException("请选择校区");
        }
        // 套餐限额：学员数上限
        Integer studentLimit = tenantCapability.studentLimit();
        if (studentLimit != null && this.count() >= studentLimit) {
            throw new CommonException("当前套餐最多可添加 {} 名学员，已达上限", studentLimit);
        }
        this.save(student);
        bizEventApi.log(BizEventCodeEnum.STUDENT_CREATE.getValue(), "student", student.getId(), student.getName(), null);
        return student.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(StudentEditParam studentEditParam) {
        Student student = this.queryEntity(studentEditParam.getId());
        String oldStatus = student.getStatus();
        BeanUtil.copyProperties(studentEditParam, student);
        if (StrUtil.isBlank(student.getCampusId())) {
            student.setCampusId(resolveDefaultCampusId());
        }
        this.updateById(student);
        // 资料编辑里改状态同样留痕，避免绕过状态机
        if (StrUtil.isNotBlank(oldStatus) && !oldStatus.equals(student.getStatus())) {
            saveStatusLog(student.getId(), oldStatus, student.getStatus(), "资料编辑调整状态");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<StudentIdParam> studentIdParamList) {
        List<String> studentIdList = CollStreamUtil.toList(studentIdParamList, StudentIdParam::getId);
        if (ObjectUtil.isNotEmpty(studentIdList)) {
            this.removeByIds(studentIdList);
        }
    }

    @Override
    public Student detail(StudentIdParam studentIdParam) {
        Student student = this.queryEntity(studentIdParam.getId());
        fillBusinessSummary(List.of(student));
        fillTags(List.of(student));
        return student;
    }

    @Override
    public List<Student> listForExport(StudentPageParam studentPageParam) {
        QueryWrapper<Student> queryWrapper = buildQueryWrapper(studentPageParam);
        queryWrapper.lambda().orderByDesc(Student::getCreateTime);
        List<Student> studentList = this.list(queryWrapper);
        fillBusinessSummary(studentList);
        fillTags(studentList);
        return studentList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void changeStatus(StudentStatusChangeParam param) {
        Student student = this.queryEntity(param.getStudentId());
        StudentStatusMachineEnum target = StudentStatusMachineEnum.getByValue(param.getStatus());
        if (target == null || !target.canBeTarget()) {
            throw new CommonException("不支持的目标状态：{}", param.getStatus());
        }
        String fromStatus = student.getStatus();
        if (target.getValue().equals(fromStatus)) {
            throw new CommonException("学员当前已是【{}】，无需变更", target.getMessage());
        }
        String reason = StrUtil.trim(param.getReason());
        boolean resume = StudentStatusMachineEnum.STUDYING == target;
        if (!resume && StrUtil.isBlank(reason)) {
            throw new CommonException("变更为【{}】时必须填写原因", target.getMessage());
        }
        if (resume && StrUtil.isBlank(reason)) {
            reason = "复课";
        }
        student.setStatus(target.getValue());
        student.setStatusReason(reason);
        student.setStatusChangeTime(new Date());
        this.updateById(student);
        saveStatusLog(student.getId(), fromStatus, target.getValue(),
                StudentStatusMachineEnum.messageOf(fromStatus) + " → " + target.getMessage() + "：" + reason);
    }

    @Override
    public List<StudentStatusLog> listStatusLogs(String studentId) {
        if (StrUtil.isBlank(studentId)) {
            return List.of();
        }
        return studentStatusLogMapper.selectList(new LambdaQueryWrapper<StudentStatusLog>()
                .eq(StudentStatusLog::getTenantId, currentTenantId())
                .eq(StudentStatusLog::getStudentId, studentId)
                .orderByDesc(StudentStatusLog::getCreateTime));
    }

    /**
     * 写入状态变更记录（表未纳入全局租户拦截器，显式写 TENANT_ID）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private void saveStatusLog(String studentId, String fromStatus, String toStatus, String reason) {
        StudentStatusLog statusLog = new StudentStatusLog();
        statusLog.setTenantId(currentTenantId());
        statusLog.setStudentId(studentId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setReason(reason);
        studentStatusLogMapper.insert(statusLog);
    }

    /**
     * 回填学员标签（列表/详情共用）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private void fillTags(List<Student> studentList) {
        if (ObjectUtil.isEmpty(studentList)) {
            return;
        }
        List<String> studentIdList = studentList.stream().map(Student::getId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        Map<String, List<StudentTag>> tagMap = studentTagService.mapByStudentIds(studentIdList);
        for (Student student : studentList) {
            student.setTagList(tagMap.getOrDefault(student.getId(), List.of()));
        }
    }

    /**
     * 当前租户id（标签/状态记录表未登记到全局租户表清单）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    private String currentTenantId() {
        return StrUtil.blankToDefault(TenantContext.getTenantId(), "0");
    }

    @Override
    public Student queryEntity(String id) {
        Student student = this.getById(id);
        if (ObjectUtil.isEmpty(student)) {
            throw new CommonException("学员不存在，id值为：{}", id);
        }
        return student;
    }

    /**
     * 填充学员列表/详情的经营摘要：剩余课时、课包到期、所在班级、待收款
     * <p>
     * 按当前页批量查询，避免逐条 N+1。
     * </p>
     */
    private void fillBusinessSummary(List<Student> studentList) {
        if (ObjectUtil.isEmpty(studentList)) {
            return;
        }
        List<String> studentIdList = studentList.stream().map(Student::getId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        if (ObjectUtil.isEmpty(studentIdList)) {
            return;
        }

        Map<String, BigDecimal> remainMap = new HashMap<>();
        Map<String, Date> expireMap = new HashMap<>();
        List<StudentLessonAccount> accountList = lessonAccountMapper.selectList(
                new LambdaQueryWrapper<StudentLessonAccount>()
                        .in(StudentLessonAccount::getStudentId, studentIdList));
        for (StudentLessonAccount account : accountList) {
            BigDecimal remain = nvl(account.getRemainLessons());
            remainMap.merge(account.getStudentId(), remain, BigDecimal::add);
            // 只统计仍有余额的账户，取最早到期时间作为风险提示
            if (remain.compareTo(BigDecimal.ZERO) > 0 && account.getExpireTime() != null) {
                expireMap.merge(account.getStudentId(), account.getExpireTime(),
                        (first, second) -> first.before(second) ? first : second);
            }
        }

        Map<String, BigDecimal> unpaidMap = new HashMap<>();
        List<StudentOrder> unpaidOrderList = studentOrderMapper.selectList(
                new LambdaQueryWrapper<StudentOrder>()
                        .in(StudentOrder::getStudentId, studentIdList)
                        .eq(StudentOrder::getStatus, StudentOrderStatusEnum.UNPAID.getValue()));
        for (StudentOrder order : unpaidOrderList) {
            BigDecimal unpaid = nvl(order.getAmount()).subtract(nvl(order.getPaidAmount()));
            if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
                unpaidMap.merge(order.getStudentId(), unpaid, BigDecimal::add);
            }
        }

        Map<String, String> classNameMap = courseApi.listStudentClassNameMap(studentIdList);
        if (classNameMap == null) {
            classNameMap = Map.of();
        }

        for (Student student : studentList) {
            student.setRemainLessons(remainMap.getOrDefault(student.getId(), BigDecimal.ZERO));
            student.setAccountExpireTime(expireMap.get(student.getId()));
            student.setUnpaidAmount(unpaidMap.getOrDefault(student.getId(), BigDecimal.ZERO));
            student.setClassNames(classNameMap.get(student.getId()));
            student.setHasLessonAccount(remainMap.containsKey(student.getId()));
        }
    }

    /**
     * 解析默认校区：优先当前账号可见校区，否则取机构第一个校区
     * <p>
     * 仅用于接口缺省兜底，页面新增学员仍要求显式选择校区。
     * 机构未配置校区时返回 null，由调用方决定是否报错。
     * </p>
     */
    @Override
    public String resolveDefaultCampusId() {
        List<String> campusIdList = new ArrayList<>();
        collectCampusIds(sysOrgApi.orgTreeSelector("0", ""), campusIdList);
        if (campusIdList.isEmpty()) {
            return null;
        }
        List<String> visibleCampusIds = studentDataScopeService.visibleCampusIds();
        if (visibleCampusIds == null) {
            return campusIdList.get(0);
        }
        for (String campusId : campusIdList) {
            if (visibleCampusIds.contains(campusId)) {
                return campusId;
            }
        }
        return null;
    }

    private void collectCampusIds(List<JSONObject> orgList, List<String> campusIdList) {
        if (ObjectUtil.isEmpty(orgList)) {
            return;
        }
        for (JSONObject org : orgList) {
            if ("DEPT".equalsIgnoreCase(org.getStr("category"))
                    || "CAMPUS".equalsIgnoreCase(org.getStr("category"))) {
                String orgId = org.getStr("id");
                if (StrUtil.isNotBlank(orgId)) {
                    campusIdList.add(orgId);
                }
            }
            collectCampusIds(org.getBeanList("children", JSONObject.class), campusIdList);
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return ObjectUtil.defaultIfNull(value, BigDecimal.ZERO);
    }
}
