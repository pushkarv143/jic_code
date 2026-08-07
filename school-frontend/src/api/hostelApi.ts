import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  Hostel,
  HostelFee,
  HostelFeePaidStatus,
  HostelRoom,
  HostelStudent,
  HostelStudentStatus,
  HostelVisitor,
  HostelType,
  PageResponse,
} from '@/types';

export interface HostelPayload {
  name: string;
  wardenName?: string;
  wardenContact?: string;
  type: HostelType;
}

export interface HostelRoomPayload {
  roomNumber: string;
  capacity: number;
}

export interface HostelStudentListParams {
  studentId?: number;
  roomId?: number;
  status?: HostelStudentStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface HostelStudentPayload {
  studentId: number;
  roomId: number;
  allocationDate: string;
}

export interface VacatePayload {
  vacateDate: string;
}

export interface HostelVisitorListParams {
  studentId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface HostelVisitorPayload {
  studentId: number;
  visitorName: string;
  relation: string;
  phone?: string;
  visitDate: string;
  purpose?: string;
}

export interface HostelFeeListParams {
  studentId?: number;
  month?: number;
  year?: number;
  paidStatus?: HostelFeePaidStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface HostelFeePayload {
  studentId: number;
  month: number;
  year: number;
  amount: number;
}

/** Typed wrappers around every /api/v1/hostels/**, /hostel-rooms/**, /hostel-students/**, /hostel-visitors/**, /hostel-fees/** endpoint. */
export const hostelApi = {
  hostels: {
    list: async (): Promise<ApiResponse<Hostel[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<Hostel[]>>(ENDPOINTS.HOSTELS.BASE);
      return data;
    },
    create: async (payload: HostelPayload): Promise<ApiResponse<Hostel>> => {
      const { data } = await axiosInstance.post<ApiResponse<Hostel>>(ENDPOINTS.HOSTELS.BASE, payload);
      return data;
    },
    update: async (id: number, payload: HostelPayload): Promise<ApiResponse<Hostel>> => {
      const { data } = await axiosInstance.put<ApiResponse<Hostel>>(ENDPOINTS.HOSTELS.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.HOSTELS.BY_ID(id));
      return data;
    },
  },

  hostelRooms: {
    list: async (hostelId: number): Promise<ApiResponse<HostelRoom[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<HostelRoom[]>>(ENDPOINTS.HOSTELS.ROOMS(hostelId));
      return data;
    },
    create: async (hostelId: number, payload: HostelRoomPayload): Promise<ApiResponse<HostelRoom>> => {
      const { data } = await axiosInstance.post<ApiResponse<HostelRoom>>(ENDPOINTS.HOSTELS.ROOMS(hostelId), payload);
      return data;
    },
    update: async (id: number, payload: HostelRoomPayload): Promise<ApiResponse<HostelRoom>> => {
      const { data } = await axiosInstance.put<ApiResponse<HostelRoom>>(ENDPOINTS.HOSTEL_ROOMS.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.HOSTEL_ROOMS.BY_ID(id));
      return data;
    },
  },

  hostelStudents: {
    list: async (
      params: HostelStudentListParams = {},
    ): Promise<ApiResponse<PageResponse<HostelStudent>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<HostelStudent>>>(
        ENDPOINTS.HOSTEL_STUDENTS.BASE,
        { params },
      );
      return data;
    },
    create: async (payload: HostelStudentPayload): Promise<ApiResponse<HostelStudent>> => {
      const { data } = await axiosInstance.post<ApiResponse<HostelStudent>>(ENDPOINTS.HOSTEL_STUDENTS.BASE, payload);
      return data;
    },
    vacate: async (id: number, payload: VacatePayload): Promise<ApiResponse<HostelStudent>> => {
      const { data } = await axiosInstance.patch<ApiResponse<HostelStudent>>(
        ENDPOINTS.HOSTEL_STUDENTS.VACATE(id),
        payload,
      );
      return data;
    },
  },

  hostelVisitors: {
    list: async (
      params: HostelVisitorListParams = {},
    ): Promise<ApiResponse<PageResponse<HostelVisitor>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<HostelVisitor>>>(
        ENDPOINTS.HOSTEL_VISITORS.BASE,
        { params },
      );
      return data;
    },
    create: async (payload: HostelVisitorPayload): Promise<ApiResponse<HostelVisitor>> => {
      const { data } = await axiosInstance.post<ApiResponse<HostelVisitor>>(ENDPOINTS.HOSTEL_VISITORS.BASE, payload);
      return data;
    },
    checkout: async (id: number): Promise<ApiResponse<HostelVisitor>> => {
      const { data } = await axiosInstance.patch<ApiResponse<HostelVisitor>>(ENDPOINTS.HOSTEL_VISITORS.CHECKOUT(id));
      return data;
    },
  },

  hostelFees: {
    list: async (params: HostelFeeListParams = {}): Promise<ApiResponse<PageResponse<HostelFee>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<HostelFee>>>(ENDPOINTS.HOSTEL_FEES.BASE, {
        params,
      });
      return data;
    },
    create: async (payload: HostelFeePayload): Promise<ApiResponse<HostelFee>> => {
      const { data } = await axiosInstance.post<ApiResponse<HostelFee>>(ENDPOINTS.HOSTEL_FEES.BASE, payload);
      return data;
    },
    markPaid: async (id: number): Promise<ApiResponse<HostelFee>> => {
      const { data } = await axiosInstance.patch<ApiResponse<HostelFee>>(ENDPOINTS.HOSTEL_FEES.MARK_PAID(id));
      return data;
    },
  },
};

export default hostelApi;
