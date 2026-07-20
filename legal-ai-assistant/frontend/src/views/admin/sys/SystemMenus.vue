<template>
  <div class="menu-management">
    <div class="menu-header">
      <h3>菜单排序</h3>
      <el-button type="primary" @click="saveOrder" :loading="saving">
        保存排序
      </el-button>
    </div>

    <DraggableList
      v-model="menus"
      item-key="id"
      handle=".drag-handle"
      :animation="200"
      @change="handleOrderChange"
    >
      <template #default="{ element, index }">
        <div class="menu-item-content">
          <div class="menu-icon" v-if="element.icon">
            <i :class="element.icon"></i>
          </div>
          <div class="menu-info">
            <div class="menu-name">{{ element.name }}</div>
            <div class="menu-path">{{ element.path }}</div>
          </div>
          <div class="menu-type">
            <el-tag v-if="element.type === 'menu'" size="small">菜单</el-tag>
            <el-tag v-else-if="element.type === 'button'" size="small" type="info">按钮</el-tag>
            <el-tag v-else size="small" type="warning">API</el-tag>
          </div>
        </div>
      </template>

      <template #actions="{ element }">
        <el-button size="small" @click="editMenu(element)">
          编辑
        </el-button>
      </template>
    </DraggableList>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import DraggableList from '@/components/common/DraggableList.vue'
import { ElMessage } from 'element-plus'

const menus = ref([
  { id: 1, name: '仪表盘', path: '/dashboard', icon: 'el-icon-data-board', type: 'menu' },
  { id: 2, name: '用户管理', path: '/users', icon: 'el-icon-user', type: 'menu' },
  { id: 3, name: '法规管理', path: '/laws', icon: 'el-icon-document', type: 'menu' },
  { id: 4, name: '案例管理', path: '/cases', icon: 'el-icon-collection', type: 'menu' },
  { id: 5, name: '系统设置', path: '/settings', icon: 'el-icon-setting', type: 'menu' }
])

const saving = ref(false)

const handleOrderChange = ({ oldIndex, newIndex, element }) => {
}

const saveOrder = async () => {
  saving.value = true
  try {
    const orderedIds = menus.value.map(m => m.id)
    // await api.updateMenuOrder(orderedIds)
    ElMessage.success('排序已保存')
  } finally {
    saving.value = false
  }
}

const editMenu = (menu) => {
  ElMessage.info('菜单编辑功能开发中')
}
</script>

<style scoped>
.menu-management {
  padding: 20px;
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.menu-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.menu-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.menu-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-soft);
  border-radius: var(--radius-sm);
  font-size: 16px;
  color: var(--color-primary);
}

.menu-info {
  flex: 1;
}

.menu-info .menu-name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.menu-info .menu-path {
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
