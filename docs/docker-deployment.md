# Docker 容器化部署方案

> 覆盖前端（front-end）、后端（back-end）、数据库（database）三层的容器化部署，
> 基于 docker compose 编排，一条命令即可在本机或有 Docker 的任意主机上完整运行。

---

## 1. 方案总览

项目提供两种容器化形态，按场景选用：

| 方案 | 镜像数 | 数据库 | 适用场景 | 入口 |
| --- | --- | --- | --- | --- |
| **A. 三容器编排（本方案）** | 3 个（frontend / backend / db） | MySQL 8 独立容器 | 课程作业演示、标准部署、贴近生产结构 | `docker compose up -d` |
| B. 单镜像（此前已交付） | 1 个（内含 Nginx + 后端 + SQLite） | SQLite 文件 | 无数据库服务器的极简运行、只产出镜像交付 | `docker run -p 80:80 ...` |

两种方案共用同一套代码和同一套环境变量约定（`DB_DRIVER` / `DB_URL` / `JPA_DDL_AUTO` / `JPA_DIALECT`），切换不需要改代码。

---

## 2. 架构

```
                    宿主机 80 端口（唯一对外入口）
                            │
                   ┌────────▼────────┐
                   │  frontend 容器   │  Nginx 1.27-alpine
                   │  - Vue 静态资源  │  /usr/share/nginx/html
                   │  - /api 反向代理 ├────────────────┐
                   └─────────────────┘                │
                                       oep-net（compose 内部网络）
                                                      │
                   ┌──────────────────────────────────▼──┐
                   │           backend 容器               │
                   │  Spring Boot 3.3（Temurin JRE 17）   │
                   │  Tomcat :8080，/api/health 健康检查   │
                   └──────────────────┬──────────────────┘
                                      │ JDBC (mysql:3306)
                   ┌──────────────────▼──────────────────┐
                   │             db 容器                  │
                   │  MySQL 8.0 · utf8mb4                │
                   │  数据卷 mysql-data 持久化            │
                   │  首次启动自动执行 sql/schema.sql      │
                   └─────────────────────────────────────┘
```

要点：

- **只有 frontend 暴露宿主机端口（80）**。backend 和 db 都不发布端口，外部无法直接访问数据库，攻击面最小。
- 三个容器加入同一个内部网络 `oep-net`，通过**服务名**互相访问（Docker 内置 DNS）：frontend 里 Nginx `proxy_pass http://backend:8080`；backend 连 `jdbc:mysql://db:3306/online_education`。
- **启动顺序由健康检查保证**：`frontend depends_on backend(service_healthy)`，`backend depends_on db(service_healthy)`。MySQL 就绪前后端不会启动，后端就绪前 Nginx 不会启动，避免"容器起来了但接口 502"。

---

## 3. 文件清单

```
online-education-platform/
├── docker-compose.yml          # 三容器编排定义（核心）
├── backend/Dockerfile          # 后端镜像：Maven 构建 → JRE 17 运行
├── frontend/Dockerfile         # 前端镜像：Node 构建 → Nginx 运行
├── docker/
│   ├── nginx-frontend.conf     # 前端容器 Nginx 配置（/api → backend:8080）
│   ├── nginx.conf              # （单镜像方案用）
│   └── entrypoint.sh           # （单镜像方案用）
├── sql/schema.sql              # 挂载进 db 容器，首次启动自动建库建表
└── Dockerfile                  # （单镜像方案用）
```

---

## 4. 三个镜像的构成

### 4.1 db —— 直接使用官方镜像

`mysql:8.0`，不做任何定制，通过挂载和参数配置：

- `MYSQL_ROOT_PASSWORD`：root 密码，可用环境变量覆盖（见 §6）
- `MYSQL_DATABASE=online_education`：启动即建库
- 字符集参数 `utf8mb4 / utf8mb4_unicode_ci`，与项目约定一致
- 数据卷 `mysql-data:/var/lib/mysql`：数据持久化，`docker compose down` 不丢数据
- `sql/schema.sql` 挂载到 `/docker-entrypoint-initdb.d/`：**仅在数据卷为空的首次启动时执行**，自动完成建表

### 4.2 backend —— 三阶段构建

`backend/Dockerfile`：

1. `maven:3.9-eclipse-temurin-17` 阶段：先只拷 `pom.xml` 拉依赖（形成缓存层，改代码不重新下载依赖），再拷源码 `mvn package`
2. `eclipse-temurin:17-jre-jammy` 运行阶段：只放可执行 jar + curl（健康检查用）
3. `HEALTHCHECK` 调 `/api/health`，`--start-period=120s` 给 Spring Boot 首次启动留足时间

内存通过 `JAVA_OPTS=-XX:MaxRAMPercentage=70` 按容器限额自适应。

### 4.3 frontend —— 两阶段构建

