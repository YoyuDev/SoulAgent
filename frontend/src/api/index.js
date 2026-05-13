import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 获取角色列表
export function getCharacters() {
  return http.get('/character/list')
}

// 获取设置
export function getSettings() {
  return http.get('/settings')
}

// 保存设置
export function saveSettings(settings) {
  return http.post('/settings', settings)
}

// 聊天（SSE 流式输出）
export function chat(characterId, message, settings, onToken, onDone, onError) {
  return fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      characterId,
      message,
      apiUrl: settings?.apiUrl || '',
      apiKey: settings?.apiKey || '',
      modelName: settings?.modelName || '',
      embeddingApiUrl: settings?.embeddingApiUrl || '',
      embeddingApiKey: settings?.embeddingApiKey || '',
      embeddingModelName: settings?.embeddingModelName || ''
    })
  }).then(async response => {
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop()

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          if (!raw) continue
          try {
            const data = JSON.parse(raw)
            const type = eventType || 'token'
            if (type === 'token' && onToken && data.content != null) {
              onToken(data.content)
            } else if (type === 'done' && onDone) {
              onDone()
            } else if (type === 'error' && onError) {
              onError(data.message)
            }
          } catch (e) {
            console.warn('SSE parse error:', raw, e)
          }
          eventType = ''
        } else if (line.trim() === '') {
          eventType = ''
        }
      }
    }

    // 处理流结束后 buffer 中剩余的内容
    if (buffer.trim()) {
      const lines = buffer.split('\n')
      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          if (!raw) continue
          try {
            const data = JSON.parse(raw)
            const type = eventType || 'token'
            if (type === 'token' && onToken && data.content != null) {
              onToken(data.content)
            } else if (type === 'done' && onDone) {
              onDone()
            } else if (type === 'error' && onError) {
              onError(data.message)
            }
          } catch (e) {}
          eventType = ''
        }
      }
    }

    // 确保 onDone 被调用（兜底）
    if (onDone) onDone()
  }).catch(err => {
    if (onError) onError(err.message)
  })
}

// 删除人物
export function deleteCharacter(id) {
  return http.delete(`/character/${id}`)
}

// 清空聊天记录
export function clearChatHistory(characterId) {
  return http.delete(`/chat/history/${characterId}`)
}

// 获取历史消息（分页）
export function getChatHistory(characterId, before, size = 20) {
  const params = { size }
  if (before) params.before = before
  return http.get(`/chat/history/${characterId}`, { params })
}

// 创建人物（支持文件上传，SSE 进度）
export function createCharacter(formData, settings, onProgress) {
  if (settings?.apiKey) {
    formData.append('apiKey', settings.apiKey)
    formData.append('apiUrl', settings.apiUrl || '')
    formData.append('modelName', settings.modelName || '')
    formData.append('embeddingApiUrl', settings.embeddingApiUrl || '')
    formData.append('embeddingApiKey', settings.embeddingApiKey || '')
    formData.append('embeddingModelName', settings.embeddingModelName || '')
  }
  return fetch('/api/character/create', {
    method: 'POST',
    body: formData
  }).then(async response => {
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let result = null

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop()

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:') && eventType) {
          const data = JSON.parse(line.slice(5).trim())
          if (eventType === 'progress' && onProgress) {
            onProgress(data)
          } else if (eventType === 'done') {
            result = data
          } else if (eventType === 'error') {
            throw new Error(data.message)
          }
          eventType = ''
        }
      }
    }
    return result
  })
}
