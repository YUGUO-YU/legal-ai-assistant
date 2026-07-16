<template>
  <div class="document-page">
    <div class="page-header">
      <div class="header-content">
        <h2>法律文书生成</h2>
        <p>选择文书模板，输入案件信息，快速生成法律文书</p>
      </div>
    </div>

    <el-row :gutter="24">
      <el-col :span="8">
        <el-card class="template-card">
          <div class="search-box">
            <el-input
              v-model="templateSearch"
              placeholder="搜索模板"
              clearable
              prefix-icon="Search"
            />
          </div>

          <el-tabs v-model="activeCategory" class="category-tabs">
            <el-tab-pane label="民事诉讼" name="民事诉讼">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('民事诉讼')"
                  :key="tpl.templateCode"
                  :class="['template-item', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="劳动人事" name="劳动人事">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('劳动人事')"
                  :key="tpl.templateCode"
                  :class="['template-item', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="商业函件" name="商业函件">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('商业函件')"
                  :key="tpl.templateCode"
                  :class="['template-item', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Message /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="知识产权" name="知识产权">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('知识产权')"
                  :key="tpl.templateCode"
                  :class="['template-item', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Connection /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="行政诉讼" name="行政诉讼">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('行政诉讼')"
                  :key="tpl.templateCode"
                  :class="['template-item', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Coin /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="刑事诉讼" name="刑事诉讼">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('刑事诉讼')"
                  :key="tpl.templateCode"
                  :class="['template-item', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Warning /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="form-card">
          <div class="selected-template-info" v-if="selectedTemplate">
            <el-alert
              :title="'已选择：' + getTemplateName(selectedTemplate)"
              type="success"
              :closable="false"
              show-icon
            />
          </div>

          <el-form :model="formData" label-width="120px" :rules="formRules" ref="formRef">
            <el-form-item label="案件类型" prop="caseType">
              <el-select v-model="formData.caseType" placeholder="请选择案件类型">
                <el-option label="合同纠纷" value="contract" />
                <el-option label="劳动争议" value="labor" />
                <el-option label="侵权纠纷" value="tort" />
                <el-option label="婚姻家庭" value="family" />
                <el-option label="借款纠纷" value="loan" />
                <el-option label="建设工程" value="construction" />
              </el-select>
            </el-form-item>

            <el-form-item label="原告/申请人" prop="plaintiffName">
              <el-input v-model="formData.plaintiffName" placeholder="请输入原告或申请人姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="原告地址" prop="plaintiffAddress">
              <el-input v-model="formData.plaintiffAddress" placeholder="请输入原告地址">
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="被告/被申请人" prop="defendantName">
              <el-input v-model="formData.defendantName" placeholder="请输入被告或被申请人姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="被告地址" prop="defendantAddress">
              <el-input v-model="formData.defendantAddress" placeholder="请输入被告地址">
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="诉讼请求" prop="claimAmount">
              <el-input-number v-model="formData.claimAmount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%">
                <template #append>元</template>
              </el-input-number>
            </el-form-item>

            <el-form-item label="请求描述" prop="claimDescription">
              <el-input v-model="formData.claimDescription" type="textarea" :rows="3" placeholder="请简要描述诉讼请求" />
            </el-form-item>

            <el-form-item label="事实与理由" prop="facts">
              <el-input v-model="formData.facts" type="textarea" :rows="5" placeholder="请详细描述案件事实与理由" />
            </el-form-item>

            <el-form-item label="管辖法院">
              <el-input v-model="formData.courtName" placeholder="请输入管辖法院名称">
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-divider content-position="left">企业信息（选填）</el-divider>

            <el-form-item label="统一社会信用代码">
              <el-input v-model="formData.unifiedSocialCreditCode" placeholder="请输入统一社会信用代码">
                <template #prefix><el-icon><Document /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="法定代表人">
              <el-input v-model="formData.legalRepresentative" placeholder="请输入法定代表人姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="职务">
              <el-input v-model="formData.position" placeholder="请输入职务">
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="住所地">
              <el-input v-model="formData.residenceAddress" placeholder="请输入住所地">
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="联系电话">
              <el-input v-model="formData.plaintiffPhone" placeholder="请输入联系电话">
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleDraft" :loading="loading" :disabled="!selectedTemplate" class="ripple-effect btn-press">
                <el-icon v-if="!loading"><Document /></el-icon>
                生成文书
              </el-button>
              <el-button type="success" @click="showPasteDialog" :disabled="!selectedTemplate">
                <el-icon><DocumentCopy /></el-icon>
                一键粘贴识别
              </el-button>
              <el-button @click="resetForm">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <loading v-if="loading" text="正在生成法律文书..." />
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="showResult" title="生成结果" size="60%" direction="rtl">
      <div v-if="draftResult" class="result-content">
        <pre class="document-content">{{ draftResult.documentContent }}</pre>

        <div class="actions">
          <el-button type="primary" @click="copyDocument">
            <el-icon><CopyDocument /></el-icon>
            复制文书
          </el-button>
          <el-button @click="downloadDocument">
            <el-icon><Download /></el-icon>
            下载文档
          </el-button>
          <el-button @click="printDocument">
            <el-icon><Printer /></el-icon>
            打印
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="showPasteDialogVisible" title="一键粘贴识别" width="600px" :close-on-click-modal="false">
      <div class="paste-dialog-content">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            本地智能识别：粘贴法律文本后自动提取当事人、金额、事实等关键信息
          </template>
          <template #description>
            完全在本地完成，毫秒级响应。支持起诉状、仲裁申请书、合同等法律文本。
          </template>
        </el-alert>
        <el-input
          v-model="pasteText"
          type="textarea"
          :rows="10"
          placeholder="请在此粘贴案件相关信息，例如：&#10;原告：李某，住北京市朝阳区&#10;被告：王某，住上海市浦东新区&#10;诉讼请求：要求被告支付欠款10万元及利息..."
          class="paste-textarea"
        />
        <div class="paste-tips">
          <el-tag type="success" size="small">本地识别</el-tag>
          <span>支持识别：当事人、地址、身份证号、电话、金额、诉讼请求、事实理由、法院、案件类型、争议类型、统一社会信用代码、法定代表人、职务、住所地等全部字段。</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPasteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExtractInfo" :loading="extracting">
          <el-icon v-if="!extracting"><DocumentCopy /></el-icon>
          本地识别并填充
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  User,
  Message,
  Connection,
  OfficeBuilding,
  Refresh,
  CopyDocument,
  Download,
  Printer,
  Search,
  DocumentCopy,
  Coin,
  Warning
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import { useUsageMemory } from '@/composables/useUsageMemory'

