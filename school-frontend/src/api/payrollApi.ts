import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  GeneratePayrollResult,
  PageResponse,
  PayrollDashboardSummary,
  PayrollEmployeeType,
  PayrollRun,
  PayrollStatus,
  SalarySlip,
  SalaryStructure,
} from '@/types';

export interface SalaryStructureListParams {
  employeeId?: number;
  employeeType?: PayrollEmployeeType;
}

export interface SalaryStructurePayload {
  employeeId: number;
  employeeType: PayrollEmployeeType;
  basicSalary: number;
  hra: number;
  da: number;
  otherAllowances: number;
  pfPercentage: number;
  esiPercentage: number;
}

export interface PayrollListParams {
  employeeId?: number;
  employeeType?: PayrollEmployeeType;
  month?: number;
  year?: number;
  status?: PayrollStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface GeneratePayrollPayload {
  employeeType: PayrollEmployeeType;
  month: number;
  year: number;
}

export interface MarkPaidPayload {
  paymentDate: string;
}

export interface PayrollDashboardParams {
  month?: number;
  year?: number;
}

/** Typed wrappers around every /api/v1/salary-structures/**, /payroll/** endpoint from the schema contract. */
export const payrollApi = {
  salaryStructures: {
    list: async (params: SalaryStructureListParams = {}): Promise<ApiResponse<SalaryStructure[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<SalaryStructure[]>>(
        ENDPOINTS.SALARY_STRUCTURES.BASE,
        { params },
      );
      return data;
    },
    create: async (payload: SalaryStructurePayload): Promise<ApiResponse<SalaryStructure>> => {
      const { data } = await axiosInstance.post<ApiResponse<SalaryStructure>>(
        ENDPOINTS.SALARY_STRUCTURES.BASE,
        payload,
      );
      return data;
    },
    update: async (id: number, payload: SalaryStructurePayload): Promise<ApiResponse<SalaryStructure>> => {
      const { data } = await axiosInstance.put<ApiResponse<SalaryStructure>>(
        ENDPOINTS.SALARY_STRUCTURES.BY_ID(id),
        payload,
      );
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.SALARY_STRUCTURES.BY_ID(id));
      return data;
    },
  },

  payroll: {
    list: async (params: PayrollListParams = {}): Promise<ApiResponse<PageResponse<PayrollRun>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<PayrollRun>>>(ENDPOINTS.PAYROLL.BASE, {
        params,
      });
      return data;
    },
    generate: async (payload: GeneratePayrollPayload): Promise<ApiResponse<GeneratePayrollResult>> => {
      const { data } = await axiosInstance.post<ApiResponse<GeneratePayrollResult>>(
        ENDPOINTS.PAYROLL.GENERATE,
        payload,
      );
      return data;
    },
    markPaid: async (id: number, payload: MarkPaidPayload): Promise<ApiResponse<PayrollRun>> => {
      const { data } = await axiosInstance.patch<ApiResponse<PayrollRun>>(ENDPOINTS.PAYROLL.MARK_PAID(id), payload);
      return data;
    },
    getSalarySlip: async (id: number): Promise<ApiResponse<SalarySlip>> => {
      const { data } = await axiosInstance.get<ApiResponse<SalarySlip>>(ENDPOINTS.PAYROLL.SALARY_SLIP(id));
      return data;
    },
    getSalarySlipPdf: async (id: number): Promise<Blob> => {
      const { data } = await axiosInstance.get<Blob>(ENDPOINTS.PAYROLL.SALARY_SLIP_PDF(id), {
        responseType: 'blob',
      });
      return data;
    },
    getDashboard: async (params: PayrollDashboardParams = {}): Promise<ApiResponse<PayrollDashboardSummary>> => {
      const { data } = await axiosInstance.get<ApiResponse<PayrollDashboardSummary>>(ENDPOINTS.PAYROLL.DASHBOARD, {
        params,
      });
      return data;
    },
  },
};

export default payrollApi;
