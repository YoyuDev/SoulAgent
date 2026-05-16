<p align="center">
  <img src="frontend/public/logo.png" width="100" alt="SoulAgent Logo" style="border-radius: 22px;" />
</p>

<h1 align="center">SoulAgent</h1>

<p align="center">
  <strong>具有人格进化能力的 AI 角色扮演系统</strong>
</p>

<p align="center">
  导入微信/QQ聊天记录，AI 自动分析人物性格，生成可对话的 AI 分身<br/>
  支持情绪变化、关系发展、记忆压缩、人格进化、随机事件等高级功能
</p>



## 核心功能

### 🎭 人格模拟系统

**性格分析**
- 大模型自动分析聊天记录
- 提取性格特征、说话风格、常用表达
- 生成完整的角色人格画像

**人格进化**
- 每 50 次对话自动进化人格
- 根据聊天内容动态调整性格特征
- 保留核心特征，微调表达方式

**动态情绪**
- 每次对话后分析当前情绪
- 情绪影响后续对话风格
- 支持多种情绪状态切换

**身份强化**
- AI 完全代入角色身份
- 不是助手，而是角色本人
- 强化自我认知和身份认同

### 💬 智能对话系统

**流式对话**
- SSE 实时流式输出
- 逐字显示 AI 回复（打字机效果）
- 流畅的对话体验

**记忆检索**
- 向量数据库自动召回相关历史对话
- 相似度阈值 0.6
- 智能关联上下文

**记忆压缩**
- 长对话自动生成摘要
- 防止"断片"现象
- 保持对话连贯性

**上下文管理**
- Redis 缓存最近 15 条消息
- 智能管理对话历史
- 平衡性能和体验

### ❤️ 关系建模系统

**多维度关系**
- 追踪亲密度、信任度两个维度
- 独立计算，相互影响
- 真实模拟人际关系发展

**关系阶段**
- 陌生人 → 熟人 → 朋友 → 亲密朋友 → 恋人
- 清晰的阶段标识
- 渐进式发展

**非线性衰减**
- 长时间不互动会降低关系分数
- 非线性的衰减曲线
- 模拟真实人际关系的维护成本

**关系可视化**
- 前端实时显示关系状态
- 直观的分数展示
- 动态更新

### 🎲 随机事件系统

**事件生成**
- 每 6 小时自动生成随机事件
- 6 种事件类型：
  - 日常生活（吃饭、运动、购物等）
  - 情绪波动（开心、难过、焦虑等）
  - 回忆往事（童年、学生时代等）
  - 提问思考（人生哲理、价值观等）
  - 观察细节（天气、环境等）
  - 活动计划（旅行、学习等）

**性格驱动**
- 根据性格判断是否分享
- 外向性格分享率更高
- 内向性格更谨慎

**时间感知**
- 深夜时段降低分享率（22:00-6:00）
- 符合真实社交规律

**独立控制**
- 每个人物可单独开启/关闭
- 创建时默认关闭
- 随时可在菜单中切换

### 🛠️ 角色管理系统

**多角色支持**
- 创建多个 AI 角色
- 独立对话、独立记忆
- 互不干扰

**微信导入**
- 上传微信导出的聊天记录
- 自动解析对话内容
- 快速生成角色画像

**个性配置**
- 自定义 API Key
- 自定义模型
- 自定义向量库配置

**主题切换**
- 深色/浅色主题
- 侧边栏折叠
- 个性化界面

---

## 快速开始

### 环境要求

- **JDK**: 17+
- **Node.js**: 18+
- **Python**: 3.8+（用于微信聊天记录导出）
- **Redis**: 用于对话上下文缓存
- **Docker**: 用于运行 Qdrant（可选）

### 安装步骤

#### 1. 启动基础设施

```bash
# Redis（如果未安装，参考 https://redis.io/docs/install/）
redis-server

# Qdrant 向量数据库（可选，未启动时记忆功能不可用，不影响其他功能）
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

#### 2. 配置 API Key

编辑 `src/main/resources/application.yml`：

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${AI_API_KEY:sk-xxx}   # 替换为你的 API Key
```

也可以在前端「设置」页面中配置。

#### 3. 启动后端

```bash
cd SoulAgent
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

#### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，已配置代理转发 `/api` 到后端。

---

## 使用方式

### 创建角色

1. 打开浏览器访问 `http://localhost:5173`
2. 点击侧边栏「创建人物」按钮
3. 填写角色名称和描述
4. （可选）上传微信导出的聊天记录文件（`.txt` 格式）
5. （可选）开启"随机事件"功能（**默认关闭**）
6. 等待 AI 分析性格并创建角色

### 开始对话

