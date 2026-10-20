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
package vip.xiaonuo.course.modular.courseschedulechange.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.course.modular.courseschedulechange.entity.CourseScheduleChange;
import vip.xiaonuo.course.modular.courseschedulechange.mapper.CourseScheduleChangeMapper;
import vip.xiaonuo.course.modular.courseschedulechange.param.CourseScheduleChangePageParam;
import vip.xiaonuo.course.modular.courseschedulechange.service.CourseScheduleChangeService;

/**
 * 调课记录Service接口实现类
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Service
public class CourseScheduleChangeServiceImpl extends ServiceImpl<CourseScheduleChangeMapper, CourseScheduleChange> implements CourseScheduleChangeService {

    @Override
    public Page<CourseScheduleChange> page(CourseScheduleChangePageParam courseScheduleChangePageParam) {
        QueryWrapper<CourseScheduleChange> queryWrapper = new QueryWrapper<CourseScheduleChange>().checkSqlInjection();
        queryWrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(courseScheduleChangePageParam.getFromScheduleId()), CourseScheduleChange::getFromScheduleId, courseScheduleChangePageParam.getFromScheduleId())
                .orderByDesc(CourseScheduleChange::getCreateTime);
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Override
    public void add(CourseScheduleChange courseScheduleChange) {
        this.save(courseScheduleChange);
    }
}
