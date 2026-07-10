<template>
  <div class="icon-picker">
    <el-input
      v-model="searchQuery"
      placeholder="搜索图标..."
      prefix-icon="el-icon-search"
      clearable
    />
    
    <div class="icon-grid">
      <div
        v-for="icon in filteredIcons"
        :key="icon"
        class="icon-item"
        :class="{ 'is-selected': selected === icon }"
        @click="select(icon)"
      >
        <i :class="icon"></i>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: String
})

const emit = defineEmits(['update:modelValue'])

const searchQuery = ref('')

const allIcons = [
  'el-icon-user',
  'el-icon-user-solid',
  'el-icon-edit',
  'el-icon-delete',
  'el-icon-search',
  'el-icon-plus',
  'el-icon-check',
  'el-icon-close',
  'el-icon-arrow-left',
  'el-icon-arrow-right',
  'el-icon-arrow-up',
  'el-icon-arrow-down',
  'el-icon-loading',
  'el-icon-setting',
  'el-icon-menu',
  'el-icon-view',
  'el-icon-data-board',
  'el-icon-data-analysis',
  'el-icon-upload',
  'el-icon-download',
  'el-icon-share',
  'el-icon-document',
  'el-icon-document-copy',
  'el-icon-folder',
  'el-icon-folder-opened',
  'el-icon-files',
  'el-icon-news',
  'el-icon-message',
  'el-icon-bell',
  'el-icon-warning',
  'el-icon-info',
  'el-icon-success',
  'el-icon-error',
  'el-icon-time',
  'el-icon-date',
  'el-icon-location',
  'el-icon-phone',
  'el-icon-lock',
  'el-icon-unlock',
  'el-icon-key',
  'el-icon-star-off',
  'el-icon-star-on',
  'el-icon-heart',
  'el-icon-like',
  'el-icon-filter',
  'el-icon-sort',
  'el-icon-rank',
  'el-icon-refresh',
  'el-icon-shopping-cart',
  'el-icon-bank-card',
  'el-icon-money',
  'el-icon-tickets',
  'el-icon-map-location',
  'el-icon-place',
  'el-icon-connection',
  'el-icon-wifi',
  'el-icon-house',
  'el-icon-office-building',
  'el-icon-shop',
  'el-icon-magic-stick',
  'el-icon-collection',
  'el-icon-box',
]

const selected = computed(() => props.modelValue)

const filteredIcons = computed(() => {
  if (!searchQuery.value) return allIcons
  return allIcons.filter(icon => 
    icon.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const select = (icon) => {
  emit('update:modelValue', icon)
}
</script>

<style scoped>
.icon-picker {
  .icon-grid {
    display: grid;
    grid-template-columns: repeat(8, 1fr);
    gap: 8px;
    margin-top: 16px;
    max-height: 300px;
    overflow-y: auto;
  }
  
  .icon-item {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    
    i {
      font-size: 18px;
      color: var(--color-text-secondary);
    }
    
    &:hover {
      background: var(--color-bg-soft);
      
      i {
        color: var(--color-primary);
      }
    }
    
    &.is-selected {
      background: rgba(102, 126, 234, 0.1);
      
      i {
        color: var(--color-primary);
      }
    }
  }
}
</style>
