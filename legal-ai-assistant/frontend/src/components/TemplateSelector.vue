<template>
  <el-dialog v-model="visible" title="选择模板" width="800px" class="template-dialog" @close="handleClose">
    <div class="template-grid">
      <div
        v-for="template in templates"
        :key="template.id"
        class="template-item"
        :class="{ active: selectedId === template.id }"
        @click="selectTemplate(template.id)"
      >
        <div class="template-preview" :style="getTemplateStyle(template)">
          <div class="template-preview-title">{{ template.name }}</div>
        </div>
        <div class="template-name">{{ template.name }}</div>
        <div class="template-desc" v-if="template.description">{{ template.description }}</div>
        <el-tag v-if="template.source === 'ai'" size="small" type="warning" effect="plain">AI推荐</el-tag>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="applyTemplate">应用模板</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  templates: { type: Array, default: () => [] },
  currentId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'apply'])

const visible = ref(props.modelValue)
const selectedId = ref(props.currentId)

watch(() => props.modelValue, (val) => { visible.value = val })
watch(() => props.currentId, (val) => { selectedId.value = val })

const selectTemplate = (id) => {
  selectedId.value = id
}

const applyTemplate = () => {
  if (!selectedId.value) {
    ElMessage.warning('请选择一个模板')
    return
  }
  emit('apply', selectedId.value)
  visible.value = false
  emit('update:modelValue', false)
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const getTemplateStyle = (template) => {
  if (template.backgroundStyle === 'gradient') {
    return { background: `linear-gradient(135deg, #${template.primaryColor} 0%, #${template.secondaryColor} 100%)` }
  }
  return { background: `#${template.primaryColor}` }
}
</script>

<style lang="scss" scoped>
.template-dialog {
  .template-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    padding: 16px 0;
  }

  .template-item {
    cursor: pointer;
    border: 2px solid transparent;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    &.active {
      border-color: #667eea;
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
    }

    .template-preview {
      height: 100px;
      display: flex;
      align-items: center;
      justify-content: center;

      .template-preview-title {
        color: #fff;
        font-weight: 600;
        font-size: 14px;
      }
    }

    .template-name {
      padding: 8px 12px 0;
      font-size: 13px;
      font-weight: 500;
      color: #1f2937;
      text-align: center;
    }

    .template-desc {
      padding: 2px 12px 4px;
      font-size: 11px;
      color: #6b7280;
      text-align: center;
    }

    .el-tag {
      margin: 0 auto 8px;
      display: block;
      width: fit-content;
    }
  }
}
</style>
