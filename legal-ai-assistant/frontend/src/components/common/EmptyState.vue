<template>
  <div class="empty-state" :class="[`empty-${size}`]">
    <div class="empty-illustration">
      <svg 
        v-if="illustration === 'search'"
        viewBox="0 0 200 160" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        class="illustration-svg"
      >
        <circle cx="80" cy="70" r="35" stroke="currentColor" stroke-width="3" fill="none" opacity="0.3"/>
        <circle cx="80" cy="70" r="25" stroke="currentColor" stroke-width="2" fill="none" opacity="0.5"/>
        <line x1="105" y1="95" x2="130" y2="120" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.4"/>
        <text x="80" y="78" text-anchor="middle" fill="currentColor" font-size="24" font-weight="bold" opacity="0.6">?</text>
      </svg>
      
      <svg 
        v-else-if="illustration === 'data'"
        viewBox="0 0 200 160" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        class="illustration-svg"
      >
        <path d="M30 60 L30 130 L170 130 L170 60 L100 60 L85 45 L30 45 Z" stroke="currentColor" stroke-width="3" fill="none" opacity="0.3"/>
        <path d="M85 45 L85 60 L100 60 L100 45 Z" stroke="currentColor" stroke-width="2" fill="none" opacity="0.5"/>
        <rect x="60" y="80" width="80" height="10" rx="2" fill="currentColor" opacity="0.2"/>
        <rect x="60" y="95" width="60" height="10" rx="2" fill="currentColor" opacity="0.15"/>
        <rect x="60" y="110" width="70" height="10" rx="2" fill="currentColor" opacity="0.1"/>
      </svg>
      
      <svg 
        v-else-if="illustration === 'chart'"
        viewBox="0 0 200 160" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        class="illustration-svg"
      >
        <rect x="30" y="30" width="140" height="100" rx="8" stroke="currentColor" stroke-width="3" fill="none" opacity="0.2"/>
        <line x1="30" y1="100" x2="170" y2="100" stroke="currentColor" stroke-width="2" opacity="0.3"/>
        <line x1="100" y1="30" x2="100" y2="130" stroke="currentColor" stroke-width="2" opacity="0.3"/>
        <circle cx="50" cy="85" r="5" fill="currentColor" opacity="0.2"/>
        <circle cx="80" cy="70" r="5" fill="currentColor" opacity="0.15"/>
        <circle cx="110" cy="90" r="5" fill="currentColor" opacity="0.1"/>
        <circle cx="140" cy="60" r="5" fill="currentColor" opacity="0.05"/>
      </svg>
      
      <svg 
        v-else-if="illustration === 'inbox'"
        viewBox="0 0 200 160" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        class="illustration-svg"
      >
        <path d="M30 50 L30 110 L170 110 L170 50 Z" stroke="currentColor" stroke-width="3" fill="none" opacity="0.2"/>
        <path d="M30 50 L100 90 L170 50" stroke="currentColor" stroke-width="3" fill="none" opacity="0.3"/>
        <rect x="60" y="70" width="80" height="60" rx="4" stroke="currentColor" stroke-width="2" fill="none" opacity="0.2"/>
        <line x1="75" y1="85" x2="125" y2="85" stroke="currentColor" stroke-width="2" opacity="0.15"/>
        <line x1="75" y1="100" x2="115" y2="100" stroke="currentColor" stroke-width="2" opacity="0.1"/>
      </svg>
      
      <svg 
        v-else
        viewBox="0 0 200 160" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        class="illustration-svg"
      >
        <ellipse cx="100" cy="80" rx="50" ry="35" stroke="currentColor" stroke-width="3" fill="none" opacity="0.2"/>
        <circle cx="65" cy="85" r="20" stroke="currentColor" stroke-width="2" fill="none" opacity="0.3"/>
        <circle cx="120" cy="90" r="18" stroke="currentColor" stroke-width="2" fill="none" opacity="0.25"/>
        <path d="M100 115 L100 145 M85 130 L100 145 L115 130" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.4"/>
      </svg>
    </div>
    
    <div class="empty-content">
      <h3 class="empty-title">{{ title }}</h3>
      <p class="empty-description">{{ description }}</p>
      
      <div class="empty-actions" v-if="$slots.action">
        <slot name="action"></slot>
      </div>
      
      <div class="empty-actions" v-else-if="actionText">
        <el-button type="primary" @click="handleAction">
          {{ actionText }}
        </el-button>
        <el-button v-if="secondaryActionText" @click="handleSecondaryAction">
          {{ secondaryActionText }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前没有可显示的内容'
  },
  illustration: {
    type: String,
    default: 'default'
  },
  size: {
    type: String,
    default: 'medium'
  },
  actionText: {
    type: String,
    default: ''
  },
  secondaryActionText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['action', 'secondary-action'])

const handleAction = () => {
  emit('action')
}

const handleSecondaryAction = () => {
  emit('secondary-action')
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
  
  &.empty-small {
    padding: 24px 16px;
    
    .empty-illustration {
      width: 120px;
      height: 96px;
    }
    
    .empty-title {
      font-size: 14px;
    }
    
    .empty-description {
      font-size: 12px;
    }
  }
  
  &.empty-medium {
    .empty-illustration {
      width: 160px;
      height: 128px;
    }
  }
  
  &.empty-large {
    padding: 64px 40px;
    
    .empty-illustration {
      width: 200px;
      height: 160px;
    }
    
    .empty-title {
      font-size: 20px;
    }
  }
}

.empty-illustration {
  margin-bottom: 24px;
  color: var(--color-primary);
}

.illustration-svg {
  width: 100%;
  height: 100%;
}

.empty-content {
  max-width: 400px;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.empty-description {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
