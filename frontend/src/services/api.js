import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') || localStorage.getItem('jwt');
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

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('jwt');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/auth') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: async (username, password) => {
    const res = await api.post('/auth/login', { username, password });
    if (res.data.token) {
      localStorage.setItem('token', res.data.token);
    }
    return res.data;
  },
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('jwt');
    localStorage.removeItem('activeWorkspaceId');
    window.location.href = '/login';
  }
};

export const dashboardService = {
  getSummary: async (workspaceId) => {
    const res = await api.get('/dashboard/summary', { params: { workspaceId } });
    return res.data;
  }
};

export const workspaceService = {
  getAll: async () => {
    const res = await api.get('/workspaces');
    return res.data;
  },
  create: async (data) => {
    const res = await api.post('/workspaces', data);
    return res.data;
  }
};

export const documentService = {
  getAll: async (workspaceId) => {
    const res = await api.get('/documents', { params: { workspaceId } });
    return res.data;
  },
  upload: async (formData) => {
    const res = await api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res.data;
  },
  delete: async (id) => {
    await api.delete(`/documents/${id}`);
  },
  search: async (query, workspaceId) => {
    const res = await api.get('/documents/search', { params: { query, workspaceId } });
    return res.data;
  }
};

export const chatService = {
  getConversations: async (workspaceId) => {
    const res = await api.get('/chat/conversations', { params: { workspaceId } });
    return res.data;
  },
  sendMessage: async (message, conversationId, workspaceId) => {
    const res = await api.post('/chat/message', { message, conversationId, workspaceId });
    return res.data;
  }
};

export default api;
