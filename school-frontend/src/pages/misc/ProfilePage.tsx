import { useState } from 'react';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import Stack from '@mui/material/Stack';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import LockResetOutlinedIcon from '@mui/icons-material/LockResetOutlined';
import PageHeader from '@/components/common/PageHeader';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { updateUser } from '@/store/authSlice';
import { authApi } from '@/api/authApi';
import { formatRoleLabel } from '@/utils/format';
import dayjs from 'dayjs';
import {
  profileSchema,
  changePasswordSchema,
  PASSWORD_RULE_MESSAGE,
  type ProfileFormValues,
  type ChangePasswordFormValues,
} from './ProfilePage.schema';

/** View/edit own profile plus a real change-password form wired to the auth API. */
export function ProfilePage() {
  const user = useAppSelector((state) => state.auth.user);
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    formState: { errors: profileErrors, isDirty: profileDirty },
  } = useForm<ProfileFormValues>({
    resolver: yupResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      email: user?.email ?? '',
      phone: user?.phone ?? '',
    },
  });

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    reset: resetPasswordForm,
    formState: { errors: passwordErrors },
  } = useForm<ChangePasswordFormValues>({
    resolver: yupResolver(changePasswordSchema),
  });

  const onProfileSubmit = async (values: ProfileFormValues) => {
    if (!user) return;
    setSavingProfile(true);
    try {
      // Round 1 has no dedicated "update profile" endpoint in the contract yet;
      // update the local/Redux copy so the UI reflects changes immediately.
      dispatch(updateUser({ ...user, ...values }));
      enqueueSnackbar('Profile updated successfully.', { variant: 'success' });
    } finally {
      setSavingProfile(false);
    }
  };

  const onPasswordSubmit = async (values: ChangePasswordFormValues) => {
    setSavingPassword(true);
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
        // The form has always collected this and dropped it here; the API
        // validates it as mandatory, so the request failed before reaching the
        // password check.
        confirmPassword: values.confirmPassword,
      });
      enqueueSnackbar('Password changed successfully.', { variant: 'success' });
      resetPasswordForm();
    } catch (err: any) {
      const message =
        err?.response?.data?.message ??
        'Could not change password right now (backend unreachable). Please try again later.';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSavingPassword(false);
    }
  };

  if (!user) return null;

  const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();

  return (
    <Box>
      <PageHeader
        title="My Profile"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Profile' }]}
        subtitle="View and manage your personal information and security settings"
      />

      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent sx={{ textAlign: 'center', py: 4 }}>
              <Avatar
                sx={{
                  width: 92,
                  height: 92,
                  mx: 'auto',
                  bgcolor: 'primary.main',
                  fontSize: 32,
                  fontWeight: 700,
                  mb: 2,
                }}
              >
                {initials}
              </Avatar>
              <Typography variant="h6" fontWeight={700}>
                {user.firstName} {user.lastName}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {user.email}
              </Typography>
              <Chip label={formatRoleLabel(user.role)} color="primary" size="small" sx={{ mt: 1 }} />
              <Divider sx={{ my: 2.5 }} />
              <Stack spacing={1.25} sx={{ textAlign: 'left' }}>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">
                    Username
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {user.username}
                  </Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">
                    Phone
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {user.phone ?? '-'}
                  </Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">
                    Gender
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {user.gender ?? '-'}
                  </Typography>
                </Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="body2" color="text.secondary">
                    Last Login
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {user.lastLogin ? dayjs(user.lastLogin).format('DD MMM YYYY, hh:mm A') : '-'}
                  </Typography>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card sx={{ mb: 3 }}>
            <CardHeader title="Personal Information" subheader="Update your basic account details" />
            <CardContent>
              <Box component="form" onSubmit={handleProfileSubmit(onProfileSubmit)} noValidate>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="First Name"
                      fullWidth
                      {...registerProfile('firstName')}
                      error={!!profileErrors.firstName}
                      helperText={profileErrors.firstName?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Last Name"
                      fullWidth
                      {...registerProfile('lastName')}
                      error={!!profileErrors.lastName}
                      helperText={profileErrors.lastName?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Email"
                      fullWidth
                      {...registerProfile('email')}
                      error={!!profileErrors.email}
                      helperText={profileErrors.email?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Phone"
                      fullWidth
                      {...registerProfile('phone')}
                      error={!!profileErrors.phone}
                      helperText={profileErrors.phone?.message}
                    />
                  </Grid>
                </Grid>
                <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                  <Button
                    type="submit"
                    variant="contained"
                    startIcon={<SaveOutlinedIcon />}
                    disabled={!profileDirty || savingProfile}
                  >
                    Save Changes
                  </Button>
                </Box>
              </Box>
            </CardContent>
          </Card>

          <Card>
            <CardHeader title="Change Password" subheader={PASSWORD_RULE_MESSAGE} />
            <CardContent>
              <Box component="form" onSubmit={handlePasswordSubmit(onPasswordSubmit)} noValidate>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      label="Current Password"
                      type={showCurrent ? 'text' : 'password'}
                      fullWidth
                      {...registerPassword('currentPassword')}
                      error={!!passwordErrors.currentPassword}
                      helperText={passwordErrors.currentPassword?.message}
                      InputProps={{
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton onClick={() => setShowCurrent((s) => !s)} edge="end">
                              {showCurrent ? <VisibilityOffOutlinedIcon /> : <VisibilityOutlinedIcon />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="New Password"
                      type={showNew ? 'text' : 'password'}
                      fullWidth
                      {...registerPassword('newPassword')}
                      error={!!passwordErrors.newPassword}
                      helperText={passwordErrors.newPassword?.message}
                      InputProps={{
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton onClick={() => setShowNew((s) => !s)} edge="end">
                              {showNew ? <VisibilityOffOutlinedIcon /> : <VisibilityOutlinedIcon />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Confirm New Password"
                      type={showNew ? 'text' : 'password'}
                      fullWidth
                      {...registerPassword('confirmPassword')}
                      error={!!passwordErrors.confirmPassword}
                      helperText={passwordErrors.confirmPassword?.message}
                    />
                  </Grid>
                </Grid>
                <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                  <Button
                    type="submit"
                    variant="contained"
                    color="secondary"
                    startIcon={<LockResetOutlinedIcon />}
                    disabled={savingPassword}
                  >
                    Update Password
                  </Button>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default ProfilePage;
