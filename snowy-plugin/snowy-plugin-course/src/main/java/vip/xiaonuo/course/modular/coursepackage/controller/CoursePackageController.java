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
package vip.xiaonuo.course.modular.coursepackage.controller;

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
import vip.xiaonuo.course.modular.coursepackage.entity.CoursePackage;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackageAddParam;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackageEditParam;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackageIdParam;
import vip.xiaonuo.course.modular.coursepackage.param.CoursePackagePageParam;
import vip.xiaonuo.course.modular.coursepackage.service.CoursePackageService;

import javax.validation.Valid;
import java.util.List;

/**
 * 课包控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "课包控制器")
@RestController
@Validated
public class CoursePackageController {

    @Resource
    private CoursePackageService coursePackageService;

    /**
     * 获取课包分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取课包分页")
    @GetMapping("/course/package/page")
    public CommonResult<Page<CoursePackage>> page(CoursePackagePageParam coursePackagePageParam) {
        return CommonResult.data(coursePackageService.page(coursePackagePageParam));
    }

    /**
     * 添加课包
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加课包")
    @CommonLog("添加课包")
    @PostMapping("/course/package/add")
    public CommonResult<String> add(@RequestBody @Valid CoursePackageAddParam coursePackageAddParam) {
        coursePackageService.add(coursePackageAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑课包
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "编辑课包")
    @CommonLog("编辑课包")
    @PostMapping("/course/package/edit")
    public CommonResult<String> edit(@RequestBody @Valid CoursePackageEditParam coursePackageEditParam) {
        coursePackageService.edit(coursePackageEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除课包
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "删除课包")
    @CommonLog("删除课包")
    @PostMapping("/course/package/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<CoursePackageIdParam> coursePackageIdParamList) {
        coursePackageService.delete(coursePackageIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取课包详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取课包详情")
    @GetMapping("/course/package/detail")
    public CommonResult<CoursePackage> detail(@Valid CoursePackageIdParam coursePackageIdParam) {
        return CommonResult.data(coursePackageService.detail(coursePackageIdParam));
    }
}
