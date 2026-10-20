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
package vip.xiaonuo.course.modular.course.service.impl;

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
import vip.xiaonuo.course.modular.course.entity.Course;
import vip.xiaonuo.course.modular.course.enums.CourseStatusEnum;
import vip.xiaonuo.course.modular.course.mapper.CourseMapper;
import vip.xiaonuo.course.modular.course.param.CourseAddParam;
import vip.xiaonuo.course.modular.course.param.CourseEditParam;
import vip.xiaonuo.course.modular.course.param.CourseIdParam;
import vip.xiaonuo.course.modular.course.param.CoursePageParam;
import vip.xiaonuo.course.modular.course.service.CourseService;

import java.util.List;

/**
 * 课程Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Override
    public Page<Course> page(CoursePageParam coursePageParam) {
        QueryWrapper<Course> queryWrapper = new QueryWrapper<Course>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(coursePageParam.getSearchKey())) {
            queryWrapper.lambda().like(Course::getName, coursePageParam.getSearchKey());
        }
        if (ObjectUtil.isNotEmpty(coursePageParam.getCategoryId())) {
            queryWrapper.lambda().eq(Course::getCategoryId, coursePageParam.getCategoryId());
        }
        if (ObjectUtil.isAllNotEmpty(coursePageParam.getSortField(), coursePageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(coursePageParam.getSortOrder());
            queryWrapper.orderBy(true, coursePageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(coursePageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(Course::getCreateTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseAddParam courseAddParam) {
        Course course = BeanUtil.toBean(courseAddParam, Course.class);
        boolean repeatName = this.count(new LambdaQueryWrapper<Course>().eq(Course::getName, course.getName())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的课程：{}", course.getName());
        }
        if (ObjectUtil.isEmpty(course.getStatus())) {
            course.setStatus(CourseStatusEnum.ENABLE.getValue());
        }
        this.save(course);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CourseEditParam courseEditParam) {
        Course course = this.queryEntity(courseEditParam.getId());
        BeanUtil.copyProperties(courseEditParam, course);
        boolean repeatName = this.count(new LambdaQueryWrapper<Course>()
                .eq(Course::getName, course.getName()).ne(Course::getId, course.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的课程：{}", course.getName());
        }
        this.updateById(course);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CourseIdParam> courseIdParamList) {
        List<String> courseIdList = CollStreamUtil.toList(courseIdParamList, CourseIdParam::getId);
        if (ObjectUtil.isNotEmpty(courseIdList)) {
            this.removeByIds(courseIdList);
        }
    }

    @Override
    public Course detail(CourseIdParam courseIdParam) {
        return this.queryEntity(courseIdParam.getId());
    }

    @Override
    public Course queryEntity(String id) {
        Course course = this.getById(id);
        if (ObjectUtil.isEmpty(course)) {
            throw new CommonException("课程不存在，id值为：{}", id);
        }
        return course;
    }
}
