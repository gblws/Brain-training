<template>
  <view class="badge" :class="`badge-${tone}`">
    <view class="badge-ring">
      <view class="badge-core">
        <view class="badge-crown"></view>
        <view class="badge-star badge-star-outer"></view>
        <view class="badge-star badge-star-inner"></view>
        <text class="badge-mark">{{ iconMark }}</text>
      </view>
    </view>
    <view class="badge-ribbon left"></view>
    <view class="badge-ribbon right"></view>
  </view>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  rank: {
    type: String,
    default: 'bronze'
  }
});

const tone = computed(() => {
  const raw = String(props.rank || '').toLowerCase();
  if (['empty', 'bronze', 'silver', 'gold', 'prismatic'].includes(raw)) {
    return raw;
  }
  return 'bronze';
});

const iconMark = computed(() => {
  const map = {
    empty: '',
    bronze: '✦',
    silver: '✦',
    gold: '✦',
    prismatic: '✧'
  };
  return map[tone.value] || 'B';
});
</script>

<style scoped>
.badge {
  width: 96rpx;
  height: 96rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-ring {
  width: 78rpx;
  height: 78rpx;
  border-radius: 50%;
  padding: 6rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  box-shadow: 0 10rpx 22rpx rgba(15, 23, 42, 0.16);
}

.badge-core {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.badge-core::before,
.badge-core::after {
  content: '';
  position: absolute;
  inset: 8rpx;
  border-radius: 50%;
  opacity: 0.28;
}

.badge-core::after {
  inset: 18rpx;
  opacity: 0.22;
}

.badge-crown {
  position: absolute;
  inset: 8rpx;
  border-radius: 50%;
  clip-path: polygon(
    50% 0%,
    64% 10%,
    82% 8%,
    90% 22%,
    100% 50%,
    90% 78%,
    82% 92%,
    64% 90%,
    50% 100%,
    36% 90%,
    18% 92%,
    10% 78%,
    0% 50%,
    10% 22%,
    18% 8%,
    36% 10%
  );
  opacity: 0.9;
}

.badge-star {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  clip-path: polygon(
    50% 0%,
    60% 26%,
    88% 18%,
    74% 42%,
    100% 50%,
    74% 58%,
    88% 82%,
    60% 74%,
    50% 100%,
    40% 74%,
    12% 82%,
    26% 58%,
    0% 50%,
    26% 42%,
    12% 18%,
    40% 26%
  );
}

.badge-star-outer {
  width: 42rpx;
  height: 42rpx;
  opacity: 0.92;
}

.badge-star-inner {
  width: 24rpx;
  height: 24rpx;
  opacity: 0.96;
}

.badge-mark {
  position: relative;
  z-index: 1;
  font-size: 18rpx;
  font-weight: 800;
  transform: translateY(1rpx);
}

.badge-ribbon {
  position: absolute;
  bottom: 8rpx;
  width: 24rpx;
  height: 28rpx;
  clip-path: polygon(0 0, 100% 0, 80% 100%, 50% 78%, 20% 100%);
  z-index: 1;
}

.badge-ribbon.left {
  left: 18rpx;
  transform: rotate(-10deg);
}

.badge-ribbon.right {
  right: 18rpx;
  transform: rotate(10deg);
}

.badge-empty .badge-ring {
  background: linear-gradient(145deg, #5a616c 0%, #1f2937 100%);
  box-shadow: inset 0 8rpx 18rpx rgba(0, 0, 0, 0.42), 0 10rpx 22rpx rgba(15, 23, 42, 0.2);
}

.badge-empty .badge-core {
  background: radial-gradient(circle at 42% 30%, #69717d 0%, #202833 42%, #0f1720 100%);
}

.badge-empty .badge-core::before,
.badge-empty .badge-core::after {
  border: 2rpx solid rgba(255, 255, 255, 0.1);
}

.badge-empty .badge-crown {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.08) 0%, rgba(0, 0, 0, 0.26) 100%);
}

.badge-empty .badge-star-outer {
  background: linear-gradient(145deg, #707987 0%, #303945 52%, #171d26 100%);
  opacity: 0.5;
}

.badge-empty .badge-star-inner {
  background: linear-gradient(145deg, #848d99 0%, #434d59 56%, #232a33 100%);
  opacity: 0.46;
}

.badge-empty .badge-mark {
  color: transparent;
}

.badge-empty .badge-ribbon {
  background: linear-gradient(180deg, #2c3440 0%, #171d25 100%);
  opacity: 0.9;
}

.badge-bronze .badge-ring {
  background:
    radial-gradient(circle at 50% 20%, rgba(255, 255, 255, 0.76), transparent 28%),
    linear-gradient(145deg, #efc79d 0%, #d58a4a 44%, #975122 100%);
}

.badge-bronze .badge-core {
  background:
    radial-gradient(circle at 50% 50%, #fff3e3 0%, #ebb47a 44%, #bf6d32 100%);
}

.badge-bronze .badge-core::before,
.badge-bronze .badge-core::after {
  border: 2rpx solid rgba(255, 241, 225, 0.88);
}

.badge-bronze .badge-crown {
  background: linear-gradient(145deg, rgba(255, 243, 222, 0.9) 0%, rgba(191, 110, 49, 0.72) 100%);
}

.badge-bronze .badge-star-outer {
  background: linear-gradient(145deg, #fff0df 0%, #e2a76f 48%, #ab5e2d 100%);
}

.badge-bronze .badge-star-inner {
  background: linear-gradient(145deg, #fffaf1 0%, #f0c595 54%, #cc7b3d 100%);
}

.badge-bronze .badge-mark {
  color: #fff8ef;
  text-shadow: 0 1rpx 0 rgba(114, 57, 19, 0.36);
}

.badge-bronze .badge-ribbon {
  background: linear-gradient(180deg, #d58947 0%, #9d5728 100%);
}

.badge-silver .badge-ring {
  background:
    radial-gradient(circle at 50% 20%, rgba(255, 255, 255, 0.84), transparent 28%),
    linear-gradient(145deg, #f7fbff 0%, #c8d6e6 44%, #8798ac 100%);
}

.badge-silver .badge-core {
  background:
    radial-gradient(circle at 50% 50%, #ffffff 0%, #edf3fa 44%, #b7c5d5 100%);
}

.badge-silver .badge-core::before,
.badge-silver .badge-core::after {
  border: 2rpx solid rgba(255, 255, 255, 0.92);
}

.badge-silver .badge-crown {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.92) 0%, rgba(168, 183, 199, 0.76) 100%);
}

.badge-silver .badge-star-outer {
  background: linear-gradient(145deg, #ffffff 0%, #dde7f0 46%, #9caec1 100%);
}

.badge-silver .badge-star-inner {
  background: linear-gradient(145deg, #ffffff 0%, #eef4fa 52%, #b7c5d6 100%);
}

.badge-silver .badge-mark {
  color: #607185;
  text-shadow: 0 1rpx 0 rgba(255, 255, 255, 0.5);
}

.badge-silver .badge-ribbon {
  background: linear-gradient(180deg, #c3d1de 0%, #8d9db2 100%);
}

.badge-gold .badge-ring {
  background:
    radial-gradient(circle at 50% 20%, rgba(255, 255, 255, 0.8), transparent 28%),
    linear-gradient(145deg, #fff4bf 0%, #f2b84f 44%, #ad6c14 100%);
}

.badge-gold .badge-core {
  background:
    radial-gradient(circle at 50% 50%, #fff9db 0%, #ffd76b 46%, #d98b28 100%);
}

.badge-gold .badge-core::before,
.badge-gold .badge-core::after {
  border: 2rpx solid rgba(255, 248, 196, 0.92);
}

.badge-gold .badge-crown {
  background: linear-gradient(145deg, rgba(255, 248, 198, 0.96) 0%, rgba(238, 188, 88, 0.78) 100%);
}

.badge-gold .badge-star-outer {
  background: linear-gradient(145deg, #fffcea 0%, #ffd86a 44%, #d38b2a 100%);
}

.badge-gold .badge-star-inner {
  background: linear-gradient(145deg, #fffef3 0%, #ffe8a2 50%, #ebb750 100%);
}

.badge-gold .badge-mark {
  color: #8a4f00;
  text-shadow: 0 1rpx 0 rgba(255, 255, 255, 0.5);
}

.badge-gold .badge-ribbon {
  background: linear-gradient(180deg, #f2b85a 0%, #cd7d18 100%);
}

.badge-prismatic .badge-ring {
  background:
    radial-gradient(circle at 50% 18%, rgba(255, 255, 255, 0.92), transparent 22%),
    linear-gradient(145deg, #ffe6ff 0%, #f4b2df 24%, #d7b5ff 50%, #a8f0ff 76%, #ffcbdd 100%);
}

.badge-prismatic .badge-core {
  background:
    radial-gradient(circle at 35% 28%, #ffffff 0%, #ffe5f8 30%, #ddb9ff 58%, #9ce7ff 100%);
}

.badge-prismatic .badge-core::before,
.badge-prismatic .badge-core::after {
  border: 2rpx solid rgba(255, 255, 255, 0.94);
}

.badge-prismatic .badge-crown {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.94) 0%, rgba(239, 193, 255, 0.7) 34%, rgba(157, 228, 255, 0.78) 100%);
}

.badge-prismatic .badge-star-outer {
  background: linear-gradient(145deg, #ffffff 0%, #ffd9f3 30%, #d9b4ff 62%, #97e6ff 100%);
}

.badge-prismatic .badge-star-inner {
  background: linear-gradient(145deg, #ffffff 0%, #ffeef9 34%, #edd2ff 60%, #b8f0ff 100%);
}

.badge-prismatic .badge-mark {
  color: #9b4fe0;
  text-shadow: 0 1rpx 0 rgba(255, 255, 255, 0.75);
}

.badge-prismatic .badge-ribbon {
  background: linear-gradient(180deg, #f2c4ff 0%, #b879ff 54%, #73dfff 100%);
}
</style>
