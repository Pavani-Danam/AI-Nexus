import api from './api';

export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'INDEXED' | 'FAILED';

export interface DocumentResponse {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  status: DocumentStatus;
  userId: number;
  workspaceId: number;
  createdAt: string;
  updatedAt: string;
}

export const documentService = {
  uploadDocument: async (file: File, workspaceId: number): Promise<DocumentResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('workspaceId', workspaceId.toString());

    const response = await api.post<DocumentResponse>('/documents', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getDocuments: async (workspaceId: number, search?: string): Promise<DocumentResponse[]> => {
    const params: Record<string, string> = { workspaceId: workspaceId.toString() };
    if (search && search.trim() !== '') {
      params.search = search.trim();
    }
    const response = await api.get<DocumentResponse[]>('/documents', { params });
    return Array.isArray(response.data) ? response.data : [];
  },

  getDocumentById: async (id: number): Promise<DocumentResponse> => {
    const response = await api.get<DocumentResponse>(`/documents/${id}`);
    return response.data;
  },

  deleteDocument: async (id: number): Promise<{ message: string }> => {
    const response = await api.delete<{ message: string }>(`/documents/${id}`);
    return response.data;
  },
};
