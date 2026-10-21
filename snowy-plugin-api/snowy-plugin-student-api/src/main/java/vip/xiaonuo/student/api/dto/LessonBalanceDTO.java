/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时需要注意以下几点：
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@163.com商议合作。
 * 6.如需更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为：https://www.xiaonuo.vip
 */
package vip.xiaonuo.student.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 学员课时余额（跨插件调用用）
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class LessonBalanceDTO {

    /** 学员id */
    private String studentId;

    /** 课程id */
    private String courseId;

    /** 总课时 */
    private BigDecimal totalLessons;

    /** 已用课时 */
    private BigDecimal usedLessons;

    /** 剩余课时 */
    private BigDecimal remainLessons;

    /** 课包到期时间 */
    private Date expireTime;

    /** 账户状态：NORMAL 正常 / FROZEN 冻结 / CLEARED 已清零 */
    private String status;
}
