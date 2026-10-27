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
package vip.xiaonuo.student.modular.studentstatus.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 学员状态变更记录实体
 * <p>
 * 仅追加不修改，记录每次状态流转的原因与操作人，复课也走同一条记录链路。
 * 该表未登记到全局租户表清单，服务层显式写入/过滤 TENANT_ID。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Getter
@Setter
@TableName("STUDENT_STATUS_LOG")
public class StudentStatusLog extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 变更前状态 */
    @Schema(description = "变更前状态")
    private String fromStatus;

    /** 变更后状态 */
    @Schema(description = "变更后状态")
    private String toStatus;

    /** 变更原因 */
    @Schema(description = "变更原因")
    private String reason;
}
