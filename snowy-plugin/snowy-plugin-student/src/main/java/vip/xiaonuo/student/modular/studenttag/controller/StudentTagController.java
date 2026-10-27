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
package vip.xiaonuo.student.modular.studenttag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.student.modular.studenttag.entity.StudentTag;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagAddParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagBindParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagEditParam;
import vip.xiaonuo.student.modular.studenttag.param.StudentTagIdParam;
import vip.xiaonuo.student.modular.studenttag.service.StudentTagService;

import javax.validation.Valid;
import java.util.List;

/**
 * 学员标签控制器
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Tag(name = "学员标签控制器")
@RestController
@Validated
public class StudentTagController {

    @Resource
    private StudentTagService studentTagService;

    /**
     * 标签字典
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "获取学员标签列表")
    @GetMapping("/student/tag/list")
    public CommonResult<List<StudentTag>> list() {
        return CommonResult.data(studentTagService.listTags());
    }

    /**
     * 某学员的标签
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "获取学员已绑定标签")
    @GetMapping("/student/tag/studentTags")
    public CommonResult<List<StudentTag>> studentTags(@RequestParam String studentId) {
        return CommonResult.data(studentTagService.listByStudent(studentId));
    }

    /**
     * 新增标签
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "新增学员标签")
    @CommonLog("新增学员标签")
    @PostMapping("/student/tag/add")
    public CommonResult<String> add(@RequestBody @Valid StudentTagAddParam param) {
        return CommonResult.data(studentTagService.add(param));
    }

    /**
     * 编辑标签
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "编辑学员标签")
    @CommonLog("编辑学员标签")
    @PostMapping("/student/tag/edit")
    public CommonResult<String> edit(@RequestBody @Valid StudentTagEditParam param) {
        studentTagService.edit(param);
        return CommonResult.ok();
    }

    /**
     * 删除标签
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "删除学员标签")
    @CommonLog("删除学员标签")
    @PostMapping("/student/tag/delete")
    public CommonResult<String> delete(@RequestBody @Valid StudentTagIdParam param) {
        studentTagService.delete(param);
        return CommonResult.ok();
    }

    /**
     * 绑定学员标签（全量覆盖）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "绑定学员标签")
    @CommonLog("绑定学员标签")
    @PostMapping("/student/tag/bind")
    public CommonResult<String> bind(@RequestBody @Valid StudentTagBindParam param) {
        studentTagService.bind(param);
        return CommonResult.ok();
    }
}
