#!/usr/bin/env bash
# ============================================================
# chameleon 数据库备份脚本
# 用法：
#   MYSQL_HOST=... MYSQL_USER=... MYSQL_PWD=... ./scripts/backup.sh [保留天数]
# 说明：
#   - 备份业务库（结构 + 数据 + 存储过程/触发器/事件）；
#   - 默认保留 30 天，更早的备份自动清理；
#   - 密码通过环境变量传入，不落在命令行与脚本里。
# ============================================================
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_DB="${MYSQL_DB:-chameleon}"
KEEP_DAYS="${1:-30}"
BACKUP_DIR="${BACKUP_DIR:-./backup}"
STAMP="$(date +%Y%m%d-%H%M%S)"

if [ -z "${MYSQL_PWD:-}" ]; then
  echo "请通过环境变量提供 MYSQL_PWD" >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"
TARGET="${BACKUP_DIR}/${MYSQL_DB}-${STAMP}.sql"

echo ">> 备份 ${MYSQL_DB} → ${TARGET}"
mysqldump \
  --host="${MYSQL_HOST}" \
  --port="${MYSQL_PORT}" \
  --user="${MYSQL_USER}" \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  --default-character-set=utf8mb4 \
  "${MYSQL_DB}" > "${TARGET}"

gzip -f "${TARGET}"
echo ">> 备份完成：${TARGET}.gz（$(du -h "${TARGET}.gz" | cut -f1)）"

# 清理过期备份
find "${BACKUP_DIR}" -name "${MYSQL_DB}-*.sql.gz" -mtime "+${KEEP_DAYS}" -print -delete

# 提示恢复命令
echo ">> 恢复命令：gunzip -c ${TARGET}.gz | mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER} -p ${MYSQL_DB}"
