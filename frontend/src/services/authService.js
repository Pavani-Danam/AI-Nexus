import api from './api';

const authService = {
  async register(data) {
    // Expected payload: { name, email, password }
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  async login(credentials) {
    // Expected payload: { email, password }
    const response = await api.post('/auth/login', credentials);
    if (response.data && response.data.accessToken) {
      localStorage.setItem('accessToken', response.data.accessToken);
      if (response.data.refreshToken) {
        localStorage.setItem('refreshToken', response.data.refreshToken);
      }
      if (response.data.user) {
        localStorage.setItem('user', JSON.stringify(response.data.user));
      }
    }
    return response.data;
  }
};

export default authService;
