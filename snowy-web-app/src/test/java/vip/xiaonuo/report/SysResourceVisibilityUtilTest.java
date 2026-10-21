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
package vip.xiaonuo.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.xiaonuo.sys.core.util.SysResourceVisibilityUtil;
import vip.xiaonuo.sys.modular.resource.entity.SysMenu;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜单可见性工具单测
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
class SysResourceVisibilityUtilTest {

    @Test
    @DisplayName("编码为空的菜单不受功能开关影响")
    void nullCodeMenuAlwaysEnabled() {
        SysMenu menu = new SysMenu();
        menu.setName("未编码菜单");
        assertTrue(SysResourceVisibilityUtil.isFuncEnabled(menu));
    }

    @Test
    @DisplayName("未登记功能键的菜单不受功能开关影响")
    void unknownMenuAlwaysEnabled() {
        SysMenu menu = new SysMenu();
        menu.setCode("sysUserPage");
        assertTrue(SysResourceVisibilityUtil.isFuncEnabled(menu));
    }

    @Test
    @DisplayName("空菜单对象不报错")
    void nullMenuSafe() {
        assertTrue(SysResourceVisibilityUtil.isFuncEnabled(null));
    }
}
