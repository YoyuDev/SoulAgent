<template>
  <div class="chat-view">
    <!-- 未选择人物 -->
    <div v-if="!character" class="welcome">
      <button
        v-if="showMenuButton"
        class="menu-btn welcome-menu-btn"
        title="角色列表"
        @click="$emit('toggleMenu')"
      >
        <el-icon :size="20"><Menu /></el-icon>
      </button>
      <div class="welcome-logo">
        <img src="/logo.png" alt="SoulAgent" class="welcome-icon" />
        <h2>SoulAgent</h2>
      </div>
      <p>选择一个人物开始对话，或创建一个新人物</p>
    </div>

    <!-- 聊天区 -->
    <template v-else>
      <div class="chat-header">
        <button
          v-if="showMenuButton"
          class="menu-btn"
          title="角色列表"
          @click="$emit('toggleMenu')"
        >
          <el-icon :size="20"><Menu /></el-icon>
        </button>
        <div class="header-profile" title="查看角色资料" @click="$emit('viewInfo')">
          <el-avatar :size="30" :src="character.avatar || undefined">
            {{ character.name?.[0] }}
          </el-avatar>
          <span>{{ character.name }}</span>
        </div>
        <div v-if="emotion || relationship?.stageDesc" class="header-badges">
          <span v-if="emotion" class="emotion-badge">
            <span class="emotion-dot"></span>
            <span class="badge-text">{{ emotion }}</span>
          </span>
          <span v-if="relationship?.stageDesc" class="relationship-badge" :class="{ 'stage-new': relationship.stage === 'stranger' }">
            <span class="badge-text">{{ relationship.stageDesc }}</span>
          </span>
        </div>
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
                <button
                  v-if="msg.content"
                  class="speak-btn"
                  :class="{ speaking: speakingIndex === i }"
                  :title="speakingIndex === i ? '停止朗读' : '朗读'"
                  @click="toggleSpeak(msg.content, i)"
                >
                  <el-icon :size="12">
                    <VideoPause v-if="speakingIndex === i" />
                    <Headset v-else />
                  </el-icon>
                </button>
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
          <button
            class="mic-btn"
            :class="{ recording: recognizing }"
            :disabled="loading"
            :title="recognizing ? '停止语音输入' : '语音输入'"
            @click="toggleVoice"
          >
            <el-icon :size="18"><Microphone /></el-icon>
          </button>
          <button class="send-btn" :disabled="!input.trim() || loading" @click="send">
            <el-icon :size="18"><Promotion /></el-icon>
          </button>
        </div>
        <div class="input-hint">
          <template v-if="recognizing">
            <span class="voice-wave" aria-hidden="true">
              <span
                v-for="n in 5"
                :key="n"
                class="voice-bar"
                :style="{ animationDelay: `${(n - 1) * 0.12}s` }"
              ></span>
            </span>
            <span>正在聆听... 点击麦克风停止</span>
          </template>
          <span v-else>按 Enter 发送</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  character: { type: Object, default: null },
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  hasMoreHistory: { type: Boolean, default: false },
  emotion: { type: String, default: '' },
  relationship: { type: Object, default: null },
  voiceLanguage: { type: String, default: 'zh-CN' },
  ttsEnabled: { type: Boolean, default: true },
  ttsLanguage: { type: String, default: 'zh-CN' },
  ttsVoice: { type: String, default: '' },
  showMenuButton: { type: Boolean, default: false }
})
const emit = defineEmits(['send', 'loadMore', 'viewInfo', 'toggleMenu'])

const input = ref('')
const msgContainer = ref(null)
const textarea = ref(null)
const loadingMore = ref(false)
let skipNextScroll = false

// 语音输入
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
const recognizing = ref(false)
let recognition = null
let baseText = ''
let finalText = ''

// 保留已确认文本，丢弃临时结果
function finalizeInput() {
  input.value = baseText + finalText
}

// 强制销毁当前识别实例，确保麦克风真正停止
function destroyRecognition() {
  if (!recognition) return
  const rec = recognition
  recognition = null
  rec.onstart = rec.onresult = rec.onerror = rec.onend = null
  try {
    rec.abort()
  } catch (e) { /* ignore */ }
}

function stopVoice() {
  recognizing.value = false
  finalizeInput()
  destroyRecognition()
}

