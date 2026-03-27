<template>
  <view class="page">
    <view class="hud">
      <view class="pill">
        <text class="pill-label">难度</text>
        <text class="pill-value">{{ difficultyLabel }}</text>
      </view>
      <view class="pill">
        <text class="pill-label">目标</text>
        <text class="pill-value">{{ targetLabel }}</text>
      </view>
      <view class="pill">
        <text class="pill-label">用时</text>
        <text class="pill-value">{{ elapsedSeconds }}s</text>
      </view>
      <view class="help-btn" @click="goGuide">?</view>
      <button class="restart" @click="initGame">重新开始</button>
    </view>

    <view class="board">
      <view class="grid" :class="`size-${activeConfig.gridSize}`" :style="{ gridTemplateColumns: `repeat(${activeConfig.gridSize}, 1fr)` }">
        <view class="cell" :class="[colorClass(num)]" v-for="num in numbers" :key="num" @click="handleClick(num)">
          <text class="num">{{ num }}</text>
        </view>
      </view>
    </view>

    <view class="result-mask" v-if="resultVisible">
      <view class="result-card">
        <text class="result-title" :class="feedbackClass">{{ feedbackTitle }}</text>
        <text class="result-line" v-for="(line, idx) in feedbackLines" :key="idx">{{ line }}</text>
        <button class="pill-action success-action" v-if="feedbackType === 'success'" @click="goNextLevel">
          <text class="pill-icon up">↑</text>
          <text class="pill-label-text">进入下一个难度</text>
        </button>
        <button class="pill-action" v-if="feedbackType !== 'success' && canLowerLevel" @click="lowerDifficulty">
          <text class="pill-icon down">↓</text>
          <text class="pill-label-text">降低难度</text>
        </button>
        <button class="pill-action" v-if="feedbackType !== 'success'" @click="retryCurrent">
          <text class="pill-icon retry"></text>
          <text class="pill-label-text">再来一次</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { post } from '../../utils/request.js';

const BASELINE_SCORE_KEY = 'baseline_scores_v1';
const ADAPTIVE_LEVEL_KEY = 'adaptive_levels_v1';
const MIN_LEVEL = 1;
const MAX_LEVEL = 4;
const LEVEL_CONFIG = {
  1: { name: '简单', gridSize: 4, colorful: true, targetSeconds: 12, timePenaltyFactor: 3, wrongPenaltyWeight: 4 },
  2: { name: '普通', gridSize: 5, colorful: false, targetSeconds: 20, timePenaltyFactor: 2, wrongPenaltyWeight: 5 },
  3: { name: '困难', gridSize: 5, colorful: true, targetSeconds: 20, timePenaltyFactor: 2, wrongPenaltyWeight: 5 },
  4: { name: '噩梦', gridSize: 6, colorful: true, targetSeconds: 28, timePenaltyFactor: 2, wrongPenaltyWeight: 6 }
};
const BASELINE_CONFIG = { name: '基准', gridSize: 5, colorful: true, targetSeconds: 15, timePenaltyFactor: 2, wrongPenaltyWeight: 5 };
const palette = ['c1', 'c2', 'c3', 'c4', 'c5', 'c6'];

const numbers = ref([]);
const nextNumber = ref(1);
const started = ref(false);
const finished = ref(false);
const startTime = ref(0);
const elapsed = ref(0);
const wrongCount = ref(0);
const isBaselineMode = ref(false);
const currentLevel = ref(2);
const resultVisible = ref(false);
const feedbackType = ref('mid');
const feedbackTitle = ref('');
const feedbackLines = ref([]);
let timer = null;

const elapsedSeconds = computed(() => (elapsed.value / 1000).toFixed(1));
const activeConfig = computed(() => (isBaselineMode.value ? BASELINE_CONFIG : LEVEL_CONFIG[currentLevel.value] || LEVEL_CONFIG[2]));
const maxNumber = computed(() => activeConfig.value.gridSize * activeConfig.value.gridSize);
const targetLabel = computed(() => (nextNumber.value > maxNumber.value ? '完成' : nextNumber.value));
const difficultyLabel = computed(() => (isBaselineMode.value ? '基准' : activeConfig.value.name));
const canLowerLevel = computed(() => currentLevel.value > MIN_LEVEL);
const feedbackClass = computed(() => (feedbackType.value === 'fail' ? 'fail' : feedbackType.value === 'success' ? 'success' : 'mid'));

