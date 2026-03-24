<template>
  <view class="page">
    <view class="bg-orb orb-a"></view>
    <view class="bg-orb orb-b"></view>

    <template v-if="loggedIn">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="hero-title">Training Data</text>
          <text class="hero-sub">View streaks, duration and top records on one page</text>
        </view>
        <view class="refresh-btn" @click="loadHistory">↻</view>
      </view>

      <view class="hero-tip">
        <text class="hero-tip-text">{{ summaryTip }}</text>
      </view>

      <view class="summary-card">
        <view class="summary-main">
          <view class="summary-ring">
            <text class="summary-ring-label">连续</text>
            <text class="summary-ring-value">{{ streakDays }}</text>
            <text class="summary-ring-unit">天</text>
          </view>
        </view>

        <view class="summary-stats">
          <view class="summary-stat">
            <text class="summary-stat-value">{{ todayTrainingText }}</text>
            <text class="summary-stat-label">今日训练时长</text>
          </view>
          <view class="summary-stat">
            <text class="summary-stat-value">{{ totalTrainingText }}</text>
            <text class="summary-stat-label">总训练时长</text>
          </view>
          <view class="summary-stat">
            <text class="summary-stat-value">{{ totalTrainingDays }}</text>
            <text class="summary-stat-label">总训练天数</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content">
      <view class="section-card">
        <view class="section-head">
          <text class="section-title">近7天状态</text>
        </view>

        <view class="week-strip">
          <view class="day-item" v-for="item in weekItems" :key="item.date" :class="{ active: item.active, today: item.isToday }">
            <text class="day-week">{{ item.weekLabel }}</text>
            <text class="day-date">{{ item.dayLabel }}</text>
            <view class="day-dot"></view>
          </view>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">最高纪录</text>
        </view>

        <view class="rank-grid">
          <view class="rank-card" v-for="item in rankCards" :key="item.key">
            <RecordRankBadge :rank="item.rankKey" />
            <view class="rank-info">
              <text class="rank-game">{{ item.label }}</text>
              <text class="rank-name" :class="{ 'rank-name-empty': item.rankKey === 'empty' }">{{ item.rankLabel }}</text>
              <text class="rank-meta">{{ item.meta }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { get, getAuthToken } from '../../utils/request.js';
import RecordRankBadge from '../../components/RecordRankBadge.vue';

const GAME_CONFIG = [
  { key: 'schulte', names: ['Schulte Grid'], label: '舒尔特方格' },
  { key: 'stroop', names: ['Stroop Challenge'], label: '斯特鲁普挑战' },
  { key: 'memory', names: ['Memory Matrix'], label: '记忆矩阵' }
];

const DIFFICULTY_TEXT_MAP = {
  0: '基准',
  1: '简单',
  2: '普通',
  3: '困难',
  4: '噩梦'
};

const records = ref([]);
const loggedIn = ref(false);
const redirectingToLogin = ref(false);

const dateKey = (value) => {
  if (!value) return '';
  const text = String(value).trim();
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) {
    return text.slice(0, 10);
  }
  const date = new Date(text);
  if (Number.isNaN(date.getTime())) return '';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const toDate = (value) => {
  const key = dateKey(value);
  if (!key) return null;
  return new Date(`${key}T00:00:00`);
};

const todayKey = () => dateKey(new Date().toISOString());

const recordDateSet = computed(() => new Set(records.value.map((item) => dateKey(item.createTime)).filter(Boolean)));

const streakDays = computed(() => {
  const dates = Array.from(recordDateSet.value)
    .map((item) => new Date(`${item}T00:00:00`).getTime())
    .filter((item) => Number.isFinite(item))
    .sort((a, b) => b - a);

  if (!dates.length) return 0;

  let streak = 1;
  for (let i = 1; i < dates.length; i += 1) {
    const gap = (dates[i - 1] - dates[i]) / 86400000;
    if (gap === 1) {
      streak += 1;
      continue;
    }
    if (gap === 0) {
      continue;
    }
    break;
  }
  return streak;
});

const todayRecords = computed(() => {
  const key = todayKey();
  return records.value.filter((item) => dateKey(item.createTime) === key);
});

const todayDurationSeconds = computed(() => todayRecords.value.reduce((sum, item) => sum + Number(item.durationSeconds || 0), 0));

const totalTrainingSeconds = computed(() => records.value.reduce((sum, item) => sum + Number(item.durationSeconds || 0), 0));

const totalTrainingDays = computed(() => recordDateSet.value.size);

const todayTrainingText = computed(() => formatHourMinute(todayDurationSeconds.value));

const totalTrainingText = computed(() => formatHourMinute(totalTrainingSeconds.value));

const weekItems = computed(() => {
  const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
  const today = toDate(new Date().toISOString());
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today);
    date.setDate(today.getDate() - (6 - index));
    const key = dateKey(date.toISOString());
    return {
      date: key,
      weekLabel: index === 6 ? '今天' : `周${weekdays[date.getDay()]}`,
      dayLabel: `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`,
      active: recordDateSet.value.has(key),
      isToday: key === todayKey()
    };
  });
});

