class OperationRecorder {
  constructor() {
    this.isRecording = false
    this.operations = []
    this.startTime = null
    this.maxOperations = 1000
  }

  start() {
    if (this.isRecording) return
    
    this.isRecording = true
    this.operations = []
    this.startTime = Date.now()
    
    this.setupListeners()
    console.log('Operation recording started')
  }

  stop() {
    if (!this.isRecording) return
    
    this.isRecording = false
    this.removeListeners()
    console.log('Operation recording stopped', this.operations.length, 'operations captured')
    
    return this.getRecordingData()
  }

  setupListeners() {
    this.clickHandler = (e) => {
      const target = e.target
      if (this.shouldIgnore(target)) return
      
      this.record({
        type: 'click',
        timestamp: Date.now(),
        data: {
          tagName: target.tagName,
          className: target.className,
          id: target.id,
          name: target.name,
          text: target.textContent?.substring(0, 50),
          value: target.value,
          href: target.href,
          action: this.getAction(target)
        },
        selector: this.getSelector(target)
      })
    }
    
    this.inputHandler = (e) => {
      const target = e.target
      if (this.shouldIgnore(target)) return
      
      this.record({
        type: 'input',
        timestamp: Date.now(),
        data: {
          tagName: target.tagName,
          name: target.name,
          id: target.id,
          value: target.value,
          type: target.type
        },
        selector: this.getSelector(target)
      })
    }
    
    this.submitHandler = (e) => {
      const form = e.target
      const formData = new FormData(form)
      
      this.record({
        type: 'submit',
        timestamp: Date.now(),
        data: {
          action: form.action,
          method: form.method,
          formData: Object.fromEntries(formData)
        },
        selector: this.getSelector(form)
      })
    }
    
    this.navigationHandler = (e) => {
      this.record({
        type: 'navigation',
        timestamp: Date.now(),
        data: {
          from: window.location.href,
          to: e.detail?.to || window.location.href
        }
      })
    }

    document.addEventListener('click', this.clickHandler, true)
    document.addEventListener('input', this.inputHandler, true)
    document.addEventListener('submit', this.submitHandler, true)
    window.addEventListener('popstate', this.navigationHandler)
  }

  removeListeners() {
    document.removeEventListener('click', this.clickHandler, true)
    document.removeEventListener('input', this.inputHandler, true)
    document.removeEventListener('submit', this.submitHandler, true)
    window.removeEventListener('popstate', this.navigationHandler)
  }

  shouldIgnore(target) {
    const ignoredTags = ['SCRIPT', 'STYLE', 'META', 'LINK', 'NOSCRIPT']
    if (ignoredTags.includes(target.tagName)) return true
    
    const ignoredClasses = ['el-overlay', 'context-menu', 'notification-toast']
    if (ignoredClasses.some(cls => target.classList?.contains(cls))) return true
    
    return false
  }

  getSelector(element) {
    if (element.id) return `#${element.id}`
    
    let selector = element.tagName.toLowerCase()
    
    if (element.className) {
      const classes = element.className.split(' ').filter(c => c && !c.includes(' ')).slice(0, 2)
      if (classes.length) {
        selector += '.' + classes.join('.')
      }
    }
    
    return selector
  }

  getAction(element) {
    if (element.tagName === 'BUTTON') return element.textContent?.trim()
    if (element.tagName === 'A') return `navigate to ${element.href}`
    if (element.type === 'submit') return 'form submit'
    if (element.type === 'button') return element.textContent?.trim()
    return 'click'
  }

  record(operation) {
    if (!this.isRecording) return
    
    operation.id = this.operations.length + 1
    operation.elapsed = Date.now() - this.startTime
    
    this.operations.push(operation)
    
    if (this.operations.length > this.maxOperations) {
      this.operations.shift()
    }
  }

  getRecordingData() {
    return {
      startTime: this.startTime,
      endTime: Date.now(),
      duration: Date.now() - this.startTime,
      url: window.location.href,
      userAgent: navigator.userAgent,
      operations: [...this.operations]
    }
  }

  getOperations() {
    return [...this.operations]
  }

  clear() {
    this.operations = []
    this.startTime = Date.now()
  }
}

export const operationRecorder = new OperationRecorder()
export default operationRecorder
