<template>
  <view class="page">
    <view class="hud">
      <view class="pill">
        <text class="pill-label">Target</text>
        <text class="pill-value">{{ nextNumber }}</text>
      </view>
      <view class="pill">
        <text class="pill-label">Time</text>
        <text class="pill-value">{{ elapsedSeconds }}s</text>
      </view>
      <view class="help-btn" @click="goGuide">?</view>
      <button class="restart" @click="initGame">Restart</button>
    </view>

    <view class="board">
      <view class="grid">
        <view
          class="cell"
          :class="[colorClass(num)]"
          v-for="num in numbers"
          :key="num"
          @click="handleClick(num)"
        >
          <text class="num">{{ num }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue';
import { post } from '../../utils/request.js';

const numbers = ref([]);
const nextNumber = ref(1);
const started = ref(false);
const startTime = ref(0);
const elapsed = ref(0);
let timer = null;

const elapsedSeconds = computed(() => (elapsed.value / 1000).toFixed(1));
const palette = ['c1', 'c2', 'c3', 'c4', 'c5', 'c6'];

const colorClass = (num) => palette[(num - 1) % palette.length];

const goGuide = () => {
  uni.navigateTo({ url: '/pages/guides/intro?game=schulte' });
};

const shuffle = (arr) => {
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const randomIndex = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[randomIndex]] = [arr[randomIndex], arr[i]];
  }
  return arr;
};

const initGame = () => {
  const base = Array.from({ length: 25 }, (_, i) => i + 1);
  numbers.value = shuffle(base);
  nextNumber.value = 1;
  started.value = false;
  startTime.value = 0;
  elapsed.value = 0;
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
};

const startTimer = () => {
  startTime.value = Date.now();
  timer = setInterval(() => {
    elapsed.value = Date.now() - startTime.value;
  }, 100);
};

const finishGame = async () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }

  const seconds = Number(elapsedSeconds.value);
  const score = Math.max(0, Math.round(100 - seconds * 2));

  await post('/api/v1/game/submit', {
    gameName: 'Schulte Grid',
    score,
    accuracy: 1,
    createTime: new Date().toISOString()
  });

  uni.showModal({
    title: 'Result',
    content: `Time ${seconds}s, Score ${score}`,
    showCancel: false
  });
};

const handleClick = (num) => {
  if (!started.value) {
    started.value = true;
    startTimer();
  }

  if (num !== nextNumber.value) return;

  nextNumber.value += 1;
  if (nextNumber.value > 25) {
    finishGame();
  }
};

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

initGame();
</script>

<style scoped>
.page {
  --bg: #ececec;
  --board: #ffffff;
  --line: #18181b;
  min-height: 100vh;
  padding: 22rpx;
  background: radial-gradient(circle at 20% 0%, #f7f7f7, var(--bg) 60%);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.hud {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.pill {
  flex: 1;
  background: #ffffff;
  border: 3rpx solid #d0d0d0;
  border-radius: 14rpx;
  padding: 10rpx 14rpx;
  display: flex;
  flex-direction: column;
}

.pill-label {
  color: #71717a;
  font-size: 20rpx;
}

.pill-value {
  color: #111827;
  font-family: 'Arial Black', Impact, sans-serif;
  font-size: 36rpx;
  line-height: 1;
}

.restart {
  width: 130rpx;
  height: 78rpx;
  line-height: 78rpx;
  margin: 0;
  background: #111827;
  color: #fff;
  border-radius: 14rpx;
  font-size: 24rpx;
}

.help-btn {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: #f8fafc;
  border: 2rpx solid #cbd5e1;
  color: #2563eb;
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  line-height: 58rpx;
}

.board {
  background: var(--board);
  border: 6rpx solid var(--line);
  box-shadow: 0 10rpx 22rpx rgba(0, 0, 0, 0.12);
}

.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
}

.cell {
  aspect-ratio: 1 / 1;
  border-right: 3rpx solid var(--line);
  border-bottom: 3rpx solid var(--line);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  transition: transform 120ms ease, background-color 120ms ease, opacity 120ms ease;
}

.cell:nth-child(5n) {
  border-right: none;
}

.cell:nth-last-child(-n + 5) {
  border-bottom: none;
}

.num {
  font-family: 'Arial Black', Impact, sans-serif;
  font-size: 94rpx;
  line-height: 1;
  letter-spacing: -1rpx;
}

.c1 .num { color: #2563eb; }
.c2 .num { color: #f7b500; }
.c3 .num { color: #111827; }
.c4 .num { color: #dc2626; }
.c5 .num { color: #8b5cf6; }
.c6 .num { color: #0ea5e9; }

@media (min-width: 900px) {
  .page {
    max-width: 760px;
    margin: 0 auto;
  }
}
</style>
