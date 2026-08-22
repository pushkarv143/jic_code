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

/*
 * registerSchema is gone with the self-registration form. Accounts are created by
 * the school, which generates the username and a first-time password and emails
 * them — see StudentServiceImpl and CredentialGenerator on the backend.
 */

/**
 * The forced password change a school-provisioned account completes at first
 * sign-in. `passwordComplexity` is the same rule the API enforces on the new
 * password, so the client and the server agree on what "strong enough" means.
 */
export const changePasswordSchema = yup.object({
  currentPassword: yup.string().required('Enter the password from your email'),
  newPassword: passwordComplexity,
  confirmPassword: yup
    .string()
    .required('Please confirm your new password')
    .oneOf([yup.ref('newPassword')], 'Passwords must match'),
});
export type ChangePasswordFormValues = yup.InferType<typeof changePasswordSchema>;

export const forgotPasswordSchema = yup.object({
  email: yup.string().email('Enter a valid email').required('Email is required'),
});
export type ForgotPasswordFormValues = yup.InferType<typeof forgotPasswordSchema>;

/**
 * Step 1 of a passcode flow. One field for either an email address or a mobile
 * number, because the server works out which it is and the user should not have to
 * say. Kept separate from forgotPasswordSchema, which is email-only and still used
 * by the older reset-link flow.
 */
export const otpDestinationSchema = yup.object({
  destination: yup
    .string()
    .required('Enter your email address or mobile number')
    .test(
      'email-or-phone',
      'Enter a valid email address or 10-digit mobile number',
      (value) => {
        if (!value) return false;
        const trimmed = value.trim();
        return trimmed.includes('@')
          ? /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)
          // Permissive on purpose, matching the API: digits with optional + - ( )
          // and spaces, and at least ten digits once those are stripped.
          : /^[0-9+\-\s()]+$/.test(trimmed) && trimmed.replace(/[^0-9]/g, '').length >= 10;
      },
    ),
});
export type OtpDestinationFormValues = yup.InferType<typeof otpDestinationSchema>;

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