function toggleVoice() {
  if (!SpeechRecognition) {
    ElMessage.warning('当前浏览器不支持语音输入，请使用 Chrome 或 Edge')
    return
  }

  if (recognizing.value) {
    stopVoice()
    return
  }

  // 清理可能残留的旧实例
  destroyRecognition()

  baseText = input.value
  finalText = ''

  const rec = new SpeechRecognition()
  rec.lang = props.voiceLanguage || 'zh-CN'
  rec.continuous = true
  rec.interimResults = true

  rec.onresult = (event) => {
    if (recognition !== rec) return
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        finalText += transcript
      } else {
        interim += transcript
      }
    }
    input.value = baseText + finalText + interim
  }

  rec.onerror = (event) => {
    if (recognition !== rec) return
    recognizing.value = false
    const err = event.error
    console.warn('[语音输入] 识别错误:', err)
    if (err === 'not-allowed' || err === 'service-not-allowed') {
      ElMessage.error('麦克风权限被拒绝，请在浏览器中允许访问麦克风')
    } else if (err === 'audio-capture') {
      ElMessage.error('未检测到麦克风设备')
    } else if (err === 'no-speech') {
      ElMessage.warning('未检测到语音，请重试')
    } else if (err === 'network') {
      ElMessage.error('无法连接语音识别服务，请检查网络（浏览器语音识别依赖 Google 服务，国内网络下不可用）')
    } else if (err === 'language-not-supported') {
      ElMessage.error('当前语音输入语言不被识别服务支持，请在设置中更换')
    } else if (err !== 'aborted') {
      ElMessage.error('语音识别出错：' + err)
    }
  }

  rec.onend = () => {
    if (recognition !== rec) return
    recognition = null
    recognizing.value = false
    finalizeInput()
  }

  recognition = rec
  recognizing.value = true
  try {
    rec.start()
  } catch (e) {
    recognition = null
    recognizing.value = false
    ElMessage.error('语音识别启动失败，请重试')
  }
}

onBeforeUnmount(() => {
  destroyRecognition()
  stopSpeaking()
})

// 语音输出
const synth = window.speechSynthesis
const speakingIndex = ref(-1)
let currentUtterance = null

function stopSpeaking() {
  currentUtterance = null
  speakingIndex.value = -1
  if (synth) synth.cancel()
}

function speak(text, index) {
  if (!synth) {
    ElMessage.warning('当前浏览器不支持语音朗读，请使用 Chrome 或 Edge')
    return
  }

  stopSpeaking()

  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = props.ttsLanguage || 'zh-CN'
  utterance.rate = 1

  // 指定音色；若系统已无该语音包则回退到按语言自动选择
  if (props.ttsVoice) {
    const picked = (synth.getVoices() || []).find(v => v.name === props.ttsVoice)
    if (picked) utterance.voice = picked
  }

  utterance.onend = () => {
    if (currentUtterance === utterance) stopSpeaking()
  }
  utterance.onerror = () => {
    if (currentUtterance === utterance) stopSpeaking()
  }

  currentUtterance = utterance
  speakingIndex.value = index

  // Chrome 在 cancel() 之后立即 speak() 可能丢弃新的朗读，稍作延迟
  setTimeout(() => {
    if (currentUtterance === utterance) synth.speak(utterance)
  }, 60)
}

function toggleSpeak(text, index) {
  if (speakingIndex.value === index) {
    stopSpeaking()
    return
  }
  speak(text, index)
}

// 回复生成完毕后自动朗读
watch(() => props.loading, (val, oldVal) => {
  if (val || !oldVal) return
  if (!props.ttsEnabled) return

  const last = props.messages[props.messages.length - 1]
  if (!last || last.role !== 'assistant') return

  const text = (last.content || '').trim()
  if (!text || text.startsWith('[')) return

  speak(text, props.messages.length - 1)
})

// 切换角色时停止朗读
watch(() => props.character?.id, () => {
  stopSpeaking()
})

function send() {
  const text = input.value.trim()
  if (!text || props.loading) return
  if (recognizing.value) stopVoice()
  stopSpeaking()
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
  /* 兜底：内容再长也不许把页面撑宽（移动端会被浏览器整体缩放，出现右侧空白） */
  overflow: hidden;
}

