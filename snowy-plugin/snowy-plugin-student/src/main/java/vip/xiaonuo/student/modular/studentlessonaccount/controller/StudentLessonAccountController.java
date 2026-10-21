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
package vip.xiaonuo.student.modular.studentlessonaccount.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.common.annotation.CommonLog;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.student.api.dto.LessonBalanceDTO;
import vip.xiaonuo.student.modular.studentlessonaccount.entity.StudentLessonAccount;
import vip.xiaonuo.student.modular.studentlessonaccount.param.StudentLessonAccountPageParam;
import vip.xiaonuo.student.modular.studentlessonaccount.param.StudentLessonAdjustParam;
import vip.xiaonuo.student.modular.studentlessonaccount.service.StudentLessonAccountService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课时账户控制器
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Tag(name = "课时账户控制器")
@RestController
@Validated
public class StudentLessonAccountController {

    @Resource
    private StudentLessonAccountService studentLessonAccountService;

    /**
     * 获取课时账户分页
     *
     * @author 会飞的猪
     * @date 2026/10/20 10:00
     */
    @Operation(summary = "获取课时账户分页")
    @GetMapping("/student/lessonAccount/page")
    public CommonResult<Page<StudentLessonAccount>> page(@Valid StudentLessonAccountPageParam studentLessonAccountPageParam) {
        return CommonResult.data(studentLessonAccountService.page(studentLessonAccountPageParam));
    }

    /**
     * 批量查询学员在指定课程下的课时余额
     * <p>
     * 课务域（点名、班级学员列表）用它展示「还剩多少课时」，避免每个页面各查一次。
     * </p>
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "批量查询课时余额")
    @GetMapping("/student/lessonAccount/balance")
    public CommonResult<List<LessonBalanceDTO>> balance(
            @RequestParam("studentIds") String studentIds, @RequestParam("courseId") String courseId) {
        List<String> studentIdList = StrUtil.split(studentIds, ',', true, true);
        return CommonResult.data(studentLessonAccountService.listByStudentAndCourse(studentIdList, courseId)
                .stream().map(account -> {
                    LessonBalanceDTO balance = new LessonBalanceDTO();
                    balance.setStudentId(account.getStudentId());
                    balance.setCourseId(account.getCourseId());
                    balance.setTotalLessons(account.getTotalLessons());
                    balance.setUsedLessons(account.getUsedLessons());
                    balance.setRemainLessons(account.getRemainLessons());
                    balance.setExpireTime(account.getExpireTime());
                    balance.setStatus(account.getStatus());
                    return balance;
                }).collect(Collectors.toList()));
    }

    /**
     * 人工调整课时
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     */
    @Operation(summary = "人工调整课时")
    @CommonLog("人工调整课时")
    @PostMapping("/student/lessonAccount/adjust")
    public CommonResult<String> adjust(@RequestBody @Valid StudentLessonAdjustParam studentLessonAdjustParam) {
        studentLessonAccountService.adjust(studentLessonAdjustParam);
        return CommonResult.ok();
    }
}
