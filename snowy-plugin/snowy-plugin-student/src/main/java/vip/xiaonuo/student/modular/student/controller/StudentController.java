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
package vip.xiaonuo.student.modular.student.controller;

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
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentEditParam;
import vip.xiaonuo.student.modular.student.param.StudentIdParam;
import vip.xiaonuo.student.modular.student.param.StudentPageParam;
import vip.xiaonuo.student.modular.student.service.StudentService;

import javax.validation.Valid;
import java.util.List;

/**
 * 学员控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "学员控制器")
@RestController
@Validated
public class StudentController {

    @Resource
    private StudentService studentService;

    /**
     * 获取学员分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取学员分页")
    @GetMapping("/student/page")
    public CommonResult<Page<Student>> page(StudentPageParam studentPageParam) {
        return CommonResult.data(studentService.page(studentPageParam));
    }

    /**
     * 添加学员
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "添加学员")
    @CommonLog("添加学员")
    @PostMapping("/student/add")
    public CommonResult<String> add(@RequestBody @Valid StudentAddParam studentAddParam) {
        return CommonResult.data(studentService.add(studentAddParam));
    }

    /**
     * 编辑学员
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "编辑学员")
    @CommonLog("编辑学员")
    @PostMapping("/student/edit")
    public CommonResult<String> edit(@RequestBody @Valid StudentEditParam studentEditParam) {
        studentService.edit(studentEditParam);
        return CommonResult.ok();
    }

    /**
     * 删除学员
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "删除学员")
    @CommonLog("删除学员")
    @PostMapping("/student/delete")
    public CommonResult<String> delete(@RequestBody @Valid @NotEmpty(message = "集合不能为空")
                                       List<StudentIdParam> studentIdParamList) {
        studentService.delete(studentIdParamList);
        return CommonResult.ok();
    }

    /**
     * 获取学员详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取学员详情")
    @GetMapping("/student/detail")
    public CommonResult<Student> detail(@Valid StudentIdParam studentIdParam) {
        return CommonResult.data(studentService.detail(studentIdParam));
    }
}
