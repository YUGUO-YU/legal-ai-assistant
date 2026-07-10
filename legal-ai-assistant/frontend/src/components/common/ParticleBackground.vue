<template>
  <div class="particle-background" ref="containerRef">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  particleCount: {
    type: Number,
    default: 80
  },
  particleColor: {
    type: String,
    default: '#667eea'
  },
  lineColor: {
    type: String,
    default: 'rgba(102, 126, 234, 0.15)'
  },
  particleSize: {
    type: Number,
    default: 2
  },
  interactive: {
    type: Boolean,
    default: true
  },
  speed: {
    type: Number,
    default: 0.5
  }
})

const containerRef = ref(null)
const canvasRef = ref(null)
let animationId = null
let particles = []
let mouse = { x: null, y: null, radius: 150 }

class Particle {
  constructor(canvas) {
    this.canvas = canvas
    this.x = Math.random() * canvas.width
    this.y = Math.random() * canvas.height
    this.size = Math.random() * 2 + props.particleSize
    this.speedX = (Math.random() - 0.5) * props.speed
    this.speedY = (Math.random() - 0.5) * props.speed
    this.color = props.particleColor
  }

  update() {
    this.x += this.speedX
    this.y += this.speedY

    if (props.interactive && mouse.x !== null) {
      const dx = mouse.x - this.x
      const dy = mouse.y - this.y
      const distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < mouse.radius) {
        const force = (mouse.radius - distance) / mouse.radius
        this.x -= dx * force * 0.02
        this.y -= dy * force * 0.02
      }
    }

    if (this.x < 0 || this.x > this.canvas.width) {
      this.speedX *= -1
    }
    if (this.y < 0 || this.y > this.canvas.height) {
      this.speedY *= -1
    }
  }

  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = this.color
    ctx.fill()
  }
}

const init = () => {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  canvas.width = container.offsetWidth
  canvas.height = container.offsetHeight

  particles = []
  for (let i = 0; i < props.particleCount; i++) {
    particles.push(new Particle(canvas))
  }
}

const animate = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  particles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })

  drawLines()

  animationId = requestAnimationFrame(animate)
}

const drawLines = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < 120) {
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(102, 126, 234, ${1 - distance / 120})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }
}

const handleMouseMove = (e) => {
  if (!props.interactive) return
  const rect = canvasRef.value.getBoundingClientRect()
  mouse.x = e.clientX - rect.left
  mouse.y = e.clientY - rect.top
}

const handleMouseLeave = () => {
  mouse.x = null
  mouse.y = null
}

const handleResize = () => {
  init()
}

onMounted(() => {
  init()
  animate()
  
  if (props.interactive) {
    canvasRef.value?.addEventListener('mousemove', handleMouseMove)
    canvasRef.value?.addEventListener('mouseleave', handleMouseLeave)
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})

watch(() => props.particleCount, () => {
  init()
})
</script>

<style scoped>
.particle-background {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  
  canvas {
    display: block;
    width: 100%;
    height: 100%;
  }
}
</style>
