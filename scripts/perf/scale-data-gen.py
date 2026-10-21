"""压测数据生成器（批量 SQL，单连接执行）。

为 scale01~scale10 租户批量生成真实形态的教培业务数据：
  每租户 3 校区 / 12 用户 / 8 课程 / 16 课包 / 20 班级 / 800 学员
  1000 订单 / 1120 课时账户 / 约 4200 课时流水 / 240 排课 / 3200 点名
  200 线索 / 400 跟进 / 20000 埋点事件
"""
import subprocess
import sys
import time

MYSQL = "D:/data/mysql-8.0.39-winx64/bin/mysql.exe"
DB = "chameleon"

SUR = "王李张刘陈杨黄赵周吴徐孙马朱胡林郭何高罗郑梁谢宋唐许韩冯邓曹彭曾肖田董袁潘蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤"
GIV = "伟芳娜秀英敏静丽强磊军洋勇艳杰娟涛明超霞平刚桂华建文志国春小永晓东峰飞云海山宁欣"

COURSE_NAMES = ["素描基础", "书法启蒙", "钢琴一对一", "少儿英语", "美术色彩", "国画山水",
                "乐高创意", "数理思维", "播音主持", "硬笔书法"]
SOURCES = ["地推", "转介绍", "线上广告", "门店到访", "电话咨询"]
LEAD_STATUS = ["NEW", "FOLLOWED", "TRIALED", "ENROLLED", "LOST"]
EVENT_CODES = ["PAGE_VIEW", "PAGE_VIEW", "PAGE_VIEW", "FEATURE_CLICK", "ORDER_CREATE", "ORDER_PAY",
               "LEAD_CREATE", "LEAD_FOLLOW", "ATTEND_CONFIRM", "SCHEDULE_GENERATE"]
EVENT_PAGES = ["/workbench", "/student", "/student/order", "/course/schedule", "/course/attend",
               "/crm", "/report/dashboard", "/report/finance", "/course/class", "/automation"]
PKG_LESSONS = [20, 30, 40, 60]


