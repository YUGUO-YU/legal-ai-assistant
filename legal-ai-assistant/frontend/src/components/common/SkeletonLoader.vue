<template>
  <div class="skeleton-loader" :class="{ 'skeleton-animated': animated }">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  animated: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
.skeleton-loader {
  .skeleton {
    background: linear-gradient(
      90deg,
      var(--color-border-light) 25%,
      var(--color-border) 50%,
      var(--color-border-light) 75%
    );
    background-size: 200% 100%;
    border-radius: var(--radius-md);
  }

  .skeleton-animated .skeleton {
    animation: shimmer 1.5s infinite;
  }
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
