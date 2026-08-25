import api from './api';

const authService = {
  async register(data) {
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  async login(credentials) {
    const payload = {
      email: credentials.email || credentials.username,
      username: credentials.username || credentials.email,
      password: credentials.password,
    };
    const response = await api.post('/auth/login', payload);
    const data = response.data;
    const token = data.accessToken || data.token;
    if (token) {
      localStorage.setItem('nexus_access_token', token);
      localStorage.setItem('token', token);
      if (data.refreshToken) {
        localStorage.setItem('nexus_refresh_token', data.refreshToken);
      }
      if (data.user) {
        localStorage.setItem('nexus_user', JSON.stringify(data.user));
      }
    }
    return data;
  },

  async logout() {
    const refreshToken = localStorage.getItem('nexus_refresh_token');
    try {
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch {
      // Clean up locally regardless
    } finally {
      localStorage.removeItem('nexus_access_token');
      localStorage.removeItem('nexus_refresh_token');
      localStorage.removeItem('nexus_user');
      localStorage.removeItem('token');
      localStorage.removeItem('jwt');
      localStorage.removeItem('activeWorkspaceId');
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
    return localStorage.getItem('nexus_access_token') || localStorage.getItem('token');
  },

  getRefreshToken() {
    return localStorage.getItem('nexus_refresh_token');
  }
};

export default authService;
