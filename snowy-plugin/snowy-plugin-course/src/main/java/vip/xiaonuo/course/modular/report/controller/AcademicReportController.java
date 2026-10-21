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
package vip.xiaonuo.course.modular.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.course.api.dto.AcademicAttendanceDTO;
import vip.xiaonuo.course.api.dto.AcademicClassFillDTO;
import vip.xiaonuo.course.api.dto.AcademicTeacherWorkloadDTO;
import vip.xiaonuo.course.modular.report.service.AcademicReportService;
import vip.xiaonuo.student.api.dto.AcademicLessonProgressDTO;
import vip.xiaonuo.student.api.dto.AcademicRiskStudentDTO;
import vip.xiaonuo.student.api.param.AcademicReportQueryParam;

import java.io.IOException;
import java.util.List;

/**
 * 教务报表控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "教务报表控制器")
@RestController
@Validated
public class AcademicReportController {

    @Resource
    private AcademicReportService academicReportService;

    /**
     * 出勤汇总
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "出勤汇总")
    @SaCheckPermission("/course/report/academic/attendance")
    @GetMapping("/course/report/academic/attendance")
    public CommonResult<AcademicAttendanceDTO> attendance(AcademicReportQueryParam param) {
        return CommonResult.data(academicReportService.attendance(param));
    }

    /**
     * 班级满班率
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "班级满班率")
    @SaCheckPermission("/course/report/academic/classFill")
    @GetMapping("/course/report/academic/classFill")
    public CommonResult<List<AcademicClassFillDTO>> classFill(AcademicReportQueryParam param) {
        return CommonResult.data(academicReportService.classFill(param));
    }

    /**
     * 教师课时
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "教师课时")
    @SaCheckPermission("/course/report/academic/teacherWorkload")
    @GetMapping("/course/report/academic/teacherWorkload")
    public CommonResult<List<AcademicTeacherWorkloadDTO>> teacherWorkload(AcademicReportQueryParam param) {
        return CommonResult.data(academicReportService.teacherWorkload(param));
    }

    /**
     * 学员课消进度
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "学员课消进度")
    @SaCheckPermission("/course/report/academic/lessonProgress")
    @GetMapping("/course/report/academic/lessonProgress")
    public CommonResult<List<AcademicLessonProgressDTO>> lessonProgress(AcademicReportQueryParam param) {
        return CommonResult.data(academicReportService.lessonProgress(param));
    }

    /**
     * 风险学员
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "风险学员")
    @SaCheckPermission("/course/report/academic/riskStudents")
    @GetMapping("/course/report/academic/riskStudents")
    public CommonResult<List<AcademicRiskStudentDTO>> riskStudents(AcademicReportQueryParam param) {
        return CommonResult.data(academicReportService.riskStudents(param));
    }

    /**
     * 导出教务报表
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "导出教务报表")
    @SaCheckPermission("/course/report/academic/export")
    @GetMapping("/course/report/academic/export")
    public void export(AcademicReportQueryParam param,
                       @RequestParam(required = false, defaultValue = "ATTENDANCE") String type,
                       HttpServletResponse response) throws IOException {
        academicReportService.export(param, type, response);
    }
}
