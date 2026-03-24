<template>
  <view class="result-mask" v-if="visible">
    <view class="result-card">
      <text class="result-title" :class="titleClass">{{ title }}</text>
      <text class="result-line" v-for="(line, idx) in lines" :key="idx">{{ line }}</text>

      <button class="pill-action success-action" v-if="showNext" @click="$emit('next')">
        <text class="pill-icon up">↑</text>
        <text class="pill-label-text">进入下一个难度</text>
      </button>
      <button class="pill-action" v-if="showLower" @click="$emit('lower')">
        <text class="pill-icon down">↓</text>
        <text class="pill-label-text">降低难度</text>
      </button>
      <button class="pill-action" v-if="showRetry" @click="$emit('retry')">
        <text class="pill-icon retry"></text>
        <text class="pill-label-text">再来一次</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  type: { type: String, default: 'mid' }, // fail | mid | success
  lines: { type: Array, default: () => [] },
  showLower: { type: Boolean, default: false },
  showRetry: { type: Boolean, default: false },
  showNext: { type: Boolean, default: false }
});

defineEmits(['lower', 'retry', 'next']);

const titleClass = computed(() => {
  if (props.type === 'fail') return 'fail';
  if (props.type === 'success') return 'success';
  return 'mid';
});
</script>

<style scoped>
.result-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.result-card {
  width: 80%;
  background: #ffffff;
  border-radius: 16rpx;
  padding: 26rpx 22rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  align-items: center;
}

.result-title {
  font-size: 42rpx;
  font-weight: 800;
}

.result-title.fail {
  color: #dc2626;
}

.result-title.mid {
  color: #f59e0b;
}

.result-title.success {
  color: #16a34a;
}

.result-line {
  font-size: 26rpx;
  color: #374151;
}

.pill-action {
  margin: 8rpx auto 0;
  width: 68%;
  height: 58rpx;
  border-radius: 999rpx;
  background: #f6f1e7;
  border: none;
  color: #111827;
  padding: 0 10rpx;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
}

.pill-icon {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #2f354d;
  border: 2rpx solid rgba(255, 255, 255, 0.18);
  text-align: center;
  line-height: 32rpx;
  font-size: 20rpx;
  font-weight: 700;
  color: #f87171;
  flex-shrink: 0;
  z-index: 1;
}

.pill-icon.retry {
  position: relative;
  font-size: 0;
  color: transparent;
}

.pill-icon.up {
  color: #22c55e;
}

.pill-icon.retry::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 20rpx;
  height: 20rpx;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 3rpx solid #f59e0b;
}

.pill-icon.retry::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 6rpx;
  height: 6rpx;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: #f59e0b;
}

.pill-label-text {
  position: absolute;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 26rpx;
  font-weight: 700;
  color: #111827;
  line-height: 58rpx;
}

.success-action .pill-label-text {
  color: #14532d;
}
</style>
