<template>
  <view class="page">
    <view class="panel">
      <text class="title">{{ guide.title }}</text>
      <text class="desc">{{ guide.desc }}</text>
    </view>

    <view class="panel">
      <text class="sub-title">Demo</text>

      <view v-if="currentGameId === 'schulte'" class="demo schulte">
        <view class="mini-grid schulte-grid">
          <view
            class="mini-cell schulte-cell"
            :class="{ active: num === schulteStep + 1 }"
            v-for="(num, idx) in schulteOrder"
            :key="idx"
          >
            <text class="mini-num" :class="{ blue: num % 2 === 0 || num === 1 }">{{ num }}</text>
          </view>
        </view>
        <view class="cursor schulte-cursor" :style="schulteCursorStyle" />
      </view>

      <view v-else-if="currentGameId === 'stroop'" class="demo stroop">
        <view class="stroop-card">
          <text class="stroop-word" :style="{ color: currentStroopRound.color }">{{ currentStroopRound.word }}</text>
        </view>
        <view class="stroop-actions">
          <view
            class="stroop-btn"
            :class="{ active: idx === currentStroopRound.correctIndex }"
            v-for="(item, idx) in stroopOptions"
            :key="item.label"
            :style="{ color: item.color, borderColor: idx === currentStroopRound.correctIndex ? item.color : '#cbd5e1' }"
          >
            {{ item.label }}
          </view>
        </view>
        <view class="cursor stroop-cursor" :style="stroopCursorStyle" />
        <text class="stroop-round">Demo {{ stroopStep + 1 }}/3</text>
      </view>

      <view v-else class="demo memory">
        <view class="mini-grid">
          <view class="mini-cell" :class="memoryCellClass(idx)" v-for="idx in 9" :key="idx" />
        </view>
        <view v-if="memoryCursorVisible" class="cursor memory-cursor" :style="memoryCursorStyle" />
      </view>
    </view>

    <view class="panel">
      <text class="sub-title">How To Play</text>
      <text class="step" v-for="(item, index) in guide.steps" :key="index">
        {{ index + 1 }}. {{ item }}
      </text>
    </view>

    <view class="panel">
      <text class="sub-title">Tips</text>
      <text class="tip">{{ guide.tip }}</text>
    </view>

    <button class="start-btn" @click="startGame">Start This Game</button>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

const gameMap = {
  schulte: {
    title: 'Schulte Grid',
    route: '/pages/games/schulte',
    desc: 'Tap numbers in ascending order as fast as possible.',
    steps: [
      'Find and tap number 1.',
      'Continue tapping 2, 3, 4... in order.',
      'Finish at number 25.'
    ],
    tip: 'Focus your eyes on the center and use peripheral vision.'
  },
  stroop: {
    title: 'Stroop Challenge',
    route: '/pages/games/stroop',
    desc: 'Judge the font color, not the text meaning.',
    steps: [
      'Read the center word quickly.',
      'Ignore the word content.',
      'Tap the button matching the display color.'
    ],
    tip: 'Prioritize color recognition speed over reading.'
  },
  memory: {
    title: 'Memory Matrix',
    route: '/pages/games/memory',
    desc: 'Remember highlighted cells and reproduce them.',
    steps: [
      'Observe highlighted cells during preview.',
      'Wait for the grid to hide.',
      'Tap the same cells to pass the level.'
    ],
    tip: 'Group cells into chunks for easier recall.'
  }
};

const guide = ref(gameMap.schulte);
const currentGameId = ref('schulte');
const schulteOrder = ref([1, 2, 3, 4]);
const schulteStep = ref(0);
const stroopOptions = [
  { label: 'RED', color: '#e11d48' },
  { label: 'BLUE', color: '#2563eb' },
  { label: 'GREEN', color: '#16a34a' }
];
const stroopRounds = ref([]);
const stroopStep = ref(0);
const memoryTargets = ref([]);
const memoryClicks = ref([]);
const memoryPhase = ref('preview');
const memoryCursorVisible = ref(false);
const memoryClickIndex = ref(0);
let schulteTimer = null;
let stroopTimer = null;
const memoryTimers = [];

