<template>
  <view class="page">
    <view class="help-btn" @click="goGuide">?</view>
    <view class="hero">
      <text class="hero-title">Train your memory and attention</text>
      <text class="hero-sub">Level {{ level }}/10 - {{ gridSize }}x{{ gridSize }}</text>
    </view>

    <view class="board-wrap">
      <view class="tips">
        <text v-if="phase === 'preview'">Watch highlighted cells for 2 seconds</text>
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

      <button class="restart" @click="startLevel">Reset Level</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { post } from '../../utils/request.js';

const maxLevel = 10;
const level = ref(1);
const phase = ref('idle');
const targetSet = ref(new Set());
const selectedSet = ref(new Set());
const wrongSet = ref(new Set());
const roundLocked = ref(false);

const gridSize = computed(() => 4 + Math.floor((level.value - 1) / 3));
const totalCells = computed(() => gridSize.value * gridSize.value);

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

const startLevel = () => {
  targetSet.value = pickUniquePositions(level.value, totalCells.value);
  selectedSet.value = new Set();
  wrongSet.value = new Set();
  roundLocked.value = false;
  phase.value = 'preview';
  setTimeout(() => {
    phase.value = 'answer';
  }, 2000);
};

const cellClass = (displayIndex) => {
  const idx = displayIndex - 1;
  if (wrongSet.value.has(idx)) return 'wrong';
  if (phase.value === 'preview' && targetSet.value.has(idx)) return 'preview';
  if (phase.value === 'answer' && selectedSet.value.has(idx)) return 'selected';
  return '';
};

const submitAndReset = async (score, accuracy, message) => {
  await post('/api/v1/game/submit', {
    gameName: 'Memory Matrix',
    score,
    accuracy,
    createTime: new Date().toISOString()
  });

  uni.showModal({
    title: 'Result',
    content: message,
    showCancel: false,
    success: () => {
      level.value = 1;
      startLevel();
    }
  });
};

const selectCell = (idx) => {
  if (phase.value !== 'answer' || roundLocked.value) return;
  if (selectedSet.value.has(idx)) return;

  // Clicked wrong cell: flash red for 1 second, then repeat same difficulty.
  if (!targetSet.value.has(idx)) {
    roundLocked.value = true;
    wrongSet.value = new Set([idx]);
    setTimeout(() => {
      startLevel();
    }, 1000);
    return;
  }

  const next = new Set(selectedSet.value);
  next.add(idx);
  selectedSet.value = next;

  if (selectedSet.value.size < targetSet.value.size) return;

  if (level.value >= maxLevel) {
    submitAndReset(100, 1, 'Passed all levels');
    return;
  }

  level.value += 1;
  startLevel();
};

startLevel();
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
