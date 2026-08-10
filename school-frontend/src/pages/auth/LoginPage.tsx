import { useState } from 'react';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import Alert from '@mui/material/Alert';
import Divider from '@mui/material/Divider';
import CircularProgress from '@mui/material/CircularProgress';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import { useAppDispatch } from '@/store/hooks';
import { setCredentials } from '@/store/authSlice';
import { authApi } from '@/api/authApi';
import { loginSchema, type LoginFormValues } from './authSchemas';

/** Real login page: RHF + Yup validated, calls authApi.login, redirects to /app/dashboard on success. */
export function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { enqueueSnackbar } = useSnackbar();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: yupResolver(loginSchema),
    defaultValues: { username: '', password: '', remember: true },
  });

  const fromLocation = (location.state as { from?: { pathname?: string } })?.from;
  const redirectTo = fromLocation?.pathname || '/app/dashboard';

  const onSubmit = async (values: LoginFormValues) => {
    setSubmitting(true);
    setFormError(null);
    try {
      const response = await authApi.login({ username: values.username, password: values.password });
      dispatch(setCredentials(response));
      enqueueSnackbar(`Welcome back, ${response.user.firstName}!`, { variant: 'success' });
      navigate(redirectTo, { replace: true });
    } catch (err: any) {
      const message =
        err?.response?.data?.message ??
        'Could not sign in. The server may be unreachable — please try again shortly.';
      setFormError(message);
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={800} gutterBottom>
        Welcome back
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Sign in to access your Greenwood School dashboard.
      </Typography>

      {formError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {formError}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Username or Email"
          fullWidth
          margin="normal"
          autoFocus
          {...register('username')}
          error={!!errors.username}
          helperText={errors.username?.message}
        />
        <TextField
          label="Password"
          type={showPassword ? 'text' : 'password'}
          fullWidth
          margin="normal"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
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

        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 0.5 }}>
          <FormControlLabel control={<Checkbox defaultChecked {...register('remember')} />} label="Remember me" />
          <Link component={RouterLink} to="/forgot-password" variant="body2" underline="hover">
            Forgot password?
          </Link>
        </Box>

        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          disabled={submitting}
          sx={{ mt: 3 }}
          startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {submitting ? 'Signing in...' : 'Sign In'}
        </Button>
      </Box>

      <Divider sx={{ my: 3 }}>
        <Typography variant="caption" color="text.secondary">
          New here?
        </Typography>
      </Divider>

      <Button component={RouterLink} to="/register" variant="outlined" fullWidth>
        Create a Student / Parent Account
      </Button>
    </Box>
  );
}

export default LoginPage;
