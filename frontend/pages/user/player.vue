<template>
  <view class="page">
    <view class="bg-orb orb-a"></view>
    <view class="bg-orb orb-b"></view>

    <view v-if="!loggedIn" class="auth-screen">
      <view class="auth-card">
        <view class="auth-field">
          <view class="field-icon user-icon"></view>
          <input
            v-model="username"
            class="auth-input"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-class="placeholder"
          />
        </view>

        <view class="auth-field">
          <view class="field-icon lock-icon"></view>
          <input
            v-model="password"
            class="auth-input"
            type="password"
            maxlength="32"
            placeholder="请输入密码"
            placeholder-class="placeholder"
          />
        </view>

        <view class="auth-field" v-if="authMode === 'register'">
          <view class="field-icon edit-icon"></view>
          <input
            v-model="verifyCode"
            class="auth-input"
            type="text"
            maxlength="6"
            placeholder="请输入验证码"
            placeholder-class="placeholder"
          />
        </view>

        <button class="auth-submit" @click="submitAuth">{{ authMode === 'login' ? '登录' : '立即注册' }}</button>

        <view class="auth-links" :class="{ single: authMode === 'register' }">
          <text class="auth-link" @click="toggleAuthMode">{{ authMode === 'login' ? '注册账号' : '已有账号，去登录' }}</text>
          <text class="auth-link" v-if="authMode === 'login'">忘记密码</text>
        </view>
      </view>
    </view>

    <view v-else class="profile-screen">
      <view class="profile-header">
        <view class="profile-top">
          <view class="avatar-wrap" @click="chooseAvatar">
            <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
            <view v-else class="avatar avatar-placeholder">
              <text class="avatar-placeholder-text">上传头像</text>
            </view>
          </view>
          <view class="profile-main">
            <input
              v-if="editingNickname"
              v-model="editableNickname"
              class="profile-name-input"
              maxlength="20"
              :focus="editingNickname"
              @blur="finishNicknameEdit"
            />
            <text v-else class="profile-name" @click="startNicknameEdit">{{ nickname || '脑力玩家' }}</text>
            <text class="profile-id">账号：{{ profile.username || '-' }}</text>
          </view>
          <text class="profile-bell">🔔</text>
        </view>

        <view class="profile-stats">
          <view class="stat-item" v-for="item in statList" :key="item.label">
            <text class="stat-value">{{ item.value }}</text>
            <text class="stat-label">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="entry-grid">
        <view class="quick-item" v-for="entry in quickList" :key="entry.label" @click="go(entry.path)">
          <view class="quick-logo" :class="entry.iconClass">
            <text class="quick-logo-text">{{ entry.iconText }}</text>
          </view>
          <text class="quick-label">{{ entry.label }}</text>
        </view>
      </view>

      <view class="menu-panel">
        <view class="menu-item" :class="{ danger: item.action === 'logout' }" v-for="item in menuList" :key="item.label" @click="onMenuClick(item)">
          <text class="menu-icon">{{ item.icon }}</text>
          <text class="menu-label">{{ item.label }}</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { clearAuthToken, get, getAuthToken, post, setAuthToken } from '../../utils/request.js';

const authMode = ref('login');
const loggedIn = ref(false);

const username = ref('');
const password = ref('');
const verifyCode = ref('');
const nickname = ref('');
const editableNickname = ref('');
const editingNickname = ref(false);
const avatarUrl = ref('');
const historyCount = ref(0);
const baselineCount = ref(0);

const profile = ref({
  id: null,
  username: '',
  nickname: '',
  avatarUrl: '',
  createTime: null
});

const registerDays = computed(() => {
  const raw = profile.value?.createTime;
  if (!raw) return 0;
  const start = new Date(raw);
  if (Number.isNaN(start.getTime())) return 0;
  const diff = Date.now() - start.getTime();
  return Math.max(1, Math.floor(diff / 86400000) + 1);
});

