# syntax=docker/dockerfile:1
# ============================================================
# chameleon 后端镜像：Maven 编译 + JRE 运行
# 构建：docker build -t chameleon-web-app:latest .
# 运行：docker run -d -p 82:82 -e MYSQL_HOST=... -e MYSQL_PWD=... chameleon-web-app:latest
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先复制各模块 pom 预热依赖缓存（依赖不变时可复用层）
COPY pom.xml .
COPY snowy-common/pom.xml snowy-common/
COPY snowy-plugin-api/pom.xml snowy-plugin-api/
COPY snowy-plugin/pom.xml snowy-plugin/
COPY snowy-web-app/pom.xml snowy-web-app/
RUN mvn -B -q -DskipTests dependency:go-offline || true

# 再复制源码并打包（前端由 Dockerfile.web 单独构建）
COPY . .
RUN mvn -B -DskipTests clean package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 异步导出、日志与临时文件目录；健康检查需要 curl
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/data/chameleon-export /app/logs /app/tmp

COPY --from=builder /build/snowy-web-app/target/snowy-web-app-3.0.0.jar /app/app.jar

# 非 root 运行
RUN groupadd -r chameleon && useradd -r -g chameleon -d /app chameleon \
    && chown -R chameleon:chameleon /app
USER chameleon

EXPOSE 82

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-Xms512m -Xmx1024m -Djava.io.tmpdir=/app/tmp"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:82/favicon.ico || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
