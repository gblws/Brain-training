<template>
  <view class="page">
    <view class="panel">
      <view class="status">
        <text class="stat">Round {{ currentRound }}/{{ totalRounds }}</text>
        <view class="help-btn" @click="goGuide">?</view>
      </view>

      <view class="focus-card">
        <text class="focus-word" :style="{ color: currentColor }">{{ currentWord }}</text>
      </view>

      <view class="actions">
        <button
          class="choice"
          v-for="item in colorOptions"
          :key="item.value"
          :style="{ borderColor: item.value, color: item.value }"
          :disabled="finished || resultVisible"
          @click="answer(item.value)"
        >
          {{ item.label }}
        </button>
      </view>
    </view>

    <view class="result-mask" v-if="resultVisible">
      <view class="result-card">
        <text class="result-title">Result</text>
        <text class="result-line">Time: {{ resultTime }}s</text>
        <text class="result-line">Accuracy: {{ resultAccuracy }}%</text>
        <button class="again-btn" @click="reset">Try Again</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { post } from '../../utils/request.js';

const totalRounds = 20;
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
const startTime = ref(0);
const resultVisible = ref(false);
const resultTime = ref('0.0');
const resultAccuracy = ref(0);

const goGuide = () => {
  uni.navigateTo({ url: '/pages/guides/intro?game=stroop' });
};

const nextQuestion = () => {
  const wordIndex = Math.floor(Math.random() * words.length);
  const colorIndex = Math.floor(Math.random() * colorOptions.length);
  currentWord.value = words[wordIndex];
  currentColor.value = colorOptions[colorIndex].value;
};

const finishChallenge = async () => {
  finished.value = true;
  const accuracy = rightCount.value / totalRounds;
  const elapsedSeconds = ((Date.now() - startTime.value) / 1000).toFixed(1);
  resultTime.value = elapsedSeconds;
  resultAccuracy.value = Math.round(accuracy * 100);

  try {
    await post('/api/v1/game/submit', {
      gameName: 'Stroop Challenge',
      score: Math.round(accuracy * 100),
      accuracy,
      createTime: new Date().toISOString()
    });
  } catch (error) {
    console.error('stroop submit error', error);
  } finally {
    // Always show result popup even if submit API fails.
    resultVisible.value = true;
  }
};

const answer = (selectedColor) => {
  if (finished.value) return;
  if (!started.value) {
    started.value = true;
    startTime.value = Date.now();
  }

  if (selectedColor === currentColor.value) {
    rightCount.value += 1;
  }
  answeredCount.value += 1;

  if (answeredCount.value >= totalRounds) {
    finishChallenge();
    return;
  }

  currentRound.value = answeredCount.value + 1;
  nextQuestion();
};

const reset = () => {
  currentRound.value = 1;
  rightCount.value = 0;
  answeredCount.value = 0;
  started.value = false;
  finished.value = false;
  startTime.value = 0;
  resultVisible.value = false;
  resultTime.value = '0.0';
  resultAccuracy.value = 0;
  nextQuestion();
};

reset();
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f1f5f9 0%, #e5e7eb 55%, #dbeafe 100%);
}

.panel {
  min-height: 100vh;
  padding: 22rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16rpx;
}

.status {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat {
  padding: 8rpx 16rpx;
  background: rgba(255, 255, 255, 0.86);
  border: 2rpx solid rgba(0, 0, 0, 0.12);
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #111827;
  backdrop-filter: blur(4px);
}

.help-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #ffffff;
  border: 2rpx solid #cbd5e1;
  color: #2563eb;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  line-height: 52rpx;
}

.focus-card {
  background: rgba(255, 255, 255, 0.92);
  border: 2rpx solid rgba(0, 0, 0, 0.12);
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}

.focus-word {
  font-family: Impact, 'Arial Black', sans-serif;
  font-size: 108rpx;
  line-height: 1;
}

.actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10rpx;
}

.choice {
  margin: 0;
  height: 76rpx;
  line-height: 76rpx;
  border-width: 3rpx;
  border-style: solid;
  border-radius: 12rpx;
  background: #ffffff;
  font-family: 'Arial Black', sans-serif;
  font-size: 26rpx;
}

.result-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.result-card {
  width: 76%;
  background: #ffffff;
  border-radius: 18rpx;
  padding: 28rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  align-items: center;
}

.result-title {
  font-family: 'Arial Black', sans-serif;
  font-size: 42rpx;
  color: #111827;
}

.result-line {
  font-size: 28rpx;
  color: #374151;
}

.again-btn {
  margin-top: 8rpx;
  width: 100%;
  background: #111827;
  color: #ffffff;
  border-radius: 12rpx;
}

@media (min-width: 900px) {
  .panel {
    max-width: 900px;
    margin: 0 auto;
    padding-bottom: 30rpx;
  }

  .actions {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}
</style>
