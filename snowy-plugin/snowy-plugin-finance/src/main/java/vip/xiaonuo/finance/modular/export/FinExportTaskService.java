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
package vip.xiaonuo.finance.modular.export;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.context.TenantContext;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.util.CommonExcelUtil;
import vip.xiaonuo.finance.modular.export.mapper.FinExportTaskMapper;
import vip.xiaonuo.student.api.param.FinanceQueryParam;

import java.io.File;
import java.util.Date;

/**
 * 报表异步导出服务
 * <p>
 * 大数据量导出时先创建任务，由后台线程生成文件，前端轮询任务状态后再下载。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Slf4j
@Service
public class FinExportTaskService {

    /** 任务状态：待处理 */
    public static final String STATUS_PENDING = "PENDING";

    /** 任务状态：执行中 */
    public static final String STATUS_RUNNING = "RUNNING";

    /** 任务状态：成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 任务状态：失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 导出文件保留天数 */
    private static final int KEEP_DAYS = 3;

    @Resource
    private FinExportTaskMapper finExportTaskMapper;

    @Resource
    private FinanceExportRowBuilder financeExportRowBuilder;

    @Resource
    @Qualifier("finExportExecutor")
    private ThreadPoolTaskExecutor finExportExecutor;

    /**
     * 创建导出任务
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public FinExportTask create(String type, FinanceQueryParam param) {
        String tenantId = StrUtil.blankToDefault(TenantContext.getTenantId(), "0");
        FinExportTask task = new FinExportTask();
        task.setId(IdUtil.getSnowflakeNextIdStr());
        task.setTenantId(tenantId);
        task.setExportType(StrUtil.blankToDefault(type, "PAYMENT").toUpperCase());
        task.setParamJson(JSONUtil.toJsonStr(param));
        task.setStatus(STATUS_PENDING);
        finExportTaskMapper.insert(task);
        finExportExecutor.execute(() -> run(task));
        return task;
    }

    /**
     * 查询任务
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public FinExportTask get(String id) {
        FinExportTask task = finExportTaskMapper.selectById(id);
        if (ObjectUtil.isEmpty(task)) {
            throw new CommonException("导出任务不存在，id值为：{}", id);
        }
        return task;
    }

    /**
     * 我的导出记录（按租户隔离，可按导出类型过滤）
     *
     * @author 会飞的猪
     * @date 2026/10/26
     **/
    public Page<FinExportTask> pageTask(String exportType, Integer current, Integer size) {
        long pageNo = current == null || current < 1 ? 1L : current;
        long pageSize = size == null || size < 1 ? 20L : Math.min(size, 100);
        LambdaQueryWrapper<FinExportTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FinExportTask::getTenantId, StrUtil.blankToDefault(TenantContext.getTenantId(), "0"));
        if (StrUtil.isNotBlank(exportType)) {
            queryWrapper.eq(FinExportTask::getExportType, exportType.toUpperCase());
        }
        queryWrapper.orderByDesc(FinExportTask::getCreateTime);
        return finExportTaskMapper.selectPage(new Page<>(pageNo, pageSize), queryWrapper);
    }

    /**
     * 执行导出
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    private void run(FinExportTask task) {
        String originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(task.getTenantId());
            task.setStatus(STATUS_RUNNING);
            finExportTaskMapper.updateById(task);
            FinanceQueryParam param = JSONUtil.toBean(task.getParamJson(), FinanceQueryParam.class);
            FinanceExportRowBuilder.ExportPayload payload = financeExportRowBuilder.build(task.getExportType(), param);
            if (payload.rowList().isEmpty()) {
                throw new CommonException("无数据可导出");
            }
            File dir = exportDir();
            FileUtil.mkdir(dir);
            File file = new File(dir, task.getId() + ".xlsx");
            CommonExcelUtil.write(file, payload.sheetName(), payload.headList(), payload.rowList());
            task.setStatus(STATUS_SUCCESS);
            task.setFilePath(file.getAbsolutePath());
            task.setFileName(payload.sheetName() + "_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
            task.setRowCount((long) payload.rowList().size());
            task.setFinishTime(new Date());
            finExportTaskMapper.updateById(task);
        } catch (Exception e) {
            log.error(">>> 报表异步导出失败，任务id：{}", task.getId(), e);
            task.setStatus(STATUS_FAILED);
            task.setErrorMsg(e.getMessage());
            task.setFinishTime(new Date());
            finExportTaskMapper.updateById(task);
        } finally {
            if (StrUtil.isNotBlank(originalTenantId)) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
            cleanExpiredFile();
        }
    }

    /**
     * 导出文件目录
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public File exportDir() {
        return new File(System.getProperty("user.dir") + File.separator + "data" + File.separator + "chameleon-export");
    }

    /**
     * 清理过期导出文件
     *
     * @author 会飞的猪
 * @date 2026/10/21 10:00
     **/
    private void cleanExpiredFile() {
        File dir = exportDir();
        if (!dir.exists()) {
            return;
        }
        Date deadline = DateUtil.offsetDay(new Date(), -KEEP_DAYS);
        File[] fileList = dir.listFiles();
        if (fileList == null) {
            return;
        }
        for (File file : fileList) {
            if (file.isFile() && file.getName().endsWith(".xlsx") && file.lastModified() < deadline.getTime()) {
                FileUtil.del(file);
            }
        }
    }
}
