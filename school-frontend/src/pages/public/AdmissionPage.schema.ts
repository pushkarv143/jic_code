import * as yup from 'yup';
import dayjs from 'dayjs';

const MIN_AGE_YEARS = 3;
const MAX_AGE_YEARS = 18;

/** Matches the `admission_enquiries` table fields from SCHEMA_CONTRACT.md. */
export const admissionSchema = yup.object({
  studentName: yup.string().required("Student's name is required").max(100),
  parentName: yup.string().required("Parent / guardian's name is required").max(100),
  phone: yup
    .string()
    .required('Phone number is required')
    .matches(/^[0-9]{10}$/, 'Enter a valid 10-digit phone number'),
  email: yup.string().email('Enter a valid email').required('Email is required'),
  classApplying: yup.string().required('Please select a class'),
  dob: yup
    .mixed<dayjs.Dayjs>()
    .required('Date of birth is required')
    .test('is-valid-date', 'Enter a valid date of birth', (value) => !!value && dayjs(value).isValid())
    .test(
      'age-range',
      `Student's age must be between ${MIN_AGE_YEARS} and ${MAX_AGE_YEARS} years`,
      (value) => {
        if (!value) return false;
        const age = dayjs().diff(dayjs(value), 'year');
        return age >= MIN_AGE_YEARS && age <= MAX_AGE_YEARS;
      },
    ),
  address: yup.string().required('Address is required').min(10, 'Please provide a fuller address').max(300),
});

export type AdmissionFormValues = yup.InferType<typeof admissionSchema>;

export const CLASS_OPTIONS = [
  'Nursery',
  'LKG',
  'UKG',
  'Class 1',
  'Class 2',
  'Class 3',
  'Class 4',
  'Class 5',
  'Class 6',
  'Class 7',
  'Class 8',
  'Class 9',
  'Class 10',
  'Class 11',
  'Class 12',
];
