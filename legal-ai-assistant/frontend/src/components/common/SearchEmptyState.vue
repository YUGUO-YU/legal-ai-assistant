<template>
  <div class="search-empty-state">
    <EmptyState
      title="未找到搜索结果"
      description="没有找到与关键词匹配的内容，请尝试其他搜索词"
      illustration="search"
      size="medium"
    >
      <template #action>
        <div class="search-suggestions">
          <span class="suggestion-label">建议：</span>
          <div class="suggestion-tags">
            <el-tag 
              v-for="suggestion in suggestions" 
              :key="suggestion"
              size="small"
              class="suggestion-tag"
              @click="handleSuggestionClick(suggestion)"
            >
              {{ suggestion }}
            </el-tag>
          </div>
        </div>
      </template>
    </EmptyState>
  </div>
</template>

<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  suggestions: {
    type: Array,
    default: () => ['法规', '案例', '合同', '关键词']
  }
})

const emit = defineEmits(['suggestion-click'])

const handleSuggestionClick = (suggestion) => {
  emit('suggestion-click', suggestion)
}
</script>

<style scoped>
.search-empty-state {
  padding: 48px 20px;
}

.search-suggestions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  
  .suggestion-label {
    font-size: 13px;
    color: var(--color-text-muted);
  }
  
  .suggestion-tags {
    display: flex;
    gap: 8px;
  }
  
  .suggestion-tag {
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
    }
  }
}
</style>
