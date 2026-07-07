<template>
  <div class="table-empty-state">
    <el-empty :image-size="100" :description="text">
      <template #image>
        <svg class="empty-icon" viewBox="0 0 64 64" fill="none">
          <path d="M8 16C8 12.686 10.686 10 14 10H26L32 16H50C53.314 16 56 18.686 56 22V48C56 51.314 53.314 54 50 54H14C10.686 54 8 51.314 8 48V16Z" stroke="currentColor" stroke-width="2" fill="none"/>
          <path d="M8 22H56" stroke="currentColor" stroke-width="2"/>
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
  
  .empty-icon {
    width: 64px;
    height: 64px;
    color: var(--color-text-placeholder, #9ca3af);
  }
}
</style>
