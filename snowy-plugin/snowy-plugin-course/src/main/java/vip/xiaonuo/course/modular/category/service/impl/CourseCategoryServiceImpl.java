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
package vip.xiaonuo.course.modular.category.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.course.modular.category.entity.CourseCategory;
import vip.xiaonuo.course.modular.category.mapper.CourseCategoryMapper;
import vip.xiaonuo.course.modular.category.param.CourseCategoryAddParam;
import vip.xiaonuo.course.modular.category.param.CourseCategoryEditParam;
import vip.xiaonuo.course.modular.category.param.CourseCategoryIdParam;
import vip.xiaonuo.course.modular.category.service.CourseCategoryService;

import java.util.List;

/**
 * 课程大类Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Override
    public List<CourseCategory> list() {
        return this.list(new LambdaQueryWrapper<CourseCategory>().orderByAsc(CourseCategory::getSortCode));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CourseCategoryAddParam courseCategoryAddParam) {
        CourseCategory courseCategory = BeanUtil.toBean(courseCategoryAddParam, CourseCategory.class);
        boolean repeatName = this.count(new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getName, courseCategory.getName())
                .eq(ObjectUtil.isNotEmpty(courseCategory.getParentId()), CourseCategory::getParentId, courseCategory.getParentId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的课程大类：{}", courseCategory.getName());
        }
        this.save(courseCategory);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CourseCategoryEditParam courseCategoryEditParam) {
        CourseCategory courseCategory = this.queryEntity(courseCategoryEditParam.getId());
        BeanUtil.copyProperties(courseCategoryEditParam, courseCategory);
        boolean repeatName = this.count(new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getName, courseCategory.getName())
                .eq(ObjectUtil.isNotEmpty(courseCategory.getParentId()), CourseCategory::getParentId, courseCategory.getParentId())
                .ne(CourseCategory::getId, courseCategory.getId())) > 0;
        if (repeatName) {
            throw new CommonException("存在重复的课程大类：{}", courseCategory.getName());
        }
        this.updateById(courseCategory);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CourseCategoryIdParam> courseCategoryIdParamList) {
        List<String> courseCategoryIdList = CollStreamUtil.toList(courseCategoryIdParamList, CourseCategoryIdParam::getId);
        if (ObjectUtil.isNotEmpty(courseCategoryIdList)) {
            this.removeByIds(courseCategoryIdList);
        }
    }

    @Override
    public CourseCategory detail(CourseCategoryIdParam courseCategoryIdParam) {
        return this.queryEntity(courseCategoryIdParam.getId());
    }

    @Override
    public CourseCategory queryEntity(String id) {
        CourseCategory courseCategory = this.getById(id);
        if (ObjectUtil.isEmpty(courseCategory)) {
            throw new CommonException("课程大类不存在，id值为：{}", id);
        }
        return courseCategory;
    }
}
