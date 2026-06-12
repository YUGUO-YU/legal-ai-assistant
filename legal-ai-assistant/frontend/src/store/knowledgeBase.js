import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useKnowledgeBaseStore = defineStore('knowledgeBase', () => {
  const currentKb = ref(null)
  const kbList = ref([])
  const documents = ref([])

  const setCurrentKb = (kb) => {
    currentKb.value = kb
    localStorage.setItem('currentKb', JSON.stringify(kb))
  }

  const loadCurrentKb = () => {
    const saved = localStorage.getItem('currentKb')
    if (saved) {
      currentKb.value = JSON.parse(saved)
    }
  }

  const setKbList = (list) => {
    kbList.value = list
  }

  const addDocument = (doc) => {
    documents.value.push({
      ...doc,
      id: Date.now()
    })
  }

  const removeDocument = (docId) => {
    const index = documents.value.findIndex(d => d.id === docId)
    if (index > -1) {
      documents.value.splice(index, 1)
    }
  }

  return {
    currentKb,
    kbList,
    documents,
    setCurrentKb,
    loadCurrentKb,
    setKbList,
    addDocument,
    removeDocument
  }
})