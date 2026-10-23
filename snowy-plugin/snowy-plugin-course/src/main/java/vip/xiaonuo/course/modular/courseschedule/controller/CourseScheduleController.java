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
package vip.xiaonuo.course.modular.courseschedule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.course.api.dto.CourseScheduleViewDTO;
import vip.xiaonuo.course.api.dto.CourseScheduleGenerateResultDTO;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseSchedule;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAddParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleAdhocParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleChangeParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleGenerateParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleIdParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseSchedulePageParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleSuggestParam;
import vip.xiaonuo.course.modular.courseschedule.param.CourseScheduleWeekParam;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleExportService;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleService;

import javax.validation.Valid;
import java.util.List;

/**
 * 排课控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "排课控制器")
@RestController
@Validated
public class CourseScheduleController {

    @Resource
    private CourseScheduleService courseScheduleService;

    @Resource
    private CourseScheduleExportService courseScheduleExportService;

    /**
     * 获取排课分页（周视图）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取排课分页")
    @GetMapping("/course/schedule/page")
    public CommonResult<Page<CourseSchedule>> page(CourseSchedulePageParam courseSchedulePageParam) {
        return CommonResult.data(courseScheduleService.page(courseSchedulePageParam));
    }

    /**
     * 手动排课
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "手动排课")
    @CommonLog("手动排课")
    @PostMapping("/course/schedule/add")
    public CommonResult<String> add(@RequestBody @Valid CourseScheduleAddParam courseScheduleAddParam) {
        courseScheduleService.add(courseScheduleAddParam);
        return CommonResult.ok();
    }

    /**
     * 按周模板批量生成排课
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "按周模板批量生成排课")
    @CommonLog("按周模板批量生成排课")
    @PostMapping("/course/schedule/generate")
    public CommonResult<CourseScheduleGenerateResultDTO> generate(@RequestBody @Valid CourseScheduleGenerateParam courseScheduleGenerateParam) {
        return CommonResult.data(courseScheduleService.generate(courseScheduleGenerateParam));
    }

    /**
     * 课表区间查询（不分页）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "课表区间查询")
    @GetMapping("/course/schedule/week")
    public CommonResult<List<CourseScheduleViewDTO>> week(@Valid CourseScheduleWeekParam courseScheduleWeekParam) {
        return CommonResult.data(courseScheduleService.week(courseScheduleWeekParam));
    }

    /**
     * 推荐可排课时段
     * <p>
     * 按班级/教师/教室占用计算空闲时段，优先推荐与班级固定课表一致的星期与时间，
     * 供「手动排课」弹窗直接点选，减少反复试错。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/23 10:00
     */
    @SaCheckPermission("/course/schedule/suggest")
    @Operation(summary = "推荐可排课时段")
    @GetMapping("/course/schedule/suggest")
    public CommonResult<List<vip.xiaonuo.course.api.dto.CourseScheduleSuggestDTO>> suggest(
            CourseScheduleSuggestParam courseScheduleSuggestParam) {
        return CommonResult.data(courseScheduleService.suggest(courseScheduleSuggestParam));
    }

    /**
     * 教师可选项
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "教师可选项")
    @GetMapping("/course/schedule/teacherOptions")
    public CommonResult<List<JSONObject>> teacherOptions(@RequestParam(required = false) String searchKey) {
        return CommonResult.data(courseScheduleService.teacherOptions(searchKey));
    }

    /**
     * 导出课表（Excel：课表清单 + 周视图）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "导出课表")
    @GetMapping("/course/schedule/export")
    public void export(CourseScheduleWeekParam courseScheduleWeekParam, HttpServletResponse response) throws IOException {
        courseScheduleExportService.export(courseScheduleWeekParam, response);
    }

    /**
     * 调课
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    /**
     * 直接排课（不挂班级：老师带一群学生上课）
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "直接排课（不挂班级）")
    @CommonLog("直接排课")
    @PostMapping("/course/schedule/generateAdhoc")
    public CommonResult<CourseScheduleGenerateResultDTO> generateAdhoc(
            @RequestBody @Valid CourseScheduleAdhocParam courseScheduleAdhocParam) {
        return CommonResult.data(courseScheduleService.generateAdhoc(courseScheduleAdhocParam));
    }

    @PostMapping("/course/schedule/change")
    public CommonResult<String> change(@RequestBody @Valid CourseScheduleChangeParam courseScheduleChangeParam) {
        courseScheduleService.change(courseScheduleChangeParam);
        return CommonResult.ok();
    }

    /**
     * 取消排课
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "取消排课")
    @CommonLog("取消排课")
    @PostMapping("/course/schedule/cancel")
    public CommonResult<String> cancel(@RequestBody @Valid CourseScheduleIdParam courseScheduleIdParam) {
        courseScheduleService.cancel(courseScheduleIdParam);
        return CommonResult.ok();
    }

    /**
     * 获取排课详情
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取排课详情")
    @GetMapping("/course/schedule/detail")
    public CommonResult<CourseSchedule> detail(@Valid CourseScheduleIdParam courseScheduleIdParam) {
        return CommonResult.data(courseScheduleService.detail(courseScheduleIdParam));
    }

    /**
     * 冲突检测（前端即时校验用）
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "冲突检测")
    @GetMapping("/course/schedule/conflictCheck")
    public CommonResult<List<String>> conflictCheck(@Valid CourseScheduleAddParam courseScheduleAddParam) {
        return CommonResult.data(courseScheduleService.conflictCheck(courseScheduleAddParam.getClassId(),
                courseScheduleAddParam.getScheduleDate(), courseScheduleAddParam.getStartTime(),
                courseScheduleAddParam.getEndTime(), courseScheduleAddParam.getClassroom(),
                courseScheduleAddParam.getTeacherId(), null));
    }
}
