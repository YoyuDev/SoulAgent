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
    </el-form>

    <template #footer>
      <el-button @click="reset">恢复默认</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, watch } from 'vue'
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
  embeddingModelName: 'BAAI/bge-m3'
}

const form = reactive({ ...defaults })

watch(() => props.modelValue, (val) => {
  if (val) {
    form.apiUrl = props.settings.apiUrl || defaults.apiUrl
    form.apiKey = props.settings.apiKey || defaults.apiKey
    form.modelName = props.settings.modelName || defaults.modelName
    form.embeddingApiUrl = props.settings.embeddingApiUrl || defaults.embeddingApiUrl
    form.embeddingApiKey = props.settings.embeddingApiKey || defaults.embeddingApiKey
    form.embeddingModelName = props.settings.embeddingModelName || defaults.embeddingModelName
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
