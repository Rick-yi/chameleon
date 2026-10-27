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
package vip.xiaonuo.student.modular.studenttag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 学员标签实体
 * <p>
 * 标签是租户内共享的字典，学员与标签通过 STUDENT_TAG_REL 多对多关联。
 * 该表未登记到 GlobalConfigure 的租户表清单，服务层显式写入/过滤 TENANT_ID。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Getter
@Setter
@TableName("STUDENT_TAG")
public class StudentTag extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 标签名称 */
    @Schema(description = "标签名称")
    private String name;

    /** 标签颜色（antd tag color） */
    @Schema(description = "标签颜色")
    private String color;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortCode;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
