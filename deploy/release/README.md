# 预构建部署包（服务器无需构建）

适用场景：服务器只有 Docker，不想在上面跑 Maven/npm 构建。

## 目录结构

```
/opt/chameleon
├── app.jar                 # 后端可执行包（mvn clean package 产物）
├── dist/                   # 前端构建产物（snowy-admin-web/dist）
├── docker-compose.yml
├── nginx.conf              # HTTPS + /api 反向代理
├── ssl/
│   ├── aiglobe.xin.pem
│   └── aiglobe.xin.key
├── sql/                    # _sql 下的初始化脚本
├── scripts/db-init.sh
└── .env                    # MYSQL_ROOT_PASSWORD=...
```

## 首次部署

```bash
docker compose up -d
docker logs chameleon-db-init
curl -I https://<域名>/
```

`db-init` 只在数据库为空时执行；`SKIP_DEMO=1` 不导入演示业务数据。

## 更新版本

```bash
# 本机构建后上传
scp app.jar root@<host>:/opt/chameleon/app.jar
scp -r dist/* root@<host>:/opt/chameleon/dist/
ssh root@<host> "cd /opt/chameleon && docker compose up -d app web"
```

结构变更（新增列/索引/表）由后端启动时的 `P0SchemaMigration` 自动幂等补齐，不需要手工执行 ALTER。

## 注意

- `.env` 中的 MySQL 密码请使用强密码，并妥善保管；
- 证书到期前替换 `ssl/` 下文件后 `docker compose restart web`；
- 备份：`/opt/chameleon/backup.sh`（cron 每天 03:00，保留 14 天）。