`frontend/Dockerfile`：

1. `node:20-alpine` 阶段：`npm ci`（严格按 lockfile）→ `vite build`
2. `nginx:1.27-alpine` 运行阶段：拷入 `dist` 和 `docker/nginx-frontend.conf`

Nginx 职责：托管静态资源（`/assets/` 一年强缓存、gzip）、把 `/api/` 反代到 `backend:8080`、Vue Router history 模式的 `try_files` 回退。

---

## 5. 网络与数据流

| 路径 | 说明 |
| --- | --- |
| 浏览器 → `http://localhost/` | frontend 容器 Nginx 返回 Vue 静态页 |
| 浏览器 → `http://localhost/api/**` | Nginx 反代到 backend:8080，浏览器无感知 |
| backend → db:3306 | 内部网络直连，宿主机不可见 |

前端 axios 的 `baseURL: '/api'` 无需任何修改——这正是用 Nginx 反代而不是让前端直连后端 IP 的原因：**前端代码不感知后端地址，换环境零改动**。

---

## 6. 环境变量

所有变量都有默认值，`docker compose up` 开箱即用；正式部署时通过 `.env` 文件或 shell 环境覆盖：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | `oep-root-pass` | 数据库 root 密码，**正式部署必须改** |
| `JWT_SECRET` | 内置开发值 | JWT 签名密钥，**正式部署必须改**（≥32 字节） |
| `INIT_DEMO_DATA` | `true` | 空库时写入演示账号/课程，生产改 `false` |
| `JPA_DDL_AUTO` | `update` | 首次启动建表；表结构稳定后建议改 `none` |

覆盖方式（二选一）：

```bash
# 方式一：compose 自动读取同目录 .env 文件
cat > .env <<'EOF'
MYSQL_ROOT_PASSWORD=my-strong-pass
JWT_SECRET=change-me-at-least-32-bytes-long!!
EOF

# 方式二：临时环境变量
MYSQL_ROOT_PASSWORD=xxx JWT_SECRET=yyy docker compose up -d
```

---

## 7. 使用步骤

```bash
# 0. 前提：已安装 Docker（含 compose 插件），无其他依赖
docker --version && docker compose version

# 1. 启动（首次会构建 backend/frontend 两个镜像，约 3-5 分钟）
docker compose up -d

# 2. 观察状态：等待两个 healthcheck 变为 healthy
docker compose ps

# 3. 验证
curl http://localhost/api/health          # {"code":200,...,"status":"UP"}
# 浏览器打开 http://localhost，用演示账号登录：
#   admin / admin123 （管理员）  student / student123 （学生）

# 4. 查看日志
docker compose logs -f backend

# 5. 停止 / 彻底重来
docker compose down               # 保留数据
docker compose down -v            # 连数据卷一起删（下次启动重建库）
```

---

## 8. 健康检查与自愈

| 容器 | 检查方式 | 频率 |
| --- | --- | --- |
| db | `mysqladmin ping` | 10s |
| backend | `curl /api/health` 返回 200 | 15s，启动宽限 120s |
| frontend | `wget http://127.0.0.1/` | 15s |

三个服务均配 `restart: unless-stopped`，进程崩溃会被 Docker 自动拉起；配合健康检查，`docker compose ps` 的 STATUS 列可直接看出整条链路是否健康。

---

## 9. 与 CI/CD 的关系

- **CI（ci.yml）**：每次推送跑测试与构建，保证进镜像的代码是测试通过的
- **镜像发布（docker.yml）**：单镜像方案持续推送 `place12345/work1:latest` 到阿里云
- **本方案（compose）**：默认用 `build:` 在部署机本地构建。若部署机不想装构建工具链，可把两个 Dockerfile 交给 CI 构建后推到阿里云（镜像名已预留在 compose 里：`.../work1-backend`、`.../work1-frontend`），再把 `docker-compose.yml` 的 `build:` 段删掉、只留 `image:`，即变成"拉取即部署"

---

## 10. 常见问题

**Q：80 端口被占用了？**
把 compose 里 `"80:80"` 改成 `"8081:80"`，用 `http://localhost:8081` 访问。

**Q：想连宿主机已有的 MySQL 而不用 db 容器？**
删掉 `db` 服务和 backend 的 `depends_on`，backend 环境变量改为 `DB_URL: jdbc:mysql://host.docker.internal:3306/...`。

**Q：数据库表结构变了怎么办？**
开发期 `JPA_DDL_AUTO=update` 会自动加列；删列/改类型需手动执行 SQL 或 `down -v` 重建。

**Q：为什么不用单镜像方案？**
单镜像用 SQLite，适合演示和"只交镜像"的场景；三容器方案用真正的 MySQL，结构上与生产一致（前端静态层 / 应用层 / 数据层分离），也是课程要求的标准容器化部署形态。两者可共存，按需选用。
