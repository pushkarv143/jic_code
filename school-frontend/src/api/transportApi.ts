import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, Bus, Driver, PageResponse, PickupPoint, Route, StudentTransport } from '@/types';

export interface DriverPayload {
  name: string;
  phone: string;
  licenseNumber: string;
  address?: string;
}

export interface BusPayload {
  busNumber: string;
  capacity: number;
  driverId?: number | null;
  vehicleModel?: string;
  registrationNumber: string;
}

export interface RouteListParams {
  busId?: number;
}

export interface RoutePayload {
  routeName: string;
  busId: number;
  startPoint: string;
  endPoint: string;
}

export interface PickupPointPayload {
  pointName: string;
  pickupTime: string;
  dropTime: string;
}

export interface StudentTransportListParams {
  studentId?: number;
  routeId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface StudentTransportPayload {
  studentId: number;
  routeId: number;
  pickupPointId: number;
  monthlyFee: number;
}

/** Typed wrappers around every /api/v1/drivers/**, /buses/**, /routes/**, /pickup-points/**, /student-transport/** endpoint. */
export const transportApi = {
  drivers: {
    list: async (): Promise<ApiResponse<Driver[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<Driver[]>>(ENDPOINTS.DRIVERS.BASE);
      return data;
    },
    create: async (payload: DriverPayload): Promise<ApiResponse<Driver>> => {
      const { data } = await axiosInstance.post<ApiResponse<Driver>>(ENDPOINTS.DRIVERS.BASE, payload);
      return data;
    },
    update: async (id: number, payload: DriverPayload): Promise<ApiResponse<Driver>> => {
      const { data } = await axiosInstance.put<ApiResponse<Driver>>(ENDPOINTS.DRIVERS.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.DRIVERS.BY_ID(id));
      return data;
    },
  },

  buses: {
    list: async (): Promise<ApiResponse<Bus[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<Bus[]>>(ENDPOINTS.BUSES.BASE);
      return data;
    },
    create: async (payload: BusPayload): Promise<ApiResponse<Bus>> => {
      const { data } = await axiosInstance.post<ApiResponse<Bus>>(ENDPOINTS.BUSES.BASE, payload);
      return data;
    },
    update: async (id: number, payload: BusPayload): Promise<ApiResponse<Bus>> => {
      const { data } = await axiosInstance.put<ApiResponse<Bus>>(ENDPOINTS.BUSES.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.BUSES.BY_ID(id));
      return data;
    },
  },

  routes: {
    list: async (params: RouteListParams = {}): Promise<ApiResponse<Route[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<Route[]>>(ENDPOINTS.ROUTES.BASE, { params });
      return data;
    },
    create: async (payload: RoutePayload): Promise<ApiResponse<Route>> => {
      const { data } = await axiosInstance.post<ApiResponse<Route>>(ENDPOINTS.ROUTES.BASE, payload);
      return data;
    },
    update: async (id: number, payload: RoutePayload): Promise<ApiResponse<Route>> => {
      const { data } = await axiosInstance.put<ApiResponse<Route>>(ENDPOINTS.ROUTES.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.ROUTES.BY_ID(id));
      return data;
    },
  },

  pickupPoints: {
    list: async (routeId: number): Promise<ApiResponse<PickupPoint[]>> => {
      const { data } = await axiosInstance.get<ApiResponse<PickupPoint[]>>(ENDPOINTS.ROUTES.PICKUP_POINTS(routeId));
      return data;
    },
    create: async (routeId: number, payload: PickupPointPayload): Promise<ApiResponse<PickupPoint>> => {
      const { data } = await axiosInstance.post<ApiResponse<PickupPoint>>(
        ENDPOINTS.ROUTES.PICKUP_POINTS(routeId),
        payload,
      );
      return data;
    },
    update: async (id: number, payload: PickupPointPayload): Promise<ApiResponse<PickupPoint>> => {
      const { data } = await axiosInstance.put<ApiResponse<PickupPoint>>(ENDPOINTS.PICKUP_POINTS.BY_ID(id), payload);
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.PICKUP_POINTS.BY_ID(id));
      return data;
    },
  },

  studentTransport: {
    list: async (
      params: StudentTransportListParams = {},
    ): Promise<ApiResponse<PageResponse<StudentTransport>>> => {
      const { data } = await axiosInstance.get<ApiResponse<PageResponse<StudentTransport>>>(
        ENDPOINTS.STUDENT_TRANSPORT.BASE,
        { params },
      );
      return data;
    },
    create: async (payload: StudentTransportPayload): Promise<ApiResponse<StudentTransport>> => {
      const { data } = await axiosInstance.post<ApiResponse<StudentTransport>>(
        ENDPOINTS.STUDENT_TRANSPORT.BASE,
        payload,
      );
      return data;
    },
    update: async (id: number, payload: StudentTransportPayload): Promise<ApiResponse<StudentTransport>> => {
      const { data } = await axiosInstance.put<ApiResponse<StudentTransport>>(
        ENDPOINTS.STUDENT_TRANSPORT.BY_ID(id),
        payload,
      );
      return data;
    },
    remove: async (id: number): Promise<ApiResponse<null>> => {
      const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.STUDENT_TRANSPORT.BY_ID(id));
      return data;
    },
  },
};

export default transportApi;
