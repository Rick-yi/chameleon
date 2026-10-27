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
package vip.xiaonuo.course.modular.courseclass.enums;

import lombok.Getter;

/**
 * 班级班型
 *
 * @author 会飞的猪
 * @date 2026/10/27
 **/
@Getter
public enum CourseClassFormEnum {

    /** 一对多：按班级名册排课、点名 */
    GROUP("GROUP", "一对多"),

    /** 一对一：班级直接绑定一名学员 */
    ONE2ONE("ONE2ONE", "一对一");

    private final String value;

    private final String message;

    CourseClassFormEnum(String value, String message) {
        this.value = value;
        this.message = message;
    }

    public static boolean isOne2One(String value) {
        return ONE2ONE.getValue().equals(value);
    }
}
