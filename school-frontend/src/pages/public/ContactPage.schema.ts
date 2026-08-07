import * as yup from 'yup';

export const contactSchema = yup.object({
  name: yup.string().required('Name is required').max(100),
  email: yup.string().email('Enter a valid email').required('Email is required'),
  phone: yup
    .string()
    .required('Phone number is required')
    .matches(/^[0-9]{10}$/, 'Enter a valid 10-digit phone number'),
  subject: yup.string().required('Subject is required').max(150),
  message: yup
    .string()
    .required('Message is required')
    .min(20, 'Please provide at least 20 characters')
    .max(1000),
});

export type ContactFormValues = yup.InferType<typeof contactSchema>;
