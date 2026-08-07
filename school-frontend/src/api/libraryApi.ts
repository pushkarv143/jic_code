import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  Book,
  BookCategory,
  BookIssue,
  BookIssueStatus,
  LibraryDashboard,
  PageResponse,
} from '@/types';

export interface BookCategoryPayload {
  name: string;
}

export interface BookListParams {
  page?: number;
  size?: number;
  search?: string;
  categoryId?: number;
  sort?: string;
}

export interface BookPayload {
  title: string;
  author: string;
  isbn: string;
  categoryId: number;
  publisher?: string;
  totalCopies: number;
  rackNumber?: string;
  price?: number;
}

export interface BookIssueListParams {
  bookId?: number;
  studentId?: number;
  teacherId?: number;
  status?: BookIssueStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface IssueBookPayload {
  bookId: number;
  studentId?: number;
  teacherId?: number;
  dueDate: string;
}

export interface ReturnBookPayload {
  returnDate: string;
}

/** Typed wrappers around every /api/v1/book-categories/**, /books/**, /book-issues/**, /library/** endpoint. */
export const libraryApi = {
  bookCategories: {
    list: async (): Promise<ApiResponse<BookCategory[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<BookCategory[]>>(ENDPOINTS.BOOK_CATEGORIES.BASE);
      return data;
    },
    create: async (payload: BookCategoryPayload): Promise<ApiResponse<BookCategory>> => {
      const { data } = await axiosInstance.post<ApiResponse<BookCategory>>(ENDPOINTS.BOOK_CATEGORIES.BASE, payload);
      return data;
    },
    update: async (id: number, payload: BookCategoryPayload): Promise<ApiResponse<BookCategory>> => {
      const { data } = await axiosInstance.put<ApiResponse<BookCategory>>(
        ENDPOINTS.BOOK_CATEGORIES.BY_ID(id),
        payload,
      );
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.BOOK_CATEGORIES.BY_ID(id));
      return data;
    },
  },

  books: {
    list: async (params: BookListParams = {}): Promise<ApiResponse<PageResponse<Book>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<Book>>>(ENDPOINTS.BOOKS.BASE, { params });
      return data;
    },
    getById: async (id: number): Promise<ApiResponse<Book>> => {
      const { data } = await axiosInstance.get<ApiResponse<Book>>(ENDPOINTS.BOOKS.BY_ID(id));
      return data;
    },
    create: async (payload: BookPayload): Promise<ApiResponse<Book>> => {
      const { data } = await axiosInstance.post<ApiResponse<Book>>(ENDPOINTS.BOOKS.BASE, payload);
      return data;
    },
    update: async (id: number, payload: BookPayload): Promise<ApiResponse<Book>> => {
      const { data } = await axiosInstance.put<ApiResponse<Book>>(ENDPOINTS.BOOKS.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.BOOKS.BY_ID(id));
      return data;
    },
  },

  bookIssues: {
    list: async (params: BookIssueListParams = {}): Promise<ApiResponse<PageResponse<BookIssue>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<BookIssue>>>(ENDPOINTS.BOOK_ISSUES.BASE, {
        params,
      });
      return data;
    },
    issue: async (payload: IssueBookPayload): Promise<ApiResponse<BookIssue>> => {
      const { data } = await axiosInstance.post<ApiResponse<BookIssue>>(ENDPOINTS.BOOK_ISSUES.ISSUE, payload);
      return data;
    },
    returnBook: async (id: number, payload: ReturnBookPayload): Promise<ApiResponse<BookIssue>> => {
      const { data } = await axiosInstance.patch<ApiResponse<BookIssue>>(ENDPOINTS.BOOK_ISSUES.RETURN(id), payload);
      return data;
    },
    overdue: async (
      params: { page?: number; size?: number; sort?: string } = {},
    ): Promise<ApiResponse<PageResponse<BookIssue>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<BookIssue>>>(ENDPOINTS.BOOK_ISSUES.OVERDUE, {
        params,
      });
      return data;
    },
  },

  dashboard: {
    get: async (): Promise<ApiResponse<LibraryDashboard>> => {
      const { data } = await axiosInstance.get<ApiResponse<LibraryDashboard>>(ENDPOINTS.LIBRARY.DASHBOARD);
      return data;
    },
  },
};

export default libraryApi;
