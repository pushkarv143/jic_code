import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import teachersApi, { type TeacherPayload } from '@/api/teachersApi';
import departmentsApi from '@/api/departmentsApi';
import designationsApi from '@/api/designationsApi';
import type { Department, Designation } from '@/types';
import { buildTeacherSchema, type TeacherFormValues } from './TeacherFormPage.schema';

const BLOOD_GROUP_OPTIONS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

/** Add/edit teacher: user-account fields (username/email/password/phone/gender) + teacher fields. */
export function TeacherFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id && id !== 'new';
  const teacherId = isEdit ? Number(id) : undefined;
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [loadingInitial, setLoadingInitial] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [designations, setDesignations] = useState<Designation[]>([]);

  const schema = useMemo(() => buildTeacherSchema(isEdit), [isEdit]);

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TeacherFormValues>({
    resolver: yupResolver(schema) as any,
    defaultValues: {
      username: '',
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      phone: '',
      qualification: '',
      experienceYears: '',
      address: '',
      city: '',
      state: '',
      pincode: '',
      bloodGroup: '',
      emergencyContact: '',
      salary: '',
      departmentId: '',
      designationId: '',
    },
  });

  useEffect(() => {
    departmentsApi
      .list()
      .then((res) => setDepartments(res.data))
      .catch(() => enqueueSnackbar('Could not load departments.', { variant: 'error' }));
    designationsApi
      .list()
      .then((res) => setDesignations(res.data))
      .catch(() => enqueueSnackbar('Could not load designations.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!isEdit || !teacherId) return;
    (async () => {
      try {
        const res = await teachersApi.getById(teacherId);
        const t = res.data;
        reset({
          username: t.username ?? t.user?.username ?? '',
          email: t.email ?? t.user?.email ?? '',
          password: '',
          firstName: t.firstName ?? t.user?.firstName ?? '',
          lastName: t.lastName ?? t.user?.lastName ?? '',
          phone: t.phone ?? t.user?.phone ?? '',
          gender: t.gender ?? undefined,
          departmentId: String(t.departmentId),
          designationId: String(t.designationId),
          qualification: t.qualification ?? '',
          experienceYears: t.experienceYears != null ? String(t.experienceYears) : '',
          joiningDate: dayjs(t.joiningDate) as any,
          dateOfBirth: t.dateOfBirth ? (dayjs(t.dateOfBirth) as any) : null,
          address: t.address ?? '',
          city: t.city ?? '',
          state: t.state ?? '',
          pincode: t.pincode ?? '',
          bloodGroup: t.bloodGroup ?? '',
          emergencyContact: t.emergencyContact ?? '',
          salary: t.salary != null ? String(t.salary) : '',
          employmentType: t.employmentType,
        });
      } catch (err: any) {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this teacher.', { variant: 'error' });
      } finally {
        setLoadingInitial(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isEdit, teacherId]);

  const onSubmit = async (values: TeacherFormValues) => {
    setSubmitting(true);
    try {
      const payload: Partial<TeacherPayload> = {
        username: values.username,
        email: values.email,
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone,
        gender: values.gender,
        departmentId: Number(values.departmentId),
        designationId: Number(values.designationId),
        qualification: values.qualification || undefined,
        experienceYears: values.experienceYears ? Number(values.experienceYears) : undefined,
        joiningDate: dayjs(values.joiningDate).format('YYYY-MM-DD'),
        dateOfBirth: values.dateOfBirth ? dayjs(values.dateOfBirth).format('YYYY-MM-DD') : undefined,
        address: values.address || undefined,
        city: values.city || undefined,
        state: values.state || undefined,
        pincode: values.pincode || undefined,
        bloodGroup: values.bloodGroup || undefined,
        emergencyContact: values.emergencyContact || undefined,
        salary: values.salary ? Number(values.salary) : undefined,
        employmentType: values.employmentType,
      };
      if (!isEdit) payload.password = values.password;

      if (isEdit && teacherId) {
        await teachersApi.update(teacherId, payload);
        enqueueSnackbar('Teacher updated successfully.', { variant: 'success' });
        navigate(`/app/teachers/${teacherId}`);
      } else {
        const created = await teachersApi.create(payload as TeacherPayload);
        enqueueSnackbar('Teacher onboarded successfully.', { variant: 'success' });
        navigate(`/app/teachers/${created.data.id}`);
      }
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this teacher. Please try again.', {
        variant: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingInitial) return <PageLoader label="Loading teacher..." />;

  return (
    <Box>
      <PageHeader
        title={isEdit ? 'Edit Teacher' : 'Add Teacher'}
        subtitle={isEdit ? 'Update this teacher’s record' : 'Onboard a new teacher with account and employment details'}
        breadcrumbs={[
          { label: 'Dashboard', to: '/app/dashboard' },
          { label: 'Teachers', to: '/app/teachers' },
          { label: isEdit ? 'Edit' : 'Add' },
        ]}
      />

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Account Details" />
              <CardContent>
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
                      label="Username"
                      fullWidth
                      disabled={isEdit}
                      {...register('username')}
                      error={!!errors.username}
                      helperText={errors.username?.message}
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
                  {!isEdit && (
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Password"
                        type={showPassword ? 'text' : 'password'}
                        fullWidth
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
                    </Grid>
                  )}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Phone"
                      fullWidth
                      {...register('phone')}
                      error={!!errors.phone}
                      helperText={errors.phone?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      select
                      label="Gender"
                      fullWidth
                      defaultValue=""
                      {...register('gender')}
                      error={!!errors.gender}
                      helperText={errors.gender?.message}
                    >
                      <MenuItem value="">Select gender</MenuItem>
                      <MenuItem value="MALE">Male</MenuItem>
                      <MenuItem value="FEMALE">Female</MenuItem>
                      <MenuItem value="OTHER">Other</MenuItem>
                    </TextField>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Employment Details" />
              <CardContent>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      select
                      label="Department"
                      fullWidth
                      defaultValue=""
                      {...register('departmentId')}
                      error={!!errors.departmentId}
                      helperText={errors.departmentId?.message}
                    >
                      <MenuItem value="">Select a department</MenuItem>
                      {departments.map((d) => (
                        <MenuItem key={d.id} value={String(d.id)}>
                          {d.name}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      select
                      label="Designation"
                      fullWidth
                      defaultValue=""
                      {...register('designationId')}
                      error={!!errors.designationId}
                      helperText={errors.designationId?.message}
                    >
                      <MenuItem value="">Select a designation</MenuItem>
                      {designations.map((d) => (
                        <MenuItem key={d.id} value={String(d.id)}>
                          {d.name}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      select
                      label="Employment Type"
                      fullWidth
                      defaultValue=""
                      {...register('employmentType')}
                      error={!!errors.employmentType}
                      helperText={errors.employmentType?.message}
                    >
                      <MenuItem value="">Select type</MenuItem>
                      <MenuItem value="FULL_TIME">Full Time</MenuItem>
                      <MenuItem value="PART_TIME">Part Time</MenuItem>
                      <MenuItem value="CONTRACT">Contract</MenuItem>
                    </TextField>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Qualification" fullWidth {...register('qualification')} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Experience (years)"
                      fullWidth
                      {...register('experienceYears')}
                      error={!!errors.experienceYears}
                      helperText={errors.experienceYears?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="joiningDate"
                      control={control}
                      render={({ field }) => (
                        <DatePicker
                          label="Joining Date"
                          value={field.value ?? null}
                          onChange={(value) => field.onChange(value)}
                          slotProps={{
                            textField: {
                              fullWidth: true,
                              error: !!errors.joiningDate,
                              helperText: errors.joiningDate?.message as string | undefined,
                            },
                          }}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Salary"
                      fullWidth
                      {...register('salary')}
                      error={!!errors.salary}
                      helperText={errors.salary?.message}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Card>
              <CardHeader title="Personal & Emergency Details" />
              <CardContent>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6} md={3}>
                    <Controller
                      name="dateOfBirth"
                      control={control}
                      render={({ field }) => (
                        <DatePicker
                          label="Date of Birth"
                          value={field.value ?? null}
                          onChange={(value) => field.onChange(value)}
                          disableFuture
                          slotProps={{
                            textField: {
                              fullWidth: true,
                              error: !!errors.dateOfBirth,
                              helperText: errors.dateOfBirth?.message as string | undefined,
                            },
                          }}
                        />
                      )}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6} md={3}>
                    <TextField select label="Blood Group" fullWidth defaultValue="" {...register('bloodGroup')}>
                      <MenuItem value="">Not specified</MenuItem>
                      {BLOOD_GROUP_OPTIONS.map((bg) => (
                        <MenuItem key={bg} value={bg}>
                          {bg}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} sm={6} md={3}>
                    <TextField
                      label="Emergency Contact"
                      fullWidth
                      {...register('emergencyContact')}
                      error={!!errors.emergencyContact}
                      helperText={errors.emergencyContact?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6} md={3}>
                    <TextField label="Pincode" fullWidth {...register('pincode')} error={!!errors.pincode} helperText={errors.pincode?.message} />
                  </Grid>
                  <Grid item xs={12} sm={8}>
                    <TextField label="Address" fullWidth multiline minRows={2} {...register('address')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="City" fullWidth {...register('city')} />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField label="State" fullWidth {...register('state')} />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Stack direction="row" justifyContent="flex-end" spacing={1.5} sx={{ mt: 3 }}>
          <Button variant="outlined" color="inherit" onClick={() => navigate('/app/teachers')} disabled={submitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : <SaveOutlinedIcon />}
            disabled={submitting}
          >
            {isEdit ? 'Save Changes' : 'Onboard Teacher'}
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}

export default TeacherFormPage;
