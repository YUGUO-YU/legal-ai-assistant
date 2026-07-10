<template>
  <div
    ref="containerRef"
    class="virtual-scroll"
    :style="{ height: containerHeight + 'px' }"
    @scroll="handleScroll"
  >
    <div
      class="virtual-scroll-spacer"
      :style="{ height: totalHeight + 'px', position: 'relative' }"
    >
      <div
        class="virtual-scroll-content"
        :style="{ transform: `translateY(${offsetY}px)` }"
      >
        <slot
          v-for="item in visibleItems"
          :key="item[props.rowKey]"
          :item="item"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  rowHeight: {
    type: Number,
    default: 60
  },
  bufferSize: {
    type: Number,
    default: 5
  },
  containerHeight: {
    type: Number,
    default: 400
  }
})

const containerRef = ref(null)
const scrollTop = ref(0)

const totalHeight = computed(() => props.items.length * props.rowHeight)

const startIndex = computed(() => {
  const start = Math.floor(scrollTop.value / props.rowHeight) - props.bufferSize
  return Math.max(0, start)
})

const endIndex = computed(() => {
  const visibleCount = Math.ceil(props.containerHeight / props.rowHeight)
  const end = startIndex.value + visibleCount + props.bufferSize * 2
  return Math.min(props.items.length, end)
})

const visibleItems = computed(() => {
  return props.items.slice(startIndex.value, endIndex.value)
})

const offsetY = computed(() => startIndex.value * props.rowHeight)

const handleScroll = (e) => {
  scrollTop.value = e.target.scrollTop
}

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.addEventListener('scroll', handleScroll)
  }
})

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style scoped>
.virtual-scroll {
  overflow-y: auto;
  overflow-x: hidden;
}

.virtual-scroll-spacer {
  width: 100%;
}

.virtual-scroll-content {
  width: 100%;
}
</style>
