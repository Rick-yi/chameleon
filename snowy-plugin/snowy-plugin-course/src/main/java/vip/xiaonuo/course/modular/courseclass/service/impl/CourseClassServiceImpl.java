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
package vip.xiaonuo.course.modular.courseclass.service.impl;

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
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.course.enums.CourseStatusEnum;
import vip.xiaonuo.course.modular.courseclass.mapper.CourseClassMapper;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassAddParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassEditParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassIdParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassPageParam;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import vip.xiaonuo.auth.core.util.StpDataScopeUtil;
import vip.xiaonuo.common.util.CommonSqlUtil;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.enums.CourseClassStudentStatusEnum;
import vip.xiaonuo.course.modular.courseclassstudent.mapper.CourseClassStudentMapper;

/**
 * 班级Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseClassServiceImpl extends ServiceImpl<CourseClassMapper, CourseClass> implements CourseClassService {

    @Resource
    private CourseClassStudentMapper courseClassStudentMapper;

    @Override
    public Page<CourseClass> page(CourseClassPageParam courseClassPageParam) {
        QueryWrapper<CourseClass> queryWrapper = new QueryWrapper<CourseClass>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getSearchKey())) {
            queryWrapper.lambda().like(CourseClass::getName, courseClassPageParam.getSearchKey());
        }
        if (ObjectUtil.isNotEmpty(courseClassPageParam.getCourseId())) {
            queryWrapper.lambda().eq(CourseClass::getCourseId, courseClassPageParam.getCourseId());
        }
        // 数据范围：仅可见校区的班级
        List<String> visibleCampusIds = StpDataScopeUtil.visibleOrgIds();
        if (visibleCampusIds != null) {
            if (visibleCampusIds.isEmpty()) {
                queryWrapper.lambda().eq(CourseClass::getId, "__NO_DATA__");
            } else {
                CommonSqlUtil.safeIn(queryWrapper.lambda(), CourseClass::getCampusId, visibleCampusIds);
            }
        }
        if (ObjectUtil.isAllNotEmpty(courseClassPageParam.getSortField(), courseClassPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(courseClassPageParam.getSortOrder());
            queryWrapper.orderBy(true, courseClassPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(courseClassPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(CourseClass::getCreateTime);
        }
        Page<CourseClass> page = this.page(CommonPageRequest.defaultPage(), queryWrapper);
        fillStudentCount(page.getRecords());
        return page;
    }

    /**
     * 回填在读学员数（批量统计，避免逐个查询）
     */
    private void fillStudentCount(List<CourseClass> classList) {
        if (ObjectUtil.isEmpty(classList)) {
            return;
        }
        List<String> classIdList = classList.stream().map(CourseClass::getId).collect(Collectors.toList());
        Map<String, Long> countMap = courseClassStudentMapper.selectList(new LambdaQueryWrapper<CourseClassStudent>()
                        .in(CourseClassStudent::getClassId, classIdList)
                        .eq(CourseClassStudent::getStatus, CourseClassStudentStatusEnum.STUDYING.getValue()))
                .stream().collect(Collectors.groupingBy(CourseClassStudent::getClassId, Collectors.counting()));
        for (CourseClass courseClass : classList) {
            courseClass.setStudentCount(countMap.getOrDefault(courseClass.getId(), 0L).intValue());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseClassAddParam courseClassAddParam) {
        CourseClass courseClass = BeanUtil.toBean(courseClassAddParam, CourseClass.class);
        boolean repeatName = this.count(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getName, courseClass.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的班级：{}", courseClass.getName());
        }
        if (ObjectUtil.isEmpty(courseClass.getStatus())) {
            courseClass.setStatus(CourseStatusEnum.ENABLE.getValue());
        }
        this.save(courseClass);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CourseClassEditParam courseClassEditParam) {
        CourseClass courseClass = this.queryEntity(courseClassEditParam.getId());
        BeanUtil.copyProperties(courseClassEditParam, courseClass);
        boolean repeatName = this.count(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getName, courseClass.getName())
                .ne(CourseClass::getId, courseClass.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的班级：{}", courseClass.getName());
        }
        this.updateById(courseClass);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CourseClassIdParam> courseClassIdParamList) {
        List<String> courseClassIdList = CollStreamUtil.toList(courseClassIdParamList, CourseClassIdParam::getId);
        if (ObjectUtil.isNotEmpty(courseClassIdList)) {
            this.removeByIds(courseClassIdList);
        }
    }

    @Override
    public CourseClass detail(CourseClassIdParam courseClassIdParam) {
        return this.queryEntity(courseClassIdParam.getId());
    }

    @Override
    public CourseClass queryEntity(String id) {
        CourseClass courseClass = this.getById(id);
        if (ObjectUtil.isEmpty(courseClass)) {
            throw new CommonException("班级不存在，id值为：{}", id);
        }
        return courseClass;
    }
}
