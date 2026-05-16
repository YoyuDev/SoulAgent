<p align="center">
  <img src="frontend/public/logo.png" width="100" alt="SoulAgent Logo" style="border-radius: 22px;" />
</p>

<h1 align="center">SoulAgent</h1>

<p align="center">
  <strong>具有人格进化能力的 AI 角色扮演系统</strong>
</p>

<p align="center">
  导入微信聊天记录，AI 自动分析人物性格，生成可对话的 AI 分身<br/>
  支持情绪变化、关系发展、记忆压缩、人格进化、随机事件等高级功能
</p>

---

## 核心功能

### 🎭 人格模拟
- **性格分析** — 大模型自动分析聊天记录，提取性格特征、说话风格、常用表达
- **人格进化** — 每 50 次对话自动进化人格，根据聊天内容调整性格特征
- **动态情绪** — 每次对话后分析当前情绪，影响后续对话风格
- **身份强化** — AI 完全代入角色身份，不是助手而是角色本人

### 💬 智能对话
- **流式对话** — SSE 实时流式输出，逐字显示 AI 回复（打字机效果）
- **记忆检索** — 向量数据库自动召回相关历史对话（相似度阈值 0.6）
- **记忆压缩** — 长对话自动生成摘要，防止"断片"现象
- **上下文管理** — Redis 缓存最近 15 条消息，保持对话连贯性

### ❤️ 关系建模
- **多维度关系** — 追踪亲密度、信任度两个维度
- **关系阶段** — 陌生人 → 熟人 → 朋友 → 亲密朋友 → 恋人
- **非线性衰减** — 长时间不互动会降低关系分数
- **关系可视化** — 前端实时显示当前关系状态和分数

### 🎲 随机事件
- **事件生成** — 每 6 小时自动生成随机事件（日常生活、情绪、回忆等）
- **性格驱动** — 根据性格判断是否分享（外向性格分享率更高）
- **时间感知** — 深夜时段降低分享率（22:00-6:00）
- **独立控制** — 每个人物可单独开启/关闭随机事件功能

### 🛠️ 角色管理
- **多角色支持** — 创建多个 AI 角色，独立对话、独立记忆
- **微信导入** — 上传微信导出的聊天记录文件，自动解析
- **个性配置** — 支持自定义 API Key、模型、向量库配置
- **主题切换** — 深色/浅色主题，侧边栏折叠

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4 / Java 17 / MyBatis Plus |
| 前端 | Vue 3 / Vite 5 / Element Plus |
| AI | LangChain4j 1.3 / OpenAI Compatible API |
| 向量库 | Qdrant（REST API） |
| 缓存 | Redis（对话上下文） |
| 数据库 | SQLite（聊天记录、角色数据） |

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Python 3.8+（用于微信聊天记录导出）
- Redis
- Docker（用于运行 Qdrant，可选）

### 1. 启动基础设施

```bash
# Redis（如果未安装，参考 https://redis.io/docs/install/）
redis-server

# Qdrant 向量数据库（可选，未启动时记忆功能不可用，不影响其他功能）
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`：

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${AI_API_KEY:sk-xxx}   # 替换为你的 API Key
```

也可以在前端「设置」页面中配置 API Key、API 地址和模型名称（支持任何 OpenAI 兼容接口）。

### 3. 启动后端

```bash
cd SoulAgent
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 4. 启动前端

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
5. （可选）开启"随机事件"功能（默认关闭）
6. 等待 AI 分析性格并创建角色

### 开始对话

1. 在侧边栏选择要对话的角色
2. 在输入框输入消息
3. AI 会实时流式回复
4. 查看情绪状态和关系进度

### 管理角色

- **切换角色**：点击侧边栏角色列表
- **删除角色**：点击角色右侧「⋮」→「删除角色」
- **随机事件开关**：点击角色右侧「⋮」→「开启/关闭随机事件」
- **清空聊天记录**：点击「设置」→「清空聊天记录」