const statList = computed(() => [
  { label: '训练次数', value: historyCount.value },
  { label: '注册天数', value: registerDays.value },
  { label: '测评次数', value: baselineCount.value }
]);

const quickList = [
  { iconText: '测', iconClass: 'logo-test', label: '认知测试', path: '/pages/assessment/baseline' },
  { iconText: '图', iconClass: 'logo-radar', label: '六维结果', path: '/pages/assessment/result' },
  { iconText: '史', iconClass: 'logo-history', label: '数据分析', path: '/pages/user/analytics' }
];

const menuList = [
  { icon: '📚', label: '基准测试历史', path: '/pages/assessment/history' },
  { icon: '🧾', label: '历史记录', path: '/pages/user/records' },
  { icon: '🤖', label: 'AI分析', path: '/pages/user/ai' },
  { icon: '⚙️', label: '设置' },
  { icon: '⎋', label: '退出登录', action: 'logout' }
];

const applyProfile = (user) => {
  profile.value = user || profile.value;
  nickname.value = user?.nickname || '';
  editableNickname.value = user?.nickname || '';
  avatarUrl.value = user?.avatarUrl || '';
  loggedIn.value = !!user;
};

const loadDashboard = async () => {
  try {
    const history = await get('/api/v1/user/history');
    historyCount.value = Array.isArray(history) ? history.length : 0;
  } catch (_) {
    historyCount.value = 0;
  }
};

const loadBaselineStats = async () => {
  try {
    const list = await get('/api/v1/baseline/history');
    baselineCount.value = Array.isArray(list) ? list.length : 0;
  } catch (_) {
    baselineCount.value = 0;
  }
};

const loadMe = async () => {
  const token = getAuthToken();
  if (!token) return;
  try {
    const user = await get('/api/v1/auth/me');
    applyProfile(user);
    await Promise.all([loadDashboard(), loadBaselineStats()]);
  } catch (_) {
    clearAuthToken();
    loggedIn.value = false;
  }
};

const validateAuth = () => {
  const user = username.value.trim();
  const pass = password.value.trim();
  const code = verifyCode.value.trim();
  if (!/^1[3-9]\d{9}$/.test(user)) {
    uni.showToast({ title: '请输入有效手机号', icon: 'none' });
    return null;
  }
  if (!pass) {
    uni.showToast({ title: '请输入密码', icon: 'none' });
    return null;
  }
  if (authMode.value === 'register' && !code) {
    uni.showToast({ title: '请输入验证码', icon: 'none' });
    return null;
  }
  return { user, pass };
};

const submitAuth = async () => {
  const payload = validateAuth();
  if (!payload) return;

  try {
    let data;
    if (authMode.value === 'login') {
      data = await post('/api/v1/auth/login', {
        username: payload.user,
        password: payload.pass
      });
    } else {
      data = await post('/api/v1/auth/register', {
        username: payload.user,
        password: payload.pass,
        nickname: ''
      });
    }

    setAuthToken(data.token);
    applyProfile(data.user);
    await Promise.all([loadDashboard(), loadBaselineStats()]);
    uni.showToast({ title: authMode.value === 'login' ? '登录成功' : '注册成功', icon: 'none' });
  } catch (_) {}
};

const toggleAuthMode = () => {
  authMode.value = authMode.value === 'login' ? 'register' : 'login';
};

const startNicknameEdit = () => {
  editableNickname.value = nickname.value || '';
  editingNickname.value = true;
};

const finishNicknameEdit = async () => {
  if (!editingNickname.value) return;
  editingNickname.value = false;
  const next = editableNickname.value.trim();
  if (!next || next === nickname.value) return;
  nickname.value = next;
  await saveProfile();
};

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

const go = async (path) => {
  if (!path) return;
  if (path === '/pages/assessment/baseline') {
    try {
      const latest = await get('/api/v1/baseline/latest');
      if (latest && isTodayCreateTime(latest.createTime)) {
        uni.showModal({
          title: '今日已测试',
          content: '再次测试会覆盖今天的基准测试结果，是否继续？',
          confirmText: '继续测试',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: path });
            }
          }
        });
        return;
      }
    } catch (_) {}
  }
  uni.navigateTo({ url: path });
};