const colorClass = (num) => (activeConfig.value.colorful ? palette[(num - 1) % palette.length] : 'mono');
const goGuide = () => uni.navigateTo({ url: '/pages/guides/intro?game=schulte' });

const loadLevel = () => {
  const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
  const current = stored && typeof stored === 'object' ? stored : {};
  const raw = Number(current.schulte);
  currentLevel.value = Number.isFinite(raw) ? Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, Math.round(raw))) : 2;
};
const persistLevel = () => {
  const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
  const next = stored && typeof stored === 'object' ? stored : {};
  next.schulte = currentLevel.value;
  uni.setStorageSync(ADAPTIVE_LEVEL_KEY, next);
};

const shuffle = (arr) => {
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
};
const clearTimer = () => {
  if (timer) clearInterval(timer);
  timer = null;
};
const initGame = () => {
  numbers.value = shuffle(Array.from({ length: maxNumber.value }, (_, i) => i + 1));
  nextNumber.value = 1;
  started.value = false;
  finished.value = false;
  resultVisible.value = false;
  startTime.value = 0;
  elapsed.value = 0;
  wrongCount.value = 0;
  clearTimer();
};
const startTimer = () => {
  startTime.value = Date.now();
  timer = setInterval(() => {
    elapsed.value = Date.now() - startTime.value;
  }, 100);
};
const saveBaselineScore = (score) => {
  const current = uni.getStorageSync(BASELINE_SCORE_KEY);
  const next = current && typeof current === 'object' ? current : {};
  next.schulte = score;
  uni.setStorageSync(BASELINE_SCORE_KEY, next);
};
const openFeedback = (score, seconds) => {
  if (score < 60) {
    feedbackType.value = 'fail';
    feedbackTitle.value = '挑战失败';
  } else if (score < 80) {
    feedbackType.value = 'mid';
    feedbackTitle.value = '继续加油';
  } else {
    feedbackType.value = 'success';
    feedbackTitle.value = '挑战成功';
  }
  feedbackLines.value = [
    `本局难度：${activeConfig.value.name}`,
    `本局用时：${seconds}s`,
    `错误次数：${wrongCount.value}`,
    `本局得分：${score}`
  ];
  resultVisible.value = true;
};

const finishGame = async () => {
  if (finished.value) return;
  finished.value = true;
  clearTimer();
  const seconds = Number(elapsedSeconds.value);
  const c = activeConfig.value;
  const score = Math.min(100, Math.max(0, Math.round(100 - Math.max(0, seconds - c.targetSeconds) * c.timePenaltyFactor - wrongCount.value * c.wrongPenaltyWeight)));
  const accuracy = maxNumber.value / (maxNumber.value + wrongCount.value || 1);

  await post('/api/v1/game/submit', {
    gameName: 'Schulte Grid',
    score,
    accuracy,
    durationSeconds: Math.max(1, Math.round(seconds)),
    difficultyLevel: isBaselineMode.value ? 0 : currentLevel.value,
    difficultyName: difficultyLabel.value,
    createTime: new Date().toISOString()
  });

  if (isBaselineMode.value) {
    saveBaselineScore(score);
    uni.showModal({ title: '基准测试', content: `舒尔特方格得分 ${score}，返回评估页继续下一项。`, showCancel: false, success: () => uni.navigateBack() });
    return;
  }
  openFeedback(score, seconds);
};

const retryCurrent = () => initGame();
const lowerDifficulty = () => {
  if (currentLevel.value > MIN_LEVEL) {
    currentLevel.value -= 1;
    persistLevel();
  }
  initGame();
};
const goNextLevel = () => {
  if (currentLevel.value < MAX_LEVEL) {
    currentLevel.value += 1;
    persistLevel();
  }
  initGame();
};

const handleClick = (num) => {
  if (finished.value || resultVisible.value) return;
  if (!started.value) {
    started.value = true;
    startTimer();
  }
  if (num !== nextNumber.value) {
    wrongCount.value += 1;
    return;
  }
  nextNumber.value += 1;
  if (nextNumber.value > maxNumber.value) finishGame();
};

