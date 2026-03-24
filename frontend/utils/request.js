const BASE_URL = 'http://localhost:8080';
const AUTH_TOKEN_KEY = 'auth_token_v1';

uni.addInterceptor('request', {
  invoke(args) {
    args.url = `${BASE_URL}${args.url}`;
    args.timeout = 10000;

    const token = uni.getStorageSync(AUTH_TOKEN_KEY);
    if (token) {
      args.header = {
        ...(args.header || {}),
        'X-Auth-Token': token
      };
    }

    return args;
  }
});

const request = (options) => {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      success: (res) => {
        const payload = res.data;
        if (!payload || typeof payload.code === 'undefined') {
          reject(new Error('Invalid response structure'));
          return;
        }

        if (payload.code !== 200) {
          uni.showToast({ title: payload.message || 'Request failed', icon: 'none' });
          reject(new Error(payload.message || 'Request failed'));
          return;
        }

        resolve(payload.data);
      },
      fail: (err) => {
        uni.showToast({ title: 'Network error', icon: 'none' });
        reject(err);
      }
    });
  });
};

export const get = (url, data = {}) => request({ url, method: 'GET', data });
export const post = (url, data = {}) => request({ url, method: 'POST', data });

export const setAuthToken = (token) => {
  if (token) {
    uni.setStorageSync(AUTH_TOKEN_KEY, token);
  }
};

export const clearAuthToken = () => {
  uni.removeStorageSync(AUTH_TOKEN_KEY);
};

export const getAuthToken = () => uni.getStorageSync(AUTH_TOKEN_KEY);

export default request;
