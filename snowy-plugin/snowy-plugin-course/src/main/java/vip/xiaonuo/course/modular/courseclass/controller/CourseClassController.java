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
package vip.xiaonuo.course.modular.courseclass.controller;

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
import vip.xiaonuo.course.modular.courseclass.entity.CourseClass;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassAddParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassEditParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassIdParam;
import vip.xiaonuo.course.modular.courseclass.param.CourseClassPageParam;
import vip.xiaonuo.course.modular.courseclass.service.CourseClassService;

import cn.hutool.core.util.ObjectUtil;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 班级控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "班级控制器")
@RestController
@Validated
public class CourseClassController {

    @Resource
    private CourseClassService courseClassService;

    @Resource
    private vip.xiaonuo.sys.api.SysOrgApi sysOrgApi;

    /**
     * 获取班级分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取班级分页")
    @GetMapping("/course/class/page")
    public CommonResult<Page<CourseClass>> page(CourseClassPageParam courseClassPageParam) {
        return CommonResult.data(courseClassService.page(courseClassPageParam));
    }

    /**
     * 获取校区可选项（班级表单用，避免手填校区id）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "获取校区可选项")
    @GetMapping("/course/class/campusOptions")
    public CommonResult<List<java.util.Map<String, Object>>> campusOptions() {
        List<java.util.Map<String, Object>> optionList = new ArrayList<>();
        List<cn.hutool.json.JSONObject> orgList = sysOrgApi.orgTreeSelector("0", "");
        for (cn.hutool.json.JSONObject org : orgList) {
            collectCampus(org, optionList);
        }
        return CommonResult.data(optionList);
    }

    /**
     * 递归收集机构（校区）下拉项
     */
    private void collectCampus(cn.hutool.json.JSONObject org, List<java.util.Map<String, Object>> optionList) {
        String category = org.getStr("category");
        if ("DEPT".equals(category) || "CAMPUS".equals(category)) {
            java.util.Map<String, Object> option = new HashMap<>();
            option.put("value", org.getStr("id"));
            option.put("label", org.getStr("name"));
            optionList.add(option);
        }
        List<cn.hutool.json.JSONObject> children = org.getBeanList("children", cn.hutool.json.JSONObject.class);
        if (ObjectUtil.isNotEmpty(children)) {
            for (cn.hutool.json.JSONObject child : children) {
                collectCampus(child, optionList);
            }
        }
    }

    /**
     * 添加班级
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加班级")
    @CommonLog("添加班级")
    @PostMapping("/course/class/add")
    public CommonResult<String> add(@RequestBody @Valid CourseClassAddParam courseClassAddParam) {
        courseClassService.add(courseClassAddParam);
        return CommonResult.ok();
    }

    /**
     * 编辑班级
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "编辑班级")
    @CommonLog("编辑班级")
    @PostMapping("/course/class/edit")
    public CommonResult<String> edit(@RequestBody @Valid CourseClassEditParam courseClassEditParam) {
        courseClassService.edit(courseClassEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除班级
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "删除班级")
    @CommonLog("删除班级")
    @PostMapping("/course/class/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<CourseClassIdParam> courseClassIdParamList) {
        courseClassService.delete(courseClassIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取班级详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取班级详情")
    @GetMapping("/course/class/detail")
    public CommonResult<CourseClass> detail(@Valid CourseClassIdParam courseClassIdParam) {
        return CommonResult.data(courseClassService.detail(courseClassIdParam));
    }
}
