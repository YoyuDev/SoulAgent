<template>
  <div class="sidebar" :class="{ collapsed }">
    <div class="sidebar-header">
      <div class="logo" v-if="!collapsed">
        <img src="/logo.png" alt="SoulAgent" class="logo-img" />
        <span class="logo-text">SoulAgent</span>
      </div>
      <div class="logo" v-else>
        <img src="/logo.png" alt="SoulAgent" class="logo-img small" />
      </div>
      <el-button class="new-btn" @click="$emit('create')">
        <el-icon><Plus /></el-icon>
        <span class="btn-text">创建人物</span>
      </el-button>
    </div>

    <div class="char-list">
      <div
        v-for="c in characters"
        :key="c.id"
        class="char-item"
        :class="{ active: c.id === activeId }"
        @click="$emit('select', c.id)"
      >
        <el-avatar :size="32" :src="c.avatar || undefined">
          {{ c.name?.[0] }}
        </el-avatar>
        <span class="char-name">{{ c.name }}</span>
        <el-dropdown trigger="click" @command="cmd => handleCommand(cmd, c)" @click.stop>
          <el-icon class="more-btn"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="toggleEvent">
                <span :style="{ color: c.randomEventEnabled ? '#10a37f' : '' }">
                  {{ c.randomEventEnabled ? '关闭随机事件' : '开启随机事件' }}
                </span>
              </el-dropdown-item>
              <el-dropdown-item command="clear">清空聊天记录</el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <span style="color: #f56c6c">删除角色</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div v-if="characters.length === 0 && !collapsed" class="empty">
        <el-icon :size="40"><ChatDotRound /></el-icon>
        <p>还没有人物</p>
        <p class="hint">点击上方按钮创建</p>
      </div>
    </div>

    <div class="sidebar-footer">
      <div class="char-item" @click="$emit('toggleTheme')">
        <el-icon :size="18">
          <Sunny v-if="theme === 'dark'" />
          <Moon v-else />
        </el-icon>
        <span class="btn-text">{{ theme === 'dark' ? '浅色模式' : '深色模式' }}</span>
      </div>
      <div class="char-item" @click="$emit('settings')">
        <el-icon :size="18"><Setting /></el-icon>
        <span class="btn-text">设置</span>
      </div>
    </div>

    <!-- 折叠按钮 -->
    <div class="collapse-btn" @click="$emit('toggleCollapse')">
      <el-icon :size="16">
        <DArrowLeft v-if="!collapsed" />
        <DArrowRight v-else />
      </el-icon>
    </div>
  </div>
</template>

<script setup>
defineProps({
  characters: { type: Array, default: () => [] },
  activeId: { type: Number, default: null },
  theme: { type: String, default: 'dark' },
  collapsed: { type: Boolean, default: false }
})
import { ElMessageBox } from 'element-plus'

const emit = defineEmits(['select', 'create', 'delete', 'clearHistory', 'settings', 'toggleTheme', 'toggleCollapse', 'updateRandomEvent'])

async function handleCommand(cmd, character) {
  if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除该人物？删除后不可恢复。', '删除角色', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
      emit('delete', character.id)
    } catch {}
  } else if (cmd === 'clear') {
    try {
      await ElMessageBox.confirm('确定清空该人物的聊天记录？', '清空聊天记录', {
        confirmButtonText: '清空',
        cancelButtonText: '取消',
        type: 'warning'
      })
      emit('clearHistory', character.id)
    } catch {}
  } else if (cmd === 'toggleEvent') {
    emit('updateRandomEvent', character.id, !character.randomEventEnabled)
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-primary);
  transition: width 0.3s, background 0.3s, border-color 0.3s;
  position: relative;
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 60px;
}
.sidebar.collapsed .char-item {
  justify-content: center;
  padding: 10px 8px;
  gap: 0;
}
.sidebar.collapsed .char-name,
.sidebar.collapsed .more-btn,
.sidebar.collapsed .btn-text {
  display: none;
}
.sidebar.collapsed .char-list {
  padding: 4px 4px;
}
.sidebar.collapsed .sidebar-header {
  padding: 12px 8px;
}
.sidebar.collapsed .new-btn {
  width: 44px;
  padding: 0 !important;
  margin: 0 auto;
}
.sidebar.collapsed .sidebar-footer .char-item {
  justify-content: center;
  padding: 10px 8px;
}

/* 折叠按钮 */
.collapse-btn {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 48px;
  background: var(--bg-tertiary);
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-tertiary);
  opacity: 0;
  transition: opacity 0.2s, background 0.2s, color 0.2s;
  z-index: 10;
}
.sidebar:hover .collapse-btn { opacity: 1; }
.collapse-btn:hover {
  background: var(--bg-active);
  color: var(--text-primary);
}

/* 头部 */
.sidebar-header {
  padding: 12px;
  flex-shrink: 0;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px 12px;
}
.logo-img {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}
.logo-img.small {
  width: 32px;
  height: 32px;
  margin: 0 auto;
}
.logo-text {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.new-btn {
  width: 100%;
  height: 44px;
  background: transparent !important;
  border: 1px solid var(--border-secondary) !important;
  color: var(--text-primary) !important;
  font-size: 14px;
  border-radius: 8px;
  overflow: hidden;
  white-space: nowrap;
  padding: 0 12px !important;
}
.new-btn:hover {
  background: var(--bg-hover) !important;
}

/* 列表 */
.char-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 8px;
}

.char-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  flex-shrink: 0;
}
.char-item :deep(.el-avatar) {
  flex-shrink: 0;
}
.char-item:hover { background: var(--bg-hover); }
.char-item.active { background: var(--bg-active); }

.char-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.more-btn {
  font-size: 16px;
  color: var(--text-muted);
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
  flex-shrink: 0;
  padding: 2px;
  border-radius: 4px;
}
.char-item:hover .more-btn { opacity: 1; }
.more-btn:hover { color: var(--text-primary); background: var(--bg-active); }

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
}
.empty p { margin-top: 8px; font-size: 14px; }
.empty .hint { font-size: 12px; color: var(--text-hint); }

/* 底部 */
.sidebar-footer {
  padding: 8px;
  border-top: 1px solid var(--border-primary);
  flex-shrink: 0;
}
.sidebar-footer .char-item {
  color: var(--text-tertiary);
  font-size: 13px;
}
.sidebar-footer .char-item:hover { color: var(--text-primary); }

/* ===== 折叠状态 ===== */
.collapsed .sidebar-header { padding: 12px 8px; }
.collapsed .new-btn {
  padding: 0 !important;
  display: flex;
  align-items: center;
  justify-content: center;
}
.collapsed .btn-text { display: none; }

.collapsed .char-item {
  justify-content: center;
  padding: 10px 0;
}
.collapsed .char-name { display: none; }

.collapsed .sidebar-footer { padding: 8px 4px; }
.collapsed .sidebar-footer .char-item {
  justify-content: center;
  padding: 10px 0;
}
</style>
