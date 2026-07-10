<template>
  <component 
    :is="iconComponent" 
    :style="{ width: size, height: size }"
    :class="iconClass"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  size: {
    type: String,
    default: '1em'
  },
  color: {
    type: String,
    default: 'currentColor'
  },
  type: {
    type: String,
    default: 'element'
  }
})

const iconClass = computed(() => `icon icon-${props.name}`)

const elementIcons = {
  'user': 'el-icon-user',
  'user-solid': 'el-icon-user-solid',
  'user-filled': 'el-icon-user-filled',
  'edit': 'el-icon-edit',
  'edit-outline': 'el-icon-edit-outline',
  'delete': 'el-icon-delete',
  'delete-solid': 'el-icon-delete-solid',
  'search': 'el-icon-search',
  'plus': 'el-icon-plus',
  'plus-round': 'el-icon-plus',
  'minus': 'el-icon-minus',
  'check': 'el-icon-check',
  'close': 'el-icon-close',
  'arrow-left': 'el-icon-arrow-left',
  'arrow-right': 'el-icon-arrow-right',
  'arrow-up': 'el-icon-arrow-up',
  'arrow-down': 'el-icon-arrow-down',
  'chevron-left': 'el-icon-arrow-left',
  'chevron-right': 'el-icon-arrow-right',
  'chevron-up': 'el-icon-arrow-up',
  'chevron-down': 'el-icon-arrow-down',
  'loading': 'el-icon-loading',
  'setting': 'el-icon-setting',
  'menu': 'el-icon-menu',
  'more': 'el-icon-more',
  'more-outline': 'el-icon-more-outline',
  'view': 'el-icon-view',
  'view-outline': 'el-icon-view',
  'data-board': 'el-icon-data-board',
  'chart': 'el-icon-data-analysis',
  'upload': 'el-icon-upload',
  'download': 'el-icon-download',
  'share': 'el-icon-share',
  'document': 'el-icon-document',
  'document-copy': 'el-icon-document-copy',
  'folder': 'el-icon-folder',
  'folder-opened': 'el-icon-folder-opened',
  'files': 'el-icon-files',
  'news': 'el-icon-news',
  'message': 'el-icon-message',
  'message-solid': 'el-icon-message-solid',
  'bell': 'el-icon-bell',
  'bell-solid': 'el-icon-bell',
  'warning': 'el-icon-warning',
  'warning-outline': 'el-icon-warning-outline',
  'info': 'el-icon-info',
  'question': 'el-icon-question',
  'success': 'el-icon-success',
  'error': 'el-icon-error',
  'time': 'el-icon-time',
  'timer': 'el-icon-timer',
  'clock': 'el-icon-time',
  'date': 'el-icon-date',
  'location': 'el-icon-location',
  'location-outline': 'el-icon-location-outline',
  'phone': 'el-icon-phone',
  'phone-outline': 'el-icon-phone-outline',
  'email': 'el-icon-message',
  'url': 'el-icon-link',
  'link': 'el-icon-link',
  'unlink': 'el-icon-unlink',
  'lock': 'el-icon-lock',
  'unlock': 'el-icon-unlock',
  'key': 'el-icon-key',
  'star': 'el-icon-star-off',
  'star-solid': 'el-icon-star-on',
  'star-on': 'el-icon-star-on',
  'star-off': 'el-icon-star-off',
  'heart': 'el-icon-heart',
  'heart-solid': 'el-icon-heart',
  'like': 'el-icon-like',
  'like-solid': 'el-icon-like',
  'dislike': 'el-icon-dish',
  'filter': 'el-icon-filter',
  'filter-outline': 'el-icon-filter',
  'sort': 'el-icon-sort',
  'rank': 'el-icon-rank',
  'refresh': 'el-icon-refresh',
  'refresh-right': 'el-icon-refresh-right',
  'refresh-left': 'el-icon-refresh-left',
  'upload-filled': 'el-icon-upload2',
  'download-filled': 'el-icon-download',
  'office-building': 'el-icon-office-building',
  'school': 'el-icon-school',
  'guide': 'el-icon-guide',
  'ipad': 'el-icon-tablet',
  'monitor': 'el-icon-monitor',
  'mobile': 'el-icon-mobile',
  'mobile-phone': 'el-icon-mobile-phone',
  'printer': 'el-icon-printer',
  'camera': 'el-icon-camera',
  'video-camera': 'el-icon-video-camera',
  'scissors': 'el-icon-scissors',
  'target': 'el-icon-aim',
  'magic-stick': 'el-icon-magic-stick',
  'collection': 'el-icon-collection',
  'box': 'el-icon-box',
  'shopping': 'el-icon-shopping-cart',
  'shopping-cart': 'el-icon-shopping-cart-full',
  'shopping-cart-full': 'el-icon-shopping-cart-full',
  'coin': 'el-icon-coins',
  'wallet': 'el-icon-wallet',
  'money': 'el-icon-money',
  'bank-card': 'el-icon-bank-card',
  'credit-card': 'el-icon-credit-card',
  'tickets': 'el-icon-tickets',
  'ticket': 'el-icon-ticket',
  'map-location': 'el-icon-map-location',
  'place': 'el-icon-place',
  'guide-2': 'el-icon-guide',
  'compass': 'el-icon-compass',
  'connection': 'el-icon-connection',
  'wifi': 'el-icon-wifi',
  'hot-water': 'el-icon-hot-water',
  'coffee': 'el-icon-coffee',
  'ice-cream': 'el-icon-ice-cream',
  'dessert': 'el-icon-dessert',
  'bowl': 'el-icon-bowl',
  'tableware': 'el-icon-tableware',
  'umbrella': 'el-icon-umbrella',
  'sunny': 'el-icon-sunny',
  'cloudy': 'el-icon-cloudy',
  'sunrise': 'el-icon-sunny',
  'moon': 'el-icon-moon',
  'moon-night': 'el-icon-moon',
  'cloud': 'el-icon-cloud',
  'lightning': 'el-icon-lightning',
  'rain': 'el-icon-heavy-rain',
  'snow': 'el-icon-snowy',
  'wind': 'el-icon-wind',
  'ship': 'el-icon-ship',
  'train': 'el-icon-train',
  'bus': 'el-icon-bus',
  'car': 'el-icon-car',
  'truck': 'el-icon-truck',
  'motor-vehicle': 'el-icon-motor-vehicle',
  'bicycle': 'el-icon-bicycle',
  'train-2': 'el-icon-train',
  'metro': 'el-icon-metro',
  'airplane': 'el-icon-airplane',
  'plane': 'el-icon-paperplane',
  'paper-plane': 'el-icon-paperplane',
  'promotion': 'el-icon-promotion',
  'marketing': 'el-icon-marketing',
  'magnifying-glass': 'el-icon-search',
  'office': 'el-icon-office-building',
  'home': 'el-icon-house',
  'house': 'el-icon-house',
  'house-fill': 'el-icon-house',
  'building': 'el-icon-office-building',
  'shop': 'el-icon-shop',
  'store': 'el-icon-shop',
}

const iconComponent = computed(() => {
  if (props.type === 'element' && elementIcons[props.name]) {
    return {
      template: `<i class="${elementIcons[props.name]}"></i>`
    }
  }
  
  return {
    template: `<span>${props.name}</span>`
  }
})
</script>

<style scoped>
.icon {
  display: inline-block;
  line-height: 1;
}
</style>