const { addRecord } = useUsageMemory()

const formRef = ref(null)
const loading = ref(false)
const draftResult = ref(null)
const activeTab = ref('content')
const selectedTemplate = ref('')
const activeCategory = ref('民事诉讼')
const templateSearch = ref('')
const showResult = ref(false)
const showPasteDialogVisible = ref(false)
const pasteText = ref('')
const extracting = ref(false)

const formData = reactive({
  plaintiffName: '',
  plaintiffAddress: '',
  plaintiffPhone: '',
  plaintiffIdCard: '',
  defendantName: '',
  defendantAddress: '',
  defendantPhone: '',
  defendantIdCard: '',
  caseType: 'contract',
  claimAmount: 0,
  claimDescription: '',
  facts: '',
  courtName: '',
  employerName: '',
  employeeName: '',
  workContent: '',
  salary: '',
  startDate: '',
  disputeType: '',
  unifiedSocialCreditCode: '',
  legalRepresentative: '',
  position: '',
  residenceAddress: ''
})

const formRules = {
  plaintiffName: [
    { required: true, message: '请输入原告/申请人姓名', trigger: 'blur' }
  ],
  defendantName: [
    { required: true, message: '请输入被告/被申请人姓名', trigger: 'blur' }
  ],
  caseType: [
    { required: true, message: '请选择案件类型', trigger: 'change' }
  ]
}

const templates = ref([])

const filteredTemplatesByCategory = (category) => {
  return templates.value.filter(t =>
    t.category === category &&
    (!templateSearch.value || t.templateName.includes(templateSearch.value))
  )
}

const selectTemplate = (tpl) => {
  selectedTemplate.value = tpl.templateCode
  ElMessage.success(`已选择模板：${tpl.templateName}`)
}

const getTemplateName = (code) => {
  const tpl = templates.value.find(t => t.templateCode === code)
  return tpl ? tpl.templateName : code
}

