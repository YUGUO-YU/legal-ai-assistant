<template>
  <el-dropdown @command="handleCommand" trigger="click">
    <el-button text class="language-btn">
      <el-icon><Monitor /></el-icon>
      <span class="lang-label">{{ currentLangLabel }}</span>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :class="{ active: currentLocale === 'zh-CN' }">
          <span class="lang-option">中文简体</span>
        </el-dropdown-item>
        <el-dropdown-item command="en-US" :class="{ active: currentLocale === 'en-US' }">
          <span class="lang-option">English</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { setLocale, getLocale } from '@/i18n'
import { Monitor } from '@element-plus/icons-vue'

const currentLocale = computed(() => getLocale())

const currentLangLabel = computed(() => {
  return currentLocale.value === 'zh-CN' ? '中文' : 'EN'
})

const handleCommand = (locale) => {
  setLocale(locale)
}
</script>

<style scoped>
.language-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.language-btn:hover {
  background-color: var(--bg-color-light);
}

.lang-label {
  font-size: 12px;
}

.lang-option {
  display: block;
  width: 100%;
}

:deep(.el-dropdown-menu__item.active) {
  color: var(--primary-color);
  background-color: var(--primary-color-light);
}
</style>
