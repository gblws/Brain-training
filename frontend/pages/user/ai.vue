<template>
  <view class="page">
    <view class="bg-orb orb-a"></view>
    <view class="bg-orb orb-b"></view>

    <view class="hero">
      <text class="title">AI 智能导师</text>
      <text class="sub">基于本周测评生成训练建议</text>
    </view>

    <view class="card">
      <view class="card-head">
        <view>
          <text class="head-title">本周报告</text>
          <text class="head-time">{{ report?.weekStart || '--' }} ~ {{ report?.weekEnd || '--' }}</text>
        </view>
        <text class="status-tag" :class="statusClass">{{ statusText }}</text>
      </view>

      <view v-if="loading" class="state">加载中...</view>
      <view v-else-if="report?.status === 'processing'" class="state">AI 正在生成中，约 2-5 秒后可刷新查看</view>
      <view v-else>
        <view class="summary-box">
          <text class="label">综合分析</text>
          <text class="content">{{ report?.summary || '暂无' }}</text>
          <text v-if="report?.trend" class="content secondary">{{ report?.trend }}</text>
        </view>

        <view class="block">
          <text class="block-title">优势</text>
          <text class="line" v-for="(item, idx) in report?.strengths || []" :key="'s' + idx">- {{ item }}</text>
        </view>

        <view class="block">
          <text class="block-title">劣势</text>
          <text class="line" v-for="(item, idx) in report?.risks || []" :key="'r' + idx">- {{ item }}</text>
        </view>

        <view class="block">
          <text class="block-title">训练计划建议</text>
          <text class="line" v-for="(item, idx) in report?.suggestions || []" :key="'a' + idx">- {{ item }}</text>
        </view>

        <text class="generated-at">生成时间：{{ report?.generatedAt || '--' }}</text>
      </view>
    </view>

    <view class="actions">
      <button class="btn ghost" @click="loadReport(false)">刷新状态</button>
      <button class="btn primary" @click="loadReport(true)">重新生成</button>
    </view>
  </view>
</template>

<script setup>
import { onHide, onShow } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { get } from '../../utils/request.js';

const loading = ref(false);
const report = ref(null);
let pollTimer = null;

const clearPoll = () => {
  if (pollTimer) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
};

const schedulePoll = () => {
  clearPoll();
  pollTimer = setTimeout(() => {
    loadReport(false);
  }, 2500);
};

const loadReport = async (refresh) => {
  try {
    loading.value = true;
    const data = await get('/api/v1/ai/weekly-report', { refresh: !!refresh });
    report.value = data;
    if (data?.status === 'processing') {
      schedulePoll();
    } else {
      clearPoll();
    }
  } catch (error) {
    console.error('load ai report error', error);
  } finally {
    loading.value = false;
  }
};

const statusText = computed(() => {
  if (loading.value) return '加载中';
  if (!report.value) return '待生成';
  if (report.value.status === 'processing') return '生成中';
  if (report.value.source === 'ai-demo') return '演示结果';
  if (report.value.source === 'ai') return 'AI结果';
  return '兜底结果';
});

const statusClass = computed(() => {
  if (loading.value || !report.value) return 'tag-default';
  if (report.value.status === 'processing') return 'tag-processing';
  if (report.value.source === 'ai-demo') return 'tag-demo';
  if (report.value.source === 'ai') return 'tag-ai';
  return 'tag-fallback';
});

onShow(() => {
  loadReport(false);
});

onHide(() => {
  clearPoll();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 0 30rpx;
  background: linear-gradient(180deg, #c7eee7 0%, #e5f7f3 100%);
  position: relative;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.orb-a {
  width: 340rpx;
  height: 340rpx;
  right: -120rpx;
  top: 140rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.38), rgba(255, 255, 255, 0.02));
}

.orb-b {
  width: 280rpx;
  height: 280rpx;
  left: -90rpx;
  bottom: 120rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.02));
}

.hero {
  width: 92%;
  margin: 0 auto;
  background: linear-gradient(145deg, #35c9bc 0%, #6fe1d7 100%);
  padding: 26rpx 24rpx;
  border-radius: 18rpx;
  position: relative;
  z-index: 1;
}

.title {
  color: #ffffff;
  font-size: 38rpx;
  font-weight: 700;
}

.sub {
  display: block;
  margin-top: 6rpx;
  color: rgba(255, 255, 255, 0.9);
  font-size: 24rpx;
}

.card {
  margin: 18rpx auto 0;
  width: 92%;
  background: #ffffff;
  border-radius: 18rpx;
  padding: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
  position: relative;
  z-index: 1;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.head-title {
  display: block;
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
}

.head-time {
  display: block;
  margin-top: 4rpx;
  color: #64748b;
  font-size: 22rpx;
}

.status-tag {
  height: 44rpx;
  line-height: 44rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.tag-default {
  color: #475569;
  background: #e2e8f0;
}

.tag-processing {
  color: #8a5a00;
  background: #ffe7b3;
}

.tag-ai {
  color: #0f766e;
  background: #d1fae5;
}

.tag-demo {
  color: #7c3aed;
  background: #ede9fe;
}

.tag-fallback {
  color: #1d4ed8;
  background: #dbeafe;
}

.state {
  margin-top: 20rpx;
  color: #475569;
  font-size: 24rpx;
}

.summary-box {
  margin-top: 14rpx;
  background: #f8fffe;
  border: 2rpx solid #d7f2eb;
  border-radius: 12rpx;
  padding: 12rpx;
}

.label {
  color: #0f766e;
  font-size: 22rpx;
  font-weight: 600;
}

.content {
  margin-top: 6rpx;
  color: #1f2937;
  font-size: 24rpx;
  line-height: 1.6;
}

.content.secondary {
  margin-top: 10rpx;
  padding-top: 10rpx;
  border-top: 2rpx dashed #d7f2eb;
  color: #475569;
}

.block {
  margin-top: 14rpx;
}

.block-title {
  color: #0f172a;
  font-size: 25rpx;
  font-weight: 700;
}

.line {
  margin-top: 6rpx;
  color: #334155;
  font-size: 23rpx;
  line-height: 1.6;
}

.generated-at {
  display: block;
  margin-top: 16rpx;
  color: #64748b;
  font-size: 22rpx;
}

.actions {
  margin: 18rpx auto 0;
  width: 92%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  position: relative;
  z-index: 1;
}

.btn {
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.btn.ghost {
  background: #ffffff;
  color: #0f766e;
  border: 2rpx solid #b7e7dd;
}

.btn.primary {
  background: linear-gradient(135deg, #10b981 0%, #14b8a6 100%);
  color: #ffffff;
}
</style>
