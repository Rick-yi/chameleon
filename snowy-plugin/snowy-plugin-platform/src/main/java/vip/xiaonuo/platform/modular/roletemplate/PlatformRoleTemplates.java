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

import java.util.List;

/**
 * 教培机构的标准角色模板
 * <p>
 * 与「机构管理员（superAdmin）」并列的四个岗位角色，租户开通时自动创建。
 * 角色码与移动端视角一一对应（见 snowy-admin-web/src/store/app.js）。
 * </p>
 *
 * @author 会飞的猪
 * @date 2026/10/22 10:00
 **/
public class PlatformRoleTemplates {

    /** 角色码：机构管理员 */
    public static final String ROLE_ADMIN = "superAdmin";

    /** 数据范围：全部 */
    public static final String SCOPE_ALL = "SCOPE_ALL";
    /** 数据范围：本校区 */
    public static final String SCOPE_ORG = "SCOPE_ORG";

    /** 校长 / 运营负责人：全部经营数据 */
    public static final PlatformRoleTemplate PRINCIPAL = template(
            "principal", "校长/运营负责人", 10,
            List.of("/workbench", "/student", "/student/order", "/student/renewWarn", "/crm",
                    "/course", "/course/package", "/course/class", "/course/schedule", "/course/attend",
                    "/report/dashboard", "/report/finance", "/report/academic", "/report/crm"),
            List.of("/student", "/crm", "/course", "/finance/report", "/biz/event"),
            SCOPE_ALL, false, "机构经营总览：全量数据 + 全部业务菜单");

    /** 招生顾问：只看自己的线索与自己转化的学员 */
    public static final PlatformRoleTemplate SALES = template(
            "sales", "招生顾问", 20,
            List.of("/workbench", "/crm", "/student", "/student/order", "/report/crm"),
            List.of("/crm",
                    // 转学员需要课包、课程的只读数据
                    "/course/package/page", "/course/package/detail", "/course/page", "/course/detail",
                    "/student/page", "/student/add", "/student/edit", "/student/detail",
                    "/student/lessonAccount/page", "/student/lessonFlow/page", "/student/lessonAccount/balance",
                    "/student/order/page", "/student/order/add", "/student/order/offlinePay",
                    "/student/order/cancel", "/student/order/renew", "/student/refund"),
            SCOPE_ALL, true, "招生线索跟进与转学员，线索按跟进人过滤");

    /** 授课老师：只看自己的课与自己班的学员 */
    public static final PlatformRoleTemplate TEACHER = template(
            "teacher", "授课老师", 30,
            List.of("/workbench", "/course/schedule", "/course/attend", "/student"),
            List.of("/course/schedule/week", "/course/schedule/page", "/course/schedule/detail",
                    "/course/schedule/teacherOptions", "/course/attend", "/course/classStudent/page",
                    // 点名页需要班级/课程的只读数据（课程 id、班级名单）
                    "/course/class/page", "/course/class/detail", "/course/class/campusOptions",
                    "/course/page", "/course/detail",
                    "/student/page", "/student/detail",
                    "/student/lessonAccount/page", "/student/lessonAccount/balance", "/student/lessonFlow/page"),
            SCOPE_ORG, true, "课表与点名消课，课节按授课教师过滤");

    /** 前台 / 财务：收款、退费与财务数据 */
    public static final PlatformRoleTemplate CASHIER = template(
            "cashier", "前台/财务", 40,
            List.of("/workbench", "/student/order", "/student/renewWarn", "/report/finance"),
            List.of("/student/order", "/student/refund", "/student/renewWarn", "/student/page",
                    "/student/detail", "/finance/report"),
            SCOPE_ALL, false, "订单收款、退费审批与财务报表");

    /** 业务菜单路径前缀（机构管理员需要这些菜单的全量权限） */
    public static final List<String> BUSINESS_MENU_PATH_PREFIXES = List.of(
            "/workbench", "/student", "/crm", "/course", "/report", "/automation");

    /** 全部模板 */
    public static List<PlatformRoleTemplate> all() {
        return List.of(PRINCIPAL, SALES, TEACHER, CASHIER);
    }

    private static PlatformRoleTemplate template(String code, String name, Integer sortCode,
                                                List<String> menuPaths, List<String> apiPrefixes,
                                                String scopeCategory, Boolean selfScope, String remark) {
        PlatformRoleTemplate template = new PlatformRoleTemplate();
        template.setCode(code);
        template.setName(name);
        template.setSortCode(sortCode);
        template.setMenuPaths(menuPaths);
        template.setApiPrefixes(apiPrefixes);
        template.setScopeCategory(scopeCategory);
        template.setSelfScope(selfScope);
        template.setRemark(remark);
        return template;
    }
}
