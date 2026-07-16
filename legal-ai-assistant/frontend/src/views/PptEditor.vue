<template>
  <div class="ppt-editor">
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-input
          v-model="documentTitle"
          class="title-input"
          placeholder="PPT标题"
        />
      </div>
      <div class="header-actions">
        <el-button @click="showTemplateSelector = true" class="action-btn">
          <el-icon><Brush /></el-icon>
          模板
        </el-button>
        <el-button @click="aiEnhanceSlide" class="action-btn" :loading="enhancing">
          <el-icon><MagicStick /></el-icon>
          AI增强
        </el-button>
        <el-button @click="saveDocument" class="action-btn" :loading="saving">
          <el-icon><Document /></el-icon>
          保存
        </el-button>
        <el-button type="primary" @click="downloadPpt" class="action-btn primary" :loading="downloading">
          <el-icon><Download /></el-icon>
          下载PPTX
        </el-button>
      </div>
    </div>

    <div class="editor-container">
      <div class="slides-panel">
        <div class="panel-header">
          <span class="panel-title">幻灯片列表</span>
          <el-button size="small" @click="addSlide" class="add-slide-btn">
            <el-icon><Plus /></el-icon>
            添加
          </el-button>
        </div>
        <div class="slides-list">
          <div
            v-for="(slide, index) in slides"
            :key="slide.id"
            class="slide-item"
            :class="{ active: currentSlideIndex === index }"
            @click="selectSlide(index)"
          >
            <div class="slide-number">{{ index + 1 }}</div>
            <div class="slide-preview">
              <div class="slide-title-preview">{{ slide.title || '未命名幻灯片' }}</div>
              <div class="slide-layout-tag">{{ getLayoutLabel(slide.layout) }}</div>
            </div>
            <el-button
              size="small" type="danger" link
              @click.stop="deleteSlide(index)"
              class="delete-btn"
              :disabled="slides.length <= 1"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <PptPreview :slide="currentSlide" :template-style="previewStyle" />

      <SlideEditor :slide="currentSlide" @update="onSlideUpdate" />
    </div>

    <TemplateSelector
      v-model="showTemplateSelector"
      :templates="templates"
      :current-id="currentTemplateId"
      @apply="applyTemplate"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Document, Download, Brush, Plus, Delete, MagicStick
} from '@element-plus/icons-vue'
import { usePptStore } from '@/store/ppt'
import api from '@/api'
import PptPreview from '@/components/PptPreview.vue'
import SlideEditor from '@/components/SlideEditor.vue'
import TemplateSelector from '@/components/TemplateSelector.vue'

const route = useRoute()
const router = useRouter()
const pptStore = usePptStore()

const documentTitle = ref('')
const currentSlideIndex = ref(0)
const saving = ref(false)
const downloading = ref(false)
const enhancing = ref(false)
const showTemplateSelector = ref(false)

const slides = computed(() => pptStore.slides)
const currentSlide = computed(() => slides.value[currentSlideIndex.value] || null)
const templates = computed(() => pptStore.templates)
const currentTemplateId = computed(() => pptStore.currentDocument?.templateId || 'legal-blue')

const previewStyle = computed(() => {
  const template = templates.value.find(t => t.id === currentTemplateId.value)
  if (!template) return { background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }
  if (template.backgroundStyle === 'gradient') {
    return { background: `linear-gradient(135deg, #${template.primaryColor} 0%, #${template.secondaryColor} 100%)` }
  }
  return { background: `#${template.primaryColor}` }
})

const getLayoutLabel = (layout) => {
  const labels = { 'title_only': '仅标题', 'title_content': '标题+内容', 'two_column': '两栏', 'blank': '空白' }
  return labels[layout] || '标题+内容'
}

const goBack = () => router.back()

const selectSlide = (index) => {
  currentSlideIndex.value = index
}

const addSlide = () => {
  pptStore.addSlide(currentSlideIndex.value)
  currentSlideIndex.value = currentSlideIndex.value + 1
}

const deleteSlide = (index) => {
  if (slides.value.length <= 1) {
    ElMessage.warning('至少保留一张幻灯片')
    return
  }
  pptStore.deleteSlide(index)
  if (currentSlideIndex.value >= slides.value.length) {
    currentSlideIndex.value = slides.value.length - 1
  }
}

const onSlideUpdate = (data) => {
  pptStore.updateSlide(currentSlideIndex.value, data)
}

const applyTemplate = (id) => {
  if (pptStore.currentDocument) {
    pptStore.currentDocument.templateId = id
  }
  ElMessage.success('模板已应用')
}

const saveDocument = async () => {
  if (!pptStore.currentDocument) return
  saving.value = true
  try {
    await pptStore.updateDocument(pptStore.currentDocument.id, {
      title: documentTitle.value,
      slides: slides.value,
      templateId: currentTemplateId.value
    })
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const downloadPpt = async () => {
  if (!pptStore.currentDocument) return
  downloading.value = true
  try {
    const blob = await pptStore.downloadPpt(pptStore.currentDocument.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${documentTitle.value || 'PPT演示文稿'}.pptx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch {
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
  }
}

const aiEnhanceSlide = async () => {
  if (!currentSlide.value) {
    ElMessage.warning('请先选择一张幻灯片')
    return
  }
  const slide = currentSlide.value
  if (!slide.title || slide.title.trim() === '') {
    ElMessage.warning('请先输入幻灯片标题')
    return
  }
  enhancing.value = true
  try {
    const response = await api.ppt.enhanceSlide({
      title: slide.title,
      layout: slide.layout,
      currentBullets: slide.bulletPoints || []
    })
    const data = response?.data || response
    if (data && data.bulletPoints) {
      pptStore.updateSlide(currentSlideIndex.value, {
        bulletPoints: data.bulletPoints,
        notes: data.notes || slide.notes
      })
      ElMessage.success('AI已优化当前幻灯片内容')
    }
  } catch (e) {
    ElMessage.error('AI增强失败: ' + (e?.message || '请检查AI服务'))
  } finally {
    enhancing.value = false
  }
}

onMounted(async () => {
  await pptStore.loadTemplates()

  if (route.query.id) {
    await pptStore.loadDocument(route.query.id)
    documentTitle.value = pptStore.currentDocument?.title || ''
  } else if (route.query.title) {
    documentTitle.value = route.query.title
    ElMessage.warning('未指定文档ID，请先通过搜索结果生成PPT')
  }
})
</script>

<style lang="scss" scoped>
.ppt-editor {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .back-btn {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .title-input {
    width: 300px;

    :deep(.el-input__inner) {
      font-size: 18px;
      font-weight: 600;
      border: none;
      background: transparent;

      &:focus {
        background: #f5f5f5;
        border-radius: 8px;
      }
    }
  }

  .action-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    border-radius: 10px;

    &.primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      color: #fff;

      &:hover {
        opacity: 0.9;
        transform: translateY(-1px);
      }
    }
  }
}

.editor-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.slides-panel {
  width: 240px;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #f0f0f0;
  }

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .add-slide-btn {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.slides-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #e5e7eb;
    border-radius: 2px;
  }
}

.slide-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: #f9fafb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f0f0f0;
  }

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    border: 2px solid #667eea;
  }

  .slide-number {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #667eea;
    color: #fff;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
  }

  .slide-preview {
    flex: 1;
    min-width: 0;

    .slide-title-preview {
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .slide-layout-tag {
      font-size: 11px;
      color: var(--color-text-secondary);
      margin-top: 2px;
    }
  }

  .delete-btn {
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .delete-btn {
    opacity: 1;
  }
}
</style>
