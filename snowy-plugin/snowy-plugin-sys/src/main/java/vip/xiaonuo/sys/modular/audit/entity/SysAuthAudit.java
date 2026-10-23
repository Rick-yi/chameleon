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
package vip.xiaonuo.sys.modular.audit.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 权限变更审计实体
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Getter
@Setter
@TableName("SYS_AUTH_AUDIT")
public class SysAuthAudit extends CommonEntity {

    /** 主键 */
    @TableId
    @Schema(description = "主键")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 操作人id */
    @Schema(description = "操作人id")
    private String operatorId;

    /** 操作人姓名 */
    @Schema(description = "操作人姓名")
    private String operatorName;

    /** 动作：ROLE_RESOURCE 角色菜单授权 / ROLE_PERMISSION 角色接口授权 / USER_ROLE 用户分配角色 */
    @Schema(description = "动作")
    private String action;

    /** 对象类型：ROLE / USER */
    @Schema(description = "对象类型")
    private String targetType;

    /** 对象id */
    @Schema(description = "对象id")
    private String targetId;

    /** 对象名称 */
    @Schema(description = "对象名称")
    private String targetName;

    /** 变更摘要 */
    @Schema(description = "变更摘要")
    private String summary;

    /** 变更明细（JSON） */
    @Schema(description = "变更明细")
    private String detail;
}
