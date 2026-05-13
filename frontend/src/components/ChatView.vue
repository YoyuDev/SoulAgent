<template>
  <div class="chat-view">
    <!-- 未选择人物 -->
    <div v-if="!character" class="welcome">
      <div class="welcome-logo">
        <img src="/logo.png" alt="SoulAgent" class="welcome-icon" />
        <h2>SoulAgent</h2>
      </div>
      <p>选择一个人物开始对话，或创建一个新人物</p>
    </div>

    <!-- 聊天区 -->
    <template v-else>
      <div class="chat-header">
        <el-avatar :size="30" :src="character.avatar || undefined">
          {{ character.name?.[0] }}
        </el-avatar>
        <span>{{ character.name }}</span>
        <span v-if="emotion" class="emotion-badge">
          <span class="emotion-dot"></span>
          {{ emotion }}
        </span>
      </div>

      <div class="messages" ref="msgContainer" @scroll="onScroll">
        <div v-if="hasMoreHistory" class="load-more">
          <span v-if="loadingMore" class="loading-text">加载中...</span>
          <span v-else class="load-more-btn" @click="$emit('loadMore')">加载更多历史消息</span>
        </div>
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="message-row"
          :class="msg.role"
        >
          <!-- 角色消息：头像在左 -->
          <template v-if="msg.role === 'assistant'">
            <div class="avatar-col">
              <el-avatar :size="36" :src="character.avatar || undefined">
                {{ character.name?.[0] }}
              </el-avatar>
            </div>
            <div class="msg-body">
              <div class="msg-meta">
                <span class="msg-name">{{ character.name }}</span>
                <span class="msg-time">{{ formatTime(msg.time) }}</span>
              </div>
              <div class="bubble assistant-bubble">
                {{ msg.content }}<span v-if="loading && i === messages.length - 1" class="typing-cursor"></span>
              </div>
            </div>
          </template>

          <!-- 用户消息：头像在右 -->
          <template v-else>
            <div class="msg-body user-body">
              <div class="msg-meta user-meta">
                <span class="msg-time">{{ formatTime(msg.time) }}</span>
                <span class="msg-name">我</span>
              </div>
              <div class="bubble user-bubble">{{ msg.content }}</div>
            </div>
            <div class="avatar-col">
              <el-avatar :size="36" style="background: var(--accent)">U</el-avatar>
            </div>
          </template>
        </div>

        <div v-if="loading && (messages.length === 0 || messages[messages.length - 1]?.role !== 'assistant' || messages[messages.length - 1]?.content === '')" class="message-row assistant">
          <div class="avatar-col">
            <el-avatar :size="36" :src="character.avatar || undefined">
              {{ character.name?.[0] }}
            </el-avatar>
          </div>
          <div class="msg-body">
            <div class="msg-meta">
              <span class="msg-name">{{ character.name }}</span>
            </div>
            <div class="bubble assistant-bubble loading-bubble">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-box">
          <textarea
            v-model="input"
            placeholder="输入消息..."
            rows="1"
            @keydown.enter.exact.prevent="send"
            ref="textarea"
          ></textarea>
          <button class="send-btn" :disabled="!input.trim() || loading" @click="send">
            <el-icon :size="18"><Promotion /></el-icon>
          </button>
        </div>
        <div class="input-hint">按 Enter 发送</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  character: { type: Object, default: null },
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  hasMoreHistory: { type: Boolean, default: false },
  emotion: { type: String, default: '' }
})
const emit = defineEmits(['send', 'loadMore'])

const input = ref('')
const msgContainer = ref(null)
const textarea = ref(null)
const loadingMore = ref(false)
let skipNextScroll = false

