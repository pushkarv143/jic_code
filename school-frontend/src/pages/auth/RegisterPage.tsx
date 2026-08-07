import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import ToggleButton from '@mui/material/ToggleButton';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import FormHelperText from '@mui/material/FormHelperText';
import CircularProgress from '@mui/material/CircularProgress';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import FamilyRestroomOutlinedIcon from '@mui/icons-material/FamilyRestroomOutlined';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import { authApi } from '@/api/authApi';
import { PASSWORD_RULE_MESSAGE, registerSchema, type RegisterFormValues } from './authSchemas';

/**
 * Self-registration page for prospective STUDENT / PARENT accounts (pre-registration).
 * Calls the register endpoint and handles the (expected, in this sandbox) failure
 * gracefully since the backend may not be reachable yet.
 */
export function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: yupResolver(registerSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      username: '',
      role: 'STUDENT',
      password: '',
      confirmPassword: '',
      acceptTerms: false,
    },
  });

  const onSubmit = async (values: RegisterFormValues) => {
    setSubmitting(true);
    try {
      await authApi.register(values);
      enqueueSnackbar('Registration submitted! You can now sign in once your account is approved.', {
        variant: 'success',
      });
      navigate('/login');
    } catch (err: any) {
      // Backend is not guaranteed to be reachable in this sandbox — degrade gracefully.
      enqueueSnackbar(
        err?.response?.data?.message ??
          'Registration service is currently unreachable. Please try again once the school portal is online.',
        { variant: 'warning' },
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={800} gutterBottom>
        Create your account
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Register as a student or parent to access admission status, fees and reports.
      </Typography>

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Controller
          name="role"
          control={control}
          render={({ field }) => (
            <ToggleButtonGroup
              exclusive
              fullWidth
              value={field.value}
              onChange={(_e, value) => value && field.onChange(value)}
              sx={{ mb: 2 }}
            >
              <ToggleButton value="STUDENT">
                <SchoolOutlinedIcon sx={{ mr: 1 }} fontSize="small" /> Student
              </ToggleButton>
              <ToggleButton value="PARENT">
                <FamilyRestroomOutlinedIcon sx={{ mr: 1 }} fontSize="small" /> Parent
              </ToggleButton>
            </ToggleButtonGroup>
          )}
        />

        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              label="First Name"
              fullWidth
              {...register('firstName')}
              error={!!errors.firstName}
              helperText={errors.firstName?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Last Name"
              fullWidth
              {...register('lastName')}
              error={!!errors.lastName}
              helperText={errors.lastName?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Email"
              fullWidth
              {...register('email')}
              error={!!errors.email}
              helperText={errors.email?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Phone"
              fullWidth
              {...register('phone')}
              error={!!errors.phone}
              helperText={errors.phone?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              label="Username"
              fullWidth
              {...register('username')}
              error={!!errors.username}
              helperText={errors.username?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Password"
              type={showPassword ? 'text' : 'password'}
              fullWidth
              {...register('password')}
              error={!!errors.password}
              helperText={errors.password?.message ?? PASSWORD_RULE_MESSAGE}
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
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Confirm Password"
              type={showPassword ? 'text' : 'password'}
              fullWidth
              {...register('confirmPassword')}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
            />
          </Grid>
        </Grid>

        <Box sx={{ mt: 1 }}>
          <FormControlLabel
            control={<Checkbox {...register('acceptTerms')} />}
            label={
              <Typography variant="body2">
                I agree to the school&apos;s Terms of Use and Privacy Policy
              </Typography>
            }
          />
          {errors.acceptTerms && (
            <FormHelperText error>{errors.acceptTerms.message}</FormHelperText>
          )}
        </Box>

        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          disabled={submitting}
          sx={{ mt: 2 }}
          startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {submitting ? 'Submitting...' : 'Create Account'}
        </Button>
      </Box>

      <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 3 }}>
        Already have an account?{' '}
        <Link component={RouterLink} to="/login" underline="hover" fontWeight={600}>
          Sign in
        </Link>
      </Typography>
    </Box>
  );
}

export default RegisterPage;
