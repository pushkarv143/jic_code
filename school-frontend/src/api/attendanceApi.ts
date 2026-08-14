import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  AttendanceStatus,
  MonthlyAttendanceRow,
  PageResponse,
  StudentAttendanceReportRow,
  StudentAttendanceRow,
  StudentAttendanceSummary,
  TeacherAttendanceReportRow,
  TeacherAttendanceRow,
} from '@/types';

export interface MarkingGridParams {
  classId: number;
  sectionId: number;
  date: string;
}

export interface StudentAttendanceMarkRecord {
  studentId: number;
  status: AttendanceStatus;
  remarks?: string;
}

export interface StudentAttendanceMarkPayload {
  classId: number;
  sectionId: number;
  attendanceDate: string;
  records: StudentAttendanceMarkRecord[];
}

export interface StudentAttendanceReportParams {
  studentId?: number;
  classId?: number;
  sectionId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface MonthlyAttendanceParams {
  classId: number;
  sectionId: number;
  year: number;
  month: number;
}

export interface TeacherAttendanceMarkRecord {
  teacherId: number;
  status: AttendanceStatus;
  checkIn?: string;
  checkOut?: string;
  remarks?: string;
}

export interface TeacherAttendanceMarkPayload {
  attendanceDate: string;
  records: TeacherAttendanceMarkRecord[];
}

export interface TeacherAttendanceReportParams {
  teacherId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

/** Typed wrappers around every /api/v1/attendance/** endpoint from the schema contract. */
export const attendanceApi = {
  getStudentMarkingGrid: async (
    params: MarkingGridParams,
  ): Promise<ApiResponse<StudentAttendanceRow[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudentAttendanceRow[]>>(
      ENDPOINTS.ATTENDANCE.STUDENTS,
      { params },
    );
    return data;
  },

  markStudents: async (payload: StudentAttendanceMarkPayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.ATTENDANCE.STUDENTS_MARK,
      payload,
    );
    return data;
  },

  getStudentReport: async (
    params: StudentAttendanceReportParams = {},
  ): Promise<ApiResponse<PageResponse<StudentAttendanceReportRow>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<StudentAttendanceReportRow>>>(
      ENDPOINTS.ATTENDANCE.STUDENTS_REPORT,
      { params },
    );
    return data;
  },

  getStudentSummary: async (
    studentId: number,
    params: { startDate?: string; endDate?: string } = {},
  ): Promise<ApiResponse<StudentAttendanceSummary>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudentAttendanceSummary>>(
      ENDPOINTS.ATTENDANCE.STUDENT_SUMMARY(studentId),
      { params },
    );
    return data;
  },

  /** The signed-in student's own attendance summary. STUDENT role only. */
  getOwnSummary: async (
    params: { startDate?: string; endDate?: string } = {},
  ): Promise<ApiResponse<StudentAttendanceSummary>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudentAttendanceSummary>>(
      ENDPOINTS.ATTENDANCE.OWN_SUMMARY,
      { params },
    );
    return data;
  },

  getMonthly: async (
    params: MonthlyAttendanceParams,
  ): Promise<ApiResponse<MonthlyAttendanceRow[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<MonthlyAttendanceRow[]>>(
      ENDPOINTS.ATTENDANCE.STUDENTS_MONTHLY,
      { params },
    );
    return data;
  },

  getTeacherMarkingGrid: async (date: string): Promise<ApiResponse<TeacherAttendanceRow[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TeacherAttendanceRow[]>>(
      ENDPOINTS.ATTENDANCE.TEACHERS,
      { params: { date } },
    );
    return data;
  },

  markTeachers: async (payload: TeacherAttendanceMarkPayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.ATTENDANCE.TEACHERS_MARK,
      payload,
    );
    return data;
  },

  getTeacherReport: async (
    params: TeacherAttendanceReportParams = {},
  ): Promise<ApiResponse<PageResponse<TeacherAttendanceReportRow>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<TeacherAttendanceReportRow>>>(
      ENDPOINTS.ATTENDANCE.TEACHERS_REPORT,
      { params },
    );
    return data;
  },
};

export default attendanceApi;
