# blog · 韩行风的个人博客

一个「写完就能发」的个人博客系统：把 Obsidian / Typora 里的 Markdown 笔记上传到服务端，直接变成可在线阅读的文章与笔记库；同时把 GitHub 的提交记录抓下来，做成类似 GitHub 贡献图的代码活跃度面板。

后端是 Spring Boot 3 的 REST API，前端是内嵌在 `src/main/resources/static` 下的原生 HTML + Tailwind（CDN）。

## 功能

**文章**

- 上传 `.md` 文件即成文：标题取文件名（去掉 `.md`），摘要取正文前 20 字（在线编辑保存时取前 60 字）
- 首页分页浏览（每页 9 篇）、按标题模糊搜索、文章详情
- 后台在线编辑 Markdown、新建文章、删除、置顶 / 取消置顶
- 前台 Markdown 渲染，支持亮 / 暗主题

**笔记**

- 单篇上传（可手填目录）与整个文件夹批量上传，用 `webkitRelativePath` 还原目录层级
- 笔记标题存成相对路径（如 `mysql/索引/B+树.md`），前台按路径渲染成侧边栏目录树

**数据面板**

- 笔记统计：扫描本地 Markdown 目录，统计总篇数、总字数、每日增量
- 代码统计：抓取 GitHub 提交的 `additions` / `deletions`，生成每日热力图与累计变更行数
- 昨日数据接口 + 历史序列接口，前端一次性拉取渲染

**管理后台**

- JWT 登录（BCrypt 校验密码），登录失败限流
- 图片上传与替换（头像 / 背景 / 登录页背景）

**工程侧**

- Redis 缓存 + Caffeine 本地缓存两级
- 定时任务：每天 10:00 抓取 GitHub 提交，每天 17:06 重算笔记统计
- Spring Retry：GitHub 接口失败自动重试（最多 144 次，间隔 10 分钟）

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.2.12 |
| 语言 / 构建 | Java 21、Maven（自带 `mvnw`） |
| 持久层 | MyBatis-Plus 3.5.14（分页插件 + jsqlparser） |
| 数据库 | MySQL 8 |
| 缓存 | Redis（StringRedisTemplate + ZSET）、Caffeine（`@Cacheable`） |
| 安全 | Spring Security（仅用 `BCryptPasswordEncoder`）、jjwt 0.11.5、自定义 `HandlerInterceptor` |
| HTTP 客户端 | WebClient（spring-boot-starter-webflux） |
| 其他 | Lombok、spring-retry（`flexmark` / `jsoup` 已引入但未使用） |
| 前端 | 原生 HTML/CSS/JS + Tailwind（CDN） + markdown-it / highlight.js / github-markdown-css（已本地化到 `static/vendor`） |

## 目录结构

```
blog
├── pom.xml
├── mvnw / mvnw.cmd / .mvn
├── 建表语句.txt                       # 7 张表的建表脚本（会先 DROP）
└── src
    ├── main
    │   ├── java/org/hanxingfeng/blog
    │   │   ├── BlogApplication.java   # @EnableScheduling / @EnableRetry / @EnableAsync
    │   │   ├── Controller             # blog（前台）、node（笔记）、user（后台）、common（文件）
    │   │   ├── Service + ServiceImpl  # 业务与缓存读写
    │   │   ├── Mapper                 # MyBatis-Plus BaseMapper
    │   │   ├── Entity                 # 表实体 + 统一响应 R<T>
    │   │   ├── dto                    # GitHub / 笔记树 DTO
    │   │   ├── config                 # Security / Redis / Caffeine / MyBatis-Plus / Web / Async
    │   │   ├── Filter                 # LoginFilter（JWT 拦截器）
    │   │   └── UtilAndOther           # JWT、GitHub 抓取、笔记扫描、ZIP、文件名工具
    │   └── resources
    │       ├── application.yaml       # 本地真实配置（已在 .gitignore 中）
    │       ├── application-example.yaml
    │       └── static                 # index.html / node.html / admin.html / imageUpload.html
    │           └── vendor             # 本地化的前端库（markdown-it / highlight.js / github-markdown-css）
    └── test/java/.../BlogApplicationTests.java   # 手工调试用测试
```

