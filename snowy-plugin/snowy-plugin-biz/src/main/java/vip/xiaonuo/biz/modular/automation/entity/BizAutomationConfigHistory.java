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
package vip.xiaonuo.biz.modular.automation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

/**
 * 自动化配置变更历史
 *
 * @author 会飞的猪
 * @date 2026/10/26 18:00
 **/
@Getter
@Setter
@TableName("BIZ_AUTOMATION_CONFIG_HISTORY")
public class BizAutomationConfigHistory extends CommonEntity {

    @TableId
    @Schema(description = "主键")
    private String id;

    @Schema(description = "租户id")
    private String tenantId;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "配置版本")
    private Integer version;

    @Schema(description = "变更前开关")
    private String oldEnabled;

    @Schema(description = "变更后开关")
    private String newEnabled;

    @Schema(description = "变更前参数")
    private String oldParams;

    @Schema(description = "变更后参数")
    private String newParams;

    @Schema(description = "操作人id")
    private String operatorId;
}
