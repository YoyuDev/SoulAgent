<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="角色资料"
    width="460px"
    class="info-dialog"
  >
    <div v-if="character" class="info-body">
      <div class="info-header">
        <el-avatar :size="72" :src="character.avatar || undefined">
          {{ character.name?.[0] }}
        </el-avatar>
        <div class="info-title">
          <h3>{{ character.name }}</h3>
          <div class="info-badges">
            <span v-if="emotion" class="badge emotion">
              <span class="dot"></span>{{ emotion }}
            </span>
            <span class="badge stage">{{ relationship.stageDesc || '陌生人' }}</span>
          </div>
        </div>
      </div>

      <div class="info-desc">{{ character.description || '暂无性格描述' }}</div>

      <div class="info-grid">
        <div class="info-row">
          <span class="label">消息总数</span>
          <span class="value">{{ relationship.totalMessages ?? 0 }}</span>
        </div>
        <div class="info-row">
          <span class="label">随机事件</span>
          <span class="value" :class="{ on: character.randomEventEnabled }">
            {{ character.randomEventEnabled ? '已开启' : '已关闭' }}
          </span>
        </div>
        <div class="info-row">
          <span class="label">首次聊天</span>
          <span class="value">{{ formatTime(relationship.firstChatTime) || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">最后聊天</span>
          <span class="value">{{ formatTime(relationship.lastChatTime) || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">最近事件</span>
          <span class="value">{{ formatTime(character.lastEventTime) || '—' }}</span>
        </div>
      </div>

      <div class="score-section">
        <div class="score-item">
          <div class="score-head">
            <span class="label">亲密度</span>
            <span class="value">{{ intimacy }}</span>
          </div>
          <div class="score-track">
            <div class="score-fill" :style="{ width: intimacyPct + '%' }"></div>
          </div>
        </div>
        <div class="score-item">
          <div class="score-head">
            <span class="label">信任度</span>
            <span class="value">{{ trust }}</span>
          </div>
          <div class="score-track">
            <div class="score-fill trust" :style="{ width: trustPct + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('viewEvents')">事件记录</el-button>
      <el-button type="primary" @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getEmotion, getRelationship } from '../api/index.js'

const MAX_SCORE = 1000

const props = defineProps({
  modelValue: Boolean,
  character: { type: Object, default: null }
})
defineEmits(['update:modelValue', 'viewEvents'])

const emotion = ref('')
const relationship = ref({})

const intimacy = computed(() => Math.round(relationship.value.intimacyScore || 0))
const trust = computed(() => Math.round(relationship.value.trustScore || 0))
const intimacyPct = computed(() => Math.min(100, (intimacy.value / MAX_SCORE) * 100))
const trustPct = computed(() => Math.min(100, (trust.value / MAX_SCORE) * 100))

watch(() => props.modelValue, async (val) => {
  if (!val || !props.character) return
  emotion.value = ''
  relationship.value = {}
  try {
    const res = await getEmotion(props.character.id)
    emotion.value = res.data?.emotion || ''
  } catch {}
  try {
    const res = await getRelationship(props.character.id)
    relationship.value = res.data || {}
  } catch {}
})

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.info-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-title h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.info-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
}

.badge.emotion {
  color: var(--accent);
  background: rgba(16, 163, 127, 0.1);
}

.badge.stage {
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
}

.badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

.info-desc {
  margin-top: 16px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.info-grid {
  margin-top: 16px;
  padding: 12px 14px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.info-row .label { color: var(--text-tertiary); }
.info-row .value { color: var(--text-primary); }
.info-row .value.on { color: var(--accent); }

.score-section {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.score-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.score-head .label { color: var(--text-tertiary); }
.score-head .value { color: var(--text-primary); font-weight: 600; }

.score-track {
  height: 6px;
  border-radius: 3px;
  background: var(--bg-tertiary);
  overflow: hidden;
}

.score-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--accent);
  transition: width 0.5s ease;
}

.score-fill.trust {
  background: #a855f7;
}
</style>