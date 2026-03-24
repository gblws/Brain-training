<template>
  <view class="page">
    <view class="summary-card">
      <text class="title">基准测试历史</text>
      <text class="desc" v-if="history.length >= 2">
        相比首次测试，当前综合得分 {{ trendDelta >= 0 ? '+' : '' }}{{ trendDelta.toFixed(2) }}
      </text>
      <text class="desc" v-else>完成更多次测试后可看到进步趋势</text>
    </view>

    <view v-if="loading" class="empty">加载中...</view>
    <view v-else-if="!history.length" class="empty">暂无历史测试数据</view>

    <view v-else class="list">
      <view class="item" v-for="(item, idx) in history" :key="item.createTime + idx" @click="viewRadar(item)">
        <view class="row">
          <text class="idx">第 {{ history.length - idx }} 次</text>
          <text class="time">{{ formatTime(item.createTime) }}</text>
        </view>
        <view class="metrics">
          <text>观察力 {{ item.observationScore }}</text>
          <text>记忆力 {{ item.memoryScoreDimension }}</text>
          <text>空间力 {{ item.spatialScore }}</text>
          <text>计算力 {{ item.calculationScore }}</text>
          <text>推理力 {{ item.reasoningScore }}</text>
          <text class="gray">创造力 {{ item.creativityScore }}（待测试）</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { get } from '../../utils/request.js';

const SELECTED_RESULT_KEY = 'baseline_selected_result_v1';

const loading = ref(false);
const history = ref([]);

const averageCore = (item) => {
  const values = [
    Number(item.observationScore) || 0,
    Number(item.memoryScoreDimension) || 0,
    Number(item.spatialScore) || 0,
    Number(item.calculationScore) || 0,
    Number(item.reasoningScore) || 0
  ];
  return values.reduce((sum, val) => sum + val, 0) / values.length;
};

const trendDelta = computed(() => {
  if (history.value.length < 2) return 0;
  const latest = averageCore(history.value[0]);
  const first = averageCore(history.value[history.value.length - 1]);
  return latest - first;
});

const formatTime = (val) => {
  if (!val) return '--';
  const text = String(val).replace('T', ' ');
  const match = text.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/);
  if (match) {
    return `${match[1]} ${match[2]}`;
  }
  return text;
};

const loadHistory = async () => {
  try {
    loading.value = true;
    const data = await get('/api/v1/baseline/history');
    history.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error('load baseline history error', error);
    history.value = [];
  } finally {
    loading.value = false;
  }
};

const viewRadar = (item) => {
  if (!item) return;
  uni.setStorageSync(SELECTED_RESULT_KEY, item);
  uni.navigateTo({ url: '/pages/assessment/result?source=history' });
};

onShow(() => {
  loadHistory();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #f0f9ff 0%, #f8fafc 100%);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.summary-card,
.item {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 18rpx;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
}

.item {
  border: 2rpx solid #e2e8f0;
}

.item:active {
  transform: scale(0.995);
}

.title {
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
}

.desc {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #0f766e;
}

.empty {
  text-align: center;
  color: #64748b;
  font-size: 24rpx;
  margin-top: 20rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.idx {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
}

.time {
  font-size: 22rpx;
  color: #64748b;
}

.metrics {
  margin-top: 10rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6rpx;
  font-size: 22rpx;
  color: #334155;
}

.gray {
  color: #94a3b8;
}
</style>
