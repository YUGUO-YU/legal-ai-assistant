# Task 2.1 Report: DocQa 错误处理 UI

## Status: DONE

## Changes Made

### 1. Added `errorMsg` ref (DocQa.vue:270)
```javascript
const errorMsg = ref('')
```
Added after `kbList` ref in `<script setup>`.

### 2. Updated catch block (DocQa.vue:412)
```javascript
} catch (e) {
  aiMsg.content = '回答生成失败，请稍后重试'
  ElMessage.error('回答生成失败，请稍后重试')
  errorMsg.value = '网络错误，请检查网络连接或稍后重试'
}
```
Added `errorMsg.value = '网络错误，请检查网络连接或稍后重试'` in the catch block.

### 3. Added inline error alert in template (DocQa.vue:29-33)
```html
<el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="true" @close="errorMsg = ''" style="margin-bottom: 16px;">
  <template #default>
    <el-button size="small" type="primary" @click="errorMsg = ''; handleAsk()">重试</el-button>
  </template>
</el-alert>
```
Placed after chat-header, before chat-messages div.

Note: Used `handleAsk()` instead of `fetchAnswer()` in the retry button because `fetchAnswer` function does not exist in the codebase - the actual function is `handleAsk`.

### 4. Clear errorMsg when sending new question (DocQa.vue:323)
```javascript
const handleAsk = async () => {
  errorMsg.value = ''
  if (!question.value.trim() || loading.value) return
```
Added `errorMsg.value = ''` at the start of `handleAsk`.

## Verification
- Build: PASSED (`npm run build` - 27.10s)
- No syntax errors
- Existing error handling (ElMessage) preserved

## Commit
```
4a3f1b7 feat(DocQa): add inline error alert with retry button
```
