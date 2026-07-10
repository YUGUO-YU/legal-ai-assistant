<template>
  <div class="grid-background">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let animationId = null
let offset = 0

const draw = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height
  
  ctx.clearRect(0, 0, width, height)
  
  const gridSize = 60
  const lineWidth = 1
  const lineColor = 'rgba(102, 126, 234, 0.1)'
  
  ctx.strokeStyle = lineColor
  ctx.lineWidth = lineWidth
  
  offset = (offset + 0.2) % gridSize
  
  for (let x = -gridSize + offset; x < width + gridSize; x += gridSize) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, height)
    ctx.stroke()
  }
  
  for (let y = 0; y < height; y += gridSize) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(width, y)
    ctx.stroke()
  }
  
  const gradient = ctx.createRadialGradient(
    width / 2, height / 2, 0,
    width / 2, height / 2, Math.max(width, height) / 1.5
  )
  gradient.addColorStop(0, 'rgba(102, 126, 234, 0.05)')
  gradient.addColorStop(1, 'rgba(102, 126, 234, 0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, width, height)
  
  animationId = requestAnimationFrame(draw)
}

const handleResize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  
  const container = canvas.parentElement
  canvas.width = container.offsetWidth
  canvas.height = container.offsetHeight
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  draw()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.grid-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  
  canvas {
    display: block;
    width: 100%;
    height: 100%;
  }
}
</style>