onLoad((query) => {
  isBaselineMode.value = query?.mode === 'baseline';
  if (!isBaselineMode.value) loadLevel();
  initGame();
});
onUnmounted(clearTimer);
</script>

<style scoped>
.page { --bg: #ececec; min-height: 100vh; padding: 22rpx; background: radial-gradient(circle at 20% 0%, #f7f7f7, var(--bg) 60%); display: flex; flex-direction: column; gap: 16rpx; }
.hud { display: flex; align-items: center; gap: 12rpx; }
.pill { flex: 1; background: #fff; border: 3rpx solid #d0d0d0; border-radius: 14rpx; padding: 10rpx 14rpx; display: flex; flex-direction: column; }
.pill-label { color: #71717a; font-size: 20rpx; }
.pill-value { color: #111827; font-family: 'Arial Black', Impact, sans-serif; font-size: 30rpx; line-height: 1.1; }
.restart { width: 130rpx; height: 78rpx; line-height: 78rpx; margin: 0; background: #111827; color: #fff; border-radius: 14rpx; font-size: 24rpx; }
.help-btn { width: 62rpx; height: 62rpx; border-radius: 50%; background: #f8fafc; border: 2rpx solid #cbd5e1; color: #2563eb; font-size: 34rpx; font-weight: 700; text-align: center; line-height: 58rpx; }
.board { background: #f3f7fa; border-radius: 24rpx; padding: 14rpx; box-shadow: 0 10rpx 22rpx rgba(15, 23, 42, 0.08); }
.grid { display: grid; gap: 10rpx; }
.cell { aspect-ratio: 1 / 1; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; background: #fff; box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.1); }
.num { font-family: 'Arial Black', Impact, sans-serif; line-height: 1; letter-spacing: -1rpx; }
.size-4 .num { font-size: 96rpx; } .size-5 .num { font-size: 82rpx; } .size-6 .num { font-size: 66rpx; }
.mono .num { color: #111827; } .c1 .num { color: #2563eb; } .c2 .num { color: #f7b500; } .c3 .num { color: #111827; } .c4 .num { color: #dc2626; } .c5 .num { color: #8b5cf6; } .c6 .num { color: #0ea5e9; }
.result-mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); display: flex; align-items: center; justify-content: center; z-index: 20; }
.result-card { width: 80%; background: #fff; border-radius: 16rpx; padding: 26rpx 22rpx; display: flex; flex-direction: column; gap: 10rpx; align-items: center; }
.result-title { font-size: 42rpx; font-weight: 800; } .result-title.fail { color: #dc2626; } .result-title.mid { color: #f59e0b; } .result-title.success { color: #16a34a; }
.result-line { font-size: 26rpx; color: #374151; }
.action-btn { width: 100%; margin-top: 6rpx; border-radius: 12rpx; }
.action-btn.primary { background: #111827; color: #fff; }
.pill-action { margin: 8rpx auto 0; width: 68%; height: 58rpx; border-radius: 999rpx; background: #f6f1e7; border: none; color: #111827; padding: 0 10rpx; display: flex; align-items: center; position: relative; }
.pill-icon { width: 36rpx; height: 36rpx; border-radius: 50%; background: #2f354d; border: 2rpx solid rgba(255, 255, 255, 0.18); text-align: center; line-height: 32rpx; font-size: 20rpx; font-weight: 700; color: #f87171; flex-shrink: 0; z-index: 1; }
.pill-icon.retry { position: relative; font-size: 0; color: transparent; }
.pill-icon.up { color: #22c55e; }
.pill-icon.retry::before { content: ''; position: absolute; left: 50%; top: 50%; width: 20rpx; height: 20rpx; transform: translate(-50%, -50%); border-radius: 50%; border: 3rpx solid #f59e0b; }
.pill-icon.retry::after { content: ''; position: absolute; left: 50%; top: 50%; width: 6rpx; height: 6rpx; transform: translate(-50%, -50%); border-radius: 50%; background: #f59e0b; }
.pill-label-text { position: absolute; left: 0; right: 0; text-align: center; font-size: 26rpx; font-weight: 700; color: #111827; line-height: 58rpx; }
.success-action .pill-label-text { color: #14532d; }
@media (min-width: 900px) { .page { max-width: 760px; margin: 0 auto; } }
</style>
