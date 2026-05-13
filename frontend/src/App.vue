<template>
  <div class="app">
    <Sidebar
      :characters="characters"
      :activeId="activeId"
      :theme="theme"
      :collapsed="sidebarCollapsed"
      @select="selectCharacter"
      @create="showCreateDialog = true"
      @delete="deleteCharacter"
      @clear-history="clearHistory"
      @settings="showSettingsDialog = true"
      @toggleTheme="toggleTheme"
      @toggleCollapse="sidebarCollapsed = !sidebarCollapsed"
    />
    <ChatView
      :character="activeCharacter"
      :messages="messages"
      :loading="sending"
      :hasMoreHistory="hasMoreHistory"
      @send="sendMessage"
      @loadMore="loadMoreHistory"
    />
    <CreateCharacterDialog
      v-model="showCreateDialog"
      :settings="settings"
      :characters="characters"
      @created="onCharacterCreated"
    />
    <SettingsDialog
      v-model="showSettingsDialog"
      :settings="settings"
      @save="onSettingsSave"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { chat, getCharacters, deleteCharacter as deleteCharacterApi, clearChatHistory, getChatHistory, getSettings, saveSettings } from './api/index.js'
import { useTheme } from './composables/useTheme.js'
import Sidebar from './components/Sidebar.vue'
import ChatView from './components/ChatView.vue'
import CreateCharacterDialog from './components/CreateCharacterDialog.vue'
import SettingsDialog from './components/SettingsDialog.vue'

const { theme, toggle: toggleTheme } = useTheme()

const characters = ref([])
const activeId = ref(null)
const messages = ref([])
const sending = ref(false)
const hasMoreHistory = ref(false)
const sidebarCollapsed = ref(false)
const showCreateDialog = ref(false)
const showSettingsDialog = ref(false)

const settings = ref({
  apiUrl: 'https://api.openai.com/v1',
  apiKey: '',
  modelName: 'gpt-4o-mini',
  embeddingApiUrl: '',
  embeddingApiKey: '',
  embeddingModelName: 'text-embedding-3-small'
})

onMounted(async () => {
  // 加载设置（从数据库）
  try {
    const res = await getSettings()
    const saved = res.data || {}
    if (Object.keys(saved).length > 0) {
      Object.assign(settings.value, saved)
    }
  } catch {}
  // 加载角色列表
  getCharacters().then(res => {
    characters.value = res.data || []
  })
})

function onSettingsSave(val) {
  settings.value = val
  // 保存到数据库
  saveSettings(val).catch(() => {
    ElMessage.warning('设置保存失败')
  })
}

const activeCharacter = computed(() =>
  characters.value.find(c => c.id === activeId.value) || null
)

async function selectCharacter(id) {
  activeId.value = id
  messages.value = []
  hasMoreHistory.value = false
  await loadHistory(id)
}

async function loadHistory(characterId, before) {
  try {
    const res = await getChatHistory(characterId, before)
    const data = res.data
    const historyMessages = data.messages.map(m => ({
      role: m.role,
      content: m.content,
      time: m.createTime || Date.now(),
      id: m.id
    }))
    if (before) {
      // 加载更多：插入到消息列表前面
      messages.value = [...historyMessages, ...messages.value]
    } else {
      messages.value = historyMessages
    }
    hasMoreHistory.value = data.hasMore
  } catch (e) {
    console.error('加载历史消息失败', e)
  }
}

function loadMoreHistory() {
  if (!activeId.value || !hasMoreHistory.value || messages.value.length === 0) return
  const firstMsg = messages.value.find(m => m.id)
  if (firstMsg) {
    loadHistory(activeId.value, firstMsg.id)
  }
}

function sendMessage(text) {
  if (!activeId.value || sending.value) return

  messages.value.push({ role: 'user', content: text, time: Date.now() })
  sending.value = true

  // 创建空的 assistant 消息
  const idx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', time: Date.now() })

  chat(
    activeId.value,
    text,
    // onToken: 通过索引直接修改，确保 Vue 响应式触发
    (token) => {
      messages.value[idx].content += token
    },
    // onDone
    () => {
      sending.value = false
    },
    // onError
    (errMsg) => {
      if (!messages.value[idx].content) {
        messages.value[idx].content = '[请求失败，请重试]'
      }
      sending.value = false
    }
  )
}

function onCharacterCreated(character) {
  characters.value.push(character)
  activeId.value = character.id
  messages.value = []
}

function deleteCharacter(id) {
  deleteCharacterApi(id).then(() => {
    characters.value = characters.value.filter(c => c.id !== id)
    if (activeId.value === id) {
      activeId.value = null
      messages.value = []
    }
    ElMessage.success('角色已删除')
  })
}

function clearHistory(id) {
  clearChatHistory(id).then(() => {
    if (activeId.value === id) {
      messages.value = []
    }
    ElMessage.success('聊天记录已清空')
  })
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }

.app {
  display: flex;
  height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: background 0.3s, color 0.3s;
}
</style>
