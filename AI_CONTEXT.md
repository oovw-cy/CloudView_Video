# AI Context

本项目是 CloudView_Video，一个基于 Spring Boot / Spring Cloud 的视频平台微服务系统。

## 模块说明

- Common：公共响应、异常处理、JWT 工具、常量、通用模型
- GateWay：统一网关、路由转发、JWT 鉴权
- UserService：用户注册、登录、用户信息、关注粉丝等能力
- VideoActionService：视频投稿、点赞、投币、收藏、评论、弹幕、文件上传等核心业务
- SearchSyncService：搜索、ES 同步、Canal 增量同步
- RealtimeService：WebSocket、实时弹幕、消息推送

## AI 改造目标

基于文档中的 AI 节点把控思想，将 AI 接入研发流程，实现：

- PR 自动 Code Review
- Critical / Warning / Info 问题分级
- 自动生成修复建议
- 一键生成修复 patch
- 单元测试建议
- 质量评分
- 进入人工 CR 前的 AI 预检查

## 重点评审方向

- GateWay 鉴权是否可被绕过
- JWT / Redis token 校验是否健壮
- Controller 参数校验是否充分
- 点赞、收藏、投币、评论是否存在并发一致性问题
- MyBatis XML 是否存在慢 SQL 或索引风险
- MinIO 文件上传是否存在安全问题
- RocketMQ 消息消费是否幂等
- WebSocket 连接是否存在泄漏风险
- application.yml 是否存在敏感配置硬编码
- 单元测试是否覆盖核心业务分支