## 快速开始

### 环境要求

- JDK 21 / JDK 17
- MySQL 8.x、Redis 6.x
- 不需要预装 Maven，仓库自带 wrapper

### 1. 初始化数据库

```bash
mysql -u root -p < 建表语句.txt
```

脚本会创建 `blog` 库和 7 张表（`user`、`writings`、`summary_writing`、`node`、`node_count`、`commit_count`、`git_commit`）。

> ⚠️ 脚本里每张表都是 `DROP TABLE IF EXISTS` 后重建，**执行前先确认数据可以丢弃或已备份**。

初始化管理员账号（密码用 BCrypt 生成后写入）：

```sql
INSERT INTO `user` (user_name, password, role, ip)
VALUES ('admin', '<BCrypt 哈希>', 1, '127.0.0.1');
```

### 2. 配置

```bash
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
```

`application-example.yaml` 只覆盖了数据库和 Redis，还需要补齐 GitHub 与 JWT 部分：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/blog?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
    username: <your-username>
    password: <your-password>
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

github:
  token: <GitHub PAT，需要 repo 读权限>
  username: <GitHub 用户名，用于过滤提交作者>
  owner: <仓库所属账号>

repo:
  repo:            # 要统计提交的仓库列表
    - blog
    - java_note

SECRET: "<JWT 签名密钥，至少 32 字节>"
```

| 配置项 | 说明 |
| --- | --- |
| `github.token` | Personal Access Token，`GithubConfig` 会作为 `Bearer` 头注入 WebClient |
| `github.username` | 只统计这个作者的提交 |
| `github.owner` | 仓库归属，与 `repo.repo` 里的仓库名拼成 `/repos/{owner}/{repo}` |
| `SECRET` | HMAC-SHA256 签名密钥，`Keys.hmacShaKeyFor` 要求 **≥ 32 字节**，太短启动即报错 |

> `application.yaml` 已在 `.gitignore` 中排除（`.gitignore` 的 `Sensitive config` 段），真实密码和 token 不要提交到仓库。

### 3. 启动

```bash
# 开发
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 打包运行
./mvnw clean package -DskipTests
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

默认端口 **8080**（`application.yaml` 未配置 `server.port`），启动后访问：

| 页面 | 地址 |
| --- | --- |
| 博客首页 | http://localhost:8080/index.html |
| 笔记区 | http://localhost:8080/node.html |
| 管理后台 | http://localhost:8080/admin.html |
| 图片替换工具 | http://localhost:8080/imageUpload.html |

> 静态页面的 `API_BASE` 默认是空串（同域请求）。前后端分离部署时，改各页面里的 `const API_BASE`。

## 接口一览

统一响应体：

```json
{ "code": 1, "msg": null, "data": {}, "map": {} }
```

`code = 1` 成功，`0` 失败（失败原因在 `msg`）。

### 公开接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/blog/test` | 健康检查，返回「服务器正常运行」 |
| GET | `/blog/getBriefWritings?current=1` | 文章摘要分页，每页 9 条 |
| GET | `/blog/getOne/{id}` | 文章正文 |
| POST | `/blog/selectWriting` | 标题模糊搜索（form：`keyLike`、`pageNum`、`pageSize`） |
| GET | `/blog/selectYesterday` | 昨日笔记统计（缺数据时实时重算） |
| GET | `/blog/select` | 历史笔记统计（从 `SystemConstants.START_TIME` 起） |
| GET | `/blog/selectOneCommitCount` | 昨日代码提交统计 |
| GET | `/blog/selectAllCommit` | 今年每日提交统计（热力图数据源） |
| GET | `/blog/sumCommit` | 累计新增 / 删除 / 总变更行数 |
| GET | `/node/select` | 笔记目录树（`{id, title, path[]}`） |
| GET | `/node/selectNode/{id}` | 单篇笔记正文 |
| POST | `/file/upload` | 图片上传（multipart `file`，仅 png / jpg） |
| GET | `/file/download/{fileName}` | 图片预览 / 下载（`Content-Disposition: inline`） |
| POST | `/file/setImage` | 替换头像 / 背景 / 登录页背景（`file` + `type=AVATAR\|BACKGROUND\|LOGIN_BG`） |

