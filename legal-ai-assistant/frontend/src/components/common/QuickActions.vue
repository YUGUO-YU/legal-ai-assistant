<template>
  <Teleport to="body">
    <Transition name="quick-panel">
      <div 
        v-if="visible"
        class="quick-actions-panel"
        :class="{ 'is-collapsed': isCollapsed }"
        :style="panelStyle"
      >
        <button class="toggle-btn" @click="toggleCollapse">
          <i :class="isCollapsed ? 'el-icon-d-arrow-left' : 'el-icon-d-arrow-right'"></i>
        </button>
        
        <div class="panel-content" v-show="!isCollapsed">
          <div class="panel-header">
            <h3>快捷操作</h3>
            <button class="close-btn" @click="hide">
              <i class="el-icon-close"></i>
            </button>
          </div>
          
          <div class="panel-body">
            <div class="action-section">
              <div class="section-title">常用操作</div>
              <div class="action-grid">
                <div 
                  v-for="action in quickActions"
                  :key="action.id"
                  class="action-item"
                  @click="executeAction(action)"
                >
                  <div class="action-icon">
                    <i :class="action.icon"></i>
                  </div>
                  <div class="action-label">{{ action.label }}</div>
                </div>
              </div>
            </div>
            
            <div class="action-section" v-if="recentItems.length > 0">
              <div class="section-title">最近访问</div>
              <div class="recent-list">
                <div 
                  v-for="item in recentItems"
                  :key="item.id"
                  class="recent-item"
                  @click="navigateTo(item)"
                >
                  <i :class="item.icon || 'el-icon-document'"></i>
                  <span class="recent-label">{{ item.label }}</span>
                  <span class="recent-time">{{ formatTime(item.time) }}</span>
                </div>
              </div>
            </div>
            
            <div class="action-section">
              <div class="section-title">快捷键</div>
              <div class="shortcuts-list">
                <div 
                  v-for="shortcut in shortcuts" 
                  :key="shortcut.key"
                  class="shortcut-item"
                >
                  <kbd>{{ shortcut.key }}</kbd>
                  <span>{{ shortcut.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  position: {
    type: String,
    default: 'right'
  }
})

const emit = defineEmits(['close'])

const visible = ref(false)
const isCollapsed = ref(false)
const recentItems = ref([])

const quickActions = ref([
  { id: '1', label: '用户管理', icon: 'el-icon-user', path: '/admin/users' },
  { id: '2', label: '法规列表', icon: 'el-icon-document', path: '/admin/laws' },
  { id: '3', label: '案例管理', icon: 'el-icon-collection', path: '/admin/cases' },
  { id: '4', label: '搜索日志', icon: 'el-icon-search', path: '/admin/search-logs' },
  { id: '5', label: '告警监控', icon: 'el-icon-warning', path: '/admin/alerts' },
  { id: '6', label: '系统设置', icon: 'el-icon-setting', path: '/admin/settings' }
])

const shortcuts = [
  { key: 'Ctrl + K', label: '打开命令面板' },
  { key: 'Ctrl + /', label: '显示快捷键' },
  { key: 'Ctrl + Enter', label: '发送/提交' },
  { key: 'Escape', label: '关闭弹窗' },
  { key: '/', label: '聚焦搜索框' }
]

const panelStyle = computed(() => {
  return {
    [props.position]: '20px'
  }
})

const show = () => {
  visible.value = true
  loadRecentItems()
}

const hide = () => {
  visible.value = false
  emit('close')
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const executeAction = (action) => {
  if (action.path) {
    router.push(action.path)
    addToRecent(action)
  }
}

const navigateTo = (item) => {
  if (item.path) {
    router.push(item.path)
  }
}

const addToRecent = (action) => {
  const recent = {
    id: action.id,
    label: action.label,
    icon: action.icon,
    path: action.path,
    time: Date.now()
  }
  
  recentItems.value = [
    recent,
    ...recentItems.value.filter(r => r.id !== action.id)
  ].slice(0, 5)
  
  saveRecentItems()
}

const loadRecentItems = () => {
  try {
    const saved = localStorage.getItem('quick-actions-recent')
    if (saved) {
      recentItems.value = JSON.parse(saved)
    }
  } catch (e) {
    console.warn('Failed to load recent items:', e)
  }
}

const saveRecentItems = () => {
  try {
    localStorage.setItem('quick-actions-recent', JSON.stringify(recentItems.value))
  } catch (e) {
    console.warn('Failed to save recent items:', e)
  }
}

const formatTime = (timestamp) => {
  const diff = Date.now() - timestamp
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  
  const days = Math.floor(hours / 24)
  return `${days}天前`
}

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
.quick-actions-panel {
  position: fixed;
  bottom: 100px;
  z-index: 8000;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  
  &.is-collapsed {
    .toggle-btn {
      i {
        transform: rotate(180deg);
      }
    }
  }
}

.toggle-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-primary);
  border: none;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  
  i {
    font-size: 18px;
    transition: transform 0.3s ease;
  }
  
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 24px rgba(102, 126, 234, 0.5);
  }
}

.panel-content {
  width: 320px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
  
  .close-btn {
    padding: 4px;
    background: transparent;
    border: none;
    cursor: pointer;
    color: var(--color-text-muted);
    border-radius: var(--radius-sm);
    
    &:hover {
      background: var(--color-bg-soft);
      color: var(--color-text-primary);
    }
  }
}

.panel-body {
  padding: 16px 20px;
  max-height: 500px;
  overflow-y: auto;
}

.action-section {
  margin-bottom: 20px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: var(--color-bg-soft);
    
    .action-icon {
      transform: scale(1.1);
    }
  }
  
  .action-icon {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-radius: 10px;
    color: var(--color-primary);
    font-size: 18px;
    transition: transform 0.2s ease;
  }
  
  .action-label {
    font-size: 12px;
    color: var(--color-text-secondary);
    text-align: center;
  }
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
  
  &:hover {
    background: var(--color-bg-soft);
  }
  
  i {
    font-size: 16px;
    color: var(--color-text-muted);
  }
  
  .recent-label {
    flex: 1;
    font-size: 13px;
    color: var(--color-text-primary);
  }
  
  .recent-time {
    font-size: 11px;
    color: var(--color-text-muted);
  }
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 12px;
  
  kbd {
    padding: 4px 8px;
    background: var(--color-bg-soft);
    border: 1px solid var(--color-border);
    border-radius: 4px;
    font-size: 11px;
    font-family: monospace;
    color: var(--color-text-secondary);
  }
  
  span {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.quick-panel-enter-active,
.quick-panel-leave-active {
  transition: all 0.3s ease;
}

.quick-panel-enter-from,
.quick-panel-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
