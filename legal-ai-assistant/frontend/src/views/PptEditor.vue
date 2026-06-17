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
          @change="onTitleChange"
        />
      </div>
      <div class="header-actions">
        <el-button @click="showTemplateSelector = true" class="action-btn">
          <el-icon><Brush /></el-icon>
          模板
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
              size="small"
              type="danger"
              link
              @click.stop="deleteSlide(index)"
              class="delete-btn"
              :disabled="slides.length <= 1"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="preview-panel">
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
            <div class="preview-title">{{ currentSlide?.title || '' }}</div>
            <div class="preview-content">
              <div
                v-for="(point, idx) in currentSlide?.bulletPoints"
                :key="idx"
                class="preview-bullet"
              >
                • {{ point }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="edit-panel">
        <div class="panel-header">
          <span class="panel-title">编辑当前幻灯片</span>
        </div>
        <div class="edit-form" v-if="currentSlide">
          <el-form label-position="top" size="default">
            <el-form-item label="布局类型">
              <el-select v-model="currentSlide.layout" @change="onLayoutChange">
                <el-option label="仅标题" value="title_only" />
                <el-option label="标题+内容" value="title_content" />
                <el-option label="两栏布局" value="two_column" />
                <el-option label="空白" value="blank" />
              </el-select>
            </el-form-item>
            <el-form-item label="幻灯片标题">
              <el-input
                v-model="currentSlide.title"
                placeholder="请输入标题"
                @input="onSlideChange"
              />
            </el-form-item>
            <el-form-item label="内容要点">
              <div
                v-for="(point, index) in currentSlide.bulletPoints"
                :key="index"
                class="bullet-item"
              >
                <el-input
                  v-model="currentSlide.bulletPoints[index]"
                  placeholder="请输入内容"
                  @input="onSlideChange"
                />
                <el-button
                  type="danger"
                  link
                  @click="removeBullet(index)"
                  :disabled="currentSlide.bulletPoints.length <= 1"
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
                v-model="currentSlide.notes"
                type="textarea"
                :rows="3"
                placeholder="演讲者备注"
                @input="onSlideChange"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <el-dialog v-model="showTemplateSelector" title="选择模板" width="800px" class="template-dialog">
      <div class="template-grid">
        <div
          v-for="template in templates"
          :key="template.id"
          class="template-item"
          :class="{ active: currentTemplateId === template.id }"
          @click="selectTemplate(template.id)"
        >
          <div class="template-preview" :style="getTemplateStyle(template)">
            <div class="template-preview-title">{{ template.name }}</div>
          </div>
          <div class="template-name">{{ template.name }}</div>
          <el-tag v-if="template.source === 'ai'" size="small" type="warning">AI推荐</el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="showTemplateSelector = false">取消</el-button>
        <el-button type="primary" @click="applyTemplate">应用模板</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Document, Download, Brush, Plus, Delete, ZoomIn, ZoomOut, Remove } from '@element-plus/icons-vue'
import { usePptStore } from '@/store/ppt'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const pptStore = usePptStore()

const documentTitle = ref('')
const currentSlideIndex = ref(0)
const zoom = ref(0.8)
const saving = ref(false)
const downloading = ref(false)
const showTemplateSelector = ref(false)
const selectedTemplateId = ref('')

const slides = computed(() => pptStore.slides)
const currentSlide = computed(() => slides.value[currentSlideIndex.value])
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

const getTemplateStyle = (template) => {
  if (template.backgroundStyle === 'gradient') {
    return { background: `linear-gradient(135deg, #${template.primaryColor} 0%, #${template.secondaryColor} 100%)` }
  }
  return { background: `#${template.primaryColor}` }
}

const getLayoutLabel = (layout) => {
  const labels = {
    'title_only': '仅标题',
    'title_content': '标题+内容',
    'two_column': '两栏',
    'blank': '空白'
  }
  return labels[layout] || '标题+内容'
}

const goBack = () => {
  router.back()
}

const onTitleChange = () => {
}

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

const onLayoutChange = () => {
  pptStore.updateSlide(currentSlideIndex.value, currentSlide.value)
}

const onSlideChange = () => {
  pptStore.updateSlide(currentSlideIndex.value, currentSlide.value)
}

const addBullet = () => {
  if (!currentSlide.value.bulletPoints) {
    currentSlide.value.bulletPoints = []
  }
  currentSlide.value.bulletPoints.push('')
  onSlideChange()
}

const removeBullet = (index) => {
  if (currentSlide.value.bulletPoints.length <= 1) return
  currentSlide.value.bulletPoints.splice(index, 1)
  onSlideChange()
}

const zoomIn = () => {
  if (zoom.value < 1.5) zoom.value += 0.1
}

const zoomOut = () => {
  if (zoom.value > 0.5) zoom.value -= 0.1
}

const selectTemplate = (id) => {
  selectedTemplateId.value = id
}

const applyTemplate = () => {
  if (pptStore.currentDocument) {
    pptStore.currentDocument.templateId = selectedTemplateId.value
  }
  showTemplateSelector.value = false
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
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const downloadPpt = async () => {
  if (!pptStore.currentDocument) return
  downloading.value = true
  try {
    const response = await api.get(`/ppt/${pptStore.currentDocument.id}/download`, {
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.download = `${documentTitle.value || 'PPT演示文稿'}.pptx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
  }
}

onMounted(async () => {
  await pptStore.loadTemplates()

  if (route.query.id) {
    await pptStore.loadDocument(route.query.id)
    documentTitle.value = pptStore.currentDocument?.title || ''
  } else if (route.query.title) {
    documentTitle.value = route.query.title
  }

  if (route.query.searchResults) {
    try {
      const results = JSON.parse(route.query.searchResults)
      pptStore.generating = true
      await pptStore.generatePpt(documentTitle.value, results)
      documentTitle.value = pptStore.currentDocument?.title || ''
    } catch (error) {
      ElMessage.error('生成PPT失败')
    }
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
    color: #1f2937;
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
      color: #1f2937;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .slide-layout-tag {
      font-size: 11px;
      color: #6b7280;
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

.preview-panel {
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
    }
  }
}

.edit-panel {
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
      padding: 8px 12px;
      font-size: 13px;
      font-weight: 500;
      color: #1f2937;
      text-align: center;
    }
  }
}
</style>
