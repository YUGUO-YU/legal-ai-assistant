<template>
  <div class="slide-editor">
    <div class="panel-header">
      <span class="panel-title">编辑当前幻灯片</span>
    </div>
    <div class="edit-form" v-if="slide">
      <el-form label-position="top" size="default">
        <el-form-item label="布局类型">
          <el-select v-model="editSlide.layout" @change="onChange">
            <el-option label="仅标题" value="title_only" />
            <el-option label="标题+内容" value="title_content" />
            <el-option label="两栏布局" value="two_column" />
            <el-option label="空白" value="blank" />
          </el-select>
        </el-form-item>
        <el-form-item label="幻灯片标题">
          <el-input
            v-model="editSlide.title"
            placeholder="请输入标题"
            @input="onChange"
          />
        </el-form-item>
        <el-form-item label="内容要点">
          <div
            v-for="(point, index) in editSlide.bulletPoints"
            :key="index"
            class="bullet-item"
          >
            <el-input
              v-model="editSlide.bulletPoints[index]"
              placeholder="请输入内容"
              @input="onChange"
            />
            <el-button
              type="danger"
              link
              @click="removeBullet(index)"
              :disabled="editSlide.bulletPoints.length <= 1"
            >
              <el-icon><Remove /></el-icon>
            </el-button>
          </div>
          <el-button size="small" @click="addBullet" class="add-bullet-btn">
            <el-icon><Plus /></el-icon>
            添加要点
          </el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="editSlide.notes"
            type="textarea"
            :rows="3"
            placeholder="演讲者备注"
            @input="onChange"
          />
        </el-form-item>
      </el-form>
    </div>
    <div class="empty-state" v-else>
      <el-icon :size="48"><Picture /></el-icon>
      <p>选择一张幻灯片开始编辑</p>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, toRaw } from 'vue'
import { Picture, Plus, Remove } from '@element-plus/icons-vue'

const props = defineProps({
  slide: { type: Object, default: null }
})

const emit = defineEmits(['update'])

const editSlide = ref(null)

watch(() => props.slide, (val) => {
  editSlide.value = val ? { ...toRaw(val), bulletPoints: [...(val.bulletPoints || [])] } : null
}, { immediate: true })

const onChange = () => {
  if (editSlide.value) {
    emit('update', { ...editSlide.value })
  }
}

const addBullet = () => {
  if (!editSlide.value.bulletPoints) {
    editSlide.value.bulletPoints = []
  }
  editSlide.value.bulletPoints.push('')
  onChange()
}

const removeBullet = (index) => {
  if (editSlide.value.bulletPoints.length <= 1) return
  editSlide.value.bulletPoints.splice(index, 1)
  onChange()
}
</script>

<style lang="scss" scoped>
.slide-editor {
  width: 320px;
  background: #fff;
  border-left: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
  }
}

.edit-form {
  flex: 1;
  padding: 16px;
  overflow-y: auto;

  :deep(.el-form-item) {
    margin-bottom: 16px;

    .el-form-item__label {
      font-size: 13px;
      font-weight: 500;
      color: #374151;
    }
  }
}

.bullet-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.add-bullet-btn {
  margin-top: 8px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;

  p {
    margin-top: 12px;
    font-size: 14px;
  }
}
</style>
