<template>
  <view class="page">
    <view class="header">
      <text class="header-title">个人中心</text>
      <view class="avatar-wrap" @click="chooseAvatar">
        <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar avatar-placeholder">
          <text class="avatar-placeholder-text">上传头像</text>
        </view>
      </view>
      <text class="avatar-tip">点击头像可本地上传</text>
    </view>

    <view class="card">
      <text class="label">昵称</text>
      <input
        v-model="nickname"
        class="nickname-input"
        type="text"
        maxlength="20"
        placeholder="请输入昵称"
        placeholder-class="placeholder"
      />
      <button class="save-btn" @click="saveProfile">保存昵称</button>
      <text class="hint">当前仅本地存储，后续可接入登录注册与后端联调。</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';

const STORAGE_AVATAR_KEY = 'player_avatar_url';
const STORAGE_NICKNAME_KEY = 'player_nickname';

const avatarUrl = ref('');
const nickname = ref('');

const loadProfile = () => {
  avatarUrl.value = uni.getStorageSync(STORAGE_AVATAR_KEY) || '';
  nickname.value = uni.getStorageSync(STORAGE_NICKNAME_KEY) || '';
};

const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const localPath = res.tempFilePaths?.[0];
      if (!localPath) return;
      avatarUrl.value = localPath;
      uni.setStorageSync(STORAGE_AVATAR_KEY, localPath);
      uni.showToast({ title: '头像已更新', icon: 'none' });
    }
  });
};

const saveProfile = () => {
  const value = nickname.value.trim();
  nickname.value = value;
  uni.setStorageSync(STORAGE_NICKNAME_KEY, value);
  uni.showToast({ title: '昵称已保存', icon: 'none' });
};

loadProfile();
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f3f6fb;
}

.header {
  padding: 38rpx 24rpx 30rpx;
  background: linear-gradient(145deg, #4d84e8 0%, #2f6fdd 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.header-title {
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 600;
}

.avatar-wrap {
  width: 146rpx;
  height: 146rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.9);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.2);
}

.avatar {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
}

.avatar-placeholder-text {
  color: #eaf2ff;
  font-size: 22rpx;
}

.avatar-tip {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
}

.card {
  margin: 22rpx 20rpx 0;
  background: #ffffff;
  border-radius: 18rpx;
  padding: 26rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.06);
}

.label {
  font-size: 28rpx;
  color: #1f2937;
  font-weight: 600;
}

.nickname-input {
  height: 82rpx;
  border-radius: 12rpx;
  border: 2rpx solid #dbe2ef;
  padding: 0 22rpx;
  font-size: 28rpx;
  color: #111827;
  background: #f9fbff;
}

.placeholder {
  color: #94a3b8;
}

.save-btn {
  margin-top: 2rpx;
  background: #2aa4f4;
  color: #ffffff;
  border-radius: 12rpx;
}

.hint {
  font-size: 22rpx;
  line-height: 1.6;
  color: #64748b;
}
</style>