const onMenuClick = async (item) => {
  if (item.action === 'logout') {
    await logout();
    return;
  }
  if (item.path) {
    await go(item.path);
  }
};

const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const localPath = res.tempFilePaths?.[0];
      if (!localPath) return;
      avatarUrl.value = localPath;
      try {
        const user = await post('/api/v1/auth/profile', {
          nickname: nickname.value.trim(),
          avatarUrl: localPath
        });
        applyProfile(user);
        uni.showToast({ title: '头像已更新', icon: 'none' });
      } catch (_) {}
    }
  });
};

const saveProfile = async () => {
  if (!loggedIn.value) return;
  try {
    const user = await post('/api/v1/auth/profile', {
      nickname: nickname.value.trim(),
      avatarUrl: avatarUrl.value
    });
    applyProfile(user);
    uni.showToast({ title: '资料已保存', icon: 'none' });
  } catch (_) {}
};

const logout = async () => {
  try {
    await post('/api/v1/auth/logout');
  } catch (_) {}
  clearAuthToken();
  loggedIn.value = false;
  username.value = '';
  password.value = '';
  verifyCode.value = '';
  nickname.value = '';
  editableNickname.value = '';
  editingNickname.value = false;
  avatarUrl.value = '';
  historyCount.value = 0;
  baselineCount.value = 0;
  profile.value = {
    id: null,
    username: '',
    nickname: '',
    avatarUrl: '',
    createTime: null
  };
  uni.showToast({ title: '已退出登录', icon: 'none' });
};

loadMe();
</script>

<style scoped>
.page {
  --bg-main: #d5f2ec;
  --bg-top: #35c9bc;
  --bg-top-2: #6fe1d7;
  min-height: 100vh;
  background: linear-gradient(180deg, #bfece3 0%, var(--bg-main) 55%, #e8f8f3 100%);
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
  right: -120rpx;
  top: 120rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.35), rgba(255, 255, 255, 0.04));
}

.orb-b {
  width: 280rpx;
  height: 280rpx;
  left: -90rpx;
  bottom: 120rpx;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0.03));
}

.auth-screen {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
}

.auth-card {
  width: 80%;
  margin: 0 auto;
}

