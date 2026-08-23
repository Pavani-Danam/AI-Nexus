import api from './api';

export const workflowService = {
  getWorkflows: async (workspaceId) => {
    const response = await api.get(`/api/workflows?workspaceId=${workspaceId}`);
    return response.data;
  },

  getWorkflowById: async (id) => {
    const response = await api.get(`/api/workflows/${id}`);
    return response.data;
  },

  createWorkflow: async (data) => {
    const response = await api.post('/api/workflows', data);
    return response.data;
  },

  updateWorkflow: async (id, data) => {
    const response = await api.put(`/api/workflows/${id}`, data);
    return response.data;
  },

  deleteWorkflow: async (id) => {
    const response = await api.delete(`/api/workflows/${id}`);
    return response.data;
  },

  updateWorkflowStatus: async (id, status) => {
    const response = await api.patch(`/api/workflows/${id}/status?status=${status}`);
    return response.data;
  },

  executeWorkflow: async (workflowId, payload = {}) => {
    const response = await api.post(`/api/workflows/${workflowId}/execute`, payload);
    return response.data;
  },

  getExecutionById: async (executionId) => {
    const response = await api.get(`/api/workflows/executions/${executionId}`);
    return response.data;
  },

  getExecutionsByWorkflow: async (workflowId) => {
    const response = await api.get(`/api/workflows/${workflowId}/executions`);
    return response.data;
  },

  getExecutionsByWorkspace: async (workspaceId) => {
    const response = await api.get(`/api/workflows/executions/workspace/${workspaceId}`);
    return response.data;
  }
};

export default workflowService;
