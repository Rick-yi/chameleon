# syntax=docker/dockerfile:1
# ============================================================
# chameleon 多阶段构建：Maven 编译 + JRE 运行
# 构建：docker build -t chameleon-web-app:latest .
# 运行：docker run -d -p 82:82 -e SPRING_DATASOURCE_... chameleon-web-app:latest
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先复制 pom 预热依赖缓存（依赖不变时可复用层）
COPY pom.xml .
COPY snowy-common/pom.xml snowy-common/
COPY snowy-plugin-api/pom.xml snowy-plugin-api/
COPY snowy-plugin/pom.xml snowy-plugin/
COPY snowy-web-app/pom.xml snowy-web-app/
COPY snowy-admin-web/pom.xml snowy-admin-web/ 2>/dev/null || true
RUN mvn -B -q -DskipTests dependency:go-offline || true

# 再复制源码并打包
COPY . .
RUN mvn -B -DskipTests clean package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 导出文件目录（异步导出落盘位置）与日志目录
RUN mkdir -p /app/data/chameleon-export /app/logs

COPY --from=builder /build/snowy-web-app/target/snowy-web-app-3.0.0.jar /app/app.jar

# 非 root 运行
RUN groupadd -r chameleon && useradd -r -g chameleon -d /app chameleon \
    && chown -R chameleon:chameleon /app
USER chameleon

EXPOSE 82

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms512m -Xmx1024m -Djava.io.tmpdir=/app/tmp"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:82/doc.html || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
