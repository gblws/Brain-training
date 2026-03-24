<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">数据分析</text>
    </view>

    <view class="card">
      <view class="card-head">
        <picker mode="selector" :range="abilityOptions" range-key="label" :value="abilityIndex" @change="onAbilityChange">
          <view class="title-picker">
            <text class="card-title">{{ abilityText }}</text>
            <text class="title-arrow">▾</text>
          </view>
        </picker>
      </view>

      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="!points.length" class="empty">暂无数据</view>
      <view v-else class="chart-section">
        <view class="axis-wrap">
          <view class="y-axis">
            <text class="y-label" v-for="tick in yTicks" :key="tick">{{ tick }}</text>
          </view>

          <view class="plot">
            <view class="grid-line" v-for="(tick, idx) in yTicks" :key="tick" :style="`top:${(idx / (yTicks.length - 1)) * 100}%`" />
            <svg class="line-layer" viewBox="0 0 100 100" preserveAspectRatio="none">
              <line
                v-if="chartPoints.length === 1"
                x1="0"
                :y1="singleLineY"
                x2="100"
                :y2="singleLineY"
                stroke="#1d74f5"
                stroke-width="1"
              />
              <polyline
                v-else
                :points="polylinePoints"
                fill="none"
                stroke="#1d74f5"
                stroke-width="1"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <view class="point-layer" v-if="dotStyles.length">
              <view class="dot" v-for="(s, idx) in dotStyles" :key="`dot-${idx}`" :style="s" />
            </view>
          </view>
        </view>

        <view class="x-axis">
          <view class="x-label" v-for="item in xTicks" :key="item.index" :style="item.style">
            {{ item.label }}
          </view>
        </view>
      </view>

      <view class="period-tabs">
        <view
          v-for="item in periodOptions"
          :key="item.key"
          class="period-tab"
          :class="{ active: period === item.key }"
          @click="period = item.key"
        >
          {{ item.label }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { get } from '../../utils/request.js';

const loading = ref(false);
const baselineHistory = ref([]);
const period = ref('month');
const ability = ref('overall');
const ENABLE_TEMP_WEEK_MOCK = true;

const periodOptions = [
  { key: 'week', label: '近一周' },
  { key: 'month', label: '近一月' },
  { key: 'year', label: '近一年' }
];

const abilityOptions = [
  { key: 'overall', label: '综合分数' },
  { key: 'observation', label: '观察力' },
  { key: 'memory', label: '记忆力' },
  { key: 'spatial', label: '空间力' },
  { key: 'calculation', label: '计算力' },
  { key: 'reasoning', label: '推理力' },
  { key: 'creativity', label: '创造力' }
];

const periodText = computed(() => periodOptions.find((item) => item.key === period.value)?.label || '近一月');
const abilityText = computed(() => abilityOptions.find((item) => item.key === ability.value)?.label || '综合');
const abilityIndex = computed(() => {
  const idx = abilityOptions.findIndex((item) => item.key === ability.value);
  return idx < 0 ? 0 : idx;
});

const onAbilityChange = (event) => {
  const index = Number(event?.detail?.value);
  if (!Number.isFinite(index) || index < 0 || index >= abilityOptions.length) return;
  ability.value = abilityOptions[index].key;
};

const toDate = (value) => {
  if (!value) return null;
  const raw = String(value).trim();
  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T');
  const time = Date.parse(normalized);
  if (!Number.isFinite(time)) return null;
  return new Date(time);
};

const dateKey = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const monthKey = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  return `${y}-${m}`;
};

const scoreByAbility = (item) => {
  switch (ability.value) {
    case 'observation':
      return Number(item.observationScore) || 0;
    case 'memory':
      return Number(item.memoryScoreDimension) || 0;
    case 'spatial':
      return Number(item.spatialScore) || 0;
    case 'calculation':
      return Number(item.calculationScore) || 0;
    case 'reasoning':
      return Number(item.reasoningScore) || 0;
    case 'creativity':
      return Number(item.creativityScore) || 0;
    case 'overall':
    default: {
      const values = [
        Number(item.observationScore) || 0,
        Number(item.memoryScoreDimension) || 0,
        Number(item.spatialScore) || 0,
        Number(item.calculationScore) || 0,
        Number(item.reasoningScore) || 0,
        Number(item.creativityScore) || 0
      ];
      return values.reduce((sum, value) => sum + value, 0) / values.length;
    }
  }
};

const points = computed(() => {
  const list = Array.isArray(baselineHistory.value) ? baselineHistory.value : [];
  const now = new Date();
  const start =
    period.value === 'week'
      ? new Date(now.getFullYear(), now.getMonth(), now.getDate() - 6)
      : period.value === 'month'
        ? new Date(now.getFullYear(), now.getMonth(), now.getDate() - 29)
        : new Date(now.getFullYear(), now.getMonth() - 11, 1);

  const bucket = new Map();
  list.forEach((item) => {
    const time = toDate(item.createTime);
    if (!time || time < start || time > now) return;

    const key = period.value === 'year' ? monthKey(time) : dateKey(time);
    const label = period.value === 'year' ? `${key}-01` : key;
    const current = bucket.get(key) || { key, label, ts: time.getTime(), sum: 0, count: 0 };
    current.sum += scoreByAbility(item);
    current.count += 1;
    if (time.getTime() > current.ts) current.ts = time.getTime();
    bucket.set(key, current);
  });

  return Array.from(bucket.values())
    .sort((a, b) => a.ts - b.ts)
    .map((item) => ({
      label: item.label,
      ts: item.ts,
      value: Number((item.sum / item.count).toFixed(2))
    }));
});

const pointCount = computed(() => points.value.length);
const yMin = 0;
const yMax = 100;
const yTicks = ['100', '75', '50', '25', '0'];

