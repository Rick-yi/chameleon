"""生成「业务接口 → 按钮资源」的 SQL 脚本。

业务接口此前没有登记为 SYS_RESOURCE 的按钮资源，导致：
  1. 角色管理页看不到业务接口，只能全量授权；
  2. 角色模板无法按接口前缀精确授权。
"""
import pathlib

MENU = {
    "menu_student_001": "学员管理",
    "menu_order_001": "收款台",
    "menu_renew_001": "续费预警",
    "menu_course_001": "课程管理",
    "menu_pkg_001": "课包管理",
    "menu_class_001": "班级管理",
    "menu_schedule_001": "排课课表",
    "menu_attend_001": "点名消课",
    "menu_crm_001": "招生线索",
    "menu_report_dash_001": "经营看板",
    "menu_report_fin_001": "财务报表",
    "menu_report_acad_001": "教务报表",
    "menu_report_crm_001": "招生报表",
    "menu_event_001": "运营埋点",
    "menu_automation_001": "自动化中心",
}

# 接口 → (菜单ID, 按钮名称)
API_BUTTON = {
    # 学员管理
    "/student/page": ("menu_student_001", "学员分页"),
    "/student/add": ("menu_student_001", "新增学员"),
    "/student/edit": ("menu_student_001", "编辑学员"),
    "/student/delete": ("menu_student_001", "删除学员"),
    "/student/detail": ("menu_student_001", "学员详情"),
    "/student/lessonAccount/page": ("menu_student_001", "课时账户分页"),
    "/student/lessonAccount/adjust": ("menu_student_001", "课时调整"),
    "/student/lessonAccount/balance": ("menu_student_001", "课时余额"),
    "/student/lessonFlow/page": ("menu_student_001", "课时流水分页"),
    # 收款台
    "/student/order/page": ("menu_order_001", "订单分页"),
    "/student/order/add": ("menu_order_001", "报名开单"),
    "/student/order/detail": ("menu_order_001", "订单详情"),
    "/student/order/offlinePay": ("menu_order_001", "线下收款"),
    "/student/order/payCallback": ("menu_order_001", "支付回调"),
    "/student/order/cancel": ("menu_order_001", "取消订单"),
    "/student/order/renew": ("menu_order_001", "续费下单"),
    "/student/refund/page": ("menu_order_001", "退费单分页"),
    "/student/refund/apply": ("menu_order_001", "提交退费申请"),
    "/student/refund/approve": ("menu_order_001", "退费审批"),
    "/student/refund/confirm": ("menu_order_001", "退费确认"),
    "/student/refund/cancel": ("menu_order_001", "退费撤销"),
    "/student/refund/detail": ("menu_order_001", "退费单详情"),
    # 续费预警
    "/student/renewWarn/page": ("menu_renew_001", "续费预警分页"),
    "/student/renewWarn/scan": ("menu_renew_001", "续费预警扫描"),
    "/student/renewWarn/ignore": ("menu_renew_001", "忽略续费预警"),
    # 课程管理
    "/course/page": ("menu_course_001", "课程分页"),
    "/course/add": ("menu_course_001", "新增课程"),
    "/course/edit": ("menu_course_001", "编辑课程"),
    "/course/delete": ("menu_course_001", "删除课程"),
    "/course/detail": ("menu_course_001", "课程详情"),
    "/course/category/list": ("menu_course_001", "课程分类列表"),
    "/course/category/add": ("menu_course_001", "新增课程分类"),
    "/course/category/edit": ("menu_course_001", "编辑课程分类"),
    "/course/category/delete": ("menu_course_001", "删除课程分类"),
    "/course/category/detail": ("menu_course_001", "课程分类详情"),
    # 课包管理
    "/course/package/page": ("menu_pkg_001", "课包分页"),
    "/course/package/add": ("menu_pkg_001", "新增课包"),
    "/course/package/edit": ("menu_pkg_001", "编辑课包"),
    "/course/package/delete": ("menu_pkg_001", "删除课包"),
    "/course/package/detail": ("menu_pkg_001", "课包详情"),
    # 班级管理
    "/course/class/page": ("menu_class_001", "班级分页"),
    "/course/class/add": ("menu_class_001", "新增班级"),
    "/course/class/edit": ("menu_class_001", "编辑班级"),
    "/course/class/delete": ("menu_class_001", "删除班级"),
    "/course/class/detail": ("menu_class_001", "班级详情"),
    "/course/class/campusOptions": ("menu_class_001", "校区选项"),
    "/course/classStudent/page": ("menu_class_001", "班级学员分页"),
    "/course/classStudent/add": ("menu_class_001", "学员入班"),
    "/course/classStudent/delete": ("menu_class_001", "学员退班"),
    # 排课课表
    "/course/schedule/page": ("menu_schedule_001", "排课分页"),
    "/course/schedule/add": ("menu_schedule_001", "手动排课"),
    "/course/schedule/detail": ("menu_schedule_001", "排课详情"),
    "/course/schedule/generate": ("menu_schedule_001", "批量生成排课"),
    "/course/schedule/generateAdhoc": ("menu_schedule_001", "直接排课"),
    "/course/schedule/change": ("menu_schedule_001", "调课"),
    "/course/schedule/cancel": ("menu_schedule_001", "取消排课"),
    "/course/schedule/week": ("menu_schedule_001", "课表周视图"),
    "/course/schedule/teacherOptions": ("menu_schedule_001", "教师选项"),
    "/course/schedule/export": ("menu_schedule_001", "导出课表"),
    "/course/schedule/conflictCheck": ("menu_schedule_001", "排课冲突检测"),
    "/course/scheduleChange/page": ("menu_schedule_001", "调课记录分页"),
    # 点名消课
    "/course/attend/page": ("menu_attend_001", "点名记录分页"),
    "/course/attend/confirm": ("menu_attend_001", "点名确认消课"),
    "/course/attend/cancel": ("menu_attend_001", "撤销点名"),
    # 招生线索
    "/crm/lead/page": ("menu_crm_001", "线索分页"),
    "/crm/lead/add": ("menu_crm_001", "新增线索"),
    "/crm/lead/edit": ("menu_crm_001", "编辑线索"),
    "/crm/lead/delete": ("menu_crm_001", "删除线索"),
    "/crm/lead/detail": ("menu_crm_001", "线索详情"),
    "/crm/follow/page": ("menu_crm_001", "跟进记录分页"),
    "/crm/follow/add": ("menu_crm_001", "新增跟进"),
    "/crm/trial/page": ("menu_crm_001", "试听分页"),
    "/crm/trial/add": ("menu_crm_001", "新增试听"),
    "/crm/trial/cancel": ("menu_crm_001", "取消试听"),
    # 报表
    "/finance/report/summary": ("menu_report_dash_001", "经营概览"),
    "/finance/report/dashboard": ("menu_report_dash_001", "经营看板"),
    "/finance/report/trend": ("menu_report_dash_001", "收支趋势"),
    "/finance/report/channel": ("menu_report_fin_001", "收款渠道"),
    "/finance/report/courseRank": ("menu_report_fin_001", "课程排行"),
    "/finance/report/campusRank": ("menu_report_fin_001", "校区排行"),
    "/finance/report/attendTrend": ("menu_report_fin_001", "课消趋势"),
    "/finance/report/campusOptions": ("menu_report_fin_001", "校区选项"),
    "/finance/report/consume": ("menu_report_fin_001", "课消分析"),
    "/finance/report/dailyStat": ("menu_report_fin_001", "日汇总"),
    "/finance/report/dailyStat/rebuild": ("menu_report_fin_001", "重算日汇总"),
    "/finance/report/reconcile": ("menu_report_fin_001", "对账"),
    "/finance/report/cache/clear": ("menu_report_fin_001", "清理报表缓存"),
    "/finance/report/export": ("menu_report_fin_001", "导出报表"),
    "/finance/report/export/async": ("menu_report_fin_001", "异步导出"),
    "/finance/report/export/async/status": ("menu_report_fin_001", "异步导出状态"),
    "/finance/report/export/async/download": ("menu_report_fin_001", "异步导出下载"),
    "/finance/payment/page": ("menu_report_fin_001", "收款单分页"),
    "/finance/refund/page": ("menu_report_fin_001", "退费单分页"),
    "/finance/stat": ("menu_report_fin_001", "财务统计"),
    "/course/report/academic/attendance": ("menu_report_acad_001", "出勤汇总"),
    "/course/report/academic/classFill": ("menu_report_acad_001", "满班率"),
    "/course/report/academic/teacherWorkload": ("menu_report_acad_001", "教师课时负荷"),
    "/course/report/academic/lessonProgress": ("menu_report_acad_001", "学员课时进度"),
    "/course/report/academic/riskStudents": ("menu_report_acad_001", "风险学员"),
    "/course/report/academic/export": ("menu_report_acad_001", "导出教务报表"),
    "/crm/report/funnel": ("menu_report_crm_001", "招生漏斗"),
    "/crm/report/source": ("menu_report_crm_001", "来源渠道"),
    "/crm/report/followRank": ("menu_report_crm_001", "顾问跟进排行"),
    "/crm/report/trial": ("menu_report_crm_001", "试听转化"),
    "/crm/report/export": ("menu_report_crm_001", "导出招生报表"),
    # 埋点
    "/biz/event/page": ("menu_event_001", "埋点流水分页"),
    "/biz/event/summary": ("menu_event_001", "埋点统计"),
    "/biz/event/codes": ("menu_event_001", "埋点码字典"),
    # 自动化中心
    "/biz/automation/rule": ("menu_automation_001", "自动化规则列表"),
    "/biz/automation/toggle": ("menu_automation_001", "自动化规则开关"),
    "/biz/automation/run": ("menu_automation_001", "立即执行自动化规则"),
    "/biz/automation/log": ("menu_automation_001", "自动化执行记录"),
}

