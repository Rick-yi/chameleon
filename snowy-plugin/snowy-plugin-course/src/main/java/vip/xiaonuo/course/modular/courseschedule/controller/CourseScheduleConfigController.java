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

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseScheduleClosure;
import vip.xiaonuo.course.modular.courseschedule.entity.CourseTeacherUnavailable;
import vip.xiaonuo.course.modular.courseschedule.service.CourseScheduleConfigService;

import java.util.Date;
import java.util.List;

/**
 * 排课配置控制器：停课日、教师不可用时段
 * <p>
 * 读写接口分别复用「排课分页」「手动排课」的权限码，避免为新增配置单独铺授权。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/23 14:00
 **/
@Tag(name = "排课配置控制器")
@RestController
@Validated
public class CourseScheduleConfigController {

    @Resource
    private CourseScheduleConfigService courseScheduleConfigService;

    /**
     * 停课日列表
     */
    @Operation(summary = "停课日列表")
    @SaCheckPermission("/course/schedule/page")
    @GetMapping("/course/schedule/closure/list")
    public CommonResult<List<CourseScheduleClosure>> closureList(@RequestParam String startDate,
                                                                 @RequestParam String endDate) {
        return CommonResult.data(courseScheduleConfigService.listClosures(
                cn.hutool.core.date.DateUtil.parse(startDate), cn.hutool.core.date.DateUtil.parse(endDate)));
    }

    /**
     * 新增停课日
     */
    @Operation(summary = "新增停课日")
    @CommonLog("新增停课日")
    @SaCheckPermission("/course/schedule/add")
    @PostMapping("/course/schedule/closure/add")
    public CommonResult<String> closureAdd(@RequestBody ClosureAddParam param) {
        courseScheduleConfigService.addClosure(param.getClosureDate(), param.getName(),
                param.getRemark(), param.getCampusId());
        return CommonResult.ok();
    }

    /**
     * 删除停课日
     */
    @Operation(summary = "删除停课日")
    @CommonLog("删除停课日")
    @SaCheckPermission("/course/schedule/add")
    @PostMapping("/course/schedule/closure/delete")
    public CommonResult<String> closureDelete(@RequestBody ConfigIdParam param) {
        courseScheduleConfigService.deleteClosure(param.getId());
        return CommonResult.ok();
    }

    /**
     * 教师不可用时段列表
     */
    @Operation(summary = "教师不可用时段列表")
    @SaCheckPermission("/course/schedule/page")
    @GetMapping("/course/schedule/teacherUnavailable/list")
    public CommonResult<List<CourseTeacherUnavailable>> unavailableList(@RequestParam(required = false) String teacherId,
                                                                        @RequestParam String startTime,
                                                                        @RequestParam String endTime) {
        return CommonResult.data(courseScheduleConfigService.listUnavailable(teacherId,
                cn.hutool.core.date.DateUtil.parse(startTime), cn.hutool.core.date.DateUtil.parse(endTime)));
    }

    /**
     * 新增教师不可用时段
     */
    @Operation(summary = "新增教师不可用时段")
    @CommonLog("新增教师不可用时段")
    @SaCheckPermission("/course/schedule/add")
    @PostMapping("/course/schedule/teacherUnavailable/add")
    public CommonResult<String> unavailableAdd(@RequestBody UnavailableAddParam param) {
        courseScheduleConfigService.addUnavailable(param.getTeacherId(), param.getStartTime(),
                param.getEndTime(), param.getReason());
        return CommonResult.ok();
    }

    /**
     * 删除教师不可用时段
     */
    @Operation(summary = "删除教师不可用时段")
    @CommonLog("删除教师不可用时段")
    @SaCheckPermission("/course/schedule/add")
    @PostMapping("/course/schedule/teacherUnavailable/delete")
    public CommonResult<String> unavailableDelete(@RequestBody ConfigIdParam param) {
        courseScheduleConfigService.deleteUnavailable(param.getId());
        return CommonResult.ok();
    }

    @Getter
    @Setter
    public static class ClosureAddParam {

        /** 停课日期 */
        private Date closureDate;

        /** 停课名称 */
        private String name;

        /** 备注 */
        private String remark;

        /** 校区id（为空表示全机构） */
        private String campusId;
    }

    @Getter
    @Setter
    public static class UnavailableAddParam {

        /** 教师id */
        private String teacherId;

        /** 开始时间 */
        private Date startTime;

        /** 结束时间 */
        private Date endTime;

        /** 原因 */
        private String reason;
    }

    @Getter
    @Setter
    public static class ConfigIdParam {

        /** 主键 */
        private String id;
    }
}
