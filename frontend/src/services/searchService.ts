import api from './api';

export interface SearchResultItem {
  documentId: number | null;
  filename: string;
  chunkIndex: number | null;
  score: number;
  content: string;
  characterCount: number | null;
  fileType: string;
  vectorId: string;
}

export interface SearchResponse {
  query: string;
  workspaceId: number;
  totalResults: number;
  results: SearchResultItem[];
}

export const searchService = {
  search: async (
    query: string,
    workspaceId: number,
    topK: number = 5
  ): Promise<SearchResponse> => {
    const params = {
      q: query.trim(),
      workspaceId,
      topK,
    };
    const response = await api.get<SearchResponse>('/search', { params });
    return response.data;
  },
};
