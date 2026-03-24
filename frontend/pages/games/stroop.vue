<template>
  <view class="page">
    <view class="panel">
      <view class="status">
        <text class="stat">难度 {{ currentLevelName }}</text>
        <text class="stat">Round {{ currentRound }}/{{ totalRounds }}</text>
        <text class="stat">Left {{ remainingSeconds }}s</text>
        <view class="help-btn" @click="goGuide">?</view>
      </view>
      <view class="focus-card">
        <text class="focus-word" :style="{ color: currentColor }">{{ currentWord }}</text>
      </view>
      <view class="actions">
        <button class="choice" v-for="item in colorOptions" :key="item.value" :style="{ borderColor: item.value, color: item.value }" :disabled="finished || resultVisible" @click="answer(item.value)">
          {{ item.label }}
        </button>
      </view>
    </view>

    <GameResultModal
      :visible="resultVisible"
      :title="feedbackTitle"
      :type="feedbackType"
      :lines="feedbackLines"
      :show-next="feedbackType === 'success'"
      :show-lower="feedbackType !== 'success' && canLowerLevel"
      :show-retry="feedbackType !== 'success'"
      @next="goNextLevel"
      @lower="lowerDifficulty"
      @retry="retryCurrent"
    />
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { post } from '../../utils/request.js';
import GameResultModal from '../../components/GameResultModal.vue';

const BASELINE_SCORE_KEY = 'baseline_scores_v1';
const ADAPTIVE_LEVEL_KEY = 'adaptive_levels_v1';
const MIN_LEVEL = 1;
const MAX_LEVEL = 4;
const LEVEL_CONFIG = {
  1: { name: '简单', rounds: 20, perRoundMs: 3000 },
  2: { name: '普通', rounds: 20, perRoundMs: 2000 },
  3: { name: '困难', rounds: 20, perRoundMs: 1200 },
  4: { name: '噩梦', rounds: 25, perRoundMs: 800 }
};
const colorOptions = [
  { label: 'RED', value: '#e11d48' },
  { label: 'BLUE', value: '#3b82f6' },
  { label: 'GREEN', value: '#22c55e' },
  { label: 'YELLOW', value: '#f59e0b' },
  { label: 'BLACK', value: '#111111' }
];
const words = ['RED', 'BLUE', 'GREEN', 'YELLOW', 'BLACK'];

const currentRound = ref(1);
const currentWord = ref('RED');
const currentColor = ref('#e11d48');
const rightCount = ref(0);
const answeredCount = ref(0);
const started = ref(false);
const finished = ref(false);
const currentLevel = ref(2);
const remainingMs = ref(0);
const sessionStartTime = ref(0);
const resultVisible = ref(false);
const feedbackType = ref('mid');
const feedbackTitle = ref('');
const feedbackLines = ref([]);
const isBaselineMode = ref(false);
let roundTimer = null;

const currentLevelConfig = computed(() => LEVEL_CONFIG[currentLevel.value] || LEVEL_CONFIG[1]);
const currentLevelName = computed(() => (isBaselineMode.value ? '基准' : currentLevelConfig.value.name));
const totalRounds = computed(() => (isBaselineMode.value ? 20 : currentLevelConfig.value.rounds));
const perRoundLimitMs = computed(() => (isBaselineMode.value ? 1500 : currentLevelConfig.value.perRoundMs));
const remainingSeconds = computed(() => (remainingMs.value / 1000).toFixed(1));
const canLowerLevel = computed(() => currentLevel.value > MIN_LEVEL);

const goGuide = () => uni.navigateTo({ url: '/pages/guides/intro?game=stroop' });
const loadLevel = () => {
  const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
  const current = stored && typeof stored === 'object' ? stored : {};
  const raw = Number(current.stroop);
  currentLevel.value = Number.isFinite(raw) ? Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, Math.round(raw))) : 2;
};
const persistLevel = () => {
  const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
  const next = stored && typeof stored === 'object' ? stored : {};
  next.stroop = currentLevel.value;
  uni.setStorageSync(ADAPTIVE_LEVEL_KEY, next);
};
const clearRoundTimer = () => {
  if (roundTimer) clearInterval(roundTimer);
  roundTimer = null;
};
const beginRoundTimer = () => {
  clearRoundTimer();
  remainingMs.value = perRoundLimitMs.value;
  const deadline = Date.now() + perRoundLimitMs.value;
  roundTimer = setInterval(() => {
    const ms = deadline - Date.now();
    remainingMs.value = Math.max(0, ms);
    if (ms <= 0) {
      clearRoundTimer();
      handleRoundResult(false);
    }
  }, 100);
};
const nextQuestion = () => {
  currentWord.value = words[Math.floor(Math.random() * words.length)];
  currentColor.value = colorOptions[Math.floor(Math.random() * colorOptions.length)].value;
  beginRoundTimer();
};
const saveBaselineScore = (score) => {
  const current = uni.getStorageSync(BASELINE_SCORE_KEY);
  const next = current && typeof current === 'object' ? current : {};
  next.stroop = score;
  uni.setStorageSync(BASELINE_SCORE_KEY, next);
};
const openFeedback = (score, accuracyPercent) => {
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
    `本局难度：${currentLevelName.value}`,
    `总轮数：${totalRounds.value}`,
    `正确题数：${rightCount.value}`,
    `正确率：${accuracyPercent}%`,
    `本局得分：${score}`
  ];
  resultVisible.value = true;
};

