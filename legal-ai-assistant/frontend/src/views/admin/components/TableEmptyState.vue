<template>
  <div class="table-empty-state">
    <el-empty :image-size="100" :description="text">
      <template #image>
        <svg class="empty-icon" viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="10" y="20" width="100" height="70" rx="8" stroke="currentColor" stroke-width="2" fill="none" opacity="0.3"/>
          <rect x="20" y="35" width="80" height="8" rx="4" fill="currentColor" opacity="0.2"/>
          <rect x="20" y="50" width="60" height="8" rx="4" fill="currentColor" opacity="0.15"/>
          <rect x="20" y="65" width="40" height="8" rx="4" fill="currentColor" opacity="0.1"/>
          <circle cx="85" cy="25" r="15" fill="var(--color-bg-page)" stroke="currentColor" stroke-width="2"/>
          <path d="M80 25L83 28L90 21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.5"/>
        </svg>
      </template>
      <slot />
    </el-empty>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '暂无数据'
  }
})
</script>

<style scoped>
.table-empty-state {
  padding: 40px 0;
  display: flex;
  justify-content: center;
  animation: adminFadeIn 0.5s ease both;
}
.empty-icon {
  width: 120px;
  height: 100px;
  color: var(--color-text-placeholder);
}
</style>
