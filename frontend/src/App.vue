<template>
  <div class="app">
    <!-- 手机端：抽屉式侧边栏 -->
    <el-drawer
      v-if="isMobile"
      v-model="mobileDrawerOpen"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="sidebar-drawer"
    >
      <Sidebar v-bind="sidebarCommonProps" :collapsed="false" :in-drawer="true" v-on="sidebarHandlers" />
    </el-drawer>

    <!-- PC / 平板：内联侧边栏 -->
    <Sidebar v-else v-bind="sidebarCommonProps" :collapsed="sidebarCollapsed" v-on="sidebarHandlers" />

    <ChatView
      :character="activeCharacter"
      :messages="messages"
      :loading="sending"
      :hasMoreHistory="hasMoreHistory"
      :emotion="currentEmotion"
      :relationship="relationshipData"
      :voiceLanguage="settings.voiceLanguage"
      :ttsEnabled="settings.ttsEnabled"
      :ttsLanguage="settings.ttsLanguage"
      :ttsVoice="settings.ttsVoice"
      :showMenuButton="isMobile"
      @send="sendMessage"
      @loadMore="loadMoreHistory"
      @viewInfo="openActiveCharacterInfo"
      @toggleMenu="mobileDrawerOpen = true"
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
    <CharacterInfoDialog
      v-model="showInfoDialog"
      :character="infoCharacter"
      @viewEvents="openCharacterEvents(infoCharacter)"
    />
    <EventHistoryDialog
      v-model="showEventsDialog"
      :character="eventsCharacter"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { chat, getCharacters, deleteCharacter as deleteCharacterApi, clearChatHistory, getChatHistory, getSettings, saveSettings, getEmotion, getRelationship, getUnsharedEvents, markEventAsShared, updateRandomEventEnabled } from './api/index.js'
import { useTheme } from './composables/useTheme.js'
import { useBreakpoint } from './composables/useBreakpoint.js'
import Sidebar from './components/Sidebar.vue'
import ChatView from './components/ChatView.vue'
import CreateCharacterDialog from './components/CreateCharacterDialog.vue'
import SettingsDialog from './components/SettingsDialog.vue'
import CharacterInfoDialog from './components/CharacterInfoDialog.vue'
import EventHistoryDialog from './components/EventHistoryDialog.vue'

const { theme, toggle: toggleTheme } = useTheme()
const { isMobile, isTablet } = useBreakpoint()

const characters = ref([])
const activeId = ref(null)
const messages = ref([])
const sending = ref(false)
const hasMoreHistory = ref(false)
const sidebarCollapsed = ref(false)
const showCreateDialog = ref(false)
const showSettingsDialog = ref(false)
const showInfoDialog = ref(false)
const infoCharacter = ref(null)
const showEventsDialog = ref(false)
const eventsCharacter = ref(null)
const currentEmotion = ref('')
const relationshipData = ref(null)
const mobileDrawerOpen = ref(false)

// 平板档自动折叠侧边栏，PC 档恢复展开
watch(isTablet, (val) => {
  sidebarCollapsed.value = val
}, { immediate: true })

// 侧边栏在两处复用（PC 内联 / 手机抽屉），统一 props 与事件，避免重复绑定
const sidebarCommonProps = computed(() => ({
  characters: characters.value,
  activeId: activeId.value,
  theme: theme.value
}))

const sidebarHandlers = {
  select: (id) => {
    selectCharacter(id)
    mobileDrawerOpen.value = false
  },
  create: () => { showCreateDialog.value = true },
  delete: deleteCharacter,
  clearHistory,
  settings: () => { showSettingsDialog.value = true },
  toggleTheme,
  toggleCollapse: () => { sidebarCollapsed.value = !sidebarCollapsed.value },
  updateRandomEvent: handleUpdateRandomEvent,
  viewInfo: openCharacterInfo,
  viewEvents: openCharacterEvents
}

const settings = ref({
  apiUrl: 'https://api.openai.com/v1',
  apiKey: '',
  modelName: 'gpt-4o-mini',
  embeddingApiUrl: '',
  embeddingApiKey: '',
  embeddingModelName: 'text-embedding-3-small',
  randomEventEnabled: true,
  voiceLanguage: 'zh-CN',
  ttsEnabled: true,
  ttsLanguage: 'zh-CN',
  ttsVoice: ''
})

