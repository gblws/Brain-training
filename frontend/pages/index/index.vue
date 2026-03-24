<template>
  <view class="page">
    <view class="header">
      <view class="logo-wrap">
        <view class="logo-block b1" />
        <view class="logo-block b2" />
        <view class="logo-block b3" />
        <view class="logo-block b4" />
        <text class="title">脑力训练</text>
      </view>
    </view>

    <view class="baseline-entry" @click="goBaseline">
      <text class="baseline-title">认知基准测试</text>
      <text class="baseline-sub">首次建议先完成，系统将生成六维初始能力图谱</text>
    </view>

    <view class="game-grid">
      <view
        class="game-card"
        v-for="item in gameCards"
        :key="item.title"
        :class="{ disabled: !item.route }"
        @click="openGame(item)"
      >
        <image v-if="item.logo" class="logo" :src="item.logo" mode="aspectFill" />
        <view v-else class="logo placeholder-logo">
          <text class="placeholder-text">?</text>
        </view>
        <view class="title-slot">
          <text class="card-title">{{ item.title }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app';
import { get, getAuthToken } from '../../utils/request.js';

const BASELINE_COMPLETE_KEY = 'baseline_completed_v1';
const BASELINE_PROMPT_KEY = 'baseline_prompted_once_v1';

const gameCards = [
  {
    title: '舒尔特方格',
    route: '/pages/games/schulte',
    logo: '/images/60a44c5f72e9a404a60e7cbcc28ff2cb.png'
  },
  {
    title: '斯特鲁普挑战',
    route: '/pages/games/stroop',
    logo: '/images/e4300c9568032d7b76375387cc7a281f.png'
  },
  {
    title: '记忆矩阵',
    route: '/pages/games/memory',
    logo: '/images/51b09395ab22ab9e9bc49470de221533.png'
  },
  {
    title: '敬请期待',
    route: '',
    logo: ''
  }
];

const openGame = (item) => {
  if (!item.route) return;
  uni.navigateTo({ url: item.route });
};

const localDateString = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const isTodayCreateTime = (value) => {
  if (!value) return false;
  const text = String(value).trim();
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) {
    return text.slice(0, 10) === localDateString();
  }
  const time = new Date(text).getTime();
  if (Number.isNaN(time)) return false;
  const parsed = new Date(time);
  const y = parsed.getFullYear();
  const m = String(parsed.getMonth() + 1).padStart(2, '0');
  const d = String(parsed.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}` === localDateString();
};

const goBaseline = async () => {
  const token = getAuthToken();
  if (token) {
    try {
      const latest = await get('/api/v1/baseline/latest');
      if (latest && isTodayCreateTime(latest.createTime)) {
        uni.showModal({
          title: '今日已测试',
          content: '再次测试会覆盖今天的基准测试结果，是否继续？',
          confirmText: '继续测试',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/assessment/baseline' });
            }
          }
        });
        return;
      }
    } catch (_) {}
  }
  uni.navigateTo({ url: '/pages/assessment/baseline' });
};

onShow(() => {
  const completed = !!uni.getStorageSync(BASELINE_COMPLETE_KEY);
  const prompted = !!uni.getStorageSync(BASELINE_PROMPT_KEY);
  if (completed || prompted) return;

  uni.setStorageSync(BASELINE_PROMPT_KEY, true);
  uni.showModal({
    title: '首次建议',
    content: '建议先完成认知基准测试，再开始日常训练。是否现在开始？',
    success: (res) => {
      if (res.confirm) {
        goBaseline();
      }
    }
  });
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #ecfeff 0%, #f8fafc 100%);
  display: flex;
  flex-direction: column;
  padding: 30rpx 24rpx 24rpx;
  gap: 24rpx;
}

.header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12rpx 0 8rpx;
}

.logo-wrap {
  position: relative;
  width: 100%;
  max-width: 560rpx;
  height: 130rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-block {
  position: absolute;
  width: 96rpx;
  height: 86rpx;
  border-radius: 16rpx;
  box-shadow: 0 6rpx 14rpx rgba(15, 23, 42, 0.12);
}

.b1 {
  left: 58rpx;
  top: 6rpx;
  background: #3aa9ff;
  transform: rotate(-8deg);
}

.b2 {
  right: 56rpx;
  top: 4rpx;
  background: #ffc53d;
  transform: rotate(7deg);
}

.b3 {
  left: 84rpx;
  bottom: 6rpx;
  background: #76c442;
  transform: rotate(9deg);
}

.b4 {
  right: 84rpx;
  bottom: 6rpx;
  background: #ff9a3c;
  transform: rotate(-7deg);
}

.title {
  position: relative;
  z-index: 1;
  font-size: 62rpx;
  font-weight: 900;
  letter-spacing: 4rpx;
  color: transparent;
  background: linear-gradient(180deg, #ffe382 0%, #ffb42a 55%, #f08300 100%);
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow:
    0 3rpx 0 #8a4a0a,
    0 10rpx 16rpx rgba(146, 64, 14, 0.28);
}

.baseline-entry {
  background: #ffffff;
  border: 2rpx solid #bae6fd;
  border-radius: 16rpx;
  padding: 18rpx 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  box-shadow: 0 8rpx 18rpx rgba(2, 132, 199, 0.08);
}

.baseline-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0c4a6e;
}

.baseline-sub {
  font-size: 22rpx;
  color: #475569;
}

.game-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.game-card {
  background: #ffffff;
  border-radius: 18rpx;
  border: 2rpx solid #e2e8f0;
  padding: 18rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
  min-height: 320rpx;
  align-items: center;
}

.game-card.disabled {
  background: #f8fafc;
  border-style: dashed;
  box-shadow: none;
}

.logo {
  width: 100%;
  height: 170rpx;
  border-radius: 12rpx;
  background: #e2e8f0;
}

.placeholder-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  background: repeating-linear-gradient(-45deg, #e2e8f0 0 8rpx, #f1f5f9 8rpx 16rpx);
}

.placeholder-text {
  font-size: 56rpx;
  color: #64748b;
  font-weight: 700;
}

.title-slot {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 8rpx;
}

.card-title {
  font-size: 34rpx;
  color: #0b2545;
  font-weight: 700;
  font-family: "STKaiti", "KaiTi", "FangSong", serif;
  letter-spacing: 1rpx;
  text-align: center;
}

@media (min-width: 900px) {
  .page {
    max-width: 920px;
    margin: 0 auto;
  }

  .game-card {
    min-height: 360rpx;
  }
}
</style>
