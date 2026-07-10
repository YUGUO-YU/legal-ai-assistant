<template>
  <Teleport to="body">
    <Transition name="command-fade">
      <div v-if="visible" class="command-palette-overlay" @click.self="hide">
        <div class="command-palette" ref="paletteRef">
          <div class="command-input-wrapper">
            <i class="el-icon-search"></i>
            <input
              ref="inputRef"
              v-model="query"
              type="text"
              placeholder="输入命令或搜索..."
              class="command-input"
              @keydown="handleKeydown"
            />
            <kbd class="escape-hint">ESC</kbd>
          </div>
          
          <div class="command-results" v-if="query">
            <div 
              v-for="(group, gIndex) in filteredGroups"
              :key="gIndex"
              class="result-group"
            >
              <div class="group-title">{{ group.title }}</div>
              <div
                v-for="(item, iIndex) in group.items"
                :key="item.id"
                class="result-item"
                :class="{ 'is-selected': selectedIndex === getGlobalIndex(gIndex, iIndex) }"
                @click="execute(item)"
                @mouseenter="selectedIndex = getGlobalIndex(gIndex, iIndex)"
              >
                <i :class="item.icon || 'el-icon-document'"></i>
                <span class="item-label">{{ item.label }}</span>
                <span v-if="item.shortcut" class="item-shortcut">
                  <kbd>{{ item.shortcut }}</kbd>
                </span>
              </div>
            </div>
            
            <div v-if="filteredGroups.length === 0" class="no-results">
              没有找到匹配的命令
            </div>
          </div>
          
          <div class="command-tips" v-else>
            <div class="tip-item">
              <span class="tip-icon">↑↓</span>
              <span>导航</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">Enter</span>
              <span>执行</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">ESC</span>
              <span>关闭</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const visible = ref(false)
const query = ref('')
const selectedIndex = ref(0)
const inputRef = ref(null)
const paletteRef = ref(null)

const commandGroups = [
  {
    title: '导航',
    items: [
      { id: 'nav-dashboard', label: '工作台', icon: 'el-icon-house', path: '/' },
      { id: 'nav-users', label: '用户管理', icon: 'el-icon-user', path: '/admin/users' },
      { id: 'nav-laws', label: '法规管理', icon: 'el-icon-document', path: '/admin/laws' },
      { id: 'nav-cases', label: '案例管理', icon: 'el-icon-collection', path: '/admin/cases' },
      { id: 'nav-alerts', label: '告警监控', icon: 'el-icon-warning', path: '/admin/alerts' }
    ]
  },
  {
    title: '操作',
    items: [
      { id: 'action-refresh', label: '刷新当前页面', icon: 'el-icon-refresh', shortcut: 'F5', action: () => location.reload() },
      { id: 'action-export', label: '导出数据', icon: 'el-icon-download' },
      { id: 'action-import', label: '导入数据', icon: 'el-icon-upload2' }
    ]
  },
  {
    title: '系统',
    items: [
      { id: 'sys-settings', label: '系统设置', icon: 'el-icon-setting', path: '/admin/settings' },
      { id: 'sys-logs', label: '操作日志', icon: 'el-icon-tickets', path: '/admin/audit-logs' },
      { id: 'sys-help', label: '帮助文档', icon: 'el-icon-question' }
    ]
  }
]

const filteredGroups = computed(() => {
  if (!query.value) return commandGroups
  
  const q = query.value.toLowerCase()
  
  return commandGroups.map(group => ({
    ...group,
    items: group.items.filter(item => 
      item.label.toLowerCase().includes(q)
    )
  })).filter(group => group.items.length > 0)
})

const flatItems = computed(() => {
  return filteredGroups.value.flatMap(group => group.items)
})

const getGlobalIndex = (gIndex, iIndex) => {
  let index = 0
  for (let i = 0; i < gIndex; i++) {
    index += filteredGroups.value[i].items.length
  }
  return index + iIndex
}

const handleKeydown = (e) => {
  const maxIndex = flatItems.value.length - 1
  
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, maxIndex)
      break
    case 'ArrowUp':
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      break
    case 'Enter':
      e.preventDefault()
      if (flatItems.value[selectedIndex.value]) {
        execute(flatItems.value[selectedIndex.value])
      }
      break
    case 'Escape':
      hide()
      break
  }
}

const execute = (item) => {
  if (item.action) {
    item.action()
  } else if (item.path) {
    router.push(item.path)
  }
  hide()
}

const show = () => {
  visible.value = true
  query.value = ''
  selectedIndex.value = 0
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const hide = () => {
  visible.value = false
  query.value = ''
}

watch(query, () => {
  selectedIndex.value = 0
})

const handleGlobalKeydown = (e) => {
  if (e.ctrlKey && e.key === 'k') {
    e.preventDefault()
    visible.value ? hide() : show()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
})

defineExpose({ show, hide })
</script>

<style scoped>
.command-palette-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 15vh;
  z-index: 9999;
}

.command-palette {
  width: 580px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: paletteIn 0.2s ease;
}

@keyframes paletteIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.command-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  
  i {
    font-size: 20px;
    color: var(--color-text-muted);
  }
}

.command-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--color-text-primary);
  
  &::placeholder {
    color: var(--color-text-muted);
  }
}

.escape-hint {
  padding: 4px 8px;
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.command-results {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
}

.result-group {
  margin-bottom: 8px;
}

.group-title {
  padding: 8px 12px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s;
  
  &:hover,
  &.is-selected {
    background: var(--color-bg-soft);
  }
  
  i {
    font-size: 18px;
    color: var(--color-text-secondary);
  }
  
  .item-label {
    flex: 1;
    font-size: 14px;
    color: var(--color-text-primary);
  }
  
  .item-shortcut kbd {
    padding: 4px 8px;
    background: var(--color-bg-mute);
    border-radius: 4px;
    font-size: 11px;
    font-family: monospace;
    color: var(--color-text-muted);
  }
}

.no-results {
  padding: 40px;
  text-align: center;
  color: var(--color-text-muted);
}

.command-tips {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 16px;
  border-top: 1px solid var(--color-border-light);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
  
  .tip-icon {
    padding: 4px 8px;
    background: var(--color-bg-soft);
    border-radius: 4px;
    font-size: 11px;
  }
}

.command-fade-enter-active,
.command-fade-leave-active {
  transition: opacity 0.2s ease;
}

.command-fade-enter-from,
.command-fade-leave-to {
  opacity: 0;
}
</style>
