<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="创建人物"
    width="520px"
    :close-on-click-modal="false"
    class="create-dialog"
  >
    <!-- 进度显示 -->
    <div v-if="submitting" class="progress-section">
      <div class="progress-steps">
        <div
          v-for="(step, i) in steps"
          :key="i"
          class="step"
          :class="{ active: currentStep === i, done: currentStep > i }"
        >
          <div class="step-dot">
            <el-icon v-if="currentStep > i"><Check /></el-icon>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <span class="step-label">{{ step }}</span>
        </div>
        <div class="step-line-track">
          <div class="step-line-fill" :style="{ width: (currentStep / (steps.length - 1)) * 100 + '%' }"></div>
        </div>
      </div>
      <el-progress
        :percentage="progressPercent"
        :stroke-width="10"
        :color="'#10a37f'"
        :duration="0.6"
        style="margin-top: 24px"
      />
      <div class="progress-message" :key="progressMessage">
        <span class="message-dot"></span>
        {{ progressMessage }}
      </div>
    </div>

    <!-- 表单 -->
    <el-form v-else :model="form" label-position="top" ref="formRef" :rules="rules">
      <el-form-item label="名字" prop="name">
        <el-input v-model="form.name" placeholder="给TA起个名字" maxlength="20" />
      </el-form-item>

      <el-form-item label="性格描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="描述TA的性格特征，例如：温柔、幽默、话多..."
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="头像（可选）">
        <el-upload
          class="avatar-uploader"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="onAvatarChange"
          accept="image/*"
        >
          <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview" />
          <el-icon v-else class="avatar-placeholder"><Plus /></el-icon>
        </el-upload>
      </el-form-item>

      <el-form-item prop="chatFile">
        <template #label>
          <span>聊天记录</span>
          <span class="label-hint">支持微信导出的 txt 文件</span>
        </template>
        <el-upload
          class="chat-upload"
          :auto-upload="false"
          :show-file-list="true"
          :limit="1"
          :on-change="onChatFileChange"
          :on-remove="onChatFileRemove"
          accept=".txt,.csv"
          drag
        >
          <el-icon :size="32"><UploadFilled /></el-icon>
          <div class="upload-text">拖拽文件到此处，或<em>点击上传</em></div>
          <div class="upload-hint">微信聊天记录导出的 txt 文件</div>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)" :disabled="submitting">取消</el-button>
      <el-button v-if="!submitting" type="primary" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { createCharacter } from '../api/index.js'
import { parseWeChatChat } from '../utils/parseWeChat.js'

const props = defineProps({
  modelValue: Boolean,
  settings: { type: Object, default: () => ({}) },
  characters: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'created'])

const steps = ['导入聊天记录', '解析角色性格', '存储记忆']
const currentStep = ref(0)
const progressPercent = ref(0)
const progressMessage = ref('')

const formRef = ref(null)
const submitting = ref(false)
const avatarPreview = ref('')
const avatarFile = ref(null)
const chatFile = ref(null)

const form = reactive({
  name: '',
  description: ''
})

const rules = {
  name: [
    { required: true, message: '请输入名字', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (props.characters.some(c => c.name === value.trim())) {
          callback(new Error('该名字已存在，请换一个'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  description: [{ required: true, message: '请描述性格', trigger: 'blur' }]
}

function onAvatarChange(file) {
  avatarFile.value = file.raw
  avatarPreview.value = URL.createObjectURL(file.raw)
}

function onChatFileChange(file) {
  chatFile.value = file.raw
}

function onChatFileRemove() {
  chatFile.value = null
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (!chatFile.value) {
    ElMessage.warning('请上传聊天记录文件')
    return
  }

  submitting.value = true
  currentStep.value = 0
  progressPercent.value = 0
  progressMessage.value = '准备中...'

  try {
    const text = await chatFile.value.text()
    const parsed = parseWeChatChat(text)

    if (parsed.length === 0) {
      ElMessage.warning('未能解析出聊天记录，请检查文件格式')
      submitting.value = false
      return
    }

    const fd = new FormData()
    fd.append('name', form.name)
    fd.append('description', form.description)
    fd.append('chatData', parsed.join('\n'))
    if (avatarFile.value) {
      fd.append('avatar', avatarFile.value)
    }

    const res = await createCharacter(fd, props.settings, (data) => {
      progressMessage.value = data.message
      progressPercent.value = data.percent
      if (data.percent <= 25) currentStep.value = 0
      else if (data.percent <= 70) currentStep.value = 1
      else currentStep.value = 2
    })

    const characterName = form.name
    const characterDesc = form.description
    const characterAvatar = avatarPreview.value || ''

    // 重置表单
    form.name = ''
    form.description = ''
    avatarPreview.value = ''
    avatarFile.value = null
    chatFile.value = null

    // 关闭弹窗
    emit('update:modelValue', false)

    // 通知父组件
    emit('created', {
      id: res.id,
      name: characterName,
      description: characterDesc,
      avatar: characterAvatar
    })

    ElMessage.success('人物创建成功')
  } catch (e) {
    ElMessage.error('创建失败：' + e.message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.progress-section {
  padding: 30px 0 10px;
  animation: fadeSlideIn 0.4s ease;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  margin-bottom: 8px;
}

/* 连接线 */
.step-line-track {
  position: absolute;
  top: 16px;
  left: 16%;
  right: 16%;
  height: 2px;
  background: var(--border-secondary);
  z-index: 0;
}
.step-line-fill {
  height: 100%;
  background: #10a37f;
  transition: width 0.6s ease;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
  z-index: 1;
}

.step-dot {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--text-muted);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.step.active .step-dot {
  background: #10a37f;
  border-color: #10a37f;
  color: #fff;
  transform: scale(1.15);
  box-shadow: 0 0 0 6px rgba(16, 163, 127, 0.2);
  animation: pulse 1.5s ease-in-out infinite;
}

.step.done .step-dot {
  background: #10a37f;
  border-color: #10a37f;
  color: #fff;
  transform: scale(1);
  animation: popIn 0.3s ease;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(16, 163, 127, 0.2); }
  50% { box-shadow: 0 0 0 10px rgba(16, 163, 127, 0.08); }
}

@keyframes popIn {
  0% { transform: scale(0.7); }
  60% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.step-label {
  font-size: 12px;
  color: var(--text-muted);
  transition: all 0.3s;
}

.step.active .step-label {
  color: var(--text-primary);
  font-weight: 600;
}

.step.done .step-label {
  color: #10a37f;
}

.progress-message {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  animation: fadeSlideIn 0.3s ease;
}

.message-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10a37f;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.avatar-uploader :deep(.el-upload) {
  width: 80px; height: 80px;
  border: 2px dashed var(--border-secondary);
  border-radius: 50%;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  transition: border-color 0.2s;
}
.avatar-uploader :deep(.el-upload):hover { border-color: var(--accent); }

.avatar-preview { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder { font-size: 28px; color: var(--text-muted); }

.chat-upload :deep(.el-upload-dragger) {
  background: var(--bg-tertiary);
  border: 2px dashed var(--border-secondary);
  border-radius: 10px;
  padding: 24px;
  transition: border-color 0.2s, background 0.3s;
}
.chat-upload :deep(.el-upload-dragger):hover { border-color: var(--accent); }

.upload-text { margin-top: 8px; font-size: 13px; color: var(--text-secondary); }
.upload-text em { color: var(--accent); font-style: normal; }
.upload-hint { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.label-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 8px;
  font-weight: normal;
}
</style>
