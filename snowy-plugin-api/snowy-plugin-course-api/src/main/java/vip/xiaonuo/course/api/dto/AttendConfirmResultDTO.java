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
package vip.xiaonuo.course.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 点名确认结果
 * <p>
 * 单个学员课时不足时不再中断整班点名，而是跳过该学员消课并在此列出，前端可提示老师。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
@Getter
@Setter
public class AttendConfirmResultDTO {

    /** 本次点名人数 */
    private Integer totalCount = 0;

    /** 成功消课人数 */
    private Integer deductCount = 0;

    /** 未消课学员（课时不足/无账户/账户冻结） */
    private List<Skipped> skippedList = new ArrayList<>();

    /**
     * 未消课学员
     */
    @Getter
    @Setter
    public static class Skipped {
        /** 学员id */
        private String studentId;
        /** 学员姓名 */
        private String studentName;
        /** 未消课原因 */
        private String reason;

        public Skipped(String studentId, String studentName, String reason) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.reason = reason;
        }
    }
}