def run_script(statements, database=DB):
    """把多条 SQL 交给一个 mysql 连接执行（单次进程，速度快）"""
    script = ";\n".join(statements) + ";\n"
    p = subprocess.run(
        [MYSQL, "--default-character-set=utf8mb4", "--force", "-uroot", "-p12345678", database],
        input=script, capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    errors = [ln for ln in p.stderr.splitlines() if "ERROR" in ln]
    if errors:
        raise RuntimeError("SQL 失败:\n" + "\n".join(errors[:10]))
    return p.stdout


def query(stmt, database=DB):
    p = subprocess.run(
        [MYSQL, "--default-character-set=utf8mb4", "-uroot", "-p12345678", database, "-e", stmt],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    return p.stdout.strip()


def ensure_seq_table():
    """辅助序列表：一次生成，后面所有批量插入都基于它"""
    run_script([
        "DROP TABLE IF EXISTS TMP_SCALE_SEQ",
        """CREATE TABLE TMP_SCALE_SEQ (N int NOT NULL PRIMARY KEY) ENGINE=InnoDB""",
        "SET SESSION cte_max_recursion_depth = 100000",
        """INSERT INTO TMP_SCALE_SEQ (N)
           WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 50000)
           SELECT n FROM seq""",
    ])


def tenant_rows():
    out = query("select ID, CODE, NAME from T_TENANT where CODE like 'scale%' order by CODE")
    rows = []
    for line in out.splitlines()[1:]:
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 3:
            rows.append((parts[0], parts[1], parts[2]))
    return rows


def build_tenant(tid, tcode, tname, idx):
    pfx = f"S{idx:02d}"
    start = time.time()
    stmts = []

    def sid(prefix, n):
        return f"{pfx}{prefix}{n:015d}"

    # ---------- 组织 ----------
    orgs = query(f"select ID, CATEGORY from SYS_ORG where TENANT_ID='{tid}' and DELETE_FLAG='NOT_DELETE'")
    hq = campus = None
    for line in orgs.splitlines()[1:]:
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 2:
            if parts[1] == "COMPANY":
                hq = parts[0]
            elif campus is None:
                campus = parts[0]
    if not hq or not campus:
        raise RuntimeError(f"租户 {tcode} 缺少组织数据")

    campus_list = [campus]
    for i in range(2):
        oid = sid("O", i + 1)
        stmts.append(
            f"insert into SYS_ORG (ID,TENANT_ID,PARENT_ID,NAME,CODE,CATEGORY,SORT_CODE,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
            f"values ('{oid}','{tid}','{hq}','{tname}校区{i+2}','{tcode}_CAMPUS_{i+2}','DEPT',{i+2},'NOT_DELETE',now(),'-1')")
        campus_list.append(oid)

    # ---------- 用户（8 教师 + 4 招生顾问）----------
    pwd = query("select PASSWORD from SYS_USER where ACCOUNT='superAdmin' limit 1").splitlines()[-1]
    admin_id = query(f"select ID from SYS_USER where TENANT_ID='{tid}' and ACCOUNT='admin' limit 1").splitlines()[-1]
    teachers, sales = [], []
    for i in range(12):
        uid = sid("U", i + 1)
        role = "教师" if i < 8 else "招生顾问"
        stmts.append(
            f"insert into SYS_USER (ID,TENANT_ID,ACCOUNT,PASSWORD,NAME,NICKNAME,GENDER,PHONE,ORG_ID,USER_STATUS,SORT_CODE,"
            f"DELETE_FLAG,CREATE_TIME,CREATE_USER) values ('{uid}','{tid}','{tcode}_u{i+1}','{pwd}',"
            f"'{SUR[(i + idx * 20) % len(SUR)]}{GIV[(i * 7 + idx) % len(GIV)]}','{role}{i+1}','男',"
            f"'139{idx:02d}{i:06d}','{campus_list[i % len(campus_list)]}','ENABLE',{i+1},'NOT_DELETE',now(),'-1')")
        stmts.append(
            f"insert into SYS_RELATION (ID,TENANT_ID,OBJECT_ID,TARGET_ID,CATEGORY,EXT_JSON) values "
            f"('{sid('R', i + 1)}','{tid}','{uid}','{admin_id}','SYS_USER_HAS_ROLE',null)")
        (teachers if i < 8 else sales).append(uid)

    # ---------- 课程 ----------
    for i in range(8):
        stmts.append(
            f"insert into COURSE (ID,TENANT_ID,NAME,CLASS_TYPE,AGE_RANGE,LESSONS_PER_CLASS,LEAVE_DEDUCT,ABSENT_DEDUCT,"
            f"STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) values ('{sid('C', i+1)}','{tid}','{COURSE_NAMES[i]}',"
            f"'ONE_TO_ONE','6-12岁',1.00,1,1,'ENABLE','NOT_DELETE',now(),'-1')")

    # ---------- 课包 ----------
    for i in range(16):
        lessons = PKG_LESSONS[i % 4]
        stmts.append(
            f"insert into COURSE_PACKAGE (ID,TENANT_ID,COURSE_ID,NAME,LESSONS,GIFT_LESSONS,PRICE,LESSONS_PER_CLASS,"
            f"LEAVE_DEDUCT,ABSENT_DEDUCT,VALIDITY_DAYS,STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) values "
            f"('{sid('P', i+1)}','{tid}','{sid('C', i % 8 + 1)}','{COURSE_NAMES[i % 8]} {lessons}课时',{lessons},"
            f"{lessons // 10},3600.00,1.00,1,1,365,'ENABLE','NOT_DELETE',now(),'-1')")

    # ---------- 班级 ----------
    week_template = ('[{"weekDay":2,"startTime":"19:00","endTime":"20:30"},'
                     '{"weekDay":4,"startTime":"19:00","endTime":"20:30"}]')
    for i in range(20):
        stmts.append(
            f"insert into COURSE_CLASS (ID,TENANT_ID,COURSE_ID,NAME,CAMPUS_ID,TEACHER_ID,CLASSROOM,CAPACITY,"
            f"WEEK_TEMPLATE_JSON,STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) values ('{sid('L', i+1)}','{tid}',"
            f"'{sid('C', i % 8 + 1)}','{COURSE_NAMES[i % 8]} {i//8 + 1}班','{campus_list[i % len(campus_list)]}',"
            f"'{teachers[i % len(teachers)]}','B{201 + i}',30,'{week_template}','ENABLE','NOT_DELETE',now(),'-1')")

    # ---------- 学员 800 + 家长绑定 ----------
    sur_list = ",".join(f"'{c}'" for c in SUR)
    giv_list = ",".join(f"'{c}'" for c in GIV)
    stmts.append(
        f"insert into STUDENT (ID,TENANT_ID,NAME,GENDER,BIRTHDAY,PARENT_NAME,PARENT_PHONE,CAMPUS_ID,STATUS,"
        f"DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}S', lpad(n,15,'0')), '{tid}', "
        f"concat(elT(mod(n+{idx * 20}, {len(SUR)}), {sur_list}), elT(mod(n*7+{idx}, {len(GIV)}), {giv_list})), "
        f"if(mod(n,2)=0,'男','女'), date_add('2014-01-01', interval mod(n,4000) day), "
        f"concat(elT(mod(n+{idx * 11}, {len(SUR)}), {sur_list}),'家长'), concat('138{idx:02d}', lpad(n,6,'0')), "
        f"elT(mod(n, {len(campus_list)})+1, {','.join(repr(c) for c in campus_list)}), "
        f"if(mod(n,50)=0,'STOP','STUDYING'), 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 800")
    stmts.append(
        f"insert into STUDENT_PARENT_BIND (ID,TENANT_ID,STUDENT_ID,RELATION,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}B', lpad(n,15,'0')), '{tid}', concat('{pfx}S', lpad(n,15,'0')), '父亲', 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 800")

    # ---------- 订单 1000 ----------
    stmts.append(
        f"insert into STUDENT_ORDER (ID,TENANT_ID,STUDENT_ID,COURSE_PACKAGE_ID,COURSE_ID,CAMPUS_ID,PACKAGE_NAME,"
        f"PACKAGE_PRICE,PACKAGE_LESSONS,PACKAGE_GIFT_LESSONS,PACKAGE_VALIDITY_DAYS,AMOUNT,PAID_AMOUNT,PAY_CHANNEL,"
        f"TRADE_NO,PAY_TIME,CONSUMED_LESSONS,REFUNDED_LESSONS,STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}D', lpad(n,15,'0')), '{tid}', "
        f"concat('{pfx}S', lpad(mod(n-1,800)+1,15,'0')), concat('{pfx}P', lpad(mod(n-1,16)+1,15,'0')), "
        f"concat('{pfx}C', lpad(mod(mod(n-1,16),8)+1,15,'0')), "
        f"elT(mod(n, {len(campus_list)})+1, {','.join(repr(c) for c in campus_list)}), "
        f"'', "
        f"3600.00, elT(mod(n-1,4)+1, 20,30,40,60), elT(mod(n-1,4)+1, 2,3,4,6), 365, 3600.00, "
        f"if(n<=800, 3600.00, 0.00), elT(mod(n-1,3)+1, 'WECHAT','CASH','OFFLINE'), "
        f"if(n<=800, concat('TR{idx:02d}', lpad(n,12,'0')), null), "
        f"if(n<=800, date_sub(now(), interval mod(n,120) day), null), 0.00, 0.00, "
        f"if(n<=800,'PAID', if(n<=950,'UNPAID','CANCELED')), 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 1000")
    # 订单里的课包名称快照单独修正（按课包索引取名称）
    stmts.append(
        f"update STUDENT_ORDER o join COURSE_PACKAGE p on p.ID = o.COURSE_PACKAGE_ID "
        f"set o.PACKAGE_NAME = p.NAME, o.PACKAGE_PRICE = p.PRICE, o.PACKAGE_LESSONS = p.LESSONS, "
        f"o.PACKAGE_GIFT_LESSONS = p.GIFT_LESSONS, o.PACKAGE_VALIDITY_DAYS = p.VALIDITY_DAYS "
        f"where o.TENANT_ID = '{tid}'")

    # ---------- 课时账户（已支付订单对应的学员/课程）----------
    stmts.append(
        f"insert ignore into STUDENT_LESSON_ACCOUNT (ID,TENANT_ID,STUDENT_ID,COURSE_ID,TOTAL_LESSONS,GIFT_LESSONS,"
        f"USED_LESSONS,REMAIN_LESSONS,EXPIRE_TIME,STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}A', lpad(row_number() over (order by o.STUDENT_ID, o.COURSE_ID),15,'0')), '{tid}', "
        f"o.STUDENT_ID, o.COURSE_ID, sum(o.PACKAGE_LESSONS), sum(o.PACKAGE_GIFT_LESSONS), 0.00, "
        f"sum(o.PACKAGE_LESSONS + o.PACKAGE_GIFT_LESSONS), date_add(now(), interval 365 day), 'NORMAL', 'NOT_DELETE', now(), '-1' "
        f"from STUDENT_ORDER o where o.TENANT_ID='{tid}' and o.STATUS='PAID' "
        f"group by o.STUDENT_ID, o.COURSE_ID")

    # ---------- 购课入账流水 ----------
    stmts.append(
        f"insert into STUDENT_LESSON_FLOW (ID,TENANT_ID,STUDENT_ID,COURSE_ID,FLOW_TYPE,LESSONS,REF_ORDER_ID,"
        f"REMARK,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}F', lpad(row_number() over (order by o.ID),15,'0')), '{tid}', o.STUDENT_ID, o.COURSE_ID, 'SIGNUP', "
        f"o.PACKAGE_LESSONS + o.PACKAGE_GIFT_LESSONS, o.ID, '购课入账', 'NOT_DELETE', now(), '-1' "
        f"from STUDENT_ORDER o where o.TENANT_ID='{tid}' and o.STATUS='PAID'")

    # ---------- 班级学员 20×20 ----------
    stmts.append(
        f"insert into COURSE_CLASS_STUDENT (ID,TENANT_ID,CLASS_ID,STUDENT_ID,STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}M', lpad(c*20+j+1,15,'0')), '{tid}', concat('{pfx}L', lpad(c+1,15,'0')), "
        f"concat('{pfx}S', lpad(mod(c*40+j,800)+1,15,'0')), 'STUDYING', 'NOT_DELETE', now(), '-1' "
        f"from (select n div 20 as c, mod(n,20) as j from TMP_SCALE_SEQ where n <= 400) t")

    # ---------- 排课 20×12 ----------
    stmts.append(
        f"insert into COURSE_SCHEDULE (ID,TENANT_ID,CLASS_ID,course_id,campus_id,SCHEDULE_DATE,START_TIME,END_TIME,"
        f"CLASSROOM,TEACHER_ID,STATUS,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}H', lpad(n,15,'0')), '{tid}', concat('{pfx}L', lpad((n-1) div 12 + 1,15,'0')), "
        f"concat('{pfx}C', lpad(mod((n-1) div 12,8)+1,15,'0')), "
        f"elT(mod((n-1) div 12, {len(campus_list)})+1, {','.join(repr(c) for c in campus_list)}), "
        f"date_add(curdate(), interval (((n-1) div 12 - 8) * 7) day), '19:00:00', '20:30:00', "
        f"concat('B', 201 + ((n-1) div 12)), "
        f"elT(mod((n-1) div 12, 8)+1, {','.join(repr(t) for t in teachers)}), 'NORMAL', 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 240")

    # ---------- 点名记录（过去 8 节 × 20 人）+ 消课流水 ----------
    stmts.append(
        f"insert into COURSE_CLASS_ATTEND (ID,TENANT_ID,SCHEDULE_ID,STUDENT_ID,ATTEND_STATUS,LESSONS_DEDUCTED,"
        f"ATTEND_USER,ATTEND_TIME,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}T', lpad(n,15,'0')), '{tid}', concat('{pfx}H', lpad(((n-1) div 20) + 1,15,'0')), "
        f"concat('{pfx}S', lpad(mod(((n-1) div 20)*40 + mod(n-1,20),800)+1,15,'0')), "
        f"if(mod(n,10)=0, if(mod(n,20)=0,'LEAVE','ABSENT'), 'ATTEND'), 1.00, "
        f"elT(mod((n-1) div 20, 8)+1, {','.join(repr(t) for t in teachers)}), now(), 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 3200")
    stmts.append(
        f"insert into STUDENT_LESSON_FLOW (ID,TENANT_ID,STUDENT_ID,COURSE_ID,SCHEDULE_ID,FLOW_TYPE,LESSONS,"
        f"REMARK,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}F', lpad(1000+row_number() over (order by a.ID),15,'0')), '{tid}', a.STUDENT_ID, "
        f"s.course_id, a.SCHEDULE_ID, 'ATTEND', 1.00, "
        f"'点名消课', 'NOT_DELETE', now(), '-1' "
        f"from COURSE_CLASS_ATTEND a join COURSE_SCHEDULE s on s.ID = a.SCHEDULE_ID where a.TENANT_ID='{tid}'")

    # ---------- 线索 200 + 跟进 ----------
    stmts.append(
        f"insert into CRM_LEAD (ID,TENANT_ID,NAME,PARENT_PHONE,INTENT_COURSE_ID,SOURCE,STATUS,FOLLOW_USER_ID,"
        f"NEXT_FOLLOW_TIME,REMARK,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}E', lpad(n,15,'0')), '{tid}', "
        f"concat(elT(mod(n+{idx * 51}, {len(SUR)}), {sur_list}), elT(mod(n*3+{idx}, {len(GIV)}), {giv_list})), "
        f"concat('137{idx:02d}', lpad(n,6,'0')), concat('{pfx}C', lpad(mod(n-1,8)+1,15,'0')), "
        f"elT(mod(n-1,5)+1, {','.join(repr(s) for s in SOURCES)}), "
        f"elT(mod(n-1,5)+1, {','.join(repr(s) for s in LEAD_STATUS)}), "
        f"elT(mod(n-1,4)+1, {','.join(repr(t) for t in sales)}), date_add(now(), interval mod(n,7) day), "
        f"'压测线索', 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 200")
    stmts.append(
        f"insert into CRM_LEAD_FOLLOW (ID,TENANT_ID,LEAD_ID,FOLLOW_TYPE,CONTENT,NEXT_FOLLOW_TIME,FOLLOW_USER_ID,"
        f"DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}G', lpad(n,15,'0')), '{tid}', concat('{pfx}E', lpad(((n-1) div 2)+1,15,'0')), 'PHONE', "
        f"'电话沟通，家长有意向', date_add(now(), interval mod(n,7) day), "
        f"elT(mod(((n-1) div 2),4)+1, {','.join(repr(t) for t in sales)}), 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 400")

    # ---------- 埋点事件 20000 ----------
    stmts.append(
        f"insert into BIZ_EVENT (ID,TENANT_ID,EVENT_CODE,EVENT_NAME,USER_ID,USER_NAME,TARGET_TYPE,TARGET_ID,"
        f"TARGET_NAME,PROPS,EVENT_TIME,DELETE_FLAG,CREATE_TIME,CREATE_USER) "
        f"select concat('{pfx}V', lpad(n,15,'0')), '{tid}', "
        f"elT(mod(n-1,{len(EVENT_CODES)})+1, {','.join(repr(c) for c in EVENT_CODES)}), "
        f"elT(mod(n-1,{len(EVENT_CODES)})+1, {','.join(repr(c) for c in EVENT_CODES)}), "
        f"'{admin_id}', '机构管理员', "
        f"if(mod(n,3)=1,'page','student'), "
        f"elT(mod(n-1,{len(EVENT_PAGES)})+1, {','.join(repr(p) for p in EVENT_PAGES)}), "
        f"elT(mod(n-1,{len(EVENT_PAGES)})+1, {','.join(repr(p) for p in EVENT_PAGES)}), NULL, "
        f"date_sub(now(), interval mod(n,720) minute), 'NOT_DELETE', now(), '-1' "
        f"from TMP_SCALE_SEQ where n <= 20000")

    run_script(stmts)
    print(f"  租户 {tcode} {tname} 生成完成，{len(stmts)} 条批量语句，耗时 {time.time() - start:.1f}s")


def main():
    tenants = tenant_rows()
    if not tenants:
        print("没有找到 scale 租户")
        return 1
    print(f"开始为 {len(tenants)} 个租户生成压测数据")
    ensure_seq_table()
    for i, (tid, tcode, tname) in enumerate(tenants, start=1):
        build_tenant(tid, tcode, tname, i)
    run_script(["DROP TABLE IF EXISTS TMP_SCALE_SEQ"])
    print("全部完成")
    return 0


if __name__ == "__main__":
    sys.exit(main())
