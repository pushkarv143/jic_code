import * as yup from 'yup';

/** Same password complexity rule enforced by the backend: min 8 chars, upper, lower, digit, special char. */
export const PASSWORD_RULE_MESSAGE =
  'Must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.';

export const passwordComplexity = yup
  .string()
  .required('Password is required')
  .min(8, 'Must be at least 8 characters')
  .matches(/[A-Z]/, 'Must include an uppercase letter')
  .matches(/[a-z]/, 'Must include a lowercase letter')
  .matches(/[0-9]/, 'Must include a digit')
  .matches(/[^A-Za-z0-9]/, 'Must include a special character');

export const profileSchema = yup.object({
  firstName: yup.string().required('First name is required').max(50),
  lastName: yup.string().required('Last name is required').max(50),
  email: yup.string().email('Enter a valid email').required('Email is required'),
  phone: yup
    .string()
    .required('Phone number is required')
    .matches(/^[0-9]{10}$/, 'Enter a valid 10-digit phone number'),
});

export type ProfileFormValues = yup.InferType<typeof profileSchema>;

export const changePasswordSchema = yup.object({
  currentPassword: yup.string().required('Current password is required'),
  newPassword: passwordComplexity,
  confirmPassword: yup
    .string()
    .required('Please confirm your new password')
    .oneOf([yup.ref('newPassword')], 'Passwords must match'),
});

export type ChangePasswordFormValues = yup.InferType<typeof changePasswordSchema>;
