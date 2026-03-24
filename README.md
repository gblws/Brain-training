# Brain Training

一个基于 **Uni-app (Vue 3)** + **Spring Boot 3** 的大脑思维训练应用，包含舒尔特方格、斯特鲁普挑战、记忆矩阵三类训练游戏，并支持训练记录查看。

## 项目结构

```text
Brain-training/
├─ frontend/   # Uni-app 前端（小程序 / H5）
└─ backend/    # Spring Boot 后端（REST API）
```

## 功能概览

- 首页：三类训练游戏卡片入口
- 游戏：
  - 舒尔特方格（5x5，按顺序点击）
  - 斯特鲁普挑战（颜色与文字干扰识别）
  - 记忆矩阵（高亮记忆与复现）
- 历史记录：展示分数、正确率、训练时间
- 个人中心：基础头像与昵称展示（当前为测试页能力）

## 技术栈

- 前端：Uni-app、Vue 3
- 后端：Spring Boot 3.x、Spring Web、Spring Data JPA
- 数据库：MySQL 8.x
- 构建：Maven

## 本地运行

## 1. 后端启动

1. 创建数据库：`brain_train`
2. 配置文件：`backend/src/main/resources/application.yml`
3. 确保以下项正确：
   - `spring.datasource.url`
   - `spring.datasource.username`
   - `spring.datasource.password`
4. 在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

默认后端地址：`http://localhost:8080`

## 2. 前端启动

1. 使用 HBuilderX 打开 `frontend` 目录
2. 运行到浏览器或小程序模拟器
3. 确保前端请求基地址指向本机后端（如 `http://localhost:8080/api/v1`）

## API（当前）

- `GET /api/v1/user/history`：获取历史训练记录
- `POST /api/v1/game/submit`：提交训练结果

统一返回结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 说明

- 当前版本以单机训练为目标，账号体系（登录/注册）后续扩展。
- 历史记录已对接 MySQL 持久化。

## 后续计划

- 用户登录与多用户数据隔离
- 历史记录筛选/分页/统计图
- 排行榜与训练计划
- 游戏参数可配置化
