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
package vip.xiaonuo.course.modular.courseclassattend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import vip.xiaonuo.common.pojo.CommonEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 点名记录实体
 *
 * @author 会飞的猪
 * @date 2026/10/20 10:00
 **/
@Getter
@Setter
@TableName("COURSE_CLASS_ATTEND")
public class CourseClassAttend extends CommonEntity {

    /** id */
    @Schema(description = "id")
    private String id;

    /** 租户id */
    @Schema(description = "租户id")
    private String tenantId;

    /** 排课id */
    @Schema(description = "排课id")
    private String scheduleId;

    /** 学员id */
    @Schema(description = "学员id")
    private String studentId;

    /** 出勤状态 */
    @Schema(description = "出勤状态(到课/请假/缺勤)")
    private String attendStatus;

    /** 扣除课时数 */
    @Schema(description = "扣除课时数")
    private BigDecimal lessonsDeducted;

    /** 点名操作人 */
    @Schema(description = "点名操作人")
    private String attendUser;

    /** 备注（未消课时记录原因） */
    @Schema(description = "备注")
    private String remark;

    /** 点名时间 */
    @Schema(description = "点名时间")
    private Date attendTime;
}
