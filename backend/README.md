# 共享充电宝云平台（Share Power Bank）

基于 **Spring Cloud Alibaba** 微服务架构的共享充电宝租赁系统，覆盖「扫码借宝 → 自动计费 → 归还结算」完整业务闭环，并集成 **EMQX MQTT** 物联网设备接入与 **DeepSeek** AI 订单报表能力。

## 技术栈

| 分类 | 技术 |
| ---- | ---- |
| 微服务 | Spring Boot 3、Spring Cloud Alibaba、Nacos（注册/配置中心）、OpenFeign、Gateway、Sentinel |
| 中间件 | Redis（缓存/分布式锁）、RabbitMQ（延迟队列）、EMQX MQTT（物联网消息）、MySQL + MyBatis-Plus |
| 业务引擎 | Drools 规则引擎（计费规则动态编译热更新）、DeepSeek（Spring AI 集成） |
| 前端 | Vue3 + Element Plus + Vite（后台管理端）、uni-app 微信小程序（用户端） |

## 系统模块

```
share-powerbank
├── share-gateway         // 网关模块            [18080]
├── share-auth            // 认证中心            [9200]
├── share-modules         // 业务模块
│   ├── share-device      // 设备模块（EMQX MQTT 接入充电宝硬件）   [9205]
│   ├── share-order       // 订单模块（借用/归还流程）             [9291]
│   ├── share-payment     // 支付模块（回调 + 幂等处理）           [9211]
│   ├── share-rule        // 计费规则模块（Drools + Redis 缓存）   [9208]
│   ├── share-user        // 用户模块            [9209]
│   ├── share-stastics    // 数据统计模块         [9299]
│   ├── share-system      // 系统管理模块         [9201]
│   ├── share-gen         // 代码生成             [9202]
│   ├── share-job         // 定时任务             [9203]
│   └── share-file        // 文件服务             [9300]
├── share-common          // 通用模块（core/datasource/log/redis/security/swagger 等）
├── share-api             // 接口模块（Feign 接口定义）
├── share-visual          // 图形化管理模块（监控中心 share-monitor [9100]）
├── admin-ui              // 后台管理前端（Vue3 + Element Plus）   [80]
├── ai-report             // AI 订单报表（Spring AI + DeepSeek）  [8899]
└── miniprogram           // 微信小程序（uni-app，扫码借还入口）
```

> 实际端口以 `bootstrap.yml` / Nacos 配置为准，上表为主要模块默认端口。

## 核心业务与技术亮点

### 1. 支付幂等处理
模拟支付成功 / 支付回调通过 **SQL 条件更新** 实现幂等：

```sql
UPDATE order_info SET status = '2', pay_time = NOW()
WHERE order_no = ? AND status = '1'
```

- 只有「待支付（status=1）」的订单才能被更新为「已支付（status=2）」；
- 同一笔订单重复触发支付回调时，第二次条件更新影响行数为 0，直接静默返回，保证不重复扣费/重复发券；
- 采用数据库原子条件更新，避免「先查后改」在并发下的竞态问题。

### 2. Redis 缓存热点数据（计费规则）三重防护
实时计费为最热点读操作，对费用规则做 Redis 缓存并实现三类缓存问题的防护：

- **缓存穿透**：查询不存在的规则时，缓存 3 分钟空值标记，无效查询不再反复打库；
- **缓存击穿**：热点 key 过期瞬间，通过 `SETNX` 分布式互斥锁 + 双检 + 自旋等待，保证只有一个线程回源重建缓存；
- **缓存雪崩**：过期时间 30 分钟 + 0~5 分钟随机抖动，避免大量 key 同一时刻过期。

同时规则修改/删除后主动失效缓存，保证缓存一致性。

### 3. RabbitMQ 延迟队列（未支付自动释放槽位）
创建订单后使用 `x-delayed-message` 延迟队列，**5 秒后**若订单仍未支付，则自动取消订单并释放占用的充电宝槽位，避免资源被无效订单长时间占用。

### 4. Drools 规则引擎动态计费
计费规则以字符串形式存储在数据库中，通过 Drools **动态编译为规则**并执行，后台修改收费标准后**无需重启服务**即可实时生效，支持按充电时长分段计费、免费时长等灵活规则。

### 5. EMQX MQTT 物联网设备接入
充电宝硬件通过 **EMQX** MQTT Broker 接入，采用 **QoS 2（四步握手）** 保证消息不丢失、不重复；实现扫码租借、归还上桩等设备指令的下发与状态上报。

### 6. DeepSeek AI 订单报表
集成 Spring AI + DeepSeek，基于订单/计费数据生成经营分析报告，为运营决策提供辅助。

## 目录结构（GitHub 仓库）

```
├── backend/        // 后端微服务（share-* 全部模块，含 sql/ 数据库脚本）
├── admin-ui/       // 后台管理前端（Vue3 + Element Plus）
├── miniprogram/    // 微信小程序（uni-app）
├── ai-report/      // AI 订单报表模块（Spring AI + DeepSeek）
└── README.md
```

## 本地运行（简要）

1. 启动基础环境：MySQL、Redis、RabbitMQ、Nacos、EMQX；
2. 导入 `backend/sql/` 下的初始化脚本；
3. 在 Nacos 中维护各模块配置与数据库连接信息；
4. 依次启动 `share-auth` → 各业务模块 → `share-gateway`；
5. 前端：`admin-ui` 执行 `npm install && npm run dev`；小程序用微信开发者工具打开 `miniprogram/`。

## 说明

- `ai-report` 模块的 DeepSeek API Key 已在仓库中脱敏为 `${DEEPSEEK_API_KEY}`，运行前请通过环境变量或本地配置文件自行注入，请勿提交真实密钥；
- 项目基于若依 RuoYi-Cloud 3.6.3 脚手架二次开发，业务代码均在本仓库内。
