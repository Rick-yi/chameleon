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
package vip.xiaonuo.student.modular.studentlessonflow.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.student.modular.studentlessonflow.entity.StudentLessonFlow;
import vip.xiaonuo.student.modular.studentlessonflow.enums.LessonFlowTypeEnum;
import vip.xiaonuo.student.modular.studentlessonflow.mapper.StudentLessonFlowMapper;
import vip.xiaonuo.student.modular.studentlessonflow.param.StudentLessonFlowPageParam;
import vip.xiaonuo.student.modular.studentlessonflow.service.StudentLessonFlowService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vip.xiaonuo.student.core.service.StudentDataScopeService;
import jakarta.annotation.Resource;

/**
 * 课时流水Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class StudentLessonFlowServiceImpl extends ServiceImpl<StudentLessonFlowMapper, StudentLessonFlow> implements StudentLessonFlowService {

    @Resource
    private StudentDataScopeService studentDataScopeService;

    @Resource
    private vip.xiaonuo.course.api.CourseApi courseApi;

    @Override
    public Page<StudentLessonFlow> page(StudentLessonFlowPageParam studentLessonFlowPageParam) {
        QueryWrapper<StudentLessonFlow> queryWrapper = new QueryWrapper<StudentLessonFlow>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(studentLessonFlowPageParam.getStudentId()), StudentLessonFlow::getStudentId, studentLessonFlowPageParam.getStudentId())
                .eq(ObjectUtil.isNotEmpty(studentLessonFlowPageParam.getCourseId()), StudentLessonFlow::getCourseId, studentLessonFlowPageParam.getCourseId())
                .orderByDesc(StudentLessonFlow::getCreateTime);
        // 数据范围：仅可见校区的学员流水
        List<String> visibleStudentIds = studentDataScopeService.visibleStudentIdList();
        if (visibleStudentIds != null) {
            if (visibleStudentIds.isEmpty()) {
                queryWrapper.lambda().eq(StudentLessonFlow::getId, StudentDataScopeService.NO_DATA_ID);
            } else {
                queryWrapper.lambda().in(StudentLessonFlow::getStudentId, visibleStudentIds);
            }
        }
        Page<StudentLessonFlow> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillClassName(page.getRecords());
        return page;
    }

    /**
     * 回填消课所属班级名（流水 → 排课 → 班级）
     */
    private void fillClassName(List<StudentLessonFlow> flowList) {
        if (ObjectUtil.isEmpty(flowList)) {
            return;
        }
        List<String> scheduleIdList = flowList.stream().map(StudentLessonFlow::getScheduleId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (ObjectUtil.isEmpty(scheduleIdList)) {
            return;
        }
        Map<String, String> classNameMap = courseApi.listScheduleClassNameMap(scheduleIdList);
        for (StudentLessonFlow flow : flowList) {
            flow.setClassName(classNameMap.get(flow.getScheduleId()));
        }
    }

    @Override
    public void add(StudentLessonFlow studentLessonFlow) {
        this.save(studentLessonFlow);
    }

    @Override
    public StudentLessonFlow getDeductFlow(String scheduleId, String studentId) {
        return this.getOne(new LambdaQueryWrapper<StudentLessonFlow>()
                .eq(StudentLessonFlow::getScheduleId, scheduleId)
                .eq(StudentLessonFlow::getStudentId, studentId)
                .eq(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue())
                .orderByDesc(StudentLessonFlow::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public List<StudentLessonFlow> listDeductFlows(String scheduleId, String studentId) {
        return this.list(new LambdaQueryWrapper<StudentLessonFlow>()
                .eq(StudentLessonFlow::getScheduleId, scheduleId)
                .eq(StudentLessonFlow::getStudentId, studentId)
                .eq(StudentLessonFlow::getFlowType, LessonFlowTypeEnum.ATTEND.getValue())
                .orderByAsc(StudentLessonFlow::getCreateTime));
    }
}