### 管理接口（需要登录）

先登录拿 token：

```bash
curl -X POST http://localhost:8080/admin/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"admin","password":"******"}'

# => {"code":1,"data":{"token":"eyJhbGciOiJIUzI1NiJ9..."}}
```

之后所有 `/admin/**` 请求都要带请求头 `token: <JWT>`（注意不是 `Authorization`），有效期 1 小时。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/admin/login` | 登录，返回 JWT（唯一免鉴权的 admin 接口） |
| GET | `/admin/update` | 手动刷新笔记统计 + GitHub 提交统计 |
| POST | `/admin/writingsUpload` | 上传 `.md` 文章（同名会拒绝） |
| POST | `/admin/nodeUpload` | 上传单篇笔记（`file` + 可选 `path`） |
| POST | `/admin/nodeBatchUpload` | 批量上传笔记（`files[]` + `paths` JSON 数组） |
| POST | `/admin/updateWritingById?upOrIn=0\|1` | 新建（0）/ 更新（1）文章，body 为 `Writings` JSON |
| GET | `/admin/delete/{id}` | 删除文章（同时删 `summary_writing` 与单篇 Redis 缓存） |
| GET | `/admin/topWriting/{id}/{isTop}` | 置顶（1）/ 取消置顶（0） |
| GET | `/admin/test` | 后台连通性测试 |

## 数据模型

| 表 | 作用 |
| --- | --- |
| `user` | 管理员账号，`password` 存 BCrypt 哈希 |
| `writings` | 文章正文，`content` 为 LONGTEXT，`preview` 为摘要 |
| `summary_writing` | 文章摘要（列表页用），与 `writings` **同 id**，另有 `is_top` 置顶标记 |
| `node` | 笔记，`title` 存相对路径（`a/b/c.md`），`body` 为原文 |
| `node_count` | 每日笔记统计快照，主键是 `today_time` |
| `commit_count` | 每日代码变更汇总（`additions` / `deletions` / `total_changes`） |
| `git_commit` | 提交明细，按 `commit_sha` 去重 |

## 统计口径与缓存

**笔记统计**（`UpdateNowData`）

1. 递归扫描 `SystemConstants.mdFilePath` 下所有 `.md`
2. 文件名长度 > 14 时，按「父目录 + 去掉前 11 位字符的文件名」判定重复（应对 Obsidian 导出时的时间戳前缀），同组只保留**体积最大**的一份
3. 总字数 = 文件字符数（`content.length()`），今日增量 = 今天总量 − 昨天的快照；增量为负时按 0 / 随机值兜底

**GitHub 提交**（`getGitHubCommit`）

- 只统计「昨天」（UTC）区间内的提交，`author` 过滤为 `github.username`
- 先按 `sha` 查 `git_commit`，已存在则跳过；新提交再逐条查详情拿增删行数
- 汇总写入 `commit_count`（同一天已有记录则更新）
- `@Retryable(maxAttempts = 144, backoff = 600000ms)`：失败后每 10 分钟重试一次，最多 24 小时

**缓存**

| 数据 | 位置 | Key | TTL |
| --- | --- | --- | --- |
| 文章摘要分页 | Redis | `PageSummaryWriting` | 38 分钟 |
| 单篇文章 | Redis | `Writing{id}` | 无（写操作时覆盖） |
| 昨日提交统计 | Redis | `CommitCount:{date}` | 36 分钟 |
| 今年提交序列 | Redis ZSET | `CommitDataList:` | 2400 秒 |
| 累计行数 | Redis | `sumCount` | 42 分钟 |
| 历史笔记统计 | Redis | `nodeCountList` | 32 分钟 |
| `selectAllCommit` | Caffeine | `commitCount::all` | 5 分钟 |
| 登录失败计数 | Redis | `loginCount` | 首次写入 1 小时（后续累加会覆盖 TTL） |

## 定时任务

| 时间 | 任务 | 实现 |
| --- | --- | --- |
| 每天 10:00 | 抓取昨日 GitHub 提交 | `CommitStatistics.scheduleCommitStat` → `@Async("commitStatExecutor")` |
| 每天 17:06 | 重算笔记统计 | `updateCurrentNoteStatistics.as` |

线程池 `commitStatExecutor`：核心 2 / 最大 4 / 队列 10。

## 已知限制与待办

- **硬编码路径**：`SystemConstants` 里写死了 Windows 路径（Markdown 源目录、上传目录、静态图片目录），`commonController` 里也有一份 `E:\obsdian\blog\...`。换机器 / 部署到 Linux 必须改成配置项。
- **上传后缀大小写**：`/file/upload`、`/file/setImage` 只认小写 `png` / `jpg`，`.PNG` 会被拒绝。
- **上传返回地址**：`/file/upload` 返回的 `url` 硬编码为 `http://localhost:8080/...`。
- **分页缓存 key**：`getBriefWritings` 用固定 key `PageSummaryWriting` 缓存，翻页会命中同一份数据；置顶时会删掉它，但删除文章只清单篇缓存，列表缓存仍会残留最多 38 分钟。
- **鉴权范围**：`SecurityConfig` 目前 `permitAll()`，实际鉴权只靠 `LoginFilter` 拦 `/admin/**`；`/file/upload`、`/file/setImage` 等写接口对外公开，公网部署前建议收紧。
- **登录限流**：`loginCount` 是全局计数（非按 IP），累计 5 次失败后锁定；登录成功会删除该 key，但失败累加时 Redis TTL 被覆盖，长时间不登录成功理论上会一直锁着。
- **CDN 依赖**：`cdn.jsdelivr.net` 在国内经常被重置，导致 `markdown-it` / `highlight.js` 加载失败、整页脚本中断，因此这三个库已本地化到 `static/vendor/`（版本见 `static/vendor/NOTICE.md`）；目前只剩 `cdn.tailwindcss.com` 仍走公网，Tailwind 加载失败只影响样式、不影响功能，需要完全离线时同样可以自托管。
- **未使用的依赖**：`flexmark`、`jsoup`、`spring-boot-starter-thymeleaf` 已在 `pom.xml` 中引入，但当前代码没有实际使用（Markdown 渲染在前端由 markdown-it 完成）。
- **目录里的临时产物**：`GithubService.txt`（未启用的草稿）、`test.html` / `admin-test.html`（调试页）、`index备份.html` 不参与主流程。
- **测试类连真实环境**：`BlogApplicationTests` 是手工调试集合，`@SpringBootTest` 会读 `application.yaml` 连远程 MySQL / Redis，跑 `mvn test` 前务必确认配置指向本地。
- 代码中的 TODO：文章缓存防击穿 / 穿透 / 雪崩、后台页面同步更新、在线写作、笔记接口加缓存。

## 部署提示

- 生产环境用外部化配置覆盖敏感项，不要把密码写进镜像：

```bash
java -jar blog-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://db:3306/blog \
  --spring.datasource.username=blog \
  --spring.datasource.password=$DB_PASSWORD \
  --spring.data.redis.host=redis \
  --github.token=$GITHUB_TOKEN \
  --SECRET=$JWT_SECRET
```

- Markdown 源目录在容器内通常不存在，`/admin/update` 与 17:06 的定时任务会扫不到文件；容器化时建议改为挂载卷 + 配置项。
- 头像 / 背景 / 登录页背景的替换直接写入 `src/main/resources/static/images` 这个**固定源码路径**，用 `java -jar` 部署时该目录通常不存在，替换会失败；容器化或生产部署需要改成外部目录 + 静态资源映射。


## 致谢
感谢 **C_lay**、**Cloud-FER**、**梦暗**、**w幻颖**与**夜幕**对本项目给予的大力精神支持（排名不分先后，以首字母排序）
