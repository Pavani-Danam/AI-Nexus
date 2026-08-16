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

  getDocumentById: async (id: number): Promise<DocumentResponse> => {
    const response = await api.get<DocumentResponse>(`/documents/${id}`);
    return response.data;
  },

  getDocumentsByWorkspace: async (workspaceId: number): Promise<DocumentResponse[]> => {
    const response = await api.get<any>(`/documents/workspace/${workspaceId}`);
    // Backend returns Page<DocumentResponse> or List<DocumentResponse>
    if (response.data && Array.isArray(response.data.content)) {
      return response.data.content;
    }
    if (Array.isArray(response.data)) {
      return response.data;
    }
    return [];
  },
};
