<template>
  <view class="page">
    <view class="head">
      <text class="title">Training History</text>
      <button size="mini" @click="loadHistory">Refresh</button>
    </view>

    <view v-if="loading" class="empty">Loading...</view>
    <view v-else-if="!records.length" class="empty">No records yet</view>

    <view v-else class="list">
      <view class="item" v-for="item in records" :key="item.id">
        <text class="name">{{ item.gameName }}</text>
        <text class="meta">Score: {{ item.score }} | Accuracy: {{ formatAcc(item.accuracy) }}</text>
        <text class="time">{{ formatTime(item.createTime) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { get } from '../../utils/request.js';

const loading = ref(false);
const records = ref([]);

const formatTime = (val) => {
  if (!val) return '--';
  const text = String(val).replace('T', ' ');
  // Keep display at minute precision: YYYY-MM-DD HH:mm
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

const loadHistory = async () => {
  try {
    loading.value = true;
    records.value = await get('/api/v1/user/history');
  } catch (error) {
    console.error('history load error', error);
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
  padding: 28rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 34rpx;
  font-weight: 700;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.item {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 22rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
}

.meta,
.time,
.empty {
  color: #6b7280;
  font-size: 24rpx;
}
</style>
