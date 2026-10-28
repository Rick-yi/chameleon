#!/usr/bin/env bash
# ============================================================
# 集成测试（需要本机 MySQL）
#
# 与单元测试的区别：
#   - 单测（mvn test）纯逻辑，不连库，CI 默认可跑；
#   - 集成测试连本机独立库 chameleon_it，验证条件更新、并发、账务一致性。
#
# 为什么会话库是独立的：集成测试会真的写数据，绝不能连演示库 chameleon。
# 首次运行或结构变更后需要重建测试库：
#   ./scripts/db-init.sh 配 MYSQL_DB=chameleon_it SKIP_DEMO=1
# ============================================================
set -euo pipefail

cd "$(dirname "$0")/.."

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PWD="${MYSQL_PWD:-12345678}"
MYSQL_DB="${MYSQL_DB:-chameleon_it}"

# 1) 检查 MySQL 是否可达
if ! command -v mysql >/dev/null 2>&1; then
  echo "未找到 mysql 客户端，请把 MySQL bin 目录加入 PATH" >&2
  exit 1
fi
if ! mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PWD" \
      -e "SELECT 1" >/dev/null 2>&1; then
  echo "无法连接 MySQL ${MYSQL_HOST}:${MYSQL_PORT}，请先启动 mysqld" >&2
  exit 1
fi

# 2) 检查测试库存在（避免误连演示库）
if ! mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PWD" \
      -N -e "SHOW DATABASES LIKE '${MYSQL_DB}'" | grep -q "$MYSQL_DB"; then
  echo "测试库 ${MYSQL_DB} 不存在。初始化：" >&2
  echo "  export MYSQL_DB=${MYSQL_DB} SKIP_DEMO=1 && ./scripts/db-init.sh" >&2
  exit 1
fi

# 3) 先全量 install，避免个别模块用仓库旧 jar（本仓库已知构建坑）
#    强制用工程自带 JDK17：系统默认是 1.8，会报「无效的标记 --release」
JAVA_HOME="$(cd "$(dirname "$0")/.." && pwd)/.jdk/jdk-17"
export JAVA_HOME
echo ">> JAVA_HOME=${JAVA_HOME}"
echo ">> 全量 install（跳过测试）"
mvn -s .jdk/maven-settings-local.xml -q -B clean install -DskipTests

# 4) 运行集成测试（*IT 不在默认 surefire 范围内，需显式指定）
echo ">> 运行集成测试"
mvn -s .jdk/maven-settings-local.xml -B -pl snowy-web-app test \
  -Dtest='*IT' -Dsurefire.failIfNoSpecifiedTests=false