const handleDraft = async () => {
  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择文书模板')
    return
  }

  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await api.document.draft({
        templateCode: selectedTemplate.value,
        caseType: formData.caseType,
        caseData: {
          plaintiffName: formData.plaintiffName,
          plaintiffAddress: formData.plaintiffAddress,
          plaintiffPhone: formData.plaintiffPhone,
          plaintiffIdCard: formData.plaintiffIdCard,
          defendantName: formData.defendantName,
          defendantAddress: formData.defendantAddress,
          defendantPhone: formData.defendantPhone,
          defendantIdCard: formData.defendantIdCard,
          claimAmount: formData.claimAmount,
          claimDescription: formData.claimDescription,
          facts: formData.facts,
          courtName: formData.courtName,
          employerName: formData.employerName,
          employeeName: formData.employeeName,
          workContent: formData.workContent,
          salary: formData.salary,
          startDate: formData.startDate,
          disputeType: formData.disputeType,
          unifiedSocialCreditCode: formData.unifiedSocialCreditCode,
          legalRepresentative: formData.legalRepresentative,
          position: formData.position,
          residenceAddress: formData.residenceAddress
        },
        includeRiskPrompt: true
      })

      draftResult.value = res.data
      showResult.value = true
      activeTab.value = 'content'
      ElMessage.success('文书生成成功')
      addRecord('document', `起草"${selectedTemplate.value}"`, '文书生成成功')
    } catch (e) {
      console.error(e)
      ElMessage.error('生成失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  draftResult.value = null
}

const copyDocument = () => {
  if (draftResult.value?.documentContent) {
    navigator.clipboard.writeText(draftResult.value.documentContent)
    ElMessage.success('已复制到剪贴板')
  }
}

const downloadDocument = () => {
  if (!draftResult.value?.documentContent) {
    ElMessage.warning('请先生成文书')
    return
  }

  const content = draftResult.value.documentContent
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `法律文书_${Date.now()}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)

  ElMessage.success('文档已下载')
}

const printDocument = () => {
  window.print()
}

const showPasteDialog = async () => {
  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择文书模板')
    return
  }
  
  try {
    const clipboardText = await navigator.clipboard.readText()
    if (clipboardText && clipboardText.trim()) {
      pasteText.value = clipboardText
      showPasteDialogVisible.value = true
      await handleExtractInfo()
    } else {
      pasteText.value = ''
      showPasteDialogVisible.value = true
      ElMessage.info('剪贴板为空，请手动粘贴案件信息')
    }
  } catch (err) {
    console.warn('无法自动读取剪贴板，可能是浏览器权限限制:', err)
    pasteText.value = ''
    showPasteDialogVisible.value = true
    ElMessage.warning('无法自动读取剪贴板，请手动粘贴案件信息')
  }
}

const handleExtractInfo = async () => {
  if (!pasteText.value.trim()) {
    ElMessage.warning('请先粘贴案件信息')
    return
  }
  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择文书模板')
    return
  }

  extracting.value = true
  try {
    const res = await api.document.extractInfo(pasteText.value, selectedTemplate.value)
    console.debug('[Document.extractInfo] response received')
    if (res.data) {
      const info = res.data
      if (info.plaintiffName) formData.plaintiffName = info.plaintiffName
      if (info.plaintiffAddress) formData.plaintiffAddress = info.plaintiffAddress
      if (info.plaintiffPhone) formData.plaintiffPhone = info.plaintiffPhone
      if (info.plaintiffIdCard) formData.plaintiffIdCard = info.plaintiffIdCard
      if (info.defendantName) formData.defendantName = info.defendantName
      if (info.defendantAddress) formData.defendantAddress = info.defendantAddress
      if (info.defendantPhone) formData.defendantPhone = info.defendantPhone
      if (info.defendantIdCard) formData.defendantIdCard = info.defendantIdCard
      if (info.claimAmount !== null && info.claimAmount !== undefined && info.claimAmount !== '') {
        const amt = Number(info.claimAmount)
        if (!Number.isNaN(amt)) formData.claimAmount = amt
      }
      if (info.claimDescription) formData.claimDescription = info.claimDescription
      if (info.facts) formData.facts = info.facts
      if (info.courtName) formData.courtName = info.courtName
      if (info.employerName) formData.employerName = info.employerName
      if (info.employeeName) formData.employeeName = info.employeeName
      if (info.workContent) formData.workContent = info.workContent
      if (info.salary) formData.salary = info.salary
      if (info.startDate) formData.startDate = info.startDate
      if (info.disputeType) formData.disputeType = info.disputeType
      if (info.caseType) formData.caseType = info.caseType
      if (info.unifiedSocialCreditCode) formData.unifiedSocialCreditCode = info.unifiedSocialCreditCode
      if (info.legalRepresentative) formData.legalRepresentative = info.legalRepresentative
      if (info.position) formData.position = info.position
      if (info.residenceAddress) formData.residenceAddress = info.residenceAddress

      const totalFilled = [
        info.plaintiffName, info.plaintiffAddress, info.defendantName, info.defendantAddress,
        info.claimAmount, info.claimDescription, info.facts, info.courtName,
        info.employerName, info.employeeName, info.workContent, info.salary,
        info.startDate, info.disputeType, info.caseType, info.plaintiffPhone,
        info.defendantPhone, info.claimBasis, info.evidence,
        info.unifiedSocialCreditCode, info.legalRepresentative, info.position, info.residenceAddress
      ].filter(v => v !== null && v !== undefined && String(v).trim() !== '').length

      if (totalFilled > 0) {
        const source = info.dataSource || '本地识别'
        ElMessage.success(`已自动填充 ${totalFilled} 项信息（${source}），请检查并补全`)
        showPasteDialogVisible.value = false
      } else {
        if (info.success === false && info.errorMessage) {
          ElMessage.warning(info.errorMessage)
        } else {
          ElMessage.warning('未能从文本中识别到关键信息，请检查粘贴内容或手动填写')
        }
        showPasteDialogVisible.value = false
      }
    } else {
      ElMessage.error(res.message || '信息提取失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('信息提取失败: ' + (e.message || '未知错误'))
  } finally {
    extracting.value = false
  }
}

const loadTemplates = async () => {
  try {
    const res = await api.document.getTemplates()
    templates.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style lang="scss" scoped>
.document-page {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  margin-bottom: 24px;

  .header-content {
    h2 {
      margin: 0 0 8px 0;
      font-size: 26px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea, #764ba2);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    p {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: 14px;
    }
  }
}

.template-card {
  border: 1px solid rgba(102, 126, 234, 0.15);
  border-radius: 20px;
  background: rgba(19, 17, 28, 0.7);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);

  :deep(.el-card__body) {
    padding: 20px;
  }

  .search-box {
    margin-bottom: 20px;

    :deep(.el-input__wrapper) {
      border-radius: 12px;
      padding: 8px 16px;
    }
  }
}

.category-tabs {
  :deep(.el-tabs__header) {
    position: relative;
  }

  :deep(.el-tabs__nav-wrap::after) {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 2px;
    background: var(--color-border);
    z-index: 0;
  }

  :deep(.el-tabs__item) {
    font-weight: 500;
    position: relative;
    transition: color var(--transition-base);

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%) scaleX(0);
      width: 100%;
      height: 2px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      transition: transform var(--transition-smooth);
      z-index: 1;
    }

    &.is-active {
      color: #667eea;

      &::after {
        transform: translateX(-50%) scaleX(1);
      }
    }

    &:hover {
      color: #667eea;

      &::after {
        transform: translateX(-50%) scaleX(0.5);
      }
    }
  }

  :deep(.el-tabs__active-bar) {
    display: none;
  }
}

.template-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.template-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(102, 126, 234, 0.15);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #667eea;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
    transform: translateX(4px);
  }

  &.active {
    transform: scale(1.02);
    border: 2px solid var(--color-primary);
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.2);
  }

  .template-icon {
    width: 44px;
    height: 44px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 20px;
  }

  .template-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;

    .template-name {
      font-size: 14px;
      font-weight: 500;
      color: var(--color-text-primary);
    }
  }
}

.form-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);

  :deep(.el-card__body) {
    padding: 28px;
  }

  .selected-template-info {
    margin-bottom: 24px;

    :deep(.el-alert) {
      border-radius: 12px;
    }
  }

  :deep(.el-form-item__label) {
    font-weight: 500;
    color: var(--color-text-secondary);
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 10px;
    padding: 12px 16px;
  }

  :deep(.el-button--primary) {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    border-radius: 10px;
    padding: 12px 28px;
    transition: all 0.3s;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }

    &:disabled {
      background: rgba(102, 126, 234, 0.1);
    }
  }

  :deep(.el-button:not(.el-button--primary)) {
    border-radius: 10px;
    padding: 12px 20px;
  }
}

.result-content {
  padding: 8px;

  .source-banner {
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 6px;

    .el-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .document-content {
    background: linear-gradient(135deg, #f8fafc, #f1f5f9);
    padding: 24px;
    border-radius: 16px;
    white-space: pre-wrap;
    font-family: 'Songti SC', 'SimSun', serif;
    line-height: 2;
    font-size: 14px;
    max-height: 500px;
    overflow-y: auto;
    border: 1px solid rgba(102, 126, 234, 0.12);
  }

  .risk-content {
    padding: 20px;
    background: linear-gradient(135deg, #fefce8, #fef9c3);
    border-radius: 14px;
    line-height: 1.8;
    border: 1px solid #fef08a;
  }

  .laws-list {
    .law-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px;
      border-bottom: 1px solid #f3f4f6;
      color: var(--color-text-secondary);
      font-size: 14px;

      .el-icon {
        color: #667eea;
        font-size: 18px;
      }
    }
  }

  .actions {
    margin-top: 24px;
    display: flex;
    gap: 14px;
    padding-top: 20px;
    border-top: 1px solid #f3f4f6;

    :deep(.el-button) {
      border-radius: 10px;
      padding: 10px 20px;
    }
  }
}

.paste-dialog-content {
  .paste-textarea {
    margin-top: 16px;
  }

  .paste-tips {
    margin-top: 12px;
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}
</style>
