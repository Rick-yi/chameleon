#!/usr/bin/env bash
# ============================================================
# chameleon 数据库升级脚本（按顺序执行幂等脚本）
# 用法：
#   MYSQL_HOST=... MYSQL_USER=... MYSQL_PWD=... ./scripts/upgrade.sh
# 说明：
#   - 只执行「可重复执行」的脚本（INSERT IGNORE / 幂等检查）；
#   - 一次性 ALTER 脚本（chameleon_p0_migration.sql、chameleon_c1_migration.sql）不自动执行，
#     因为列已存在会报错，需要人工确认后再跑；
#   - 升级前建议先执行 scripts/backup.sh。
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

run() {
  local file="$1"
  local path="${SQL_DIR}/${file}"
  if [ ! -f "${path}" ]; then
    echo "-- 跳过（不存在）：${file}"
    return
  fi
  echo "-- 执行：${file}"
  mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PWD}" \
    "${MYSQL_DB}" < "${path}"
}

# 幂等脚本：可安全重复执行
run "chameleon_tenant_migration.sql"
run "chameleon_menu.sql"
run "chameleon_report_r2r4_migration.sql"
run "chameleon_report_permission.sql"

# 一次性脚本：如需执行请手动执行（列已存在会报错）
cat <<'TXT'

以下脚本为一次性结构升级，需人工确认后执行：
  chameleon_p0_migration.sql
  chameleon_c1_migration.sql

TXT

echo ">> 升级完成（应用重启后 P0SchemaMigration 会再做一次幂等检查与补列）"
