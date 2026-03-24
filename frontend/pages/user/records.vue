<template>
  <view class="page">
    <view v-if="!loggedIn" class="empty-card">
      <text class="empty-title">Record History</text>
      <text class="empty-text">Please log in first to view your own game records.</text>
    </view>

    <template v-else>
    <view class="head">
      <view>
        <text class="title">历史记录</text>
      </view>
      <view class="refresh-btn" @click="loadHistory">↻</view>
    </view>

    <view v-if="loading" class="empty">加载中...</view>
    <view v-else-if="!records.length" class="empty">暂无记录</view>

    <view v-else class="list">
      <view class="item" v-for="item in records" :key="item.id">
        <view class="item-main">
          <text class="name">{{ mapGameName(item.gameName) }}</text>
          <text class="time">{{ formatTime(item.createTime) }}</text>
        </view>
        <view class="meta-row">
          <text class="meta score">得分 {{ item.score }}</text>
          <text class="meta">正确率 {{ formatAcc(item.accuracy) }}</text>
          <text class="meta">时长 {{ formatDuration(item.durationSeconds) }}</text>
        </view>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { get, getAuthToken } from '../../utils/request.js';

const GAME_NAME_MAP = {
  'Schulte Grid': '舒尔特方格',
  'Stroop Challenge': '斯特鲁普挑战',
  'Memory Matrix': '记忆矩阵'
};

const loading = ref(false);
const records = ref([]);
const loggedIn = ref(false);

const mapGameName = (value) => GAME_NAME_MAP[value] || value || '未知游戏';

const formatTime = (val) => {
  if (!val) return '--';
  const text = String(val).replace('T', ' ');
  const match = text.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}):(\d{2})/);
  if (match) {
    return `${match[1]} ${match[2]}:${match[3]}`;
  }
  return text;
};

const formatAcc = (val) => {
  if (val === null || typeof val === 'undefined') return '--';
  return `${Math.round(Number(val) * 100)}%`;
};

const formatDuration = (seconds) => {
  const total = Math.max(0, Number(seconds || 0));
  if (!total) return '0分';
  const minute = Math.floor(total / 60);
  const second = total % 60;
  if (!minute) return `${second}秒`;
  if (!second) return `${minute}分`;
  return `${minute}分${second}秒`;
};

const loadHistory = async () => {
  loggedIn.value = !!getAuthToken();
  if (!loggedIn.value) {
    records.value = [];
    return;
  }
  try {
    loading.value = true;
    const data = await get('/api/v1/user/history');
    records.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error('record history load error', error);
    records.value = [];
  } finally {
    loading.value = false;
  }
};

onShow(() => {
  loadHistory();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 26rpx 24rpx 32rpx;
  background: linear-gradient(180deg, #eefaf8 0%, #f8fbff 100%);
}

.empty-card {
  min-height: calc(100vh - 58rpx);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 0 50rpx;
}

.empty-title {
  color: #0f172a;
  font-size: 38rpx;
  font-weight: 800;
}

.empty-text {
  margin-top: 14rpx;
  color: #64748b;
  font-size: 28rpx;
  line-height: 1.6;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}

.title {
  display: block;
  color: #0f172a;
  font-size: 38rpx;
  font-weight: 800;
}

.sub {
  display: block;
  margin-top: 6rpx;
  color: #64748b;
  font-size: 24rpx;
}

.refresh-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  background: #ddf6f0;
  color: #0f766e;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 700;
}

.list {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.item {
  background: #ffffff;
  border-radius: 22rpx;
  padding: 20rpx;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.06);
}

.item-main {
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
  align-items: center;
}

.name {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
}

.time,
.empty {
  color: #64748b;
  font-size: 24rpx;
}

.meta-row {
  margin-top: 14rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.meta {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #eff8f7;
  color: #0f766e;
  font-size: 22rpx;
}

.meta.score {
  background: #fff4e8;
  color: #b45309;
}

.empty {
  margin-top: 24rpx;
  text-align: center;
}
</style>
