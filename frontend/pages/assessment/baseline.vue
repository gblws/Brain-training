<template>
  <view class="page">
    <view class="header-card">
      <view class="title-line">
        <text class="title">认知基准测试</text>
      </view>
      <view class="desc-line">
        <text class="desc">依次完成下列测试以获得六位数据图</text>
      </view>
    </view>

    <view class="task-list">
      <view class="task-item" v-for="task in tasks" :key="task.key">
        <view class="task-main">
          <text class="task-name">{{ task.name }}</text>
          <text class="task-score" :class="{ done: taskScores[task.key] !== null }">
            {{ taskScores[task.key] === null ? '未完成' : '得分 ' + taskScores[task.key] }}
          </text>
        </view>
        <button class="task-btn" :disabled="!isLoggedIn" @click="startTask(task)">开始</button>
      </view>
    </view>

    <button class="submit-btn" :disabled="!isLoggedIn || !canSubmit || submitting" @click="submitBaseline">
      {{ submitting ? '提交中...' : '提交基准测试' }}
    </button>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { get, getAuthToken, post } from '../../utils/request.js';

const SCORE_KEY = 'baseline_scores_v1';
const COMPLETE_KEY = 'baseline_completed_v1';
const BASELINE_RESULT_KEY = 'baseline_latest_result_v1';

const submitting = ref(false);
const isLoggedIn = ref(false);
const todayCompleted = ref(false);
const taskScores = ref({
  stroop: null,
  schulte: null,
  memory: null
});

const tasks = [
  { key: 'stroop', name: '斯特鲁普挑战', route: '/pages/games/stroop' },
  { key: 'schulte', name: '舒尔特方格', route: '/pages/games/schulte' },
  { key: 'memory', name: '记忆矩阵', route: '/pages/games/memory' }
];

const canSubmit = computed(() => {
  return Object.values(taskScores.value).every((score) => score !== null);
});

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

const redirectToLogin = (message) => {
  uni.showToast({ title: message, icon: 'none' });
  setTimeout(() => {
    uni.switchTab({ url: '/pages/user/player' });
  }, 250);
};

const ensureLoggedIn = async () => {
  const token = getAuthToken();
  if (!token) {
    isLoggedIn.value = false;
    redirectToLogin('请先登录再进行基准测试');
    return false;
  }
  try {
    await get('/api/v1/auth/me');
    isLoggedIn.value = true;
    return true;
  } catch (_) {
    isLoggedIn.value = false;
    redirectToLogin('登录已失效，请重新登录');
    return false;
  }
};

const checkTodayCompleted = async () => {
  try {
    const latest = await get('/api/v1/baseline/latest');
    todayCompleted.value = !!latest && isTodayCreateTime(latest.createTime);
  } catch (_) {
    todayCompleted.value = false;
  }
};

const loadScores = () => {
  const saved = uni.getStorageSync(SCORE_KEY);
  if (!saved || typeof saved !== 'object') {
    taskScores.value = { stroop: null, schulte: null, memory: null };
    return;
  }

  taskScores.value = {
    stroop: Number.isFinite(Number(saved.stroop)) ? Number(saved.stroop) : null,
    schulte: Number.isFinite(Number(saved.schulte)) ? Number(saved.schulte) : null,
    memory: Number.isFinite(Number(saved.memory)) ? Number(saved.memory) : null
  };
};

const startTask = async (task) => {
  const ok = await ensureLoggedIn();
  if (!ok) return;
  uni.navigateTo({ url: `${task.route}?mode=baseline` });
};

const doSubmitBaseline = async () => {
  try {
    submitting.value = true;
    const data = await post('/api/v1/baseline/submit', {
      stroopScore: taskScores.value.stroop,
      schulteScore: taskScores.value.schulte,
      memoryScore: taskScores.value.memory
    });

    uni.setStorageSync(BASELINE_RESULT_KEY, data);
    uni.setStorageSync(COMPLETE_KEY, true);
    uni.removeStorageSync(SCORE_KEY);
    uni.navigateTo({ url: '/pages/assessment/result' });
  } catch (error) {
    console.error('submit baseline error', error);
  } finally {
    submitting.value = false;
  }
};

const submitBaseline = async () => {
  const ok = await ensureLoggedIn();
  if (!ok) return;
  if (!canSubmit.value || submitting.value) return;

  if (todayCompleted.value) {
    uni.showModal({
      title: '今日已测试',
      content: '再次提交会覆盖今天的基准测试结果，是否继续？',
      confirmText: '继续提交',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          doSubmitBaseline();
        }
      }
    });
    return;
  }

  await doSubmitBaseline();
};

onShow(() => {
  ensureLoggedIn().then(async (ok) => {
    if (!ok) return;
    loadScores();
    await checkTodayCompleted();
  });
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #f0f9ff 0%, #f8fafc 100%);
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.header-card,
.task-list {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
}

.title-line,
.desc-line {
  display: flex;
  justify-content: center;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.desc {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #475569;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.task-item {
  border: 2rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 14rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}

.task-main {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.task-name {
  font-size: 28rpx;
  color: #0f172a;
  font-weight: 600;
}

.task-score {
  font-size: 22rpx;
  color: #ef4444;
}

.task-score.done {
  color: #16a34a;
}

.task-btn {
  margin: 0;
  min-width: 130rpx;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: 10rpx;
  background: #0ea5e9;
  color: #ffffff;
  font-size: 24rpx;
}

.submit-btn {
  background: #111827;
  color: #ffffff;
  border-radius: 12rpx;
}

.submit-btn[disabled] {
  background: #94a3b8;
}
</style>
