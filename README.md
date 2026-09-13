# 校园幸运抽奖（若依重构版）

本项目以若依 Spring Boot 2 为底座，使用 Java 8、Spring Boot 2.5.15、MyBatis-Plus、MySQL 8、Redis，以及 Vue 3 + Vite + JavaScript。

## 目录

- `backend/`：若依后端与 `ruoyi-lottery` 抽奖业务模块。
- `frontend/`：Vue 3 三端前端。
- `sql/`：若依基础 SQL 与抽奖业务 SQL。
- `docs/`：设计和实施文档。

## 本地启动

1. 创建 MySQL 8 数据库 `wechat_lottery_java`，依次执行 `backend/sql/ry_20260417.sql` 与 `sql/lottery_schema.sql`。
   初始化管理员账号为 `admin`，初始密码为 `admin`；仅用于首次登录，上线前必须修改。
   如需联调演示数据，再执行 `sql/test_data.sql`。该脚本可重复执行，测试记录均带有“[测试]”标记。
2. 启动 Redis，并按 `.env.example` 配置本机环境变量。
3. 在 `backend` 执行 `mvn clean package -DskipTests`，运行 `ruoyi-admin/target/ruoyi-admin.jar`。
4. 在 `frontend` 执行 `npm install`、`npm run dev`。

真实微信密钥和线上数据库密码只能配置在部署环境中，禁止提交到 Git。

## 测试账号与数据

- 管理员：`admin / admin`
- 商户：`test1 / test1`、`test2 / test2`、`test3 / test3`
- 测试用户：`18800000001` 至 `18800000008`，验证码统一为 `1234`
- 演示数据包含 3 个商户、6 个奖品、8 个用户、5 条抽奖记录和 4 张奖券（其中 2 张已核销），用于核对各端统计和库存变化。

测试账号仅供本地验收。正式部署前必须删除测试数据并更换所有默认密码。