---

## 微信聊天记录导出

> **注意：本项目技术仅用于学习和技术交流，请勿用于非法用途！**

推荐使用 [PyWxDump](https://github.com/alanhzw/PyWxDump) 导出微信聊天记录。

### 环境要求

- Windows 10/11
- 微信 PC 版（建议 3.9.5，PyWxDump 最高支持此版本）
- Python 3.8+

> 如果当前微信版本高于 3.9.5，需要降级。降级前请备份微信记录目录，避免数据丢失。

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

导出的 txt 文件格式：

```
[2024-01-15 09:30:22] 我：早上好
[2024-01-15 09:31:05] TA: 早啊，今天天气不错
[2024-01-15 09:31:30] 我：是的，要不要出去走走
```

---

## 项目结构

```
SoulAgent/
├── src/main/java/cn/soulagent/
│   ├── controller/
│   │   ├── CharacterController.java      # 角色管理
│   │   ├── ChatController.java           # 对话 & 历史记录
│   │   ├── PersonalityController.java    # 性格管理
│   │   ├── RelationshipController.java   # 关系查询
│   │   └── RandomEventController.java    # 随机事件
│   ├── service/
│   │   ├── AiModelFactory.java           # AI 模型工厂（支持自定义 API）
│   │   ├── CharacterService.java         # 角色创建流程
│   │   ├── ChatService.java              # 流式对话
│   │   ├── PersonalityService.java       # 性格分析 & 进化
│   │   ├── MemoryService.java            # 向量记忆 & 摘要压缩
│   │   ├── RelationshipService.java      # 关系建模
│   │   ├── RandomEventService.java       # 随机事件生成
│   │   └── RedisService.java             # 对话上下文缓存
│   ├── entity/
│   │   ├── SoulCharacter.java            # 角色实体
│   │   ├── ChatMessage.java              # 聊天消息
│   │   ├── Personality.java              # 性格实体
│   │   ├── CharacterRelationship.java    # 关系实体
│   │   ├── ConversationSummary.java      # 对话摘要
│   │   └── RandomEvent.java              # 随机事件
│   ├── mapper/                # MyBatis Mapper
│   ├── config/                # 配置（Qdrant、调度任务等）
│   └── skill/                 # 技能系统
│       ├── Skill.java         # 技能接口
│       ├── SkillContext.java  # 技能上下文
│       ├── SkillResult.java   # 技能结果
│       ├── SkillRouter.java   # 技能路由
│       └── impl/
│           └── ChatSkill.java # 聊天技能实现
├── src/main/resources/
│   ├── application.yml        # 应用配置
│   └── schema.sql             # 数据库建表
├── tools/
│   └── export_wechat.py       # 微信聊天记录一键导出脚本
├── frontend/
│   └── src/
│       ├── api/               # API 调用
│       ├── components/
│       │   ├── Sidebar.vue           # 侧边栏
│       │   ├── ChatView.vue          # 对话界面
│       │   ├── CreateCharacterDialog.vue  # 创建角色
│       │   ├── SettingsDialog.vue    # 设置面板
│       │   └── RelationshipPanel.vue # 关系面板
│       ├── composables/        # 组合式函数
│       └── App.vue             # 主应用
└── pom.xml
```

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

> 支持任何 OpenAI 兼容的 API，如 DeepSeek、Moonshot、通义千问等。

---

## 数据库表结构

### soul_character（角色表）
- `id` - 主键
- `name` - 角色名称
- `description` - 角色描述
- `avatar` - 头像 URL
- `random_event_enabled` - 随机事件开关（0=关闭，1=开启）
- `last_event_time` - 上次事件生成时间

### chat_message（聊天消息表）
- `id` - 主键
- `character_id` - 角色 ID
- `sender` - 发送者（user/assistant）
- `content` - 消息内容
- `timestamp` - 时间戳
- `emotion` - 当时的情绪

### personality（性格表）
- `id` - 主键
- `character_id` - 角色 ID
- `traits` - 性格特征
- `speaking_style` - 说话风格
- `emotion_baseline` - 情绪基线
- `common_phrases` - 常用短语
- `current_emotion` - 当前情绪
- `conversation_count` - 对话次数（用于进化）

### character_relationship（关系表）
- `id` - 主键
- `character_id` - 角色 ID
- `intimacy_score` - 亲密度分数
- `trust_score` - 信任度分数
- `relationship_stage` - 关系阶段
- `total_messages` - 总消息数
- `first_chat_time` - 首次聊天时间
- `last_chat_time` - 最后聊天时间

### conversation_summary（对话摘要表）
- `id` - 主键
- `character_id` - 角色 ID
- `summary_text` - 摘要内容
- `created_at` - 创建时间
- `message_count` - 摘要涵盖的消息数

### random_event（随机事件表）
- `id` - 主键
- `character_id` - 角色 ID
- `event_type` - 事件类型
- `event_content` - 事件内容
- `event_time` - 事件时间
- `is_shared` - 是否已分享
- `share_time` - 分享时间

---

## 高级功能说明

### 人格进化机制

- **触发条件**：每 50 次对话自动触发
- **进化方式**：LLM 分析最近对话内容，调整性格特征
- **增量更新**：保留核心特征，微调表达方式
- **可配置**：可通过修改 `PersonalityService.EVOLUTION_INTERVAL` 调整间隔

### 记忆压缩机制

- **触发条件**：对话超过 30 条消息时
- **压缩方式**：LLM 生成对话摘要
- **存储方式**：存入 `conversation_summary` 表
- **使用方式**：下次对话时注入摘要信息

### 随机事件系统

- **生成频率**：每 6 小时检查一次
- **事件类型**：
  - 日常生活（吃饭、运动、购物等）
  - 情绪波动（开心、难过、焦虑等）
  - 回忆往事（童年、学生时代等）
  - 提问思考（人生哲理、价值观等）
  - 观察细节（天气、环境等）
  - 活动计划（旅行、学习等）
- **分享概率**：
  - 基础概率：50%
  - 外向性格：+10% 每项特征
  - 内向性格：-10% 每项特征
  - 深夜时段（22:00-6:00）：×0.7
  - 情绪/提问类事件：×1.2

### 关系衰减机制

- **衰减条件**：超过 24 小时未互动
- **衰减公式**：
  ```
  衰减系数 = max(0.5, 1 - 0.01 × 天数)
  新分数 = 旧分数 × 衰减系数
  ```
- **保护机制**：亲密度和信任度最低降至 0

---

## 常见问题

### Q: 记忆功能不工作？
A: 检查 Qdrant 是否启动：`docker ps | grep qdrant`。未启动时记忆功能不可用，但其他功能正常。

### Q: 如何更换 API 提供商？
A: 在前端「设置」页面修改 API URL、API Key 和模型名称。支持任何 OpenAI 兼容接口。

### Q: 角色性格不准确？
A: 上传更多微信聊天记录，或手动编辑角色描述。性格会在 50 次对话后自动进化。

### Q: 随机事件不触发？
A: 检查角色是否开启了随机事件功能（创建时默认关闭）。点击角色右侧「⋮」→「开启随机事件」。

---

## License

MIT

---

## 更新日志

### v2.0 (2026-05-13)
- ✨ 新增人格进化功能
- ✨ 新增关系建模系统
- ✨ 新增随机事件功能
- ✨ 新增对话摘要压缩
- ✨ 新增动态情绪系统
- ✨ 新增记忆检索阈值
- ✨ 强化 AI 身份认同
- 🐛 修复安全漏洞（SQL 注入、API Key 泄露）
- 🐛 修复线程池泄漏
- 🐛 修复 Qdrant 过滤器

### v1.0 (初始版本)
- 基础对话功能
- 性格分析
- 微信聊天记录导入
- 向量记忆
- 流式对话