const shuffle = (arr) => {
  const list = [...arr];
  for (let i = list.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [list[i], list[j]] = [list[j], list[i]];
  }
  return list;
};

const resetSchulteDemo = () => {
  schulteOrder.value = shuffle([1, 2, 3, 4]);
  schulteStep.value = 0;
};

const schulteCursorStyle = computed(() => {
  const targetNum = schulteStep.value + 1;
  const idx = schulteOrder.value.findIndex((num) => num === targetNum);
  const row = Math.floor(idx / 2);
  const col = idx % 2;
  return {
    left: col === 0 ? '28%' : '73%',
    top: row === 0 ? '28%' : '74%'
  };
});

const startSchulteDemo = () => {
  clearInterval(schulteTimer);
  resetSchulteDemo();
  schulteTimer = setInterval(() => {
    if (schulteStep.value >= 3) {
      clearInterval(schulteTimer);
      schulteTimer = null;
      return;
    }
    schulteStep.value += 1;
  }, 2000);
};

const buildStroopRounds = () => {
  const rounds = [];
  for (let i = 0; i < 3; i += 1) {
    const wordIndex = Math.floor(Math.random() * stroopOptions.length);
    const colorIndex = Math.floor(Math.random() * stroopOptions.length);
    rounds.push({
      word: stroopOptions[wordIndex].label,
      color: stroopOptions[colorIndex].color,
      correctIndex: colorIndex
    });
  }
  return rounds;
};

const currentStroopRound = computed(() => {
  return stroopRounds.value[stroopStep.value] || {
    word: 'BLUE',
    color: '#2563eb',
    correctIndex: 1
  };
});

const stroopCursorStyle = computed(() => {
  const leftMap = ['19%', '49%', '80%'];
  return {
    top: '172rpx',
    left: leftMap[currentStroopRound.value.correctIndex] || '49%'
  };
});

const startStroopDemo = () => {
  clearInterval(stroopTimer);
  stroopRounds.value = buildStroopRounds();
  stroopStep.value = 0;
  stroopTimer = setInterval(() => {
    if (stroopStep.value >= 2) {
      clearInterval(stroopTimer);
      stroopTimer = null;
      return;
    }
    stroopStep.value += 1;
  }, 2000);
};

const clearMemoryTimers = () => {
  while (memoryTimers.length) {
    clearTimeout(memoryTimers.pop());
  }
};

const pickMemoryTargets = () => {
  const pool = [...Array(9).keys()];
  for (let i = pool.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, 3);
};

const startMemoryDemo = () => {
  clearMemoryTimers();
  memoryTargets.value = pickMemoryTargets();
  memoryClicks.value = [];
  memoryPhase.value = 'preview';
  memoryCursorVisible.value = false;
  memoryClickIndex.value = 0;

  // Keep highlighted cells visible for 2s (same as real game), then hide.
  memoryTimers.push(setTimeout(() => {
    memoryPhase.value = 'hidden';
  }, 2000));

  // Wait 2s after hiding, then start mouse replay clicks.
  memoryTimers.push(setTimeout(() => {
    memoryPhase.value = 'click';
    memoryCursorVisible.value = true;
    memoryClickIndex.value = 0;
    const stepClick = () => {
      if (memoryClickIndex.value >= memoryTargets.value.length) {
        memoryCursorVisible.value = false;
        memoryPhase.value = 'done';
        return;
      }
      const idx = memoryTargets.value[memoryClickIndex.value];
      memoryClicks.value = [...memoryClicks.value, idx];
      memoryClickIndex.value += 1;
      memoryTimers.push(setTimeout(stepClick, 700));
    };
    stepClick();
  }, 4000));
};

const memoryCellClass = (displayIndex) => {
  const idx = displayIndex - 1;
  if (memoryPhase.value === 'preview' && memoryTargets.value.includes(idx)) return 'flash';
  if ((memoryPhase.value === 'click' || memoryPhase.value === 'done') && memoryClicks.value.includes(idx)) return 'flash';
  return '';
};

const memoryCursorStyle = computed(() => {
  const target = memoryTargets.value[Math.min(memoryClickIndex.value, memoryTargets.value.length - 1)] ?? 0;
  const row = Math.floor(target / 3);
  const col = target % 3;
  const leftMap = ['17%', '50%', '82%'];
  const topMap = ['32%', '58%', '84%'];
  return {
    left: leftMap[col],
    top: topMap[row]
  };
});

