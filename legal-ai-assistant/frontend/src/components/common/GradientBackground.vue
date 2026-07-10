<template>
  <div class="gradient-background" :class="variant">
    <div class="gradient-layer layer-1"></div>
    <div class="gradient-layer layer-2"></div>
    <div class="gradient-layer layer-3"></div>
    <div class="gradient-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'purple'
  }
})
</script>

<style scoped>
.gradient-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

.gradient-layer {
  position: absolute;
  inset: 0;
  opacity: 0.8;
}

.layer-1 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  animation: gradientShift 15s ease infinite;
}

.layer-2 {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 50%, #f093fb 100%);
  animation: gradientShift 15s ease infinite reverse;
  opacity: 0.5;
}

.layer-3 {
  background: linear-gradient(135deg, #f093fb 0%, #667eea 50%, #764ba2 100%);
  animation: gradientShift 15s ease infinite;
  animation-delay: -5s;
  opacity: 0.3;
}

@keyframes gradientShift {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(2%, 2%) rotate(1deg); }
  50% { transform: translate(0, 4%) rotate(0deg); }
  75% { transform: translate(-2%, 2%) rotate(-1deg); }
}

.gradient-orbs {
  position: absolute;
  inset: 0;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
  animation: orbFloat 20s ease-in-out infinite;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: rgba(102, 126, 234, 0.4);
  top: -10%;
  right: -10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: rgba(118, 75, 162, 0.3);
  bottom: -5%;
  left: -5%;
  animation-delay: -5s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: rgba(240, 147, 251, 0.3);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -10s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(0, -50px) scale(1); }
  75% { transform: translate(-30px, -30px) scale(0.95); }
}
</style>
