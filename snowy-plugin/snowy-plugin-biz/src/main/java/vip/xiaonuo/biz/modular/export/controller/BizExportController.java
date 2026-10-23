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
package vip.xiaonuo.biz.modular.export.controller;

import cn.hutool.core.io.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.xiaonuo.biz.modular.export.entity.BizExportTask;
import vip.xiaonuo.biz.modular.export.service.BizExportTaskService;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.common.util.CommonDownloadUtil;

import java.io.File;
import java.io.IOException;

/**
 * 异步导出控制器
 * <p>
 * 通用接口：各业务模块实现 BizExportBuilder 后即可复用创建/查询/下载三个接口。
 * 任务按租户隔离，下载只允许本租户任务。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/23 11:00
 **/
@Tag(name = "异步导出控制器")
@RestController
@Validated
public class BizExportController {

    @Resource
    private BizExportTaskService bizExportTaskService;

    /**
     * 创建异步导出任务
     */
    @Operation(summary = "创建异步导出任务")
    @PostMapping("/biz/export/async")
    public CommonResult<BizExportTask> create(@RequestBody ExportCreateParam exportCreateParam) {
        return CommonResult.data(bizExportTaskService.create(exportCreateParam.getType(), exportCreateParam.getParamJson()));
    }

    /**
     * 查询导出任务状态
     */
    @Operation(summary = "查询导出任务状态")
    @GetMapping("/biz/export/status")
    public CommonResult<BizExportTask> status(@RequestParam String id) {
        return CommonResult.data(bizExportTaskService.getTask(id));
    }

    /**
     * 下载导出文件
     */
    @Operation(summary = "下载导出文件")
    @GetMapping("/biz/export/download")
    public void download(@RequestParam String id, HttpServletResponse response) throws IOException {
        BizExportTask task = bizExportTaskService.getTask(id);
        if (!BizExportTaskService.STATUS_SUCCESS.equals(task.getStatus())) {
            throw new vip.xiaonuo.common.exception.CommonException("导出任务尚未完成，当前状态：{}", task.getStatus());
        }
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            throw new vip.xiaonuo.common.exception.CommonException("导出文件已过期，请重新导出");
        }
        CommonDownloadUtil.download(task.getFileName(), FileUtil.readBytes(file), response);
    }

    /**
     * 创建导出任务参数
     */
    @Getter
    @Setter
    public static class ExportCreateParam {

        /** 导出类型编码 */
        private String type;

        /** 查询参数（JSON 字符串，由各业务 Builder 解析） */
        private String paramJson;
    }
}
