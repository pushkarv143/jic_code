import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  Exam,
  ExamResultRow,
  ExamSchedule,
  ExamType,
  Mark,
  MarkRosterRow,
  PageResponse,
  ReportCard,
} from '@/types';

export interface ExamTypePayload {
  name: string;
}

export interface ExamListParams {
  classId?: number;
  academicYearId?: number;
  examTypeId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ExamPayload {
  examTypeId: number;
  classId: number;
  academicYearId: number;
  startDate: string;
  endDate: string;
}

export interface ExamSchedulePayload {
  subjectId: number;
  examDate: string;
  startTime: string;
  endTime: string;
  maxMarks: number;
  roomNumber?: string;
}

export interface MarksListParams {
  examScheduleId?: number;
  studentId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface MarksEntryRecord {
  studentId: number;
  marksObtained: number;
  remarks?: string;
}

export interface MarksEntryPayload {
  examScheduleId: number;
  records: MarksEntryRecord[];
}

/** Typed wrappers around every /api/v1/exam-types/**, /exams/**, /exam-schedules/**, /marks/** endpoint. */
export const examApi = {
  examTypes: {
    list: async (): Promise<ApiResponse<ExamType[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<ExamType[]>>(ENDPOINTS.EXAM_TYPES.BASE);
      return data;
    },
    create: async (payload: ExamTypePayload): Promise<ApiResponse<ExamType>> => {
      const { data } = await axiosInstance.post<ApiResponse<ExamType>>(ENDPOINTS.EXAM_TYPES.BASE, payload);
      return data;
    },
    update: async (id: number, payload: ExamTypePayload): Promise<ApiResponse<ExamType>> => {
      const { data } = await axiosInstance.put<ApiResponse<ExamType>>(ENDPOINTS.EXAM_TYPES.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.EXAM_TYPES.BY_ID(id));
      return data;
    },
  },

  exams: {
    list: async (params: ExamListParams = {}): Promise<ApiResponse<PageResponse<Exam>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<Exam>>>(ENDPOINTS.EXAMS.BASE, { params });
      return data;
    },
    getById: async (id: number): Promise<ApiResponse<Exam>> => {
      const { data } = await axiosInstance.get<ApiResponse<Exam>>(ENDPOINTS.EXAMS.BY_ID(id));
      return data;
    },
    create: async (payload: ExamPayload): Promise<ApiResponse<Exam>> => {
      const { data } = await axiosInstance.post<ApiResponse<Exam>>(ENDPOINTS.EXAMS.BASE, payload);
      return data;
    },
    update: async (id: number, payload: ExamPayload): Promise<ApiResponse<Exam>> => {
      const { data } = await axiosInstance.put<ApiResponse<Exam>>(ENDPOINTS.EXAMS.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.EXAMS.BY_ID(id));
      return data;
    },
    results: async (
      examId: number,
      params: { classId?: number; sectionId?: number } = {},
    ): Promise<ApiResponse<ExamResultRow[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<ExamResultRow[]>>(ENDPOINTS.EXAMS.RESULTS(examId), {
        params,
      });
      return data;
    },
  },

  schedules: {
    list: async (examId: number): Promise<ApiResponse<ExamSchedule[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<ExamSchedule[]>>(ENDPOINTS.EXAMS.SCHEDULES(examId));
      return data;
    },
    create: async (examId: number, payload: ExamSchedulePayload): Promise<ApiResponse<ExamSchedule>> => {
      const { data } = await axiosInstance.post<ApiResponse<ExamSchedule>>(
        ENDPOINTS.EXAMS.SCHEDULES(examId),
        payload,
      );
      return data;
    },
    update: async (id: number, payload: ExamSchedulePayload): Promise<ApiResponse<ExamSchedule>> => {
      const { data } = await axiosInstance.put<ApiResponse<ExamSchedule>>(ENDPOINTS.EXAM_SCHEDULES.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.EXAM_SCHEDULES.BY_ID(id));
      return data;
    },
  },

  marks: {
    list: async (params: MarksListParams = {}): Promise<ApiResponse<PageResponse<Mark>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<Mark>>>(ENDPOINTS.MARKS.BASE, { params });
      return data;
    },
    /** Full class roster for an exam schedule (every active student, mark null if not yet entered) — use this to populate the marks-entry grid, not `list()`. */
    roster: async (examScheduleId: number): Promise<ApiResponse<MarkRosterRow[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<MarkRosterRow[]>>(ENDPOINTS.MARKS.ROSTER, {
        params: { examScheduleId },
      });
      return data;
    },
    saveEntry: async (payload: MarksEntryPayload): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.post<ApiResponse<null>>(ENDPOINTS.MARKS.ENTRY, payload);
      return data;
    },
    getReportCard: async (studentId: number, examId: number): Promise<ApiResponse<ReportCard>> => {
      const { data } = await axiosInstance.get<ApiResponse<ReportCard>>(ENDPOINTS.MARKS.REPORT_CARD(studentId), {
        params: { examId },
      });
      return data;
    },
    getReportCardPdf: async (studentId: number, examId: number): Promise<Blob> => {
      const { data } = await axiosInstance.get<Blob>(ENDPOINTS.MARKS.REPORT_CARD_PDF(studentId), {
        params: { examId },
        responseType: 'blob',
      });
      return data;
    },
  },
};

export default examApi;
