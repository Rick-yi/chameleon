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
package vip.xiaonuo.course.modular.courseclassstudent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.course.modular.courseclassstudent.entity.CourseClassStudent;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentAddParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentIdParam;
import vip.xiaonuo.course.modular.courseclassstudent.param.CourseClassStudentPageParam;
import vip.xiaonuo.course.modular.courseclassstudent.service.CourseClassStudentService;

import javax.validation.Valid;
import java.util.List;

/**
 * 班级学员控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "班级学员控制器")
@RestController
@Validated
public class CourseClassStudentController {

    @Resource
    private CourseClassStudentService courseClassStudentService;

    /**
     * 获取班级学员分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取班级学员分页")
    @GetMapping("/course/classStudent/page")
    public CommonResult<Page<CourseClassStudent>> page(CourseClassStudentPageParam courseClassStudentPageParam) {
        return CommonResult.data(courseClassStudentService.page(courseClassStudentPageParam));
    }

    /**
     * 批量入班
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "批量入班")
    @CommonLog("批量入班")
    @PostMapping("/course/classStudent/add")
    public CommonResult<String> add(@RequestBody @Valid CourseClassStudentAddParam courseClassStudentAddParam) {
        courseClassStudentService.add(courseClassStudentAddParam);
        return CommonResult.ok();
    }

    /**
     * 退班
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "退班")
    @CommonLog("退班")
    @PostMapping("/course/classStudent/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<CourseClassStudentIdParam> courseClassStudentIdParamList) {
        courseClassStudentService.delete(courseClassStudentIdParamList);
        return CommonResult.ok();
    }
}
