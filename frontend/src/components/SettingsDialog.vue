<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="设置"
    width="480px"
    class="settings-dialog"
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="API 地址">
        <el-input
          v-model="form.apiUrl"
          placeholder="https://api.openai.com/v1"
        />
        <div class="field-hint">OpenAI 兼容接口地址，支持自定义代理</div>
      </el-form-item>

      <el-form-item label="API Key">
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          placeholder="sk-..."
        />
      </el-form-item>

      <el-form-item label="对话模型">
        <el-input
          v-model="form.modelName"
          placeholder="gpt-4o-mini"
        />
      </el-form-item>

      <el-divider content-position="left">向量模型配置</el-divider>

      <el-form-item label="向量 API 地址">
        <el-input
          v-model="form.embeddingApiUrl"
          placeholder="留空则与对话 API 地址相同"
        />
      </el-form-item>

      <el-form-item label="向量 API Key">
        <el-input
          v-model="form.embeddingApiKey"
          type="password"
          show-password
          placeholder="留空则与对话 API Key 相同"
        />
      </el-form-item>

      <el-form-item label="向量模型名称">
        <el-input
          v-model="form.embeddingModelName"
          placeholder="BAAI/bge-m3"
        />
        <div class="field-hint">用于聊天记录向量化存储和记忆检索</div>
      </el-form-item>

      <el-divider content-position="left">语音输入</el-divider>

      <el-form-item label="语音输入语言">
        <el-select v-model="form.voiceLanguage" placeholder="选择语音识别语言" style="width: 100%">
          <el-option
            v-for="opt in languageOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <div class="field-hint">使用麦克风语音输入时识别的语言</div>
      </el-form-item>

      <el-divider content-position="left">语音输出</el-divider>

      <el-form-item label="自动朗读回复">
        <el-switch v-model="form.ttsEnabled" />
        <div class="field-hint">开启后角色每次回复完会自动朗读，也可点击消息旁的按钮手动朗读</div>
      </el-form-item>

      <el-form-item label="语音输出语言">
        <el-select v-model="form.ttsLanguage" placeholder="选择朗读语言" style="width: 100%">
          <el-option
            v-for="opt in languageOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <div class="field-hint">朗读使用的语言，实际音色取决于系统已安装的语音包</div>
      </el-form-item>

      <el-form-item label="语音音色">
        <el-select
          v-model="form.ttsVoice"
          filterable
          clearable
          placeholder="默认（按语言自动选择）"
          style="width: 100%"
        >
          <el-option
            v-for="v in voiceOptions"
            :key="v.voiceURI"
            :label="`${v.name} (${v.lang})`"
            :value="v.name"
          />
        </el-select>
        <div class="field-hint">
          来自系统已安装的语音包，留空则按语音输出语言自动选择；列表为空时说明系统未安装可用语音
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="reset">恢复默认</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  settings: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'save'])

const defaults = {
  apiUrl: 'https://api.openai.com/v1',
  apiKey: '',
  modelName: 'gpt-4o-mini',
  embeddingApiUrl: '',
  embeddingApiKey: '',
  embeddingModelName: 'BAAI/bge-m3',
  voiceLanguage: 'zh-CN',
  ttsEnabled: true,
  ttsLanguage: 'zh-CN',
  ttsVoice: ''
}

const languageOptions = [
  { label: '中文（简体）', value: 'zh-CN' },
  { label: '中文（繁體）', value: 'zh-TW' },
  { label: 'English (US)', value: 'en-US' },
  { label: 'English (UK)', value: 'en-GB' },
  { label: '日本語', value: 'ja-JP' },
  { label: '한국어', value: 'ko-KR' },
  { label: 'Français', value: 'fr-FR' },
  { label: 'Deutsch', value: 'de-DE' },
  { label: 'Español', value: 'es-ES' },
  { label: 'Русский', value: 'ru-RU' }
]

const form = reactive({ ...defaults })

// 系统语音包（Chrome 首次调用可能返回空，需等 voiceschanged 事件）
const synth = window.speechSynthesis
const voices = ref([])

function loadVoices() {
  if (!synth) return
  const list = synth.getVoices() || []
  if (list.length) voices.value = list
}

// 与语音输出语言一致的音色排前面，其余按语言和名称排序
const voiceOptions = computed(() => {
  const prefix = (form.ttsLanguage || '').split('-')[0].toLowerCase()
  return [...voices.value].sort((a, b) => {
    const aMatch = prefix && a.lang.toLowerCase().startsWith(prefix) ? 0 : 1
    const bMatch = prefix && b.lang.toLowerCase().startsWith(prefix) ? 0 : 1
    if (aMatch !== bMatch) return aMatch - bMatch
    return `${a.lang}${a.name}`.localeCompare(`${b.lang}${b.name}`)
  })
})

onMounted(() => {
  loadVoices()
  if (synth) synth.addEventListener('voiceschanged', loadVoices)
})

onBeforeUnmount(() => {
  if (synth) synth.removeEventListener('voiceschanged', loadVoices)
})

watch(() => props.modelValue, (val) => {
  if (val) {
    form.apiUrl = props.settings.apiUrl || defaults.apiUrl
    form.apiKey = props.settings.apiKey || defaults.apiKey
    form.modelName = props.settings.modelName || defaults.modelName
    form.embeddingApiUrl = props.settings.embeddingApiUrl || defaults.embeddingApiUrl
    form.embeddingApiKey = props.settings.embeddingApiKey || defaults.embeddingApiKey
    form.embeddingModelName = props.settings.embeddingModelName || defaults.embeddingModelName
    form.voiceLanguage = props.settings.voiceLanguage || defaults.voiceLanguage
    form.ttsEnabled = props.settings.ttsEnabled ?? defaults.ttsEnabled
    form.ttsLanguage = props.settings.ttsLanguage || defaults.ttsLanguage
    form.ttsVoice = props.settings.ttsVoice || defaults.ttsVoice
    loadVoices()
  }
})

function save() {
  if (!form.apiKey) {
    ElMessage.warning('请填写 API Key')
    return
  }
  emit('save', { ...form })
  emit('update:modelValue', false)
  ElMessage.success('设置已保存')
}

function reset() {
  Object.assign(form, defaults)
}
</script>

<style scoped>
.field-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
</style>
