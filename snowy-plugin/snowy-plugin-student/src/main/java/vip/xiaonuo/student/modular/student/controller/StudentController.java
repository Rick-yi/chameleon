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

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.student.modular.student.entity.Student;
import vip.xiaonuo.student.modular.student.dto.StudentTimelineItemDTO;
import vip.xiaonuo.student.modular.student.param.StudentAddParam;
import vip.xiaonuo.student.modular.student.param.StudentEditParam;
import vip.xiaonuo.student.modular.student.param.StudentIdParam;
import vip.xiaonuo.student.modular.student.param.StudentPageParam;
import vip.xiaonuo.student.modular.student.param.StudentQuickEnrollParam;
import vip.xiaonuo.student.modular.student.service.StudentEnrollService;
import vip.xiaonuo.student.modular.student.service.StudentImportExportService;
import vip.xiaonuo.student.modular.student.service.StudentService;
import vip.xiaonuo.student.modular.student.service.StudentTimelineService;
import vip.xiaonuo.student.modular.studentstatus.entity.StudentStatusLog;
import vip.xiaonuo.student.modular.studentstatus.param.StudentStatusChangeParam;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Resource
    private StudentEnrollService studentEnrollService;

    @Resource
    private StudentImportExportService studentImportExportService;

    @Resource
    private StudentTimelineService studentTimelineService;

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
     * 工作台快速录入学员：建档 + 报名 + 收款一次完成
     *
     * @author 会飞的猪
     * @date 2026/10/23 19:00
     */
    @Operation(summary = "快速录入学员")
    @CommonLog("快速录入学员")
    @SaCheckPermission("/student/add")
    @PostMapping("/student/quickEnroll")
    public CommonResult<java.util.Map<String, Object>> quickEnroll(
            @RequestBody @Valid StudentQuickEnrollParam studentQuickEnrollParam) {
        return CommonResult.data(studentEnrollService.quickEnroll(studentQuickEnrollParam));
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

    /**
     * 学员状态变更（停课/休学/退学/结课/复课）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "学员状态变更")
    @CommonLog("学员状态变更")
    @PostMapping("/student/status/change")
    public CommonResult<String> changeStatus(@RequestBody @Valid StudentStatusChangeParam param) {
        studentService.changeStatus(param);
        return CommonResult.ok();
    }

    /**
     * 学员状态变更记录
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "学员状态变更记录")
    @GetMapping("/student/status/logs")
    public CommonResult<List<StudentStatusLog>> statusLogs(@RequestParam String studentId) {
        return CommonResult.data(studentService.listStatusLogs(studentId));
    }

    /**
     * 下载学员导入模板
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "下载学员导入模板")
    @GetMapping(value = "/student/importTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        studentImportExportService.downloadTemplate(response);
    }

    /**
     * 批量导入学员
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "批量导入学员")
    @CommonLog("批量导入学员")
    @PostMapping("/student/import")
    public CommonResult<Map<String, Object>> importStudents(@RequestPart("file") MultipartFile file) {
        return CommonResult.data(studentImportExportService.importStudents(file));
    }

    /**
     * 导出学员
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "导出学员")
    @GetMapping(value = "/student/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportStudents(StudentPageParam studentPageParam, HttpServletResponse response) throws IOException {
        studentImportExportService.exportStudents(studentPageParam, response);
    }

    /**
     * 学员全部动态时间线（报名/收款/退费/消课/状态变更/跟进）
     *
     * @author 会飞的猪
     * @date 2026/10/27
     **/
    @Operation(summary = "学员动态时间线")
    @GetMapping("/student/timeline")
    public CommonResult<List<StudentTimelineItemDTO>> timeline(@RequestParam String studentId) {
        return CommonResult.data(studentTimelineService.timeline(studentId));
    }
}
