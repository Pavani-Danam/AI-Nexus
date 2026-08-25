import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Token & Multi-tenant Workspace Header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('nexus_access_token') || localStorage.getItem('token') || localStorage.getItem('jwt');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    const activeWorkspaceId = localStorage.getItem('activeWorkspaceId');
    if (activeWorkspaceId) {
      config.headers['X-Workspace-Id'] = activeWorkspaceId;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Avoid redirecting on auth endpoints
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = error.config && (error.config.url.includes('/auth/login') || error.config.url.includes('/auth/register'));
    if (error.response && error.response.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('nexus_access_token');
      localStorage.removeItem('token');
      localStorage.removeItem('jwt');
      localStorage.removeItem('nexus_user');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
