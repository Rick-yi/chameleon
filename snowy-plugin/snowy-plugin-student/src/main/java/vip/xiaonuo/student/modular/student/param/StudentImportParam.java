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
package vip.xiaonuo.student.modular.student.param;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 学员批量导入行
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Getter
@Setter
public class StudentImportParam {

    /** 学员姓名（必填） */
    @ExcelProperty(value = "学员姓名", index = 0)
    private String name;

    /** 性别（男/女） */
    @ExcelProperty(value = "性别", index = 1)
    private String gender;

    /** 生日（yyyy-MM-dd） */
    @ExcelProperty(value = "生日", index = 2)
    private String birthday;

    /** 家长姓名 */
    @ExcelProperty(value = "家长姓名", index = 3)
    private String parentName;

    /** 家长手机 */
    @ExcelProperty(value = "家长手机", index = 4)
    private String parentPhone;

    /** 校区名称（为空时用默认校区） */
    @ExcelProperty(value = "校区", index = 5)
    private String campusName;

    /** 状态（在读/停课/休学/退学/结课，默认为读） */
    @ExcelProperty(value = "状态", index = 6)
    private String status;

    /** 备注 */
    @ExcelProperty(value = "备注", index = 7)
    private String remark;
}
