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
package vip.xiaonuo.biz.modular.export.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import vip.xiaonuo.biz.api.export.BizExportBuilder;
import vip.xiaonuo.biz.api.export.BizExportSheet;
import vip.xiaonuo.biz.modular.export.entity.BizExportTask;
import vip.xiaonuo.biz.modular.export.mapper.BizExportTaskMapper;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonExcelUtil;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步导出服务
 * <p>
 * 大数据量导出时先建任务，由后台线程生成文件，前端轮询状态后下载。
 * 各业务模块只需实现 {@link BizExportBuilder} 并注册为 Spring Bean。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/23 11:00
 **/
@Slf4j
@Service
public class BizExportTaskService extends ServiceImpl<BizExportTaskMapper, BizExportTask> {

    public static final String STATUS_PENDING = "PENDING";

    public static final String STATUS_RUNNING = "RUNNING";

    public static final String STATUS_SUCCESS = "SUCCESS";

    public static final String STATUS_FAILED = "FAILED";

    /** 导出文件保留天数 */
    private static final int KEEP_DAYS = 3;

    @Resource
    private List<BizExportBuilder> builderList;

    @Resource
    @Qualifier("bizExportExecutor")
    private ThreadPoolTaskExecutor bizExportExecutor;

    private Map<String, BizExportBuilder> builderMap() {
        Map<String, BizExportBuilder> map = new HashMap<>();
        if (ObjectUtil.isNotEmpty(builderList)) {
            builderList.forEach(builder -> map.put(builder.type().toUpperCase(), builder));
        }
        return map;
    }

    /**
     * 创建导出任务并异步执行
     */
    public BizExportTask create(String type, String paramJson) {
        BizExportBuilder builder = builderMap().get(StrUtil.blankToDefault(type, "").toUpperCase());
        if (builder == null) {
            throw new CommonException("不支持的导出类型：{}", type);
        }
        BizExportTask task = new BizExportTask();
        task.setId(IdUtil.getSnowflakeNextIdStr());
        task.setTenantId(StrUtil.blankToDefault(TenantContext.getTenantId(), "0"));
        task.setExportType(builder.type());
        task.setParamJson(paramJson);
        task.setStatus(STATUS_PENDING);
        this.save(task);
        bizExportExecutor.execute(() -> run(task, builder));
        return task;
    }

    /**
     * 查询任务（租户拦截器保证只能查本租户）
     */
    public BizExportTask getTask(String id) {
        BizExportTask task = this.getById(id);
        if (ObjectUtil.isEmpty(task)) {
            throw new CommonException("导出任务不存在，id值为：{}", id);
        }
        return task;
    }

    /** 导出文件目录 */
    public File exportDir() {
        return new File(System.getProperty("user.dir") + File.separator + "data" + File.separator + "chameleon-export");
    }

    private void run(BizExportTask task, BizExportBuilder builder) {
        String originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(task.getTenantId());
            task.setStatus(STATUS_RUNNING);
            this.updateById(task);
            List<BizExportSheet> sheetList = builder.build(task.getParamJson());
            if (ObjectUtil.isEmpty(sheetList)) {
                throw new CommonException("无数据可导出");
            }
            long rowCount = sheetList.stream()
                    .mapToLong(sheet -> sheet.getRowList() == null ? 0 : sheet.getRowList().size()).sum();
            if (rowCount == 0) {
                throw new CommonException("无数据可导出");
            }
            File dir = exportDir();
            FileUtil.mkdir(dir);
            File file = new File(dir, task.getId() + ".xlsx");
            CommonExcelUtil.writeMulti(file, sheetList.stream()
                    .map(sheet -> new CommonExcelUtil.Sheet(sheet.getSheetName(), sheet.getHeadList(), sheet.getRowList()))
                    .toList());
            task.setStatus(STATUS_SUCCESS);
            task.setFilePath(file.getAbsolutePath());
            task.setFileName(builder.title() + "_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
            task.setRowCount(rowCount);
            task.setFinishTime(new Date());
            this.updateById(task);
        } catch (Exception e) {
            log.error(">>> 异步导出失败，任务id：{}", task.getId(), e);
            task.setStatus(STATUS_FAILED);
            task.setErrorMsg(StrUtil.maxLength(e.getMessage(), 480));
            task.setFinishTime(new Date());
            this.updateById(task);
        } finally {
            if (StrUtil.isNotBlank(originalTenantId)) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
            cleanExpiredFile();
        }
    }

    private void cleanExpiredFile() {
        File dir = exportDir();
        if (!dir.exists()) {
            return;
        }
        File[] fileList = dir.listFiles();
        if (fileList == null) {
            return;
        }
        Date deadline = DateUtil.offsetDay(new Date(), -KEEP_DAYS);
        for (File file : fileList) {
            if (file.isFile() && file.getName().endsWith(".xlsx") && file.lastModified() < deadline.getTime()) {
                FileUtil.del(file);
            }
        }
    }
}
