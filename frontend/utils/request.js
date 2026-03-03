const BASE_URL = 'http://localhost:8080';

uni.addInterceptor('request', {
  invoke(args) {
    args.url = `${BASE_URL}${args.url}`;
    args.timeout = 10000;
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

export default request;