HEADER = """-- ============================================================================
-- chameleon 业务接口注册为「按钮资源」
--   背景：业务接口此前未登记到 SYS_RESOURCE，角色管理页看不到、角色模板无法精确授权。
--   作用：把每个业务接口登记成所属菜单下的按钮资源（CODE = 接口地址），
--         之后可以在「系统管理 → 角色管理」里按接口勾选权限，也可被角色模板按前缀引用。
--   幂等：可重复执行（INSERT IGNORE）
-- ============================================================================

"""

def button_id(api: str) -> str:
    """按钮资源主键：SYS_RESOURCE.ID 只有 20 位，用接口地址哈希生成短且稳定的主键"""
    import hashlib
    return "b" + hashlib.md5(api.encode("utf-8")).hexdigest()[:18]


lines = [HEADER]
for api, (menu_id, title) in sorted(API_BUTTON.items(), key=lambda kv: (kv[1][0], kv[0])):
    lines.append(
        f"INSERT IGNORE INTO `SYS_RESOURCE` "
        f"(`ID`,`PARENT_ID`,`TITLE`,`NAME`,`CODE`,`CATEGORY`,`MODULE`,`MENU_TYPE`,`PATH`,`COMPONENT`,`ICON`,`COLOR`,"
        f"`VISIBLE`,`DISPLAY_LAYOUT`,`KEEP_LIVE`,`SORT_CODE`,`DELETE_FLAG`,`CREATE_TIME`,`CREATE_USER`) VALUES "
        f"('{button_id(api)}', '{menu_id}', '{title}', NULL, '{api}', 'BUTTON', NULL, 'BUTTON', "
        f"NULL, NULL, NULL, NULL, 'YES', 'YES', 'YES', 0, 'NOT_DELETE', NOW(), '-1');\n"
    )

out = pathlib.Path("snowy-web-app/src/main/resources/_sql/chameleon_role_api_button.sql")
out.write_text("".join(lines), encoding="utf-8", newline="\n")
print("生成", out, "共", len(API_BUTTON), "个接口")
