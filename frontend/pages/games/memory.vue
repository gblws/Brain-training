<template>
  <view class="page">
    <view class="help-btn" @click="goGuide">?</view>
    <view class="hero">
      <text class="hero-title">Train your memory and attention</text>
      <text class="hero-sub">{{ heroSub }}</text>
    </view>

    <view class="board-wrap">
      <view class="tips">
        <text v-if="phase === 'preview'">Watch highlighted cells for {{ previewSeconds }} seconds</text>
        <text v-else-if="phase === 'answer'">Repeat the pattern ({{ selectedSet.size }}/{{ targetSet.size }})</text>
        <text v-else>Preparing...</text>
      </view>

      <view class="board">
        <view class="grid" :style="{ gridTemplateColumns: `repeat(${gridSize}, 1fr)` }">
          <view
            class="cell"
            :class="cellClass(idx)"
            v-for="idx in totalCells"
            :key="idx"
            @click="selectCell(idx - 1)"
          />
        </view>
      </view>

      <button class="restart" @click="resetGame">{{ isBaselineMode ? 'Restart Test' : 'Reset Level' }}</button>
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
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { post } from '../../utils/request.js';
import GameResultModal from '../../components/GameResultModal.vue';

const BASELINE_SCORE_KEY = 'baseline_scores_v1';
const ADAPTIVE_LEVEL_KEY = 'adaptive_levels_v1';

const baselineTotalRounds = 8;
const normalTotalRounds = 10;
const DIFFICULTY_CONFIG = {
  1: {
    name: '简单',
    previewMs: 2300,
    gridSizes: [3, 3, 3, 4, 4, 4, 4, 5, 5, 5],
    targets: [3, 3, 4, 4, 5, 5, 6, 6, 7, 7]
  },
  2: {
    name: '普通',
    previewMs: 1800,
    gridSizes: [4, 4, 4, 5, 5, 5, 5, 6, 6, 6],
    targets: [4, 4, 5, 5, 6, 6, 7, 7, 8, 8]
  },
  3: {
    name: '困难',
    previewMs: 1400,
    gridSizes: [4, 4, 5, 5, 5, 6, 6, 6, 6, 6],
    targets: [5, 5, 6, 6, 7, 7, 8, 8, 8, 9]
  },
  4: {
    name: '噩梦',
    previewMs: 1000,
    gridSizes: [5, 5, 5, 6, 6, 6, 6, 7, 7, 7],
    targets: [5, 6, 7, 7, 8, 8, 9, 9, 10, 11]
  }
};
const DEFAULT_DIFFICULTY = 2;
const MIN_LEVEL = 1;
const MAX_LEVEL = 4;

const currentRound = ref(1);
const currentLevel = ref(1);
const highestLevel = ref(1);
const errorCount = ref(0);
const successCount = ref(0);
const phase = ref('idle');
const sessionStartTime = ref(0);
const targetSet = ref(new Set());
const selectedSet = ref(new Set());
const wrongSet = ref(new Set());
const roundLocked = ref(false);
const isBaselineMode = ref(false);
const difficulty = ref(DEFAULT_DIFFICULTY);
const resultVisible = ref(false);
const feedbackType = ref('mid');
const feedbackTitle = ref('');
const feedbackLines = ref([]);

const difficultyConfig = computed(() => DIFFICULTY_CONFIG[difficulty.value] || DIFFICULTY_CONFIG[DEFAULT_DIFFICULTY]);
const gridSize = computed(() => {
  if (isBaselineMode.value) {
    return 4 + Math.floor((currentLevel.value - 1) / 3);
  }
  return difficultyConfig.value.gridSizes[currentRound.value - 1] || difficultyConfig.value.gridSizes[difficultyConfig.value.gridSizes.length - 1];
});
const totalCells = computed(() => gridSize.value * gridSize.value);
const previewSeconds = computed(() => ((isBaselineMode.value ? 2000 : difficultyConfig.value.previewMs) / 1000).toFixed(1));
const canLowerLevel = computed(() => currentLevel.value > MIN_LEVEL);
const heroSub = computed(() => {
  if (isBaselineMode.value) {
    return `Round ${currentRound.value}/${baselineTotalRounds} - Level ${currentLevel.value}`;
  }
  return `${difficultyConfig.value.name} | Round ${currentRound.value}/${normalTotalRounds} | Level ${currentLevel.value} - ${gridSize.value}x${gridSize.value}`;
});

const goGuide = () => {
  uni.navigateTo({ url: '/pages/guides/intro?game=memory' });
};