const chartPoints = computed(() => {
  if (!points.value.length) return [];
  return points.value.map((item, index) => {
    const xRaw = points.value.length === 1 ? 50 : (index / (points.value.length - 1)) * 100;
    const x = Math.max(1.5, Math.min(98.5, xRaw));
    const yRaw = ((yMax - item.value) / (yMax - yMin)) * 100;
    const y = Math.max(1.5, Math.min(98.5, yRaw));
    return { x, y };
  });
});

const polylinePoints = computed(() => chartPoints.value.map((p) => `${p.x},${p.y}`).join(' '));
const singleLineY = computed(() => (chartPoints.value.length === 1 ? chartPoints.value[0].y : 50));
const dotStyles = computed(() => {
  if (chartPoints.value.length <= 1) return [];
  return chartPoints.value.map((p) => `left: calc(${p.x}% - 4rpx); top: calc(${p.y}% - 4rpx);`);
});

const xTicks = computed(() => {
  if (!points.value.length) return [];
  if (points.value.length === 1) {
    return [{ index: 0, label: points.value[0].label, style: 'left: 0; transform: none; text-align: left;' }];
  }

  const lastIndex = points.value.length - 1;
  if (points.value.length <= 3) {
    return points.value.map((item, index) => ({
      index,
      label: item.label,
      style:
        index === 0
          ? 'left: 0; transform: none; text-align: left;'
          : index === lastIndex
            ? 'right: 0; left: auto; transform: none; text-align: right;'
            : `left: ${(index / lastIndex) * 100}%; transform: translateX(-50%); text-align: center;`
    }));
  }

  const indices = [0, Math.floor(lastIndex / 2), lastIndex];
  return indices.map((idx, i) => {
    if (i === 0) {
      return { index: idx, label: points.value[idx].label, style: 'left: 0; transform: none; text-align: left;' };
    }
    if (i === indices.length - 1) {
      return { index: idx, label: points.value[idx].label, style: 'right: 0; left: auto; transform: none; text-align: right;' };
    }
    return {
      index: idx,
      label: points.value[idx].label,
      style: `left: ${(idx / lastIndex) * 100}%; transform: translateX(-50%); text-align: center;`
    };
  });
});

const loadTrend = async () => {
  try {
    loading.value = true;
    const data = await get('/api/v1/baseline/history');
    const realData = Array.isArray(data) ? data : [];
    baselineHistory.value = ENABLE_TEMP_WEEK_MOCK ? buildMockWeekData() : realData;
  } catch (error) {
    console.error('trend load error', error);
    baselineHistory.value = ENABLE_TEMP_WEEK_MOCK ? buildMockWeekData() : [];
  } finally {
    loading.value = false;
  }
};

const buildMockWeekData = () => {
  const dayMs = 24 * 60 * 60 * 1000;
  const now = new Date();
  const dayScores = [62, 68, 65, 72, 78, 74, 81];
  return dayScores.map((score, idx) => {
    const date = new Date(now.getTime() - (dayScores.length - 1 - idx) * dayMs);
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return {
      createTime: `${y}-${m}-${d}T10:00:00`,
      observationScore: score,
      memoryScoreDimension: Math.max(0, Math.min(100, score - 2)),
      spatialScore: Math.max(0, Math.min(100, score + 3)),
      calculationScore: Math.max(0, Math.min(100, score - 1)),
      reasoningScore: Math.max(0, Math.min(100, score + 1)),
      creativityScore: 50
    };
  });
};

onShow(() => {
  loadTrend();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #eef2f7;
  padding-bottom: 26rpx;
}

.hero {
  background: linear-gradient(140deg, #0d6ff3 0%, #1685ff 100%);
  padding: 34rpx 26rpx 54rpx;
}

.hero-title {
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 700;
}

.hero-sub {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.88);
  font-size: 24rpx;
}

.card {
  margin: -26rpx 20rpx 0;
  background: #ffffff;
  border-radius: 18rpx;
  padding: 20rpx;
  box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.08);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-picker {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.card-title {
  color: #0f172a;
  font-size: 34rpx;
  font-weight: 700;
}

.title-arrow {
  color: #64748b;
  font-size: 22rpx;
  line-height: 1;
}

.card-tip {
  color: #64748b;
  font-size: 22rpx;
}

.chart-section {
  margin-top: 16rpx;
}

.axis-wrap {
  display: flex;
  gap: 10rpx;
}

.y-axis {
  width: 72rpx;
  height: 320rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  padding-right: 4rpx;
  box-sizing: border-box;
}

.y-label {
  color: #94a3b8;
  font-size: 20rpx;
}

.plot {
  position: relative;
  flex: 1;
  height: 320rpx;
  border-radius: 12rpx;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f5fb 100%);
  overflow: hidden;
}

.grid-line {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 2rpx dashed #d5dfef;
}

.line-layer {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}

.point-layer {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.dot {
  position: absolute;
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #ef4444;
  border: none;
}

.x-axis {
  position: relative;
  height: 44rpx;
  margin-left: 82rpx;
}

.x-label {
  position: absolute;
  left: 0;
  transform: none;
  color: #94a3b8;
  font-size: 18rpx;
  top: 8rpx;
  white-space: nowrap;
  word-break: keep-all;
  line-height: 1;
  max-width: 210rpx;
}

.period-tabs {
  margin-top: 10rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.period-tab {
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border-radius: 999rpx;
  background: #f2f6fd;
  color: #64748b;
  font-size: 24rpx;
}

.period-tab.active {
  background: rgba(29, 116, 245, 0.14);
  color: #1d74f5;
  font-weight: 700;
}

.empty {
  margin-top: 20rpx;
  text-align: center;
  color: #64748b;
  font-size: 24rpx;
}
</style>
