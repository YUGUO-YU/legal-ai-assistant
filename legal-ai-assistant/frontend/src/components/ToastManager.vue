<template>
  <div class="toast-manager">
    <slot />
  </div>
</template>

<script setup>
import { provide } from 'vue'

const showToast = (options) => {
  const { message, type = 'info', duration = 3000, showClose = false } = options
  console.log(`[Toast] ${type}: ${message}`)
}

provide('toast', { showToast })
</script>

<style scoped>
.toast-manager {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  pointer-events: none;
  z-index: 9999;
}
</style>