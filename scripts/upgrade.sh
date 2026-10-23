#!/usr/bin/env bash
# ============================================================
# chameleon 数据库升级脚本（对已有库执行增量/幂等脚本）
# 用法：
#   MYSQL_HOST=... MYSQL_USER=... MYSQL_PWD=... ./scripts/upgrade.sh
# 说明：
#   - 升级前请先执行 scripts/backup.sh；
#   - 不包含 snowy_mysql.sql（会 DROP 底座表），也不包含演示数据；
#   - 结构变更（加列/索引/新表）由应用启动时的 P0SchemaMigration 幂等补齐，
#     本脚本只负责数据类脚本（菜单、权限、按钮、自动化配置等）；
#   - 使用 mysql --force：重复执行时的「已存在」报错会跳过，其余错误请人工确认。
# ============================================================
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_DB="${MYSQL_DB:-chameleon}"
SQL_DIR="${SQL_DIR:-$(cd "$(dirname "$0")/.." && pwd)/snowy-web-app/src/main/resources/_sql}"

if [ -z "${MYSQL_PWD:-}" ]; then
  echo "请通过环境变量提供 MYSQL_PWD" >&2
  exit 1
fi

ORDER=(
  chameleon_course.sql
  chameleon_crm.sql
  chameleon_student.sql
  chameleon_finance.sql
  chameleon_platform.sql
  chameleon_event.sql
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

for file in "${ORDER[@]}"; do
  path="${SQL_DIR}/${file}"
  if [ ! -f "${path}" ]; then
    echo "-- 跳过（不存在）：${file}"
    continue
  fi
  echo "-- 执行：${file}"
  mysql --force -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PWD}" \
    "${MYSQL_DB}" < "${path}"
done

echo ">> 升级脚本执行完成；应用启动后 P0SchemaMigration 会补齐结构变更。"
