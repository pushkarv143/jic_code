import * as yup from 'yup';

/** Shared password complexity rule matching the backend: min 8 chars, upper, lower, digit, special char. */
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

export const loginSchema = yup.object({
  username: yup.string().required('Username or email is required'),
  password: yup.string().required('Password is required'),
  remember: yup.boolean().default(true),
});
export type LoginFormValues = yup.InferType<typeof loginSchema>;

export const registerSchema = yup.object({
  firstName: yup.string().required('First name is required').max(50),
  lastName: yup.string().required('Last name is required').max(50),
  email: yup.string().email('Enter a valid email').required('Email is required'),
  phone: yup
    .string()
    .required('Phone number is required')
    .matches(/^[0-9]{10}$/, 'Enter a valid 10-digit phone number'),
  username: yup
    .string()
    .required('Username is required')
    .min(4, 'Must be at least 4 characters')
    .matches(/^[a-zA-Z0-9._]+$/, 'Only letters, numbers, dots and underscores allowed'),
  role: yup
    .mixed<'STUDENT' | 'PARENT'>()
    .oneOf(['STUDENT', 'PARENT'])
    .required('Please select who you are registering as'),
  password: passwordComplexity,
  confirmPassword: yup
    .string()
    .required('Please confirm your password')
    .oneOf([yup.ref('password')], 'Passwords must match'),
  acceptTerms: yup
    .boolean()
    .oneOf([true], 'You must accept the terms and conditions')
    .required(),
});
export type RegisterFormValues = yup.InferType<typeof registerSchema>;

export const forgotPasswordSchema = yup.object({
  email: yup.string().email('Enter a valid email').required('Email is required'),
});
export type ForgotPasswordFormValues = yup.InferType<typeof forgotPasswordSchema>;

/** Step 2 of recovery: the six digits sent to the address given in step 1. */
export const otpCodeSchema = yup.object({
  code: yup
    .string()
    .required('Enter the 6-digit code')
    .matches(/^[0-9]{6}$/, 'The code is 6 digits'),
});
export type OtpCodeFormValues = yup.InferType<typeof otpCodeSchema>;

export const resetPasswordSchema = yup.object({
  newPassword: passwordComplexity,
  confirmPassword: yup
    .string()
    .required('Please confirm your new password')
    .oneOf([yup.ref('newPassword')], 'Passwords must match'),
});
export type ResetPasswordFormValues = yup.InferType<typeof resetPasswordSchema>;
