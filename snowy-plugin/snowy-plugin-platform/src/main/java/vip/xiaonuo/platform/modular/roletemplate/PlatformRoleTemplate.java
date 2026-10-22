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
package vip.xiaonuo.platform.modular.roletemplate;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 角色模板
 * <p>
 * 描述一个「岗位角色」应该拥有的菜单、接口权限与数据范围，
 * 租户开通时自动落地为真实角色，机构管理员也可以在「角色管理」里继续调整。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
@Getter
@Setter
public class PlatformRoleTemplate {

    /** 角色编码（登录后据此判断岗位，App 端也用它决定视角） */
    private String code;

    /** 角色名称 */
    private String name;

    /** 排序 */
    private Integer sortCode;

    /** 可见菜单路径（对应 SYS_RESOURCE.PATH） */
    private List<String> menuPaths;

    /**
     * 接口权限前缀（对应按钮资源 CODE = 接口地址）
     * <p>按前缀匹配，如 /student/lesson 可覆盖 /student/lessonAccount/page</p>
     */
    private List<String> apiPrefixes;

    /**
     * 数据范围
     * <ul>
     *     <li>SCOPE_ALL 全部数据</li>
     *     <li>SCOPE_ORG 本校区</li>
     * </ul>
     */
    private String scopeCategory;

    /** 是否按「本人」过滤（老师只看自己的课、顾问只看自己的线索） */
    private Boolean selfScope;

    /** 备注 */
    private String remark;
}
