import { useState } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Alert from '@mui/material/Alert';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import CircularProgress from '@mui/material/CircularProgress';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import { authApi } from '@/api/authApi';
import {
  PASSWORD_RULE_MESSAGE,
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from './authSchemas';

/** Reads ?token= from the URL and submits a new password matching the backend's complexity rule. */
export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: yupResolver(resetPasswordSchema),
  });

  const onSubmit = async (values: ResetPasswordFormValues) => {
    setSubmitting(true);
    try {
      await authApi.resetPassword({ token, newPassword: values.newPassword });
      enqueueSnackbar('Password reset successfully. Please sign in.', { variant: 'success' });
      navigate('/login');
    } catch (err: any) {
      enqueueSnackbar(
        err?.response?.data?.message ??
          'Could not reset password right now — the link may have expired or the server is unreachable.',
        { variant: 'error' },
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={800} gutterBottom>
        Reset your password
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Choose a new password for your account.
      </Typography>

      {!token && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          No reset token was found in the link. Please use the link from your email, or request a
          new one.
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="New Password"
          type={showPassword ? 'text' : 'password'}
          fullWidth
          margin="normal"
          autoFocus
          {...register('newPassword')}
          error={!!errors.newPassword}
          helperText={errors.newPassword?.message ?? PASSWORD_RULE_MESSAGE}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={() => setShowPassword((s) => !s)} edge="end" tabIndex={-1}>
                  {showPassword ? <VisibilityOffOutlinedIcon /> : <VisibilityOutlinedIcon />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
        <TextField
          label="Confirm New Password"
          type={showPassword ? 'text' : 'password'}
          fullWidth
          margin="normal"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          disabled={submitting || !token}
          sx={{ mt: 2 }}
          startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {submitting ? 'Resetting...' : 'Reset Password'}
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

export default ResetPasswordPage;
