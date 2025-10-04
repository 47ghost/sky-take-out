# 苍穹外卖

## 1. 项目简介

本项目是基于黑马程序员“苍穹外卖”课程的学习与实践项目。在原课程的基础上，进行了一系列的现代化技术栈升级和个人探索，旨在深化对后端主流框架的理解和应用。

与原版项目相比，本项目主要进行了以下技术迭代：
- **框架升级**: 从 Spring Boot 2.x 升级到 **Spring Boot 3.x**，并适配 Jakarta EE 9+ 规范。
- **持久层重构**: 使用 **Spring Data JPA** 全面替代了原有的 MyBatis，以更加面向对象的方式进行数据访问。
- **安全框架整合**: 引入 **Spring Security** 并结合 **JWT**，实现了更加健壮和标准化的认证与授权机制。
- **API文档**: 采用 **SpringDoc OpenAPI** 替代原有的 Knife4j/Swagger2，以更好地兼容 Spring Boot 3。

通过这些改进，项目不仅跟上了当前的技术潮流，也为后续的维护和功能扩展打下了坚实的基础。

## 2. 技术栈

| 技术分类 | 技术名称 | 版本/说明 |
| --- | --- | --- |
| **核心框架** | Spring Boot | 3.x |
| **安全框架** | Spring Security | 整合 JWT 实现认证授权 |
| **持久层** | Spring Data JPA | 替代 MyBatis，简化数据访问 |
| **数据库** | MySQL | 关系型数据库 |
| **缓存** | Spring Cache & Redis | 使用 Redis 作为缓存中间件 |
| **API 文档** | SpringDoc OpenAPI | 兼容 Spring Boot 3 的 Swagger UI 实现 |
| **JSON 处理** | Fastjson | 高性能 JSON 库 |
| **令牌技术** | JJWT | Java JWT 库 |
| **实时通信** | Spring WebSocket | 用于实现后台向前端的实时消息推送 |
| **文件存储** | 阿里云 OSS | 对象存储服务 |
| **工具库** | Lombok, Apache Commons Lang | 简化代码 |
| **构建工具** | Maven | 项目管理与构建 |
| **运行环境** | Java 17 | |

## 3. 开发重点

### 3.1 依赖继承与多模块管理

项目采用 Maven 多模块架构，结构清晰，便于维护和扩展：
- `sky-take-out` (父模块): 通过 `dependencyManagement` 统一管理项目所有依赖的版本，确保版本一致性。
- `sky-common`: 通用工具模块，封装了常量、工具类、通用配置等。
- `sky-pojo`: 数据模型模块，包含所有实体类 (Entity)、数据传输对象 (DTO) 和视图对象 (VO)。
- `sky-server`: 核心业务模块，包含了 Controller, Service, Repository 以及所有业务逻辑。

### 3.2 Spring Boot 3 升级

项目从一开始就基于 Spring Boot 3.x 构建。这意味着：
- **Java 版本**: 项目需要 Java 17 作为最低运行环境。
- **Jakarta EE**: 所有 `javax.*` 包名的依赖都已替换为 `jakarta.*`，例如 `javax.servlet` -> `jakarta.servlet`。

### 3.3 Spring Data JPA 替代 MyBatis

为了实现更快的开发效率和更少的 SQL 编写，本项目使用 Spring Data JPA 作为持久层框架。
- **Repository层**: 通过继承 `JpaRepository`，无需编写任何 SQL 即可实现单表的 CRUD 和分页查询。
- **复杂查询**: 对于复杂查询场景，使用 JPQL (Java Persistence Query Language) 或 Specification 来实现，保证了代码的面向对象特性。

### 3.4 Spring Security + JWT 整合

项目摒弃了原课程中简单的拦截器校验方式，采用了业界标准的 Spring Security 框架来处理安全问题。
- **认证流程**: 用户登录时，生成 JWT 令牌；后续请求中，通过自定义的 JWT 过滤器解析令牌，并将认证信息加载到 Spring Security 的上下文中。
- **授权管理**: 利用 Spring Security 的注解（如 `@PreAuthorize`）或配置，可以方便地实现基于角色的接口访问控制。

### 3.5 SpringDoc OpenAPI API文档

为适配 Spring Boot 3，项目使用 `springdoc-openapi-starter-webmvc-ui` 生成 API 文档。启动项目后，访问 `http://localhost:8080/doc.html` 即可查看和测试所有 API 接口。

## 4. 部署指南

### 4.1 环境要求
- **Java**: 17 或更高版本
- **Maven**: 3.6 或更高版本
- **MySQL**: 5.7 或更高版本
- **Redis**: 任意稳定版本

### 4.2 配置
1.  克隆或下载本仓库到本地。
2.  在 MySQL 中创建名为 `sky_take_out` 的数据库。
3.  修改`sky-server/src/main/resources/application.yml`指定profiles为生产环境。并添加生产环境配置学习 `sky-server/src/main/resources/application-prod.yml` 文件，配置以下信息：
    - `spring.datasource`: 数据库连接信息（地址、用户名、密码）。
    - `spring.data.redis`: Redis 连接信息（地址、密码）。
    - `sky.alioss`: 阿里云 OSS 配置信息。
    - `sky.jwt`: JWT 令牌配置信息。
    - `sky.wechat`: 微信支付相关配置。

### 4.3 构建与运行
1.  在项目根目录（`sky-take-out/`）下，执行 Maven 构建命令：
    ```shell
    mvn clean package
    ```
2.  构建成功后，运行 `sky-server` 模块：
    ```shell
    java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
    ```
3.  服务启动后，即可访问 `http://localhost:8080`。

