import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import { useSnackbar } from 'notistack';
import authApi from '@/api/authApi';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { logout } from '@/store/authSlice';
import { changePasswordSchema, type ChangePasswordFormValues } from './authSchemas';

/**
 * The password change a school-provisioned account must complete before anything
 * else works.
 *
 * <p>Reached automatically: the login response carries {@code mustChangePassword},
 * and the router sends such a session here instead of to the dashboard. The API
 * enforces the same thing independently — {@code PasswordChangeRequiredFilter}
 * refuses every other endpoint — so this screen is the convenient path to a rule
 * that holds whether or not the client cooperates.
 *
 * <p>It ends at the sign-in screen rather than the dashboard, and that is not
 * politeness: changing a password revokes every refresh token the account has, so
 * the session this screen is running in is finished the moment the change succeeds.
 * Signing out explicitly and saying why is better than letting the next request
 * fail on a token that has just been invalidated.
 */
export function FirstLoginPasswordPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const mustChangePassword = useAppSelector((state) => state.auth.mustChangePassword);

  // Nobody should be able to reach this by typing the URL: an account that has
  // already chosen a password has nothing to do here, and the endpoint behind the
  // form would refuse it anyway.
  useEffect(() => {
    if (!mustChangePassword) {
      navigate('/app/dashboard', { replace: true });
    }
  }, [mustChangePassword, navigate]);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<ChangePasswordFormValues>({ resolver: yupResolver(changePasswordSchema) });

  const onSubmit = async (values: ChangePasswordFormValues) => {
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });
      enqueueSnackbar('Password changed. Please sign in with your new password.', {
        variant: 'success',
      });
      // Every refresh token was revoked by that call, so the session is already
      // over on the server. Clearing it here keeps the two in step.
      dispatch(logout());
      navigate('/login', { replace: true });
    } catch (error) {
      const message =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not change the password.';
      // Attached to the field it is about where possible — "current password is
      // incorrect" under a general banner makes the reader check the new one.
      if (message.toLowerCase().includes('current password')) {
        setError('currentPassword', { message });
      } else {
        setError('newPassword', { message });
      }
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Choose your password
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {user?.username
          ? `You are signed in as ${user.username}.`
          : 'You are signed in.'}{' '}
        This account is still using the password the school sent you.
      </Typography>

      <Alert severity="info" sx={{ mb: 3 }}>
        Pick a password only you know. The one from your email stops working straight
        away, and you will be asked to sign in again with the new one.
      </Alert>

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Password from your email"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="current-password"
          autoFocus
          {...register('currentPassword')}
          error={!!errors.currentPassword}
          helperText={errors.currentPassword?.message}
        />
        <TextField
          label="New password"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="new-password"
          {...register('newPassword')}
          error={!!errors.newPassword}
          helperText={
            errors.newPassword?.message ??
            'At least 8 characters, with an uppercase letter, a lowercase letter, a number and a symbol.'
          }
        />
        <TextField
          label="Confirm new password"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="new-password"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
        />

        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          disabled={isSubmitting}
          sx={{ mt: 3 }}
          startIcon={isSubmitting ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {isSubmitting ? 'Saving…' : 'Set password and sign in again'}
        </Button>
      </Box>
    </Box>
  );
}

export default FirstLoginPasswordPage;
