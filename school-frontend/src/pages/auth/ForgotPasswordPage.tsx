import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import MarkEmailReadOutlinedIcon from '@mui/icons-material/MarkEmailReadOutlined';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { authApi } from '@/api/authApi';
import { forgotPasswordSchema, type ForgotPasswordFormValues } from './authSchemas';

/** Sends a password reset link; always shows a generic success message regardless of whether the email exists. */
export function ForgotPasswordPage() {
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: yupResolver(forgotPasswordSchema),
  });

  const onSubmit = async (values: ForgotPasswordFormValues) => {
    setSubmitting(true);
    try {
      await authApi.forgotPassword(values);
    } catch {
      // Intentionally ignored: we never reveal whether the email exists or
      // whether the request succeeded, to avoid user enumeration.
    } finally {
      setSubmitting(false);
      setSubmitted(true);
    }
  };

  if (submitted) {
    return (
      <Box sx={{ textAlign: 'center' }}>
        <MarkEmailReadOutlinedIcon color="primary" sx={{ fontSize: 56, mb: 2 }} />
        <Typography variant="h5" fontWeight={800} gutterBottom>
          Check your inbox
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          If an account exists for that email address, we&apos;ve sent instructions to reset your
          password.
        </Typography>
        <Button component={RouterLink} to="/login" variant="outlined" startIcon={<ArrowBackIcon />}>
          Back to Sign In
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={800} gutterBottom>
        Forgot your password?
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Enter the email associated with your account and we&apos;ll send you a link to reset your
        password.
      </Typography>

      <Alert severity="info" sx={{ mb: 2 }}>
        For your security we&apos;ll always show the same confirmation, whether or not the email is
        registered.
      </Alert>

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Email Address"
          fullWidth
          margin="normal"
          autoFocus
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          disabled={submitting}
          sx={{ mt: 2 }}
          startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {submitting ? 'Sending...' : 'Send Reset Link'}
        </Button>
      </Box>

      <Typography variant="body2" align="center" sx={{ mt: 3 }}>
        <Link component={RouterLink} to="/login" underline="hover">
          Back to Sign In
        </Link>
      </Typography>
    </Box>
  );
}

export default ForgotPasswordPage;