1. 在侧边栏选择要对话的角色
2. 在输入框输入消息
3. AI 会实时流式回复
4. 查看情绪状态和关系进度

### 角色管理

- **切换角色**：点击侧边栏角色列表
- **删除角色**：点击角色右侧「⋮」→「删除角色」
- **随机事件开关**：点击角色右侧「⋮」→「开启/关闭随机事件」
- **清空聊天记录**：点击「设置」→「清空聊天记录」

---

## 微信聊天记录导出

> **⚠️ 注意：本项目技术仅用于学习和技术交流，请勿用于非法用途！**

推荐使用 [PyWxDump](https://github.com/alanhzw/PyWxDump) 导出微信聊天记录。
教程地址CSDN：https://blog.csdn.net/hahai_/article/details/161136466?spm=1001.2014.3001.5501
### 环境要求

- Windows 10/11
- 微信 PC 版（建议 3.9.5，PyWxDump 最高支持此版本）
- Python 3.8+

> **重要提示**：如果当前微信版本高于 3.9.5，需要降级。降级前请备份微信记录目录，避免数据丢失。

### 一键导出

项目提供了 `tools/export_wechat.py` 一键脚本：

```bash
# 完整流程（自动获取密钥 → 解密数据库 → 导出聊天记录）
python tools/export_wechat.py

# 指定导出目录
python tools/export_wechat.py --out E:\微信聊天记录

# 已有解密后的数据库，跳过解密直接导出
python tools/export_wechat.py --db D:\wx_dump\de_MSG0.db --out E:\微信聊天记录
```

脚本会自动完成以下 4 个步骤：

1. **检查环境** — 检测 PyWxDump 是否安装，未安装则自动 `pip install pywxdump`
2. **获取密钥** — 调用 `pywxdump info` 读取微信密钥和数据路径
3. **解密数据库** — 调用 `pywxdump decrypt` 解密聊天记录数据库
4. **导出 txt** — 将每个联系人的聊天记录导出为独立的 `.txt` 文件

### 导出格式

导出的 txt 文件格式：

```
[2024-01-15 09:30:22] 我：早上好
[2024-01-15 09:31:05] TA: 早啊，今天天气不错
[2024-01-15 09:31:30] 我：是的，要不要出去走走
```

---
## 微信聊天记录导出

> **⚠️ 注意：本项目技术仅用于学习和技术交流，请勿用于非法用途！**
## 技术架构
推荐使用 [NapCat](NapNeko/NapCatQQ: Modern protocol-side framework based on NTQQ) 导出QQ聊天记录。
教程地址CSDN：https://blog.csdn.net/hahai_/article/details/161136338?spm=1001.2014.3001.5501

## 其他工具导出
知要保证导入txt格式的聊天记录文件格式为：
```
[2024-01-15 09:30:22] 我：早上好
[2024-01-15 09:31:05] TA: 早啊，今天天气不错
[2024-01-15 09:31:30] 我：是的，要不要出去走走
```
### 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **后端** | Spring Boot 3.4 / Java 17 / MyBatis Plus | 核心业务逻辑 |
| **前端** | Vue 3 / Vite 5 / Element Plus | 用户界面 |
| **AI** | LangChain4j 1.3 / OpenAI Compatible API | 大模型集成 |
| **向量库** | Qdrant（REST API） | 向量记忆存储 |
| **缓存** | Redis | 对话上下文缓存 |
| **数据库** | SQLite | 聊天记录、角色数据 |

### 项目结构

```
SoulAgent/
├── src/main/java/cn/soulagent/
│   ├── controller/                 # REST API 控制器
│   │   ├── CharacterController     # 角色管理
│   │   ├── ChatController          # 对话聊天
│   │   ├── PersonalityController   # 性格管理
│   │   ├── RelationshipController  # 关系查询
│   │   └── RandomEventController   # 随机事件
│   │
│   ├── service/                    # 业务服务层
│   │   ├── AiModelFactory         # AI 模型工厂
│   │   ├── CharacterService       # 角色服务
│   │   ├── ChatService            # 聊天服务
│   │   ├── PersonalityService     # 性格服务
│   │   ├── MemoryService          # 记忆服务
│   │   ├── RelationshipService    # 关系服务
│   │   ├── RandomEventService     # 随机事件服务
│   │   └── RedisService           # Redis 服务
│   │
│   ├── entity/                     # 数据实体
│   │   ├── SoulCharacter          # 角色实体
│   │   ├── ChatMessage            # 聊天消息
│   │   ├── Personality            # 性格实体
│   │   ├── CharacterRelationship  # 关系实体
│   │   ├── ConversationSummary    # 对话摘要
│   │   └── RandomEvent            # 随机事件
│   │
│   ├── mapper/                     # MyBatis Mapper
│   ├── config/                     # 配置类
│   │   ├── QdrantConfig           # Qdrant 配置
│   │   ├── RedisConfig            # Redis 配置
│   │   ├── DatabaseMigration      # 数据库迁移
│   │   └── RandomEventScheduler   # 定时任务
│   │
│   └── skill/                      # 技能系统
│       ├── Skill                  # 技能接口
│       ├── SkillContext           # 技能上下文
│       ├── SkillResult            # 技能结果
│       ├── SkillRouter            # 技能路由
│       └── impl/
│           └── ChatSkill          # 聊天技能实现
│
├── src/main/resources/
│   ├── application.yml            # 应用配置
│   └── schema.sql                 # 数据库建表脚本
│
├── tools/
│   └── export_wechat.py           # 微信聊天记录导出脚本
│
├── frontend/
│   └── src/
│       ├── api/                   # API 调用
│       ├── components/            # Vue 组件
│       │   ├── Sidebar            # 侧边栏
│       │   ├── ChatView           # 对话界面
│       │   ├── CreateCharacterDialog  # 创建角色
│       │   ├── SettingsDialog     # 设置面板
│       │   └── RelationshipPanel  # 关系面板
│       ├── composables/           # 组合式函数
│       └── App.vue                # 主应用
│
└── pom.xml                        # Maven 配置
```

### 数据库设计

#### 1. soul_character（角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| name | TEXT | 角色名称 |
| description | TEXT | 角色描述 |
| avatar | TEXT | 头像 URL |
| random_event_enabled | INTEGER | 随机事件开关（0=关闭，1=开启） |
| last_event_time | INTEGER | 上次事件生成时间 |

#### 2. chat_message（聊天消息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| character_id | INTEGER | 角色 ID |
| sender | TEXT | 发送者（user/assistant） |
| content | TEXT | 消息内容 |
| timestamp | INTEGER | 时间戳 |
| emotion | TEXT | 当时的情绪 |

#### 3. personality（性格表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| character_id | INTEGER | 角色 ID |
| traits | TEXT | 性格特征 |
| speaking_style | TEXT | 说话风格 |
| emotion_baseline | TEXT | 情绪基线 |
| common_phrases | TEXT | 常用短语 |
| current_emotion | TEXT | 当前情绪 |
| conversation_count | INTEGER | 对话次数（用于进化） |

#### 4. character_relationship（关系表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| character_id | INTEGER | 角色 ID |
| intimacy_score | REAL | 亲密度分数 |
| trust_score | REAL | 信任度分数 |
| relationship_stage | TEXT | 关系阶段 |
| total_messages | INTEGER | 总消息数 |
| first_chat_time | INTEGER | 首次聊天时间 |
| last_chat_time | INTEGER | 最后聊天时间 |

#### 5. conversation_summary（对话摘要表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| character_id | INTEGER | 角色 ID |
| summary_text | TEXT | 摘要内容 |
| created_at | INTEGER | 创建时间 |
| message_count | INTEGER | 摘要涵盖的消息数 |

#### 6. random_event（随机事件表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| character_id | INTEGER | 角色 ID |
| event_type | TEXT | 事件类型 |
| event_content | TEXT | 事件内容 |
| event_time | INTEGER | 事件时间 |
| is_shared | INTEGER | 是否已分享 |
| share_time | INTEGER | 分享时间 |

---

## API 接口

### 角色管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/character/list` | 获取角色列表 |
| `POST` | `/api/character/create` | 创建角色（SSE 进度） |
| `POST` | `/api/character/update` | 更新角色信息 |
| `DELETE` | `/api/character/{id}` | 删除角色 |

### 对话聊天

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/chat` | 发送消息（SSE 流式回复） |
| `GET` | `/api/chat/history/{id}` | 获取历史消息（分页） |
| `DELETE` | `/api/chat/history/{id}` | 清空聊天记录 |

### 性格 & 关系

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/personality/{characterId}` | 获取角色性格 |
| `GET` | `/api/relationship/{characterId}` | 获取关系状态 |

### 随机事件

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/random-event/character/{characterId}/unshared` | 获取未分享事件 |
| `GET` | `/api/random-event/character/{characterId}/recent` | 获取最近事件 |
| `POST` | `/api/random-event/{eventId}/share` | 标记为已分享 |

---

## 高级功能

### 人格进化机制

**触发条件**
- 每 50 次对话自动触发人格进化

**进化方式**
- LLM 分析最近对话内容
- 调整性格特征
- 保留核心特征，微调表达方式

**可配置**
- 可通过修改 `PersonalityService.EVOLUTION_INTERVAL` 调整间隔

### 记忆压缩机制

**触发条件**
- 对话超过 30 条消息时自动触发

**压缩方式**
- LLM 生成对话摘要
- 提取关键信息
- 保持上下文连贯

**存储方式**
- 存入 `conversation_summary` 表

**使用方式**
- 下次对话时注入摘要信息
- 作为长期记忆使用

### 随机事件系统

**生成频率**
- 每 6 小时检查一次
- 定时任务自动执行

**事件类型**
- 日常生活（吃饭、运动、购物等）
- 情绪波动（开心、难过、焦虑等）
- 回忆往事（童年、学生时代等）
- 提问思考（人生哲理、价值观等）
- 观察细节（天气、环境等）
- 活动计划（旅行、学习等）

**分享概率计算**

```
基础概率：50%
外向性格：+10% 每项特征
内向性格：-10% 每项特征
深夜时段（22:00-6:00）：×0.7
情绪/提问类事件：×1.2
```

**示例**
```
外向开朗的角色在白天遇到情绪事件：
分享概率 = (50% + 10% + 10%) × 1.2 = 84%

内向安静的角色在深夜遇到日常事件：
分享概率 = (50% - 10% - 10%) × 0.7 = 21%
```

### 关系建模

**关系阶段**

| 阶段 | 亲密度要求 | 信任度要求 |
|------|-----------|-----------|
| 陌生人 | 0-20 | 0-20 |
| 熟人 | 20-40 | 20-40 |
| 朋友 | 40-60 | 40-60 |
| 亲密朋友 | 60-80 | 60-80 |
| 恋人 | 80-100 | 80-100 |

**衰减机制**

- **衰减条件**：超过 24 小时未互动
- **衰减公式**：
  ```
  衰减系数 = max(0.5, 1 - 0.01 × 天数)
  新分数 = 旧分数 × 衰减系数
  ```
- **保护机制**：亲密度和信任度最低降至 0

**关系提升**
- 每次对话微量提升亲密度
- 积极对话内容提升信任度
- 持续性互动获得额外加成

---

## 配置说明

前端「设置」页面支持以下配置项：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| API URL | OpenAI 兼容接口地址 | `https://api.openai.com/v1` |
| API Key | 接口密钥 | — |
| Model Name | 对话模型名称 | `gpt-4o-mini` |
| Embedding API URL | 向量模型接口地址（留空则复用对话配置） | — |
| Embedding API Key | 向量模型密钥 | — |
| Embedding Model Name | 向量模型名称 | `text-embedding-3-small` |

> **支持任何 OpenAI 兼容的 API**，如 DeepSeek、Moonshot、通义千问等。

---

## 常见问题

### Q: 记忆功能不工作？

**A**: 检查 Qdrant 是否启动：
```bash
docker ps | grep qdrant
```
未启动时记忆功能不可用，但其他功能正常。

### Q: 如何更换 API 提供商？

**A**: 在前端「设置」页面修改：
- API URL
- API Key
- 模型名称

支持任何 OpenAI 兼容接口。

### Q: 角色性格不准确？

**A**: 
1. 上传更多微信聊天记录
2. 手动编辑角色描述
3. 性格会在 50 次对话后自动进化

### Q: 随机事件不触发？

**A**: 
1. 检查角色是否开启了随机事件功能（创建时默认关闭）
2. 点击角色右侧「⋮」→「开启随机事件」
3. 等待 6 小时或修改定时任务间隔

### Q: 关系分数不增长？

**A**: 
1. 确保有持续的对话互动
2. 关系增长是渐进式的，需要时间积累
3. 长时间不互动会导致分数衰减

### Q: 对话出现"断片"现象？

**A**: 
1. 确保 Redis 正常运行
2. 检查对话是否超过 30 条（会自动生成摘要）
3. 摘要功能会在长对话时自动启用

---

## 更新日志

### v2.0 (2026-05-13)

**✨ 新功能**
- 人格进化功能：每 50 次对话自动进化
- 关系建模系统：亲密度、信任度双维度追踪
- 随机事件功能：性格驱动的事件分享
- 对话摘要压缩：防止长对话"断片"
- 动态情绪系统：实时情绪分析
- 记忆检索阈值：0.6 相似度门槛
- AI 身份强化：完全代入角色

**🐛 Bug 修复**
- 修复 SQL 注入漏洞
- 修复 API Key 泄露风险
- 修复线程池泄漏问题
- 修复 Qdrant 过滤器异常

**🔧 优化**
- 数据库表结构优化
- 前端用户体验优化
- 错误处理机制完善

### v1.0 (初始版本)

- 基础对话功能
- 性格分析
- 微信聊天记录导入
- 向量记忆
- 流式对话

---

## License

MIT

---

<p align="center">
  <strong>Made with ❤️ by SoulAgent Team</strong>
</p>
