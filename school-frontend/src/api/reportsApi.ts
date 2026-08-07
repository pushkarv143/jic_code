import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  AnalyticsDashboard,
  ApiResponse,
  AttendanceSummaryReport,
  FeeCollectionReport,
  LibrarySummaryReport,
  PayrollSummaryReport,
  StudentsSummaryReport,
  TeachersSummaryReport,
  TransportSummaryReport,
} from '@/types';

export interface AttendanceSummaryParams {
  startDate?: string;
  endDate?: string;
  classId?: number;
}

export interface FeeCollectionParams {
  academicYearId?: number;
}

export interface PayrollSummaryParams {
  year?: number;
}

/** Typed wrappers around every /api/v1/reports/** and /api/v1/analytics/** endpoint from the schema contract. */
export const reportsApi = {
  getStudentsSummary: async (): Promise<ApiResponse<StudentsSummaryReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudentsSummaryReport>>(ENDPOINTS.REPORTS.STUDENTS_SUMMARY);
    return data;
  },

  getTeachersSummary: async (): Promise<ApiResponse<TeachersSummaryReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<TeachersSummaryReport>>(ENDPOINTS.REPORTS.TEACHERS_SUMMARY);
    return data;
  },

  getAttendanceSummary: async (
    params: AttendanceSummaryParams = {},
  ): Promise<ApiResponse<AttendanceSummaryReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<AttendanceSummaryReport>>(
      ENDPOINTS.REPORTS.ATTENDANCE_SUMMARY,
      { params },
    );
    return data;
  },

  getFeeCollection: async (params: FeeCollectionParams = {}): Promise<ApiResponse<FeeCollectionReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<FeeCollectionReport>>(ENDPOINTS.REPORTS.FEE_COLLECTION, {
      params,
    });
    return data;
  },

  getPayrollSummary: async (params: PayrollSummaryParams = {}): Promise<ApiResponse<PayrollSummaryReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<PayrollSummaryReport>>(ENDPOINTS.REPORTS.PAYROLL_SUMMARY, {
      params,
    });
    return data;
  },

  getLibrarySummary: async (): Promise<ApiResponse<LibrarySummaryReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<LibrarySummaryReport>>(ENDPOINTS.REPORTS.LIBRARY_SUMMARY);
    return data;
  },

  getTransportSummary: async (): Promise<ApiResponse<TransportSummaryReport>> => {
    const { data } = await axiosInstance.get<ApiResponse<TransportSummaryReport>>(
      ENDPOINTS.REPORTS.TRANSPORT_SUMMARY,
    );
    return data;
  },

  getAnalyticsDashboard: async (): Promise<ApiResponse<AnalyticsDashboard>> => {
    const { data } = await axiosInstance.get<ApiResponse<AnalyticsDashboard>>(ENDPOINTS.ANALYTICS.DASHBOARD);
    return data;
  },

  exportFeeCollectionExcel: async (params: FeeCollectionParams = {}): Promise<Blob> => {
    const { data } = await axiosInstance.get<Blob>(ENDPOINTS.REPORTS.FEE_COLLECTION_EXPORT_EXCEL, {
      params,
      responseType: 'blob',
    });
    return data;
  },
};

export default reportsApi;
