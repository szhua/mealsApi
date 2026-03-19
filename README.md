# 三餐记食 API 服务

基于 Spring Boot 的三餐记食小程序后端 API 服务。

## 技术栈

- Java 17
- Spring Boot 3.2.3
- MyBatis-Flex 1.8.5
- H2 数据库
- JWT 认证

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 运行项目

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/mealsapi-1.0.0.jar
```

服务启动后：
- API 地址: http://localhost:8080/api
- 后台管理: http://localhost:8080/admin
- H2 控制台: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:file:./data/meals)

## API 接口

### 用户相关
- `POST /api/user/login` - 微信登录
- `POST /api/user/phone` - 绑定手机号
- `GET /api/user/info` - 获取用户信息
- `PUT /api/user/info` - 更新用户信息

### 分类
- `GET /api/categories` - 获取分类列表

### 菜品
- `GET /api/dishes` - 获取菜品列表
- `GET /api/dishes/{id}` - 获取菜品详情
- `POST /api/dishes` - 添加菜品
- `PUT /api/dishes/{id}` - 更新菜品
- `DELETE /api/dishes/{id}` - 删除菜品
- `POST /api/dishes/batch` - 批量获取菜品
- `GET /api/dishes/{id}/in-plans` - 检查菜品是否在规划中

### 规划
- `GET /api/plans` - 获取规划列表
- `GET /api/plans/{id}` - 获取规划详情
- `GET /api/plans/today` - 获取今日规划
- `POST /api/plans` - 创建规划
- `PUT /api/plans/{id}` - 更新规划
- `DELETE /api/plans/{id}` - 删除规划
- `GET /api/plans/check-conflict` - 检查日期冲突
- `GET /api/plans/planned-dates` - 获取已规划日期
- `POST /api/plans/calculate-nutrition` - 计算营养

### 打卡
- `GET /api/streak` - 获取打卡记录
- `POST /api/streak/check-in` - 打卡

### 文件上传
- `POST /api/upload/image` - 上传图片

## 微信小程序配置

在 `application.yml` 中配置你的微信小程序信息：

```yaml
wechat:
  appid: your_appid
  secret: your_secret
```

## 数据库表结构

- `t_user` - 用户表
- `t_category` - 分类表
- `t_dish` - 菜品表
- `t_plan` - 规划表
- `t_plan_day` - 每日规划表
- `t_streak` - 打卡记录表
- `t_check_in` - 打卡历史表

## 错误码

- `0` - 成功
- `1xxx` - 通用错误
- `2xxx` - 菜品相关错误
- `3xxx` - 规划相关错误
- `4xxx` - 文件相关错误
- `5xxx` - 用户相关错误