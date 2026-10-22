/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您使用时，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.auth.core.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 角色判断工具类
 * <p>
 * 教培业务按「岗位角色」区分可见数据：授课老师只看自己的课、招生顾问只看自己的线索。
 * 这里提供无登录态安全的角色判断，业务模块直接调用即可。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
public class StpRoleUtil {

    /** 角色码：机构管理员（机构内最高权限） */
    public static final String ROLE_ADMIN = "superAdmin";
    /** 角色码：校长/运营负责人 */
    public static final String ROLE_PRINCIPAL = "principal";
    /** 角色码：招生顾问 */
    public static final String ROLE_SALES = "sales";
    /** 角色码：授课老师 */
    public static final String ROLE_TEACHER = "teacher";
    /** 角色码：前台/财务 */
    public static final String ROLE_CASHIER = "cashier";

    /**
     * 当前用户是否拥有其中任意一个角色
     *
     * @param roleCodeList 角色码集合
     */
    public static boolean hasRole(List<String> roleCodeList) {
        if (roleCodeList == null || roleCodeList.isEmpty()) {
            return false;
        }
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            List<String> currentRoleList = StpUtil.getRoleList();
            if (currentRoleList == null || currentRoleList.isEmpty()) {
                return false;
            }
            for (String roleCode : roleCodeList) {
                if (StrUtil.isNotBlank(roleCode) && currentRoleList.contains(roleCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // 无登录上下文（定时任务、开放接口）时按无角色处理
            return false;
        }
    }

    /**
     * 当前用户是否拥有其中任意一个角色
     *
     * @param roleCode 角色码
     */
    public static boolean hasRole(String roleCode) {
        return hasRole(List.of(roleCode));
    }

    /**
     * 是否是「只看自己数据」的角色（老师/顾问）
     */
    public static boolean isSelfScopeRole() {
        return hasRole(List.of(ROLE_TEACHER, ROLE_SALES));
    }

    /**
     * 当前登录用户id（无登录态返回 null）
     */
    public static String currentUserId() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
