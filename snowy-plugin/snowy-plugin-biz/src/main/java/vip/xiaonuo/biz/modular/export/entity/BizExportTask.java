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
package vip.xiaonuo.biz.modular.export.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.util.Date;

/**
 * 异步导出任务
 *
 * @author 会飞的猪
 * @date 2026/10/23 11:00
 **/
@Getter
@Setter
@TableName("BIZ_EXPORT_TASK")
public class BizExportTask extends CommonEntity {

    /** 主键 */
    @TableId
    @Schema(description = "主键")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 导出类型编码 */
    @Schema(description = "导出类型编码")
    private String exportType;

    /** 查询参数（JSON） */
    @Schema(description = "查询参数")
    private String paramJson;

    /** 状态：PENDING/RUNNING/SUCCESS/FAILED */
    @Schema(description = "状态")
    private String status;

    /** 下载文件名 */
    @Schema(description = "下载文件名")
    private String fileName;

    /** 文件路径 */
    @Schema(description = "文件路径")
    private String filePath;

    /** 数据行数 */
    @Schema(description = "数据行数")
    private Long rowCount;

    /** 失败原因 */
    @Schema(description = "失败原因")
    private String errorMsg;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private Date finishTime;
}
