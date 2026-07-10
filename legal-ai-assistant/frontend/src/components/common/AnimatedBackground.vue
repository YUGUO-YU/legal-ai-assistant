<template>
  <div class="animated-background" :class="type">
    <component :is="backgroundComponent" />
    <div class="background-overlay"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ParticleBackground from './ParticleBackground.vue'
import GradientBackground from './GradientBackground.vue'
import GridBackground from './GridBackground.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'particle'
  }
})

const backgroundComponent = computed(() => {
  switch (props.type) {
    case 'particle': return ParticleBackground
    case 'gradient': return GradientBackground
    case 'grid': return GridBackground
    default: return ParticleBackground
  }
})
</script>

<style scoped>
.animated-background {
  position: relative;
  width: 100%;
  height: 100%;
  
  > :deep(.particle-background),
  > :deep(.gradient-background),
  > :deep(.grid-background) {
    position: absolute;
    inset: 0;
  }
}

.background-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  pointer-events: none;
}
</style>
