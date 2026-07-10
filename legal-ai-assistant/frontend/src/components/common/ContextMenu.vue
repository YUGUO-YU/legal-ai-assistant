<template>
  <Teleport to="body">
    <Transition name="context-menu">
      <div
        v-if="visible"
        ref="menuRef"
        class="context-menu"
        :style="menuStyle"
        @contextmenu.prevent
      >
        <div
          v-for="(item, index) in menus"
          :key="index"
          class="context-menu-item"
          :class="{
            'is-disabled': item.disabled,
            'is-divided': item.divided
          }"
          @click="handleClick(item)"
          @mouseenter="handleMouseEnter(item)"
        >
          <i v-if="item.icon" :class="item.icon" class="menu-icon"></i>
          <span class="menu-label">{{ item.label }}</span>
          <span v-if="item.shortcut" class="menu-shortcut">{{ item.shortcut }}</span>
          <i v-if="item.children && item.children.length" class="el-icon-arrow-right sub-arrow"></i>

          <div
            v-if="item.children && item.children.length"
            class="context-menu context-sub"
            :style="getSubMenuStyle(item)"
          >
            <div
              v-for="(child, childIndex) in item.children"
              :key="childIndex"
              class="context-menu-item"
              :class="{ 'is-disabled': child.disabled }"
              @click.stop="handleClick(child)"
            >
              <i v-if="child.icon" :class="child.icon" class="menu-icon"></i>
              <span class="menu-label">{{ child.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  menus: {
    type: Array,
    default: () => []
  },
  visible: {
    type: Boolean,
    default: false
  },
  x: {
    type: Number,
    default: 0
  },
  y: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:visible', 'select'])

const menuRef = ref(null)
const subMenuIndex = ref(-1)

const menuStyle = computed(() => {
  const style = {
    left: props.x + 'px',
    top: props.y + 'px'
  }

  if (menuRef.value) {
    const rect = menuRef.value.getBoundingClientRect()
    if (props.x + rect.width > window.innerWidth) {
      style.left = (props.x - rect.width) + 'px'
    }
    if (props.y + rect.height > window.innerHeight) {
      style.top = (props.y - rect.height) + 'px'
    }
  }

  return style
})

const getSubMenuStyle = () => {
  return {
    left: '100%',
    top: '0'
  }
}

const handleClick = (item) => {
  if (item.disabled) return

  emit('select', item)
  emit('update:visible', false)
}

const handleMouseEnter = (item) => {
  if (item.children && item.children.length) {
    subMenuIndex.value = props.menus.indexOf(item)
  } else {
    subMenuIndex.value = -1
  }
}

const close = () => {
  emit('update:visible', false)
}

const handleClickOutside = (e) => {
  if (menuRef.value && !menuRef.value.contains(e.target)) {
    close()
  }
}

const handleEsc = (e) => {
  if (e.key === 'Escape') {
    close()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEsc)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEsc)
})
</script>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 180px;
  padding: 6px 0;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.context-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.context-menu-item:hover {
  background: var(--color-bg-soft);
}

.context-menu-item.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.context-menu-item.is-disabled:hover {
  background: transparent;
}

.context-menu-item.is-divided {
  margin-top: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-light);
}

.menu-icon {
  margin-right: 10px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.menu-label {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-primary);
}

.menu-shortcut {
  margin-left: 20px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.sub-arrow {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.context-sub {
  position: absolute;
  padding: 6px 0;
  min-width: 160px;
}

.context-menu-enter-active,
.context-menu-leave-active {
  transition: all 0.15s ease;
}

.context-menu-enter-from,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
