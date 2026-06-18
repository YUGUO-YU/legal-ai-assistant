<template>
  <div class="ppt-preview">
    <div class="preview-header">
      <span class="panel-title">预览</span>
      <div class="preview-controls">
        <el-button-group size="small">
          <el-button @click="zoomOut" :disabled="zoom <= 0.5">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
          <el-button disabled>{{ Math.round(zoom * 100) }}%</el-button>
          <el-button @click="zoomIn" :disabled="zoom >= 1.5">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-button-group>
      </div>
    </div>
    <div class="preview-container" :style="{ transform: `scale(${zoom})` }">
      <div class="slide-preview-card" :style="previewStyle">
        <div class="preview-title">{{ slide?.title || '无标题' }}</div>
        <div class="preview-content">
          <div
            v-for="(point, idx) in slide?.bulletPoints || []"
            :key="idx"
            class="preview-bullet"
          >
            <span class="bullet-marker"></span>
            {{ point }}
          </div>
        </div>
        <div v-if="slide?.notes" class="preview-notes">
          <el-icon><EditPen /></el-icon>
          {{ slide.notes }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ZoomIn, ZoomOut, EditPen } from '@element-plus/icons-vue'

const props = defineProps({
  slide: { type: Object, default: null },
  templateStyle: { type: Object, default: null }
})

const zoom = ref(0.8)

const previewStyle = computed(() => {
  if (props.templateStyle) return props.templateStyle
  return { background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }
})

const zoomIn = () => {
  if (zoom.value < 1.5) zoom.value += 0.1
}

const zoomOut = () => {
  if (zoom.value > 0.5) zoom.value -= 0.1
}
</script>

<style lang="scss" scoped>
.ppt-preview {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow: hidden;

  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
  }

  .preview-controls {
    display: flex;
    gap: 8px;
  }
}

.preview-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: center center;
  transition: transform 0.2s;
}

.slide-preview-card {
  width: 800px;
  height: 450px;
  padding: 48px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  color: #fff;
  min-height: 450px;

  .preview-title {
    font-size: 36px;
    font-weight: 700;
    margin-bottom: 32px;
    text-align: center;
  }

  .preview-content {
    flex: 1;

    .preview-bullet {
      font-size: 18px;
      line-height: 2;
      opacity: 0.9;

      .bullet-marker {
        display: inline-block;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #fff;
        margin-right: 8px;
        margin-bottom: 2px;
        opacity: 0.7;
      }
    }
  }

  .preview-notes {
    font-size: 12px;
    opacity: 0.6;
    border-top: 1px solid rgba(255, 255, 255, 0.2);
    padding-top: 12px;
    display: flex;
    align-items: center;
    gap: 6px;
  }
}
</style>
