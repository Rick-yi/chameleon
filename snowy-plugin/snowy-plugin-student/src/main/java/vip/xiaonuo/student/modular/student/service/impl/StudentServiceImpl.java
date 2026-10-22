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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.auth.core.util.StpRoleUtil;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.platform.api.BizEventApi;
import vip.xiaonuo.platform.api.enums.BizEventCodeEnum;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.api.enums.StudentStatusEnum;
import vip.xiaonuo.student.modular.student.mapper.StudentMapper;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentEditParam;
import vip.xiaonuo.student.modular.student.param.StudentIdParam;
import vip.xiaonuo.student.modular.student.param.StudentPageParam;
import vip.xiaonuo.student.modular.student.service.StudentService;

import java.util.List;
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

    @Override
    public Page<Student> page(StudentPageParam studentPageParam) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<Student>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(studentPageParam.getSearchKey())) {
            queryWrapper.lambda().and(q -> q.like(Student::getName, studentPageParam.getSearchKey())
                    .or().like(Student::getParentPhone, studentPageParam.getSearchKey()));
        }
        if (ObjectUtil.isNotEmpty(studentPageParam.getCampusId())) {
            queryWrapper.lambda().eq(Student::getCampusId, studentPageParam.getCampusId());
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
        // 岗位数据范围：授课老师只看自己班里的学员
        String selfTeacherId = StpRoleUtil.hasRole(StpRoleUtil.ROLE_TEACHER) ? StpRoleUtil.currentUserId() : null;
        if (StrUtil.isNotBlank(selfTeacherId)) {
            queryWrapper.lambda().apply(
                    "id in (select cs.student_id from COURSE_CLASS_STUDENT cs join COURSE_CLASS c on c.id = cs.class_id"
                            + " where c.teacher_id = {0} and cs.delete_flag = 'NOT_DELETE')", selfTeacherId);
        }
        if (ObjectUtil.isAllNotEmpty(studentPageParam.getSortField(), studentPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(studentPageParam.getSortOrder());
            queryWrapper.orderBy(true, studentPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(studentPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(Student::getCreateTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String add(StudentAddParam studentAddParam) {
        Student student = BeanUtil.toBean(studentAddParam, Student.class);
        if (ObjectUtil.isEmpty(student.getStatus())) {
            student.setStatus(StudentStatusEnum.STUDYING.getValue());
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
        BeanUtil.copyProperties(studentEditParam, student);
        this.updateById(student);
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
        return this.queryEntity(studentIdParam.getId());
    }

    @Override
    public Student queryEntity(String id) {
        Student student = this.getById(id);
        if (ObjectUtil.isEmpty(student)) {
            throw new CommonException("学员不存在，id值为：{}", id);
        }
        return student;
    }
}
