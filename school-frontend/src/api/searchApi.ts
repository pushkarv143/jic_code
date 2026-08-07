import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, GlobalSearchResponse } from '@/types';

/** Typed wrapper around GET /api/v1/search/global?query= — powers the TopBar's global search dropdown. */
export const searchApi = {
  global: async (query: string): Promise<ApiResponse<GlobalSearchResponse>> => {
    const { data } = await axiosInstance.get<ApiResponse<GlobalSearchResponse>>(ENDPOINTS.SEARCH.GLOBAL, {
      params: { query },
    });
    return data;
  },
};

export default searchApi;
