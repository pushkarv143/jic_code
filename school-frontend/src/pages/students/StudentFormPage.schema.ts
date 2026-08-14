import * as yup from 'yup';
import dayjs from 'dayjs';
import { PHONE_MESSAGE, PHONE_PATTERN } from '@/utils/validationPatterns';

/**
 * Class/Section/Academic Year selects are kept as strings in RHF state (matching
 * the MenuItem-value-as-string pattern already used by AdmissionPage's
 * `classApplying` field) and are converted to numbers only when building the
 * API payload — this sidesteps MUI Select + RHF uncontrolled-field quirks.
 */
function idSelect(label: string) {
  return yup.string().required(`${label} is required`);
}

export const guardianSchema = yup.object({
  id: yup.number().optional(),
  name: yup.string().required('Guardian name is required').max(100),
  relation: yup.string().required('Relation is required').max(50),
  occupation: yup.string().max(100).optional(),
  phone: yup.string().required('Phone number is required').matches(PHONE_PATTERN, PHONE_MESSAGE),
  email: yup
    .string()
    .optional()
    .test('email', 'Enter a valid email', (value) => !value || yup.string().email().isValidSync(value)),
  address: yup.string().max(300).optional(),
  isPrimary: yup.boolean().default(false),
});
export type GuardianFormValues = yup.InferType<typeof guardianSchema>;

const baseStudentSchema = yup.object({
  firstName: yup.string().required('First name is required').max(50),
  lastName: yup.string().required('Last name is required').max(50),
  email: yup
    .string()
    .optional()
    .test('email', 'Enter a valid email', (value) => !value || yup.string().email().isValidSync(value)),
  phone: yup
    .string()
    .optional()
    .test('phone', PHONE_MESSAGE, (value) => !value || PHONE_PATTERN.test(value)),
  gender: yup
    .mixed<'MALE' | 'FEMALE' | 'OTHER'>()
    .oneOf(['MALE', 'FEMALE', 'OTHER'], 'Gender is required')
    .required('Gender is required'),
  dateOfBirth: yup
    .mixed<dayjs.Dayjs>()
    .required('Date of birth is required')
    .test('is-valid', 'Enter a valid date of birth', (value) => !!value && dayjs(value).isValid())
    .test('is-past', 'Date of birth must be in the past', (value) => !!value && dayjs(value).isBefore(dayjs(), 'day')),
  bloodGroup: yup.string().max(5).optional(),
  religion: yup.string().max(50).optional(),
  category: yup.string().max(50).optional(),
  address: yup.string().required('Address is required').max(300),
  city: yup.string().required('City is required').max(100),
  state: yup.string().required('State is required').max(100),
  pincode: yup
    .string()
    .optional()
    .test('pincode', 'Enter a valid 6-digit pincode', (value) => !value || /^[0-9]{6}$/.test(value)),
  classId: idSelect('Class'),
  sectionId: idSelect('Section'),
  academicYearId: idSelect('Academic year'),
  rollNumber: yup.string().required('Roll number is required').max(20),
  admissionDate: yup
    .mixed<dayjs.Dayjs>()
    .required('Admission date is required')
    .test('is-valid', 'Enter a valid admission date', (value) => !!value && dayjs(value).isValid()),
  guardians: yup
    .array()
    .of(guardianSchema)
    .min(1, 'At least one guardian is required')
    .required()
    .test('one-primary', 'Mark exactly one guardian as primary', (value) =>
      !!value && value.length > 0 && value.filter((g) => g?.isPrimary).length === 1,
    ),
});

/**
 * Guardian rules must not apply when editing.
 *
 * On create, guardians are part of the same payload, so requiring one — and
 * exactly one primary — is correct. On edit they are managed through their own
 * endpoints (add/update/remove guardian) and `onSubmit` does not send them at
 * all. Validating them there blocks the whole form over rows the save will not
 * touch: a student with no guardian, or two primaries, becomes uneditable with
 * no visible reason.
 */
const editStudentSchema = baseStudentSchema.shape({
  guardians: yup.array().of(guardianSchema).optional().default([]),
});

export const studentSchema = baseStudentSchema;

export function makeStudentSchema(isEdit: boolean) {
  return isEdit ? editStudentSchema : baseStudentSchema;
}

export type StudentFormValues = yup.InferType<typeof baseStudentSchema>;
