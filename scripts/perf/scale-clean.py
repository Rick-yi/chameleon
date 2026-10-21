"""清理压测租户（scale%）的业务数据，便于重新造数。"""
import subprocess

MYSQL = "D:/data/mysql-8.0.39-winx64/bin/mysql.exe"
DB = "chameleon"

TABLES = [
    "biz_automation_config", "biz_automation_log", "biz_event", "client_user", "course", "course_category",
    "course_class", "course_class_attend", "course_class_student", "course_package", "course_schedule",
    "course_schedule_change", "course_schedule_student", "crm_lead", "crm_lead_follow", "crm_trial_appointment",
    "dev_config", "dev_dict", "dev_job", "fin_daily_stat", "fin_export_task", "notify_log", "student",
    "student_lesson_account", "student_lesson_flow", "student_order", "student_parent_bind", "student_refund",
    "student_renew_warn", "sys_group", "sys_org", "sys_position", "sys_relation", "sys_role", "sys_user",
    "t_tenant_login",
]

p = subprocess.run(
    [MYSQL, "--default-character-set=utf8mb4", "-uroot", "-p12345678", DB, "-e",
     "select group_concat(concat('''',ID,'''')) from T_TENANT where CODE like 'scale%'"],
    capture_output=True, text=True, encoding="utf-8")
ids = p.stdout.strip().splitlines()[-1]
if not ids:
    print("没有压测租户")
    raise SystemExit(0)

stmts = [f"delete from {t} where TENANT_ID in ({ids})" for t in TABLES]
script = ";\n".join(stmts) + ";\n"
p = subprocess.run([MYSQL, "--default-character-set=utf8mb4", "-uroot", "-p12345678", DB],
                   input=script, capture_output=True, text=True, encoding="utf-8")
print("清理完成", p.stderr[:300] if p.returncode else "")
for t in ["student", "student_order", "course_schedule", "course_class_attend", "biz_event", "crm_lead", "sys_user"]:
    q = subprocess.run([MYSQL, "--default-character-set=utf8mb4", "-uroot", "-p12345678", DB, "-e",
                        f"select count(*) from {t} where TENANT_ID in ({ids})"],
                       capture_output=True, text=True, encoding="utf-8")
    print(f"{t}: {q.stdout.strip().splitlines()[-1]}")
