#!/usr/bin/env bash
# ============================================================
# chameleon 数据库初始化（按固定顺序执行 _sql 脚本）
# 用法：
#   MYSQL_HOST=... MYSQL_USER=... MYSQL_PWD=... ./scripts/db-init.sh
#   跳过演示数据： SKIP_DEMO=1 ./scripts/db-init.sh
# 说明：
#   - 只用于「空库初始化」；已有库升级请依赖应用启动时的 P0SchemaMigration；
#   - 脚本顺序有依赖：底座补租户列必须早于按租户写关系数据的菜单脚本；
#   - 使用 mysql --force 容忍重复执行时的「已存在」类报错。
# ============================================================
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_DB="${MYSQL_DB:-chameleon}"
SQL_DIR="${SQL_DIR:-$(cd "$(dirname "$0")/.." && pwd)/snowy-web-app/src/main/resources/_sql}"
SKIP_DEMO="${SKIP_DEMO:-0}"

if [ -z "${MYSQL_PWD:-}" ]; then
  echo "请通过环境变量提供 MYSQL_PWD" >&2
  exit 1
fi

ORDER=(
  snowy_mysql.sql
  chameleon_course.sql
  chameleon_crm.sql
  chameleon_student.sql
  chameleon_finance.sql
  chameleon_platform.sql
  chameleon_event.sql
  # 底座表补租户列必须早于「按租户写关系」的菜单脚本
  chameleon_tenant_migration.sql
  chameleon_menu.sql
  chameleon_ux_menu.sql
  chameleon_c1_migration.sql
  chameleon_c2_migration.sql
  chameleon_c3_migration.sql
  chameleon_c4_migration.sql
  chameleon_p0_migration.sql
  chameleon_report_migration.sql
  chameleon_report_permission.sql
  chameleon_report_r2r4_migration.sql
  chameleon_automation.sql
  chameleon_role_api_button.sql
  chameleon_role_template.sql
)

if [ "${SKIP_DEMO}" != "1" ]; then
  ORDER+=(chameleon_demo_org.sql)
fi

echo ">> 等待 MySQL ${MYSQL_HOST}:${MYSQL_PORT} ..."
for i in $(seq 1 60); do
  if mysqladmin --default-character-set=utf8mb4 -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PWD}" ping >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo ">> 创建数据库（如不存在）${MYSQL_DB}"
mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PWD}" \
  -e "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DB}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

for file in "${ORDER[@]}"; do
  path="${SQL_DIR}/${file}"
  if [ ! -f "${path}" ]; then
    echo "-- 跳过（不存在）：${file}"
    continue
  fi
  echo "-- 执行：${file}"
  mysql --default-character-set=utf8mb4 --force -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PWD}" \
    "${MYSQL_DB}" < "${path}"
done

echo ">> 初始化完成，检查关键表："
mysql --default-character-set=utf8mb4 -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PWD}" "${MYSQL_DB}" -e "
  SELECT 'SYS_USER' T, COUNT(*) C FROM SYS_USER
  UNION ALL SELECT 'SYS_ROLE', COUNT(*) FROM SYS_ROLE
  UNION ALL SELECT 'SYS_RESOURCE', COUNT(*) FROM SYS_RESOURCE
  UNION ALL SELECT 'T_TENANT', COUNT(*) FROM T_TENANT
  UNION ALL SELECT 'COURSE', COUNT(*) FROM COURSE
  UNION ALL SELECT 'STUDENT', COUNT(*) FROM STUDENT
  UNION ALL SELECT 'BIZ_EVENT', COUNT(*) FROM BIZ_EVENT
  UNION ALL SELECT 'DEV_JOB', COUNT(*) FROM DEV_JOB;"
