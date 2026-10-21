/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.auth.core.util;

import cn.hutool.core.collection.CollectionUtil;
import vip.xiaonuo.auth.core.pojo.SaBaseLoginUser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户数据范围工具类
 * <p>
 * Snowy 原生数据范围按「接口 URL」匹配（{@link StpLoginUserUtil#getLoginUserDataScope()}），
 * 业务模块的接口通常没有单独授权记录，这里提供「按用户全部授权取并集」的解析方式：
 * <ul>
 *     <li>返回 null：任一授权为 SCOPE_ALL（或用户没有授权信息）→ 不做数据范围过滤；</li>
 *     <li>返回空集合：用户有授权但全部被限定到具体机构且没有可见机构 → 查不到数据；</li>
 *     <li>返回非空集合：可见的机构ID集合（校区即机构），业务表按 {@code CAMPUS_ID} 过滤。</li>
 * </ul>
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/21 10:00
 **/
public class StpDataScopeUtil {

    /**
     * 获取当前登录用户可见的机构ID集合
     *
     * @return null 表示不限制；空集合表示没有可见数据；非空为可见机构ID
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public static List<String> visibleOrgIds() {
        SaBaseLoginUser loginUser;
        try {
            loginUser = StpLoginUserUtil.getLoginUser();
        } catch (Exception e) {
            // 无登录上下文（定时任务、开放接口）时不限制
            return null;
        }
        if (loginUser == null || CollectionUtil.isEmpty(loginUser.getDataScopeList())) {
            return null;
        }
        Set<String> orgIdSet = new LinkedHashSet<>();
        for (SaBaseLoginUser.DataScope dataScope : loginUser.getDataScopeList()) {
            if (dataScope.isScopeAll()) {
                return null;
            }
            orgIdSet.addAll(dataScope.getDataScope());
        }
        return new ArrayList<>(orgIdSet);
    }

    /**
     * 判断是否需要按数据范围过滤
     *
     * @author 会飞的猪
     * @date 2026/10/21 10:00
     **/
    public static boolean needFilter() {
        List<String> orgIdList = visibleOrgIds();
        return orgIdList != null;
    }
}