const normalizeDifficultyLevel = (record) => {
  const raw = Number(record?.difficultyLevel);
  if (Number.isFinite(raw)) {
    return Math.max(0, Math.round(raw));
  }
  const text = String(record?.difficultyName || '').trim();
  const found = Object.entries(DIFFICULTY_TEXT_MAP).find(([, value]) => value === text);
  return found ? Number(found[0]) : 0;
};

const normalizeDifficultyName = (record) => {
  const level = normalizeDifficultyLevel(record);
  return DIFFICULTY_TEXT_MAP[level] || record?.difficultyName || '未记录';
};

const getRankInfo = (record) => {
  const score = Number(record?.score || 0);
  const level = normalizeDifficultyLevel(record);
  if (level === 4 && score >= 80) return { key: 'prismatic', label: '棱彩' };
  if (level === 3 && score >= 80) return { key: 'gold', label: '黄金' };
  if (level === 2 && score >= 80) return { key: 'silver', label: '白银' };
  if (level === 2 && score >= 60) return { key: 'bronze', label: '青铜' };
  return { key: 'empty', label: '未达标' };
};

const rankCards = computed(() => GAME_CONFIG.map((game) => {
  const gameRecords = records.value.filter((item) => game.names.includes(item.gameName) && normalizeDifficultyLevel(item) > 0);
  const ranked = gameRecords
    .map((item) => ({ record: item, rank: getRankInfo(item) }))
    .sort((a, b) => {
      const rankWeight = { empty: 0, bronze: 1, silver: 2, gold: 3, prismatic: 4 };
      const rankGap = rankWeight[b.rank.key] - rankWeight[a.rank.key];
      if (rankGap !== 0) return rankGap;
      return Number(b.record.score || 0) - Number(a.record.score || 0);
    });
  const bestRanked = ranked[0] || null;
  const best = bestRanked?.record || gameRecords.reduce((max, item) => {
    if (!max) return item;
    return Number(item.score || 0) > Number(max.score || 0) ? item : max;
  }, null);
  const rank = best ? getRankInfo(best) : { key: 'empty', label: '未达标' };
  const score = Number(best?.score || 0);
  const difficultyName = best ? normalizeDifficultyName(best) : '未记录';
  return {
    key: game.key,
    label: game.label,
    score,
    rankKey: rank.key,
    rankLabel: rank.label,
    meta: best ? `${difficultyName} · ${score}分` : '未记录 · 0分'
  };
}));

const summaryTip = computed(() => {
  if (!records.value.length) {
    return '今天还没有新的训练记录，先去完成一局热热身吧。';
  }
  if (!todayRecords.value.length) {
    return `你已经连续训练 ${streakDays.value} 天，今天继续保持会更稳。`;
  }
  return `今天已完成 ${todayTrainingText.value} 的训练，继续冲击更高评级。`;
});

const formatDuration = (seconds) => {
  const total = Math.max(0, Number(seconds || 0));
  if (!total) return '0分';
  const minute = Math.floor(total / 60);
  const second = total % 60;
  if (!minute) return `${second}秒`;
  if (!second) return `${minute}分`;
  return `${minute}分${second}秒`;
};

const formatAcc = (val) => {
  if (val === null || typeof val === 'undefined') return '--';
  return `${Math.round(Number(val) * 100)}%`;
};

const formatHourMinute = (seconds) => {
  const total = Math.max(0, Number(seconds || 0));
  if (!total) return '0时0分';
  const hour = Math.floor(total / 3600);
  const minute = Math.floor((total % 3600) / 60);
  return `${hour}时${minute}分`;
};

