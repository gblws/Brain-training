<template>
  <view class="page">
    <view class="header">
      <text class="title">Brain Training</text>
      <text class="sub">Choose a game and start now</text>
    </view>

    <view class="cards">
      <view class="card" @click="go('/pages/games/schulte')">
        <text class="card-title">Schulte Grid</text>
        <text class="card-desc">Tap numbers from 1 to 25 in order</text>
      </view>

      <view class="card" @click="go('/pages/games/stroop')">
        <text class="card-title">Stroop Challenge</text>
        <text class="card-desc">Judge the font color, not the text</text>
      </view>

      <view class="card" @click="go('/pages/games/memory')">
        <text class="card-title">Memory Matrix</text>
        <text class="card-desc">Remember flashing cells and reproduce</text>
      </view>
    </view>

    <button class="profile-btn" @click="goPlayer">Go To Player</button>
  </view>
</template>

<script setup>
const go = (url) => {
  uni.navigateTo({ url });
};

const goPlayer = () => {
  uni.switchTab({ url: '/pages/user/player' });
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 32rpx;
  gap: 28rpx;
}

.header {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
}

.sub {
  font-size: 26rpx;
  color: #6b7280;
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.06);
}

.card-title {
  font-size: 34rpx;
  font-weight: 600;
}

.card-desc {
  font-size: 24rpx;
  color: #4b5563;
}

.profile-btn {
  margin-top: 10rpx;
  background: #2563eb;
  color: #ffffff;
  border-radius: 14rpx;
}
</style>