.auth-field {
  height: 82rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 999rpx;
  padding: 0 26rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.field-icon {
  width: 28rpx;
  height: 28rpx;
  position: relative;
  border-radius: 50%;
  border: 2rpx solid #2f6f68;
}

.auth-input {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: #1f4f49;
}

.placeholder {
  color: #5f7f79;
}

.auth-submit {
  margin-top: 34rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #198b7f;
  font-size: 30rpx;
  font-weight: 700;
  box-shadow: 0 8rpx 18rpx rgba(20, 78, 72, 0.14);
}

.auth-links {
  margin-top: 20rpx;
  display: flex;
  justify-content: space-between;
}

.auth-links.single {
  justify-content: center;
}

.auth-link {
  color: #2e6861;
  font-size: 24rpx;
  font-weight: 500;
}

.user-icon::before {
  content: '';
  position: absolute;
  width: 10rpx;
  height: 10rpx;
  left: 50%;
  top: 4rpx;
  transform: translateX(-50%);
  border-radius: 50%;
  background: #2f6f68;
}

.user-icon::after {
  content: '';
  position: absolute;
  width: 14rpx;
  height: 8rpx;
  left: 50%;
  bottom: 4rpx;
  transform: translateX(-50%);
  border-radius: 8rpx 8rpx 6rpx 6rpx;
  border: 2rpx solid #2f6f68;
  border-top: none;
}

.lock-icon::before {
  content: '';
  position: absolute;
  width: 12rpx;
  height: 8rpx;
  left: 50%;
  top: 3rpx;
  transform: translateX(-50%);
  border: 2rpx solid #2f6f68;
  border-bottom: none;
  border-radius: 8rpx 8rpx 0 0;
}

.lock-icon::after {
  content: '';
  position: absolute;
  width: 12rpx;
  height: 10rpx;
  left: 50%;
  bottom: 4rpx;
  transform: translateX(-50%);
  border-radius: 4rpx;
  background: #2f6f68;
}

.edit-icon::before {
  content: '';
  position: absolute;
  width: 12rpx;
  height: 2rpx;
  left: 50%;
  top: 8rpx;
  transform: translateX(-50%) rotate(-32deg);
  background: #2f6f68;
}

.edit-icon::after {
  content: '';
  position: absolute;
  width: 12rpx;
  height: 2rpx;
  left: 50%;
  bottom: 8rpx;
  transform: translateX(-50%) rotate(-32deg);
  background: #2f6f68;
}

.profile-screen {
  position: relative;
  z-index: 1;
  padding-bottom: 26rpx;
}

.profile-header {
  background: linear-gradient(145deg, var(--bg-top) 0%, var(--bg-top-2) 100%);
  padding: 40rpx 24rpx 28rpx;
  border-radius: 0 0 24rpx 24rpx;
}

.profile-top {
  display: flex;
  align-items: center;
  width: 92%;
  margin: 0 auto;
}

.avatar-wrap {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  border: 3rpx solid rgba(255, 255, 255, 0.9);
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
}

.avatar-placeholder-text {
  color: #ffffff;
  font-size: 20rpx;
}

.profile-main {
  flex: 1;
  padding-left: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.profile-name {
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
}

.profile-name-input {
  height: 52rpx;
  line-height: 52rpx;
  width: 280rpx;
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
  border-bottom: 2rpx solid rgba(255, 255, 255, 0.7);
}

.profile-id {
  color: rgba(255, 255, 255, 0.92);
  font-size: 22rpx;
}

.profile-bell {
  font-size: 30rpx;
  color: #ffffff;
}

.profile-stats {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  text-align: center;
  width: 92%;
  margin-left: auto;
  margin-right: auto;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.stat-value {
  color: #083344;
  font-size: 34rpx;
  font-weight: 700;
}

.stat-label {
  color: rgba(8, 51, 68, 0.72);
  font-size: 22rpx;
}

.entry-grid {
  margin: 18rpx auto 0;
  width: 92%;
  background: #ffffff;
  border-radius: 16rpx;
  padding: 14rpx 10rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 0;
}

.quick-logo {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid transparent;
}

.quick-logo-text {
  font-size: 26rpx;
  font-weight: 700;
}

.quick-logo.logo-test {
  background: #e8f7f3;
  border-color: #b6e7dc;
}

.quick-logo.logo-test .quick-logo-text {
  color: #0f766e;
}

.quick-logo.logo-radar {
  background: #ecf4ff;
  border-color: #c8dcff;
}

.quick-logo.logo-radar .quick-logo-text {
  color: #2563eb;
}

.quick-logo.logo-history {
  background: #fff4ea;
  border-color: #ffdcbc;
}

.quick-logo.logo-history .quick-logo-text {
  color: #c2410c;
}

.quick-label {
  font-size: 22rpx;
  color: #334155;
}

.menu-panel {
  margin: 14rpx auto 0;
  width: 92%;
  background: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.menu-item {
  height: 86rpx;
  display: flex;
  align-items: center;
  padding: 0 20rpx;
  border-bottom: 2rpx solid #f1f5f9;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  width: 38rpx;
  font-size: 24rpx;
}

.menu-label {
  flex: 1;
  font-size: 26rpx;
  color: #1f2937;
}

.menu-arrow {
  color: #94a3b8;
  font-size: 30rpx;
}

.menu-item.danger .menu-label,
.menu-item.danger .menu-icon {
  color: #dc2626;
}
</style>