// 随机事件轮询
let eventPollingTimer = null
const POLLING_INTERVAL = 30000 // 30 秒检查一次

onMounted(async () => {
  // 加载设置（从数据库）
  try {
    const res = await getSettings()
    const saved = res.data || {}
    if (Object.keys(saved).length > 0) {
      Object.assign(settings.value, normalizeSettings(saved))
    }
  } catch {}
  // 加载角色列表
  getCharacters().then(res => {
    characters.value = res.data || []
  })
  
  // 启动随机事件轮询
  startEventPolling()
})

// 布尔配置以字符串形式存库，读写时统一转换
function normalizeSettings(val) {
  return {
    ...val,
    ttsEnabled: val.ttsEnabled !== false && val.ttsEnabled !== 'false'
  }
}

function onSettingsSave(val) {
  // 合并而非整体替换，避免丢失表单之外的配置（如 randomEventEnabled）
  settings.value = normalizeSettings({ ...settings.value, ...val })
  // 保存到数据库
  saveSettings({ ...val, ttsEnabled: String(val.ttsEnabled) }).catch(() => {
    ElMessage.warning('设置保存失败')
  })
}

function openCharacterInfo(character) {
  if (!character) return
  infoCharacter.value = character
  showInfoDialog.value = true
}

function openActiveCharacterInfo() {
  openCharacterInfo(activeCharacter.value)
}

function openCharacterEvents(character) {
  if (!character) return
  eventsCharacter.value = character
  showEventsDialog.value = true
}

const activeCharacter = computed(() =>
  characters.value.find(c => c.id === activeId.value) || null
)

async function selectCharacter(id) {
  activeId.value = id
  messages.value = []
  hasMoreHistory.value = false
  currentEmotion.value = ''
  relationshipData.value = null
  await loadHistory(id)
  loadEmotion(id)
  loadRelationship(id)
}

async function loadEmotion(characterId) {
  try {
    const res = await getEmotion(characterId)
    currentEmotion.value = res.data?.emotion || ''
  } catch {}
}

async function loadRelationship(characterId) {
  try {
    const res = await getRelationship(characterId)
    relationshipData.value = res.data
  } catch {}
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
      loadEmotion(activeId.value)
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
  characters.value.push({
    ...character,
    randomEventEnabled: character.randomEventEnabled ? 1 : 0
  })
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

function handleUpdateRandomEvent(id, enabled) {
  updateRandomEventEnabled(id, enabled).then(() => {
    // 更新本地角色列表
    const char = characters.value.find(c => c.id === id)
    if (char) {
      char.randomEventEnabled = enabled ? 1 : 0
    }
    ElMessage.success(enabled ? '已开启随机事件' : '已关闭随机事件')
  }).catch(() => {
    ElMessage.error('更新失败')
  })
}

// 随机事件轮询
function startEventPolling() {
  if (eventPollingTimer) {
    clearInterval(eventPollingTimer)
  }
  
  eventPollingTimer = setInterval(async () => {
    // 仅在明确关闭时才跳过，避免配置缺失导致轮询失效
    if (!activeId.value || settings.value.randomEventEnabled === false) {
      return
    }

    try {
      const res = await getUnsharedEvents(activeId.value)
      const events = res.data?.events || []

      // 处理所有可分享事件（倒序变正序），避免较早的事件被最新一条挡住
      const shareable = events
        .filter(e => e.eventContent?.includes('[想分享给你]'))
        .reverse()

      for (const event of shareable) {
        const content = event.eventContent.replace('[想分享给你]', '').trim()

        ElNotification({
          title: `${activeCharacter.value?.name} 想分享给你`,
          message: content,
          type: 'info',
          duration: 8000,
          position: 'bottom-right',
          onClick: () => {
            // 点击通知时标记为已分享
            markEventAsShared(event.id).catch(() => {})
          }
        })

        // 自动标记为已分享
        markEventAsShared(event.id).catch(() => {})
      }
    } catch (e) {
      console.error('检查随机事件失败:', e)
    }
  }, POLLING_INTERVAL)
}

// 监听角色切换，重新开始轮询
watch(activeId, () => {
  if (activeId.value) {
    startEventPolling()
  }
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }

.app {
  display: flex;
  height: 100vh;
  /* 移动端浏览器地址栏会吃掉 vh，dvh 更准确 */
  height: 100dvh;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: background 0.3s, color 0.3s;
}
</style>