const pickUniquePositions = (count, max) => {
  const pool = Array.from({ length: max }, (_, i) => i);
  for (let i = pool.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return new Set(pool.slice(0, count));
};

const parseDifficulty = (value) => {
  const map = { simple: 1, normal: 2, hard: 3, nightmare: 4 };
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    if (normalized in map) return map[normalized];
  }
  const n = Number(value);
  if (Number.isFinite(n)) {
    const rounded = Math.round(n);
    if (rounded >= 1 && rounded <= 4) return rounded;
  }
  return null;
};

const loadLevel = (queryDifficulty) => {
  const fromQuery = parseDifficulty(queryDifficulty);
  if (fromQuery) {
    currentLevel.value = fromQuery;
    difficulty.value = fromQuery;
    const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
    const next = stored && typeof stored === 'object' ? stored : {};
    next.memory = fromQuery;
    uni.setStorageSync(ADAPTIVE_LEVEL_KEY, next);
    return;
  }
  const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
  const current = stored && typeof stored === 'object' ? stored : {};
  const raw = Number(current.memory);
  if (Number.isFinite(raw)) {
    currentLevel.value = Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, Math.round(raw)));
  } else {
    currentLevel.value = DEFAULT_DIFFICULTY;
  }
  difficulty.value = currentLevel.value;
};

const persistLevel = () => {
  const stored = uni.getStorageSync(ADAPTIVE_LEVEL_KEY);
  const next = stored && typeof stored === 'object' ? stored : {};
  next.memory = currentLevel.value;
  uni.setStorageSync(ADAPTIVE_LEVEL_KEY, next);
};

const startRound = () => {
  const targetCount = isBaselineMode.value
    ? Math.min(currentLevel.value, totalCells.value)
    : Math.min(difficultyConfig.value.targets[currentRound.value - 1] || difficultyConfig.value.targets[difficultyConfig.value.targets.length - 1], totalCells.value);
  targetSet.value = pickUniquePositions(targetCount, totalCells.value);
  selectedSet.value = new Set();
  wrongSet.value = new Set();
  roundLocked.value = false;
  phase.value = 'preview';
  setTimeout(() => {
    phase.value = 'answer';
  }, isBaselineMode.value ? 2000 : difficultyConfig.value.previewMs);
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
    `本局难度：${difficultyConfig.value.name}`,
    `总轮数：${normalTotalRounds}`,
    `成功轮数：${successCount.value}`,
    `正确率：${accuracyPercent}%`,
    `本局得分：${score}`
  ];
  resultVisible.value = true;
};

const cellClass = (displayIndex) => {
  const idx = displayIndex - 1;
  if (wrongSet.value.has(idx)) return 'wrong';
  if (phase.value === 'preview' && targetSet.value.has(idx)) return 'preview';
  if (phase.value === 'answer' && selectedSet.value.has(idx)) return 'selected';
  return '';
};

const saveBaselineScore = (score) => {
  const current = uni.getStorageSync(BASELINE_SCORE_KEY);
  const next = current && typeof current === 'object' ? current : {};
  next.memory = score;
  uni.setStorageSync(BASELINE_SCORE_KEY, next);
};

const submitAndReset = async (score, accuracy, message) => {
  await post('/api/v1/game/submit', {
    gameName: 'Memory Matrix',
    score,
    accuracy,
    durationSeconds: Math.max(1, Math.round((Date.now() - sessionStartTime.value) / 1000)),
    difficultyLevel: isBaselineMode.value ? 0 : difficulty.value,
    difficultyName: isBaselineMode.value ? '基准' : difficultyConfig.value.name,
    createTime: new Date().toISOString()
  });

  if (isBaselineMode.value) {
    saveBaselineScore(score);
    uni.showModal({
      title: '基准测试',
      content: `记忆矩阵得分 ${score}，返回评估页继续下一项。`,
      showCancel: false,
      success: () => {
        uni.navigateBack();
      }
    });
    return;
  }
};

const finishNormalRound = (success) => {
  if (success) {
    successCount.value += 1;
  } else {
    errorCount.value += 1;
  }

  if (currentRound.value >= normalTotalRounds) {
    const accuracy = successCount.value / normalTotalRounds;
    const score = Math.min(100, Math.max(0, Math.round(accuracy * 100)));
    submitAndReset(score, accuracy, '');
    openFeedback(score, Math.round(accuracy * 100));
    return;
  }

  currentRound.value += 1;
  startRound();
};