onLoad((query) => {
  const gameId = query?.game;
  if (gameId && gameMap[gameId]) {
    currentGameId.value = gameId;
    guide.value = gameMap[gameId];
  }
  if (currentGameId.value === 'schulte') {
    startSchulteDemo();
  } else if (currentGameId.value === 'stroop') {
    startStroopDemo();
  } else if (currentGameId.value === 'memory') {
    startMemoryDemo();
  }
});

watch(currentGameId, (val) => {
  if (val === 'schulte') {
    startSchulteDemo();
    clearInterval(stroopTimer);
    clearMemoryTimers();
  } else if (val === 'stroop') {
    clearInterval(schulteTimer);
    startStroopDemo();
    clearMemoryTimers();
  } else if (val === 'memory') {
    clearInterval(schulteTimer);
    clearInterval(stroopTimer);
    startMemoryDemo();
  } else {
    clearInterval(schulteTimer);
    clearInterval(stroopTimer);
    clearMemoryTimers();
  }
});

onUnmounted(() => {
  clearInterval(schulteTimer);
  clearInterval(stroopTimer);
  clearMemoryTimers();
});

const startGame = () => {
  uni.navigateTo({ url: guide.value.route });
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  background: #f5f7fb;
}

.panel {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
}

.sub-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}

.desc,
.step,
.tip {
  font-size: 24rpx;
  line-height: 1.6;
  color: #4b5563;
}

.start-btn {
  margin-top: 8rpx;
  background: #2aa4f4;
  color: #ffffff;
  border-radius: 12rpx;
}

.demo {
  position: relative;
  background: #f8fafc;
  border: 2rpx dashed #cbd5e1;
  border-radius: 12rpx;
  padding: 16rpx;
  min-height: 200rpx;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8rpx;
}

.schulte-grid {
  grid-template-columns: repeat(2, 1fr);
  gap: 10rpx;
}

.mini-cell {
  aspect-ratio: 1/1;
  background: #e2e8f0;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mini-num {
  font-size: 62rpx;
  line-height: 1;
  font-family: 'Arial Black', Impact, sans-serif;
  color: #334155;
  font-weight: 600;
}

.mini-num.blue {
  color: #2563eb;
}

.schulte-cell {
  background: #f1f5f9;
}

.schulte-cell.active {
  box-shadow: inset 0 0 0 2rpx #93c5fd;
  animation: pulse 0.7s ease-in-out;
}

.stroop-word {
  display: block;
  text-align: center;
  font-size: 64rpx;
  line-height: 1.1;
  font-family: Impact, 'Arial Black', sans-serif;
}

.stroop-card {
  background: #ffffff;
  border: 2rpx solid #d1d5db;
  border-radius: 12rpx;
  padding: 18rpx 12rpx;
}

.stroop-actions {
  margin-top: 14rpx;
  display: flex;
  gap: 8rpx;
}

.stroop-btn {
  flex: 1;
  text-align: center;
  font-size: 22rpx;
  padding: 10rpx 0;
  border: 2rpx solid #cbd5e1;
  border-radius: 10rpx;
  background: #ffffff;
}

.stroop-btn.active {
  animation: pulse 0.8s ease-in-out;
}

.stroop-round {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #64748b;
  text-align: right;
}

.memory .mini-cell.flash {
  background: #67e8f9;
  animation: pulse 1.2s infinite;
}

.memory-cursor {
  top: 92rpx;
  left: 36%;
}

.cursor {
  position: absolute;
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: #0ea5e9;
  box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.65);
  animation: click 1.2s infinite ease-in-out;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(0.94); }
  100% { transform: scale(1); }
}

@keyframes click {
  0% {
    transform: translate(0, 0) scale(1);
    box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.65);
  }
  45% {
    transform: translate(-6rpx, -6rpx) scale(1.05);
    box-shadow: 0 0 0 14rpx rgba(14, 165, 233, 0);
  }
  100% {
    transform: translate(0, 0) scale(1);
    box-shadow: 0 0 0 0 rgba(14, 165, 233, 0);
  }
}
</style>
