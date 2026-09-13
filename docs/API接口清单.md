# API 接口清单

## 用户端

- `GET /lottery/public`：公开活动。
- `POST /lottery/auth/test-login`：开发测试登录。
- `GET /lottery/me`：用户、次数、助力和券包。
- `POST /lottery/check-in`：签到。
- `POST /lottery/assist`：好友助力。
- `POST /lottery/draw`：幂等抽奖。
- `GET /lottery/tickets`：奖券列表。

## 微信

- `GET /lottery/wechat/status`：检查是否已配置。
- `GET /lottery/wechat/authorize`：跳转 OAuth。
- `GET /lottery/wechat/callback`：授权回调。
- `GET /lottery/wechat/js-config`：JS-SDK 签名。

## 商户端

- `GET /lottery/merchant/tickets?keyword=`：本店奖券与用户搜索。
- `POST /lottery/merchant/redeem`：核销。

## 管理端

- `GET /lottery/admin/overview`：活动、奖池、商户、规则及统计。
- `PUT /lottery/admin/activity`：活动配置。
- `POST /lottery/admin/prizes`、`DELETE /lottery/admin/prizes/{id}`：奖品维护。
- `PUT /lottery/admin/prizes/{id}/move?direction=up|down`：调整转盘奖品顺序。
- `POST /lottery/admin/merchants`：商户及登录账号维护。
- `GET /lottery/admin/users`、`GET /lottery/admin/tickets`：搜索和统计。
- `PUT /lottery/admin/users/{id}/enabled?enabled=true|false`：启用或停用参与用户。
- `GET/POST/DELETE /lottery/admin/directives`：隐藏定向中奖。
- `PUT /lottery/admin/rules`：规则保存。
- `POST /lottery/admin/upload-image`：安全图片上传。