const finishBaselineRound = (success) => {
  if (success) {
    successCount.value += 1;
    highestLevel.value = Math.max(highestLevel.value, currentLevel.value);
  } else {
    errorCount.value += 1;
  }

  if (currentRound.value >= baselineTotalRounds) {
    const rawScore = highestLevel.value * 12.5;
    const score = Math.min(100, Math.max(0, Math.round(rawScore)));
    const accuracy = successCount.value / baselineTotalRounds;
    const message = `Highest Lv ${highestLevel.value}, Wrong ${errorCount.value}, Score ${score}`;
    submitAndReset(score, accuracy, message);
    return;
  }

  currentRound.value += 1;
  if (success) {
    currentLevel.value += 1;
  }
  startRound();
};

const selectCell = (idx) => {
  if (phase.value !== 'answer' || roundLocked.value || resultVisible.value) return;
  if (selectedSet.value.has(idx)) return;

  // Wrong click ends this round immediately.
  if (!targetSet.value.has(idx)) {
    roundLocked.value = true;
    wrongSet.value = new Set([idx]);
    setTimeout(() => {
      if (isBaselineMode.value) {
        finishBaselineRound(false);
      } else {
        finishNormalRound(false);
      }
    }, 700);
    return;
  }

  const next = new Set(selectedSet.value);
  next.add(idx);
  selectedSet.value = next;

  if (selectedSet.value.size < targetSet.value.size) return;

  if (isBaselineMode.value) {
    finishBaselineRound(true);
    return;
  }

  finishNormalRound(true);
};

onLoad((query) => {
  isBaselineMode.value = query?.mode === 'baseline';
  if (!isBaselineMode.value) {
    loadLevel(query?.difficulty);
  }
  resetGame();
});

const resetGame = () => {
  resultVisible.value = false;
  sessionStartTime.value = Date.now();
  currentRound.value = 1;
  if (isBaselineMode.value) {
    currentLevel.value = 1;
  } else {
    difficulty.value = currentLevel.value;
  }
  highestLevel.value = 1;
  errorCount.value = 0;
  successCount.value = 0;
  startRound();
};

const retryCurrent = () => {
  resetGame();
};

const lowerDifficulty = () => {
  if (currentLevel.value > MIN_LEVEL) {
    currentLevel.value -= 1;
    difficulty.value = currentLevel.value;
    persistLevel();
  }
  resetGame();
};

const goNextLevel = () => {
  if (currentLevel.value < MAX_LEVEL) {
    currentLevel.value += 1;
    difficulty.value = currentLevel.value;
    persistLevel();
  }
  resetGame();
};
</script>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at 18% 10%, rgba(255, 255, 255, 0.12), transparent 28%),
    radial-gradient(circle at 82% 14%, rgba(255, 255, 255, 0.1), transparent 24%),
    linear-gradient(180deg, #3b9cac 0 30%, #6a4d44 30% 100%);
}

.help-btn {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  border: 2rpx solid rgba(59, 130, 246, 0.3);
  color: #2563eb;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  line-height: 54rpx;
  z-index: 2;
}

.hero {
  padding: 46rpx 26rpx 34rpx;
  text-align: center;
  color: #eff6ff;
}

.hero-title {
  font-size: 52rpx;
  line-height: 1.15;
  font-family: 'Trebuchet MS', 'Avenir Next', sans-serif;
  font-weight: 700;
  text-shadow: 0 3rpx 8rpx rgba(0, 0, 0, 0.25);
}

.hero-sub {
  margin-top: 12rpx;
  font-size: 24rpx;
  opacity: 0.95;
}

.board-wrap {
  padding: 0 20rpx 30rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.tips {
  color: #f5f5f4;
  font-size: 24rpx;
  text-align: center;
}

.board {
  background: repeating-linear-gradient(-45deg, #352822 0 6rpx, #2f231e 6rpx 12rpx);
  border: 8rpx solid #2b211d;
  padding: 16rpx;
  box-shadow: 0 16rpx 30rpx rgba(0, 0, 0, 0.28);
}

.grid {
  display: grid;
  gap: 8rpx;
}

.cell {
  aspect-ratio: 1 / 1;
  background: #6d4f45;
  border: 2rpx solid #3e2f2a;
  transition: transform 120ms ease, background-color 120ms ease, box-shadow 120ms ease;
}

.cell.preview {
  background: #5ad0cc;
  box-shadow: inset 0 0 0 2rpx rgba(255, 255, 255, 0.25);
}

.cell.selected {
  background: #49bbb8;
  transform: scale(0.95);
}

.cell.wrong {
  background: #ef4444;
  box-shadow: inset 0 0 0 2rpx rgba(255, 255, 255, 0.28);
}

.restart {
  background: rgba(30, 20, 16, 0.78);
  color: #f8fafc;
  border: 2rpx solid rgba(255, 255, 255, 0.24);
  border-radius: 10rpx;
}

@media (min-width: 900px) {
  .page {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .hero,
  .board-wrap {
    width: min(760px, 100%);
  }
}
</style>
