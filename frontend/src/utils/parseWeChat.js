/**
 * 解析微信导出的聊天记录
 * 支持格式：
 *
 * 格式1（pywxdump 导出）：
 *   [2026-05-01 20:12:17] TA: 我不要高分
 *   [2026-05-01 20:12:25] TA: 我给你一百
 *
 * 格式2（手动导出）：
 *   2024-01-15 14:30:22 张三
 *   你好啊
 *
 * 格式3：
 *   张三 2024/01/15 14:30
 *   你好啊
 */
export function parseWeChatChat(text) {
  if (!text) return []

  const lines = text.split('\n')
  const messages = []

  // 格式1: [2026-05-01 20:12:17] TA: 消息内容
  const pattern1 = /^\[(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})\]\s*(.+?)[:：]\s*(.*)$/

  // 格式2: 2024-01-15 14:30:22 张三
  const pattern2 = /^(\d{4}[-/]\d{1,2}[-/]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?)\s+(.+)$/

  // 格式3: 张三 2024/01/15 14:30
  const pattern3 = /^(.+?)\s+(\d{4}[-/]\d{1,2}[-/]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?)$/

  // 先检测是哪种格式
  const sampleLines = lines.filter(l => l.trim()).slice(0, 10)
  const isFormat1 = sampleLines.some(l => pattern1.test(l.trim()))

  if (isFormat1) {
    // 格式1: 每行独立一条消息
    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed) continue

      const match = trimmed.match(pattern1)
      if (match) {
        messages.push({
          sender: match[2].trim(),
          content: match[3].trim()
        })
      }
    }
  } else {
    // 格式2/3: 发送者和内容分两行
    let currentSender = null
    let currentContent = []

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed) {
        if (currentContent.length > 0) currentContent.push('')
        continue
      }

      let match = trimmed.match(pattern2)
      if (!match) match = trimmed.match(pattern3)

      if (match) {
        if (currentSender && currentContent.length > 0) {
          messages.push({
            sender: currentSender,
            content: currentContent.join('\n').trim()
          })
        }
        currentSender = match[2] || match[1]
        currentContent = []
      } else {
        currentContent.push(trimmed)
      }
    }

    if (currentSender && currentContent.length > 0) {
      messages.push({
        sender: currentSender,
        content: currentContent.join('\n').trim()
      })
    }
  }

  // 过滤空消息，格式化为 "名字: 内容"
  return messages
    .filter(m => m.content.length > 0)
    .map(m => `${m.sender}: ${m.content}`)
}
