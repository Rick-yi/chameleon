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
package vip.xiaonuo.course.modular.coursepackage.service.impl;

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
import vip.xiaonuo.course.modular.coursepackage.entity.CoursePackage;
import vip.xiaonuo.course.modular.coursepackage.enums.CoursePackageStatusEnum;
import vip.xiaonuo.course.modular.coursepackage.mapper.CoursePackageMapper;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackageAddParam;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackageEditParam;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackageIdParam;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackagePageParam;
import vip.xiaonuo.course.modular.coursepackage.service.CoursePackageService;

import java.util.List;

/**
 * 课包Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CoursePackageServiceImpl extends ServiceImpl<CoursePackageMapper, CoursePackage> implements CoursePackageService {

    @Override
    public Page<CoursePackage> page(CoursePackagePageParam coursePackagePageParam) {
        QueryWrapper<CoursePackage> queryWrapper = new QueryWrapper<CoursePackage>().checkSqlInjection();
        if (ObjectUtil.isNotEmpty(coursePackagePageParam.getSearchKey())) {
            queryWrapper.lambda().like(CoursePackage::getName, coursePackagePageParam.getSearchKey());
        }
        if (ObjectUtil.isNotEmpty(coursePackagePageParam.getCourseId())) {
            queryWrapper.lambda().eq(CoursePackage::getCourseId, coursePackagePageParam.getCourseId());
        }
        if (ObjectUtil.isAllNotEmpty(coursePackagePageParam.getSortField(), coursePackagePageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(coursePackagePageParam.getSortOrder());
            queryWrapper.orderBy(true, coursePackagePageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(coursePackagePageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(CoursePackage::getCreateTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CoursePackageAddParam coursePackageAddParam) {
        CoursePackage coursePackage = BeanUtil.toBean(coursePackageAddParam, CoursePackage.class);
        boolean repeatName = this.count(new LambdaQueryWrapper<CoursePackage>()
                .eq(CoursePackage::getCourseId, coursePackage.getCourseId())
                .eq(CoursePackage::getName, coursePackage.getName())) > 0;
        if (repeatName) {
            throw new CommonException("该课程下存在重复的课包：{}", coursePackage.getName());
        }
        if (ObjectUtil.isEmpty(coursePackage.getStatus())) {
            coursePackage.setStatus(CoursePackageStatusEnum.ENABLE.getValue());
        }
        this.save(coursePackage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CoursePackageEditParam coursePackageEditParam) {
        CoursePackage coursePackage = this.queryEntity(coursePackageEditParam.getId());
        BeanUtil.copyProperties(coursePackageEditParam, coursePackage);
        boolean repeatName = this.count(new LambdaQueryWrapper<CoursePackage>()
                .eq(CoursePackage::getCourseId, coursePackage.getCourseId())
                .eq(CoursePackage::getName, coursePackage.getName())
                .ne(CoursePackage::getId, coursePackage.getId())) > 0;
        if (repeatName) {
            throw new CommonException("该课程下存在重复的课包：{}", coursePackage.getName());
        }
        this.updateById(coursePackage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<CoursePackageIdParam> coursePackageIdParamList) {
        List<String> coursePackageIdList = CollStreamUtil.toList(coursePackageIdParamList, CoursePackageIdParam::getId);
        if (ObjectUtil.isNotEmpty(coursePackageIdList)) {
            this.removeByIds(coursePackageIdList);
        }
    }

    @Override
    public CoursePackage detail(CoursePackageIdParam coursePackageIdParam) {
        return this.queryEntity(coursePackageIdParam.getId());
    }

    @Override
    public CoursePackage queryEntity(String id) {
        CoursePackage coursePackage = this.getById(id);
        if (ObjectUtil.isEmpty(coursePackage)) {
            throw new CommonException("课包不存在，id值为：{}", id);
        }
        return coursePackage;
    }
}