const finishChallenge = async () => {
  finished.value = true;
  clearRoundTimer();
  const accuracy = rightCount.value / totalRounds.value;
  const score = Math.min(100, Math.max(0, Math.round(accuracy * 100)));
  const accuracyPercent = Math.round(accuracy * 100);
  const durationSeconds = Math.max(1, Math.round((Date.now() - sessionStartTime.value) / 1000));
  try {
    await post('/api/v1/game/submit', {
      gameName: 'Stroop Challenge',
      score,
      accuracy,
      durationSeconds,
      difficultyLevel: isBaselineMode.value ? 0 : currentLevel.value,
      difficultyName: currentLevelName.value,
      createTime: new Date().toISOString()
    });
  } catch (error) {
    console.error('stroop submit error', error);
  }
  if (isBaselineMode.value) {
    saveBaselineScore(score);
    uni.showModal({ title: '基准测试', content: `斯特鲁普挑战得分 ${score}，返回评估页继续下一项。`, showCancel: false, success: () => uni.navigateBack() });
    return;
  }
  openFeedback(score, accuracyPercent);
};

const handleRoundResult = (isCorrect) => {
  if (finished.value || resultVisible.value) return;
  if (!started.value) started.value = true;
  if (isCorrect) rightCount.value += 1;
  answeredCount.value += 1;
  if (answeredCount.value >= totalRounds.value) return finishChallenge();
  currentRound.value = answeredCount.value + 1;
  nextQuestion();
};
const answer = (selectedColor) => {
  if (finished.value || resultVisible.value) return;
  clearRoundTimer();
  handleRoundResult(selectedColor === currentColor.value);
};

const reset = () => {
  clearRoundTimer();
  sessionStartTime.value = Date.now();
  currentRound.value = 1;
  rightCount.value = 0;
  answeredCount.value = 0;
  started.value = false;
  finished.value = false;
  resultVisible.value = false;
  nextQuestion();
};
const retryCurrent = () => reset();
const lowerDifficulty = () => {
  if (currentLevel.value > MIN_LEVEL) {
    currentLevel.value -= 1;
    persistLevel();
  }
  reset();
};
const goNextLevel = () => {
  if (currentLevel.value < MAX_LEVEL) {
    currentLevel.value += 1;
    persistLevel();
  }
  reset();
};

onLoad((query) => {
  isBaselineMode.value = query?.mode === 'baseline';
  if (!isBaselineMode.value) loadLevel();
  reset();
});
onUnmounted(clearRoundTimer);
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #f1f5f9 0%, #e5e7eb 55%, #dbeafe 100%); }
.panel { min-height: 100vh; padding: 22rpx; display: flex; flex-direction: column; justify-content: center; gap: 16rpx; }
.status { display: flex; justify-content: space-between; align-items: center; gap: 8rpx; }
.stat { padding: 8rpx 12rpx; background: rgba(255, 255, 255, 0.86); border: 2rpx solid rgba(0, 0, 0, 0.12); border-radius: 999rpx; font-size: 22rpx; color: #111827; }
.help-btn { width: 56rpx; height: 56rpx; border-radius: 50%; background: #fff; border: 2rpx solid #cbd5e1; color: #2563eb; font-size: 32rpx; font-weight: 700; text-align: center; line-height: 52rpx; }
.focus-card { background: rgba(255, 255, 255, 0.92); border: 2rpx solid rgba(0, 0, 0, 0.12); border-radius: 20rpx; padding: 28rpx; display: flex; justify-content: center; }
.focus-word { font-family: Impact, 'Arial Black', sans-serif; font-size: 108rpx; line-height: 1; }
.actions { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10rpx; }
.choice { margin: 0; height: 76rpx; line-height: 76rpx; border-width: 3rpx; border-style: solid; border-radius: 12rpx; background: #fff; font-family: 'Arial Black', sans-serif; font-size: 26rpx; }
@media (min-width: 900px) { .panel { max-width: 900px; margin: 0 auto; padding-bottom: 30rpx; } .actions { grid-template-columns: repeat(5, minmax(0, 1fr)); } }
</style>