const loadHistory = async () => {
  loggedIn.value = !!getAuthToken();
  if (!loggedIn.value) {
    records.value = [];
    if (!redirectingToLogin.value) {
      redirectingToLogin.value = true;
      uni.showModal({
        title: '请先登录',
        content: '登录后才可以查看训练数据。',
        showCancel: false,
        confirmText: '去登录',
        success: () => {
          uni.switchTab({ url: '/pages/user/player' });
        },
        complete: () => {
          setTimeout(() => {
            redirectingToLogin.value = false;
          }, 300);
        }
      });
    }
    return;
  }
  try {
    const data = await get('/api/v1/user/history');
    records.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error('history load error', error);
    records.value = [];
  }
};

onShow(() => {
  loadHistory();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #c4efe8 0%, #e7fbf6 44%, #f6fbff 100%);
  position: relative;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.orb-a {
  width: 360rpx;
  height: 360rpx;
  right: -130rpx;
  top: 120rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.02));
}

.orb-b {
  width: 280rpx;
  height: 280rpx;
  left: -70rpx;
  bottom: 120rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.26), rgba(255, 255, 255, 0.02));
}

.hero {
  position: relative;
  z-index: 1;
  padding: 30rpx 24rpx 18rpx;
  background: linear-gradient(160deg, #55d5c8 0%, #52bedf 72%, #8fe4cb 100%);
  border-radius: 0 0 34rpx 34rpx;
  box-shadow: 0 12rpx 36rpx rgba(35, 113, 126, 0.16);
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.hero-title {
  display: block;
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 800;
}

.hero-sub {
  display: block;
  margin-top: 6rpx;
  color: rgba(255, 255, 255, 0.88);
  font-size: 24rpx;
}

.refresh-btn {
  width: 66rpx;
  height: 66rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 700;
}

.hero-tip {
  margin-top: 18rpx;
  padding: 18rpx 22rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.08);
}

.hero-tip-text {
  color: #245664;
  font-size: 25rpx;
  line-height: 1.5;
}

.summary-card {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 28rpx;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.34) 0%, rgba(255, 255, 255, 0.2) 100%);
  backdrop-filter: blur(10rpx);
}

.summary-main {
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-ring {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.96) 0 52%, transparent 52%),
    linear-gradient(145deg, #e4ffff 0%, #94ebf5 42%, #63d3ea 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #15586a;
  box-shadow: inset 0 0 0 10rpx rgba(255, 255, 255, 0.2);
}

.summary-ring-label,
.summary-ring-unit {
  font-size: 22rpx;
}

.summary-ring-value {
  font-size: 56rpx;
  font-weight: 800;
  line-height: 1.1;
}

.summary-stats {
  margin-top: 16rpx;
  padding: 18rpx 12rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.9);
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8rpx;
}

.summary-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}

.summary-stat-value {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 800;
}

.summary-stat-label {
  color: #64748b;
  font-size: 22rpx;
}

.content {
  position: relative;
  z-index: 1;
  padding: 18rpx 24rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.section-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 26rpx;
  padding: 22rpx;
  box-shadow: 0 12rpx 30rpx rgba(15, 23, 42, 0.06);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10rpx;
}

.section-title {
  color: #0f172a;
  font-size: 32rpx;
  font-weight: 800;
}

.section-sub {
  color: #64748b;
  font-size: 22rpx;
}

.week-strip {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8rpx;
}

.day-item {
  padding: 16rpx 0 14rpx;
  border-radius: 20rpx;
  background: #f7fafc;
  border: 2rpx solid #ebf0f7;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}

.day-item.active {
  background: linear-gradient(180deg, #e8fffb 0%, #d8fbf5 100%);
  border-color: #92ead8;
}

.day-item.today {
  box-shadow: inset 0 -4rpx 0 #fb7a59;
}

.day-week {
  color: #475569;
  font-size: 22rpx;
}

.day-date {
  color: #0f172a;
  font-size: 24rpx;
  font-weight: 700;
}

.day-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #d3dce8;
}

.day-item.active .day-dot {
  background: #26b39f;
}

.rank-grid {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.rank-card {
  padding: 18rpx 18rpx 16rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #fbfdff 0%, #f1f7fb 100%);
  border: 2rpx solid #edf4fa;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.rank-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.rank-game {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
}

.rank-name {
  color: #0f766e;
  font-size: 24rpx;
  font-weight: 700;
}

.rank-name.rank-name-empty {
  color: #475569;
}

.rank-score,
.rank-meta {
  color: #64748b;
  font-size: 22rpx;
}

.empty {
  margin-top: 18rpx;
  color: #64748b;
  font-size: 24rpx;
  text-align: center;
}
</style>