.header-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px;
  margin-left: -8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

/* 移动端汉堡按钮（仅手机档显示） */
.menu-btn {
  width: 34px;
  height: 34px;
  margin-left: -6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.menu-btn:hover { background: var(--bg-hover); color: var(--accent); }
.header-profile:hover { background: var(--bg-hover); }

/* 欢迎页左上角的角色列表入口（移动端） */
.welcome { position: relative; }
.welcome-menu-btn {
  position: absolute;
  top: 4px;
  left: 10px;
}

/* 情绪 / 关系徽章容器 */
.header-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 4px;
  min-width: 0;
  flex-wrap: wrap;
}

/* 徽章文案：过长时省略，避免撑宽顶栏 */
.badge-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
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
  min-width: 0;
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

.relationship-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 400;
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
  padding: 3px 10px;
  border-radius: 12px;
  min-width: 0;
}

.stage-new {
  color: #9ca3af;
  background: rgba(156, 163, 175, 0.1);
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

.speak-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-hint);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.speak-btn:hover {
  color: var(--accent);
  background: var(--bg-hover);
}
.speak-btn.speaking {
  color: var(--accent);
  animation: speak-pulse 1.2s ease-in-out infinite;
}

@keyframes speak-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
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

.mic-btn {
  width: 34px; height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
  margin-right: 6px;
}
.mic-btn:hover:not(:disabled) { background: var(--bg-hover); color: var(--accent); }
.mic-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.mic-btn.recording {
  color: #fff;
  background: #ef4444;
  animation: mic-pulse 1.2s ease-in-out infinite;
}
.mic-btn.recording:hover { background: #dc2626; color: #fff; }

@keyframes mic-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.5); }
  50% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
}

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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-hint);
  margin-top: 6px;
  min-height: 18px;
}

/* 语音输入条状律动 */
.voice-wave {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 16px;
}

.voice-bar {
  width: 3px;
  height: 100%;
  border-radius: 2px;
  background: var(--accent);
  transform-origin: center;
  animation: voice-wave 0.9s ease-in-out infinite;
}

@keyframes voice-wave {
  0%, 100% { transform: scaleY(0.25); opacity: 0.5; }
  50% { transform: scaleY(1); opacity: 1; }
}

/* ===== 响应式：手机档（< 768px） ===== */
@media (max-width: 767px) {
  .chat-header {
    height: auto;
    min-height: 48px;
    padding: 4px 10px;
    flex-wrap: wrap;
    row-gap: 4px;
  }

  .header-profile {
    min-width: 0;
    margin-left: -4px;
    padding: 4px;
    gap: 8px;
  }
  .header-profile span {
    max-width: 45vw;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  /* 徽章换到第二行，左边缘与头像对齐，避免撑宽页面 */
  .header-badges {
    flex-basis: 100%;
    margin-left: 0;
    padding-left: 48px;
    gap: 6px;
    flex-wrap: nowrap;
  }

  .emotion-badge,
  .relationship-badge {
    font-size: 11px;
    padding: 2px 8px;
  }

  /* 情绪文案可省略，关系阶段完整保留 */
  .emotion-badge { flex: 1 1 auto; }
  .relationship-badge { flex: 0 0 auto; }

  /* 消息区：减小左右留白，放宽气泡 */
  .message-row {
    padding: 10px 12px;
    gap: 8px;
  }
  .msg-body {
    max-width: 82%;
  }

  /* 输入区：去掉 PC 端 20% 的大留白 */
  .input-area {
    padding: 10px 12px 12px;
  }
  .input-box {
    padding: 6px 6px 6px 12px;
  }

  /* 触控热区放大到 ≥44px */
  .mic-btn,
  .send-btn {
    width: 44px;
    height: 44px;
    border-radius: 10px;
  }
  .menu-btn {
    width: 44px;
    height: 44px;
  }

  /* 朗读按钮：视觉尺寸基本不变，用伪元素把热区撑到 44px */
  .speak-btn {
    position: relative;
    width: 26px;
    height: 26px;
  }
  .speak-btn::after {
    content: '';
    position: absolute;
    inset: -9px;
  }

  .welcome {
    padding: 24px;
    text-align: center;
  }
}
</style>
