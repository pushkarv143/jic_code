import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  ClassOfficial,
  ClassOfficialRole,
  Gender,
  Homeroom,
  PageResponse,
  Student,
} from '@/types';

/**
 * Appointing a class post in your own class.
 *
 * No classId or sectionId: both come from the caller's homeroom assignment. The
 * student must belong to that class, which the backend enforces — a class teacher
 * cannot name another class's student as their head boy.
 */
export interface MyClassOfficialPayload {
  studentId: number;
  role: ClassOfficialRole;
  /** Defaults to today when omitted. */
  fromDate?: string;
  remarks?: string;
}

export interface MyClassStudentListParams {
  search?: string;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

export interface GuardianPayload {
  name: string;
  relation: string;
  phone?: string;
  email?: string;
  occupation?: string;
  isPrimary?: boolean;
}

/**
 * Adding a student to your own class.
 *
 * <p>No `classId`, `sectionId` or `academicYearId`: the backend fills all three
 * from the caller's homeroom assignment, and the request type has nowhere to put
 * them. That is deliberate — there is no field here that could point the new
 * student at somebody else's class.
 */
export interface MyClassStudentCreatePayload {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  /** Optional login account. Omit to create a student with no login. */
  username?: string;
  password?: string;
  /** Omit to have the backend generate ADM{year}{sequence}. */
  admissionNumber?: string;
  admissionDate?: string;
  dateOfBirth?: string;
  gender?: Gender;
  bloodGroup?: string;
  religion?: string;
  category?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  guardians?: GuardianPayload[];
}

/** Editing a student on your own roster. Omitted fields are left unchanged. */
export type MyClassStudentUpdatePayload = Omit<
  MyClassStudentCreatePayload,
  'username' | 'password' | 'admissionNumber' | 'guardians'
>;

export const myClassApi = {
  /** The section the signed-in user is class teacher of. 403 if they hold none. */
  async getMyClass(): Promise<Homeroom> {
    const { data } = await axiosInstance.get<ApiResponse<Homeroom>>(ENDPOINTS.MY_CLASS.BASE);
    return data.data;
  },

  async getStudents(params: MyClassStudentListParams = {}): Promise<PageResponse<Student>> {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<Student>>>(
      ENDPOINTS.MY_CLASS.STUDENTS,
      { params },
    );
    return data.data;
  },

  async addStudent(payload: MyClassStudentCreatePayload): Promise<Student> {
    const { data } = await axiosInstance.post<ApiResponse<Student>>(ENDPOINTS.MY_CLASS.STUDENTS, payload);
    return data.data;
  },

  async updateStudent(studentId: number, payload: MyClassStudentUpdatePayload): Promise<Student> {
    const { data } = await axiosInstance.put<ApiResponse<Student>>(
      ENDPOINTS.MY_CLASS.STUDENT(studentId),
      payload,
    );
    return data.data;
  },

  /** Current holders of every post in your class. Needs only MY_CLASS_VIEW. */
  async getOfficials(): Promise<ClassOfficial[]> {
    const { data } = await axiosInstance.get<ApiResponse<ClassOfficial[]>>(ENDPOINTS.MY_CLASS.OFFICIALS);
    return data.data;
  },

  /**
   * Appoints one of your own students to a post.
   *
   * Gated by MY_CLASS_OFFICIALS_MANAGE, which is separate from the roster grant so
   * a school can split "maintains records" from "names the head boy".
   */
  async appointOfficial(payload: MyClassOfficialPayload): Promise<ClassOfficial> {
    const { data } = await axiosInstance.post<ApiResponse<ClassOfficial>>(
      ENDPOINTS.MY_CLASS.OFFICIALS,
      payload,
    );
    return data.data;
  },

  /** Ends an appointment, leaving the post vacant. The record is kept, not deleted. */
  async endOfficial(officialId: number): Promise<void> {
    await axiosInstance.delete(ENDPOINTS.MY_CLASS.OFFICIAL(officialId));
  },
};

export default myClassApi;
