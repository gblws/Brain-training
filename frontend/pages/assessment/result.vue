<template>
  <view class="page">
    <view class="card">
      <text class="title">基准测试结果（{{ result?.matrixVersion || 'v1' }}）</text>
      <canvas class="radar-canvas" canvas-id="radarCanvas" id="radarCanvas" width="320" height="320" />

      <view class="metrics" v-if="result">
        <text class="metric">观察力：{{ result.observationScore }}</text>
        <text class="metric">记忆力：{{ result.memoryScoreDimension }}</text>
        <text class="metric">空间力：{{ result.spatialScore }}</text>
        <text class="metric">计算力：{{ result.calculationScore }}</text>
        <text class="metric">推理力：{{ result.reasoningScore }}</text>
        <text class="metric gray">创造力：{{ result.creativityScore }}（待测试）</text>
      </view>

      <button class="history-btn" @click="goHistory">查看历史测试数据</button>
    </view>
  </view>
</template>

<script setup>
import { nextTick, ref } from 'vue';
import { onLoad, onReady } from '@dcloudio/uni-app';
import { get } from '../../utils/request.js';

const RESULT_KEY = 'baseline_latest_result_v1';
const SELECTED_RESULT_KEY = 'baseline_selected_result_v1';

const result = ref(null);

const dims = [
  { key: 'observationScore', label: '观察力', unlocked: true },
  { key: 'memoryScoreDimension', label: '记忆力', unlocked: true },
  { key: 'spatialScore', label: '空间力', unlocked: true },
  { key: 'calculationScore', label: '计算力', unlocked: true },
  { key: 'reasoningScore', label: '推理力', unlocked: true },
  { key: 'creativityScore', label: '创造力', unlocked: false }
];

const parseScore = (val) => {
  const n = Number(val);
  if (!Number.isFinite(n)) return 0;
  return Math.max(0, Math.min(100, n));
};

const drawRadar = () => {
  if (!result.value) return;

  const ctx = uni.createCanvasContext('radarCanvas');
  const size = 320;
  const center = size / 2;
  const radius = 108;
  const levels = 5;
  const step = (Math.PI * 2) / dims.length;

  ctx.clearRect(0, 0, size, size);

  for (let lv = 1; lv <= levels; lv += 1) {
    const r = (radius * lv) / levels;
    ctx.beginPath();
    for (let i = 0; i < dims.length; i += 1) {
      const angle = -Math.PI / 2 + step * i;
      const x = center + r * Math.cos(angle);
      const y = center + r * Math.sin(angle);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.setStrokeStyle('#dbe4ee');
    ctx.setLineWidth(1);
    ctx.stroke();
  }

  const points = [];

  for (let i = 0; i < dims.length; i += 1) {
    const angle = -Math.PI / 2 + step * i;
    const x = center + radius * Math.cos(angle);
    const y = center + radius * Math.sin(angle);
    const score = parseScore(result.value[dims[i].key]);
    const pr = (radius * score) / 100;
    const px = center + pr * Math.cos(angle);
    const py = center + pr * Math.sin(angle);
    points.push({ x: px, y: py });

    ctx.beginPath();
    ctx.moveTo(center, center);
    ctx.lineTo(x, y);
    if (dims[i].unlocked) {
      ctx.setLineDash([]);
      ctx.setStrokeStyle('#cbd5e1');
    } else {
      ctx.setLineDash([5, 4]);
      ctx.setStrokeStyle('#94a3b8');
    }
    ctx.setLineWidth(1);
    ctx.stroke();
    ctx.setLineDash([]);

    const lx = center + (radius + 18) * Math.cos(angle);
    const ly = center + (radius + 18) * Math.sin(angle);
    ctx.setFillStyle(dims[i].unlocked ? '#334155' : '#94a3b8');
    ctx.setFontSize(11);
    ctx.fillText(dims[i].label, lx - 14, ly + 4);
  }

  ctx.beginPath();
  for (let i = 0; i < points.length; i += 1) {
    if (i === 0) ctx.moveTo(points[i].x, points[i].y);
    else ctx.lineTo(points[i].x, points[i].y);
  }
  ctx.closePath();
  ctx.setFillStyle('rgba(14, 165, 233, 0.22)');
  ctx.fill();
  ctx.setStrokeStyle('#0284c7');
  ctx.setLineWidth(2);
  ctx.stroke();

  for (let i = 0; i < dims.length; i += 1) {
    if (dims[i].unlocked) continue;
    const prev = (i - 1 + dims.length) % dims.length;
    const next = (i + 1) % dims.length;

    ctx.setLineDash([6, 4]);
    ctx.setStrokeStyle('#94a3b8');
    ctx.setLineWidth(2);
    ctx.beginPath();
    ctx.moveTo(points[prev].x, points[prev].y);
    ctx.lineTo(points[i].x, points[i].y);
    ctx.lineTo(points[next].x, points[next].y);
    ctx.stroke();
    ctx.setLineDash([]);
  }

  for (let i = 0; i < dims.length; i += 1) {
    ctx.beginPath();
    ctx.arc(points[i].x, points[i].y, 3, 0, Math.PI * 2);
    if (dims[i].unlocked) {
      ctx.setFillStyle('#0284c7');
      ctx.fill();
    } else {
      ctx.setStrokeStyle('#94a3b8');
      ctx.setLineWidth(2);
      ctx.stroke();
    }
  }

  ctx.draw();
};

const loadLatestFromApi = async () => {
  try {
    const data = await get('/api/v1/baseline/latest');
    if (data) {
      result.value = data;
      uni.setStorageSync(RESULT_KEY, data);
    }
  } catch (error) {
    console.error('load baseline latest error', error);
  }
};

const goHistory = () => {
  uni.navigateTo({ url: '/pages/assessment/history' });
};

onLoad(async (query) => {
  if (query?.source === 'history') {
    const selected = uni.getStorageSync(SELECTED_RESULT_KEY);
    if (selected && typeof selected === 'object') {
      result.value = selected;
    }
  }

  if (!result.value) {
    const local = uni.getStorageSync(RESULT_KEY);
    if (local && typeof local === 'object') {
      result.value = local;
    } else {
      await loadLatestFromApi();
    }
  }
});

onReady(async () => {
  if (!result.value) {
    await loadLatestFromApi();
  }
  await nextTick();
  drawRadar();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #f0f9ff 0%, #f8fafc 100%);
}

.card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
}

.title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.radar-canvas {
  width: 640rpx;
  height: 640rpx;
  margin: 14rpx auto 6rpx;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8rpx;
  margin-top: 6rpx;
}

.metric {
  font-size: 24rpx;
  color: #0f172a;
}

.metric.gray {
  color: #64748b;
}

.history-btn {
  margin-top: 14rpx;
  background: #ffffff;
  color: #0f172a;
  border-radius: 12rpx;
  border: 2rpx solid #cbd5e1;
}
</style>
