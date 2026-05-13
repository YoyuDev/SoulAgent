<p align="center">
  <img src="frontend/public/logo.png" width="100" alt="SoulAgent Logo" style="border-radius: 22px;" />
</p>

<h1 align="center">SoulAgent</h1>

<p align="center">
  <strong>基于微信聊天记录的 AI 角色扮演系统</strong>
</p>

<p align="center">
  导入微信聊天记录，AI 自动分析人物性格，生成可对话的 AI 分身
</p>

---

## 功能特性

- **聊天记录导入** — 上传微信导出的 `.txt` 聊天文件，自动解析对话内容
- **AI 性格提取** — 大模型自动分析聊天风格、说话习惯、常用表达，生成角色人格画像
- **向量记忆** — 将历史对话存入 Qdrant 向量数据库，聊天时自动召回相关记忆
- **流式对话** — SSE 实时流式输出，逐字显示 AI 回复（打字机效果）
- **多角色管理** — 支持创建多个 AI 角色，独立对话、独立记忆
- **持久化历史** — 聊天记录存储在 SQLite，支持分页加载历史消息
- **深色/浅色主题** — 内置主题切换，支持侧边栏折叠

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4 / Java 17 / MyBatis Plus |
| 前端 | Vue 3 / Vite 5 / Element Plus |
| AI | LangChain4j 1.3 / OpenAI Compatible API |
| 向量库 | Qdrant（REST API） |
| 缓存 | Redis（对话上下文） |
| 数据库 | SQLite |

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

## 使用方式

1. 打开浏览器访问 `http://localhost:5173`
2. 点击侧边栏「创建人物」
3. 填写角色名称和描述
4. 上传微信导出的聊天记录文件（`.txt` 格式）
5. 等待 AI 分析性格并创建角色
6. 选择角色开始对话

## 微信聊天记录导出

> **注意：本项目技术仅用于学习和技术交流，请勿用于非法用途！**

推荐使用 [PyWxDump](https://github.com/alanhzw/PyWxDump) 导出微信聊天记录。

### 环境要求

- Windows 10/11
- 微信 PC 版（建议 3.9.5，[PyWxDump](https://github.com/alanhzw/PyWxDump) 最高支持此版本）
- Python 3.8+

> 如果当前微信版本高于 3.9.5，需要降级。降级前请备份微信记录目录，避免数据丢失。

### 一键导出

项目提供了 `tools/export_wechat.py` 一键脚本，自动完成全部流程：

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
[2024-01-15 09:30:22] 我: 早上好
[2024-01-15 09:31:05] TA: 早啊，今天天气不错
[2024-01-15 09:31:30] 我: 是的，要不要出去走走
```

### 手动导出

如果一键脚本不适用，也可以手动执行：

```bash
# 1. 安装 PyWxDump
pip install pywxdump

# 2. 登录微信后，获取密钥
python -m pywxdump.cli info

# 3. 解密数据库（替换 key 和路径）
python -m pywxdump.cli decrypt ^
  -k 你的key ^
  -i "C:\Users\你的用户名\Documents\WeChat Files\wxid_xxx\Msg\Multi\MSG0.db" ^
  -o D:\wx_dump

# 4. 解密成功后，用 Python 脚本导出（见 tools/export_wechat.py 中的 export_to_txt 函数）
```

## 项目结构

```
SoulAgent/
├── src/main/java/cn/soulagent/
│   ├── controller/          # REST API
│   │   ├── CharacterController.java   # 角色管理
│   │   └── ChatController.java        # 对话 & 历史记录
│   ├── service/
│   │   ├── AiModelFactory.java        # AI 模型工厂（支持自定义 API）
│   │   ├── CharacterService.java      # 角色创建流程
│   │   ├── ChatService.java           # 流式对话
│   │   ├── PersonalityService.java    # 性格分析
│   │   ├── MemoryService.java         # 向量记忆
│   │   └── RedisService.java          # 对话上下文缓存
│   ├── entity/              # 数据实体
│   ├── mapper/              # MyBatis Mapper
│   ├── config/              # 配置（Qdrant 等）
│   └── dto/                 # 数据传输对象
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   └── schema.sql           # 数据库建表
├── tools/
│   └── export_wechat.py     # 微信聊天记录一键导出脚本
├── frontend/
│   └── src/
│       ├── api/             # API 调用
│       ├── components/      # Vue 组件
│       │   ├── Sidebar.vue         # 侧边栏
│       │   ├── ChatView.vue        # 对话界面
│       │   ├── CreateCharacterDialog.vue  # 创建角色
│       │   └── SettingsDialog.vue  # 设置面板
│       ├── composables/     # 组合式函数
│       └── App.vue          # 主应用
└── pom.xml
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/character/list` | 获取角色列表 |
| `POST` | `/api/character/create` | 创建角色（SSE 进度） |
| `DELETE` | `/api/character/{id}` | 删除角色 |
| `POST` | `/api/chat` | 发送消息（SSE 流式回复） |
| `GET` | `/api/chat/history/{id}` | 获取历史消息（分页） |
| `DELETE` | `/api/chat/history/{id}` | 清空聊天记录 |

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

## License

MIT
