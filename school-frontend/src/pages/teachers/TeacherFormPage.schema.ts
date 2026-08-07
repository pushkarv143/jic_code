import * as yup from 'yup';
import dayjs from 'dayjs';
import { passwordComplexity } from '@/pages/auth/authSchemas';

/** Department/Designation selects kept as strings in RHF state, converted to numbers at submit time. */
function idSelect(label: string) {
  return yup.string().required(`${label} is required`);
}

export function buildTeacherSchema(isEdit: boolean) {
  return yup.object({
    username: yup
      .string()
      .required('Username is required')
      .min(4, 'Must be at least 4 characters')
      .matches(/^[a-zA-Z0-9._]+$/, 'Only letters, numbers, dots and underscores allowed'),
    email: yup.string().email('Enter a valid email').required('Email is required'),
    password: isEdit
      ? yup.string().optional()
      : passwordComplexity,
    firstName: yup.string().required('First name is required').max(50),
    lastName: yup.string().required('Last name is required').max(50),
    phone: yup
      .string()
      .required('Phone number is required')
      .matches(/^[0-9]{10}$/, 'Enter a valid 10-digit phone number'),
    gender: yup
      .mixed<'MALE' | 'FEMALE' | 'OTHER'>()
      .oneOf(['MALE', 'FEMALE', 'OTHER'], 'Gender is required')
      .required('Gender is required'),
    departmentId: idSelect('Department'),
    designationId: idSelect('Designation'),
    qualification: yup.string().max(150).optional(),
    experienceYears: yup
      .string()
      .optional()
      .test('num', 'Enter a valid number of years', (value) => !value || /^[0-9]{1,2}$/.test(value)),
    joiningDate: yup
      .mixed<dayjs.Dayjs>()
      .required('Joining date is required')
      .test('is-valid', 'Enter a valid joining date', (value) => !!value && dayjs(value).isValid()),
    dateOfBirth: yup
      .mixed<dayjs.Dayjs>()
      .nullable()
      .test('is-past', 'Date of birth must be in the past', (value) => !value || dayjs(value).isBefore(dayjs(), 'day')),
    address: yup.string().max(300).optional(),
    city: yup.string().max(100).optional(),
    state: yup.string().max(100).optional(),
    pincode: yup
      .string()
      .optional()
      .test('pincode', 'Enter a valid 6-digit pincode', (value) => !value || /^[0-9]{6}$/.test(value)),
    bloodGroup: yup.string().max(5).optional(),
    emergencyContact: yup
      .string()
      .optional()
      .test('phone', 'Enter a valid 10-digit phone number', (value) => !value || /^[0-9]{10}$/.test(value)),
    salary: yup
      .string()
      .optional()
      .test('num', 'Enter a valid amount', (value) => !value || /^[0-9]+(\.[0-9]{1,2})?$/.test(value)),
    employmentType: yup
      .mixed<'FULL_TIME' | 'PART_TIME' | 'CONTRACT'>()
      .oneOf(['FULL_TIME', 'PART_TIME', 'CONTRACT'], 'Employment type is required')
      .required('Employment type is required'),
  });
}

export type TeacherFormValues = yup.InferType<ReturnType<typeof buildTeacherSchema>>;
