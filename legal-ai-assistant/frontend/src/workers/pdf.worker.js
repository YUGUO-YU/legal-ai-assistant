import * as pdfjsLib from 'pdfjs-dist'

pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.mjs',
  import.meta.url
).href

self.onmessage = async ({ data }) => {
  const { type, buffer, pageNum } = data

  if (type === 'extractText') {
    try {
      const loadingTask = pdfjsLib.getDocument({ data: buffer })
      const pdf = await loadingTask.promise
      const totalPages = pdf.numPages
      const texts = []

      const startPage = pageNum || 1
      const endPage = pageNum || totalPages

      for (let i = startPage; i <= Math.min(endPage, totalPages); i++) {
        const page = await pdf.getPage(i)
        const content = await page.getTextContent()
        texts.push(content.items.map(item => item.str).join(' '))
      }

      self.postMessage({ success: true, texts, totalPages })
    } catch (err) {
      self.postMessage({ success: false, error: err.message })
    }
  }

  if (type === 'getMeta') {
    try {
      const loadingTask = pdfjsLib.getDocument({ data: buffer })
      const pdf = await loadingTask.promise
      const meta = await pdf.getMetadata()
      self.postMessage({ success: true, meta, numPages: pdf.numPages })
    } catch (err) {
      self.postMessage({ success: false, error: err.message })
    }
  }
}