function send() {
  const text = input.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  input.value = ''
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function onScroll() {
  if (!msgContainer.value || !props.hasMoreHistory || loadingMore.value) return
  if (msgContainer.value.scrollTop < 50) {
    loadingMore.value = true
    const prevHeight = msgContainer.value.scrollHeight
    skipNextScroll = true
    emit('loadMore')
    nextTick(() => {
      if (msgContainer.value) {
        const newHeight = msgContainer.value.scrollHeight
        msgContainer.value.scrollTop = newHeight - prevHeight
      }
      loadingMore.value = false
    })
  }
}

watch(() => props.messages.length, () => {
  nextTick(() => {
    if (!msgContainer.value) return
    if (skipNextScroll) {
      skipNextScroll = false
      return
    }
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  })
})
</script>

<style scoped>
.chat-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-primary);
  transition: background 0.3s;
}

/* 欢迎页 */
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}
.welcome-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.welcome-icon {
  width: 88px;
  height: 88px;
  border-radius: 22px;
  object-fit: cover;
  box-shadow: 0 8px 32px rgba(16, 163, 127, 0.3);
}
.welcome h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}
.welcome p { color: var(--text-tertiary); font-size: 14px; }

/* 顶栏 */
.chat-header {
  height: 52px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--border-primary);
  font-size: 15px;
  font-weight: 500;
  flex-shrink: 0;
  color: var(--text-primary);
  transition: border-color 0.3s;
}

.emotion-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 400;
  color: var(--accent);
  background: rgba(16, 163, 127, 0.1);
  padding: 3px 10px;
  border-radius: 12px;
  margin-left: 4px;
}

.emotion-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: emotion-pulse 2s ease-in-out infinite;
}

@keyframes emotion-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* 消息区 */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.load-more {
  text-align: center;
  padding: 12px 0 4px;
}
.load-more-btn {
  font-size: 13px;
  color: var(--accent);
  cursor: pointer;
  padding: 6px 16px;
  border-radius: 16px;
  background: var(--bg-tertiary);
  transition: background 0.2s;
}
.load-more-btn:hover {
  background: var(--bg-hover);
}
.loading-text {
  font-size: 13px;
  color: var(--text-muted);
}

.message-row {
  display: flex;
  gap: 12px;
  padding: 12px 24px;
  max-width: 100%;
  transition: background 0.3s;
}

/* 角色消息靠左 */
.message-row.assistant {
  justify-content: flex-start;
}

/* 用户消息靠右 */
.message-row.user {
  justify-content: flex-end;
}

.avatar-col {
  flex-shrink: 0;
  padding-top: 2px;
}

.msg-body {
  max-width: 65%;
  min-width: 0;
}

.user-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.user-meta {
  flex-direction: row-reverse;
}

.msg-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.msg-time {
  font-size: 11px;
  color: var(--text-muted);
}

.bubble {
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary);
  padding: 10px 14px;
  border-radius: 12px;
}

.assistant-bubble {
  background: var(--msg-assistant);
  border-top-left-radius: 4px;
}

.user-bubble {
  background: var(--msg-user);
  border-top-right-radius: 4px;
}

/* 打字光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--text-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursor-blink 0.8s infinite;
}
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 打字动画 */
.loading-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 12px 16px;
}
.dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--text-tertiary);
  animation: blink 1.4s infinite both;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}

/* 输入区 */
.input-area {
  padding: 12px 20% 16px;
  flex-shrink: 0;
}

.input-box {
  display: flex;
  align-items: flex-end;
  background: var(--bg-input);
  border-radius: 14px;
  padding: 8px 8px 8px 16px;
  border: 1px solid var(--border-secondary);
  transition: border-color 0.2s, background 0.3s;
}
.input-box:focus-within { border-color: var(--border-focus); }

.input-box textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  max-height: 150px;
  font-family: inherit;
}
.input-box textarea::placeholder { color: var(--text-muted); }

.send-btn {
  width: 34px; height: 34px;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
  flex-shrink: 0;
}
.send-btn:hover { background: var(--accent-hover); }
.send-btn:disabled { background: var(--text-muted); cursor: not-allowed; }

.input-hint {
  text-align: center;
  font-size: 12px;
  color: var(--text-hint);
  margin-top: 6px;
}
</style>
