<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="事件记录"
    width="480px"
    class="event-dialog"
  >
    <div v-if="character" class="event-body">
      <div class="event-subtitle">
        {{ character.name }} · 共 {{ events.length }} 条事件
      </div>

      <div v-if="loading" class="event-empty">加载中...</div>
      <div v-else-if="events.length === 0" class="event-empty">暂无事件记录</div>

      <div v-else class="event-list">
        <div v-for="e in events" :key="e.id" class="event-item">
          <div class="event-line">
            <span class="event-dot" :class="e.eventType"></span>
          </div>
          <div class="event-main">
            <div class="event-head">
              <span class="event-type" :class="e.eventType">{{ typeLabel(e.eventType) }}</span>
              <span class="event-time">{{ formatTime(e.eventTime) }}</span>
              <span class="event-shared" :class="{ on: e.isShared }">
                {{ e.isShared ? '已分享' : '未分享' }}
              </span>
            </div>
            <div class="event-content">{{ cleanContent(e.eventContent) }}</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getRecentEvents } from '../api/index.js'

const LIMIT = 100

const TYPE_LABELS = {
  daily_life: '日常生活',
  emotion: '情绪',
  memory: '回忆',
  question: '提问',
  observation: '观察',
  activity: '活动'
}

const props = defineProps({
  modelValue: Boolean,
  character: { type: Object, default: null }
})
defineEmits(['update:modelValue'])

const events = ref([])
const loading = ref(false)

watch(() => props.modelValue, async (val) => {
  if (!val || !props.character) return
  events.value = []
  loading.value = true
  try {
    const res = await getRecentEvents(props.character.id, LIMIT)
    events.value = res.data?.events || []
  } catch {
    events.value = []
  } finally {
    loading.value = false
  }
})

function typeLabel(type) {
  return TYPE_LABELS[type] || '事件'
}

function cleanContent(text) {
  return (text || '')
    .replace(/\[想分享给你\]/g, '')
    .replace(/\[暂时不想分享\]/g, '')
    .trim()
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.event-subtitle {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-bottom: 14px;
}

.event-empty {
  padding: 40px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.event-list {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.event-item {
  display: flex;
  gap: 12px;
}

/* 时间线 */
.event-line {
  position: relative;
  width: 12px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}
.event-line::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--border-secondary);
}
.event-item:first-child .event-line::before { top: 12px; }
.event-item:last-child .event-line::before { bottom: calc(100% - 12px); }

.event-dot {
  position: relative;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 8px;
  background: var(--accent);
  flex-shrink: 0;
}

.event-main {
  flex: 1;
  min-width: 0;
  padding-bottom: 18px;
}

.event-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.event-type {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  color: var(--accent);
  background: rgba(16, 163, 127, 0.1);
}

.event-type.emotion { color: #f59e0b; background: rgba(245, 158, 11, 0.12); }
.event-type.question { color: #a855f7; background: rgba(168, 85, 247, 0.12); }
.event-type.memory { color: #3b82f6; background: rgba(59, 130, 246, 0.12); }

.event-time {
  font-size: 12px;
  color: var(--text-muted);
}

.event-shared {
  font-size: 11px;
  color: var(--text-hint);
  margin-left: auto;
}
.event-shared.on { color: var(--accent); }

.event-content {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--bg-tertiary);
  padding: 8px 12px;
  border-radius: 8px;
}
</style>