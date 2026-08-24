import api from './api';

const authService = {
  async register(data) {
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  async login(credentials) {
    const response = await api.post('/auth/login', credentials);
    if (response.data && response.data.accessToken) {
      localStorage.setItem('nexus_access_token', response.data.accessToken);
      if (response.data.refreshToken) {
        localStorage.setItem('nexus_refresh_token', response.data.refreshToken);
      }
      if (response.data.user) {
        localStorage.setItem('nexus_user', JSON.stringify(response.data.user));
      }
    }
    return response.data;
  },

  async logout() {
    const refreshToken = localStorage.getItem('nexus_refresh_token');
    try {
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch {
      // Proceed with local cleanup even if the server request fails
    } finally {
      localStorage.removeItem('nexus_access_token');
      localStorage.removeItem('nexus_refresh_token');
      localStorage.removeItem('nexus_user');
    }
  },

  getCurrentUser() {
    try {
      const userStr = localStorage.getItem('nexus_user');
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  getAccessToken() {
    return localStorage.getItem('nexus_access_token');
  },

  getRefreshToken() {
    return localStorage.getItem('nexus_refresh_token');
  }
};

export default authService;
