import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  DuesSummary,
  FeeCategory,
  FeePayment,
  FeeReceipt,
  FeeStructure,
  GenerateDuesResult,
  PageResponse,
  Scholarship,
  ScholarshipType,
  StudentFee,
  StudentFeeStatus,
  PaymentMode,
} from '@/types';

export interface FeeCategoryPayload {
  name: string;
  description?: string;
}

export interface FeeStructureListParams {
  classId?: number;
  academicYearId?: number;
  feeCategoryId?: number;
}

export interface FeeStructurePayload {
  classId: number;
  academicYearId: number;
  feeCategoryId: number;
  amount: number;
  dueDate: string;
}

export interface StudentFeeListParams {
  studentId?: number;
  classId?: number;
  sectionId?: number;
  status?: StudentFeeStatus;
  academicYearId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface GenerateDuesPayload {
  classId: number;
  academicYearId: number;
  feeStructureIds: number[];
}

export interface FeePaymentPayload {
  studentFeeId: number;
  amount: number;
  paymentDate: string;
  paymentMode: PaymentMode;
  transactionId?: string;
}

export interface FeePaymentListParams {
  studentId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface DuesSummaryParams {
  classId?: number;
  academicYearId?: number;
}

export interface ScholarshipListParams {
  studentId?: number;
  academicYearId?: number;
  page?: number;
  size?: number;
}

export interface ScholarshipPayload {
  studentId: number;
  title: string;
  amount: number;
  type: ScholarshipType;
  academicYearId: number;
}

/** Typed wrappers around every /api/v1/fee-*, /student-fees/**, /fees/** endpoint from the schema contract. */
export const feesApi = {
  feeCategories: {
    list: async (): Promise<ApiResponse<FeeCategory[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<FeeCategory[]>>(ENDPOINTS.FEE_CATEGORIES.BASE);
      return data;
    },
    create: async (payload: FeeCategoryPayload): Promise<ApiResponse<FeeCategory>> => {
      const { data } = await axiosInstance.post<ApiResponse<FeeCategory>>(
        ENDPOINTS.FEE_CATEGORIES.BASE,
        payload,
      );
      return data;
    },
    update: async (id: number, payload: FeeCategoryPayload): Promise<ApiResponse<FeeCategory>> => {
      const { data } = await axiosInstance.put<ApiResponse<FeeCategory>>(
        ENDPOINTS.FEE_CATEGORIES.BY_ID(id),
        payload,
      );
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.FEE_CATEGORIES.BY_ID(id));
      return data;
    },
  },

  feeStructures: {
    list: async (params: FeeStructureListParams = {}): Promise<ApiResponse<FeeStructure[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<FeeStructure[]>>(
        ENDPOINTS.FEE_STRUCTURES.BASE,
        { params },
      );
      return data;
    },
    create: async (payload: FeeStructurePayload): Promise<ApiResponse<FeeStructure>> => {
      const { data } = await axiosInstance.post<ApiResponse<FeeStructure>>(
        ENDPOINTS.FEE_STRUCTURES.BASE,
        payload,
      );
      return data;
    },
    update: async (id: number, payload: FeeStructurePayload): Promise<ApiResponse<FeeStructure>> => {
      const { data } = await axiosInstance.put<ApiResponse<FeeStructure>>(
        ENDPOINTS.FEE_STRUCTURES.BY_ID(id),
        payload,
      );
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.FEE_STRUCTURES.BY_ID(id));
      return data;
    },
  },

  studentFees: {
    list: async (
      params: StudentFeeListParams = {},
    ): Promise<ApiResponse<PageResponse<StudentFee>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<StudentFee>>>(
        ENDPOINTS.STUDENT_FEES.BASE,
        { params },
      );
      return data;
    },
    getById: async (id: number): Promise<ApiResponse<StudentFee>> => {
      const { data } = await axiosInstance.get<ApiResponse<StudentFee>>(ENDPOINTS.STUDENT_FEES.BY_ID(id));
      return data;
    },
    generate: async (payload: GenerateDuesPayload): Promise<ApiResponse<GenerateDuesResult>> => {
      const { data } = await axiosInstance.post<ApiResponse<GenerateDuesResult>>(
        ENDPOINTS.STUDENT_FEES.GENERATE,
        payload,
      );
      return data;
    },
  },

  feePayments: {
    create: async (payload: FeePaymentPayload): Promise<ApiResponse<{ payment: FeePayment; updatedStudentFee: StudentFee }>> => {
      const { data } = await axiosInstance.post<
        ApiResponse<{ payment: FeePayment; updatedStudentFee: StudentFee }>
      >(ENDPOINTS.FEE_PAYMENTS.BASE, payload);
      return data;
    },
    list: async (
      params: FeePaymentListParams = {},
    ): Promise<ApiResponse<PageResponse<FeePayment>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<FeePayment>>>(
        ENDPOINTS.FEE_PAYMENTS.BASE,
        { params },
      );
      return data;
    },
    getReceipt: async (id: number): Promise<ApiResponse<FeeReceipt>> => {
      const { data } = await axiosInstance.get<ApiResponse<FeeReceipt>>(
        ENDPOINTS.FEE_PAYMENTS.RECEIPT(id),
      );
      return data;
    },
    getReceiptPdf: async (id: number): Promise<Blob> => {
      const { data } = await axiosInstance.get<Blob>(ENDPOINTS.FEE_PAYMENTS.RECEIPT_PDF(id), {
        responseType: 'blob',
      });
      return data;
    },
  },

  dues: {
    getSummary: async (params: DuesSummaryParams = {}): Promise<ApiResponse<DuesSummary>> => {
      const { data } = await axiosInstance.get<ApiResponse<DuesSummary>>(ENDPOINTS.FEES.DUES_SUMMARY, {
        params,
      });
      return data;
    },
  },

  scholarships: {
    list: async (
      params: ScholarshipListParams = {},
    ): Promise<ApiResponse<PageResponse<Scholarship>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<Scholarship>>>(
        ENDPOINTS.SCHOLARSHIPS.BASE,
        { params },
      );
      return data;
    },
    create: async (payload: ScholarshipPayload): Promise<ApiResponse<Scholarship>> => {
      const { data } = await axiosInstance.post<ApiResponse<Scholarship>>(
        ENDPOINTS.SCHOLARSHIPS.BASE,
        payload,
      );
      return data;
    },
    update: async (id: number, payload: ScholarshipPayload): Promise<ApiResponse<Scholarship>> => {
      const { data } = await axiosInstance.put<ApiResponse<Scholarship>>(
        ENDPOINTS.SCHOLARSHIPS.BY_ID(id),
        payload,
      );
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.SCHOLARSHIPS.BY_ID(id));
      return data;
    },
  },
};

export default feesApi;
