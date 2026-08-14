import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Avatar from '@mui/material/Avatar';
import Badge from '@mui/material/Badge';
import Stack from '@mui/material/Stack';
import Divider from '@mui/material/Divider';
import Typography from '@mui/material/Typography';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import CircularProgress from '@mui/material/CircularProgress';
import Tooltip from '@mui/material/Tooltip';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import PhotoCameraOutlinedIcon from '@mui/icons-material/PhotoCameraOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import { useForm, useFieldArray, Controller, type Control } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import studentsApi, { type GuardianPayload, type StudentPayload } from '@/api/studentsApi';
import classesApi from '@/api/classesApi';
import academicYearsApi from '@/api/academicYearsApi';
import type { AcademicYear, SchoolClass, Section } from '@/types';
import { makeStudentSchema, type StudentFormValues } from './StudentFormPage.schema';

const RELATION_OPTIONS = ['Father', 'Mother', 'Guardian', 'Grandfather', 'Grandmother', 'Uncle', 'Aunt', 'Other'];
const BLOOD_GROUP_OPTIONS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

const emptyGuardian = { name: '', relation: '', occupation: '', phone: '', email: '', address: '', isPrimary: true };

/** Add/edit student: personal, guardians, address, academic sections + photo upload. Used for `new` and `:id/edit`. */
/**
 * A `<TextField select>` wired through react-hook-form's Controller.
 *
 * MUI's Select is controlled: `register()` supplies name/onChange/ref, but the
 * displayed value comes from the `value` prop, which register never sets. Bound
 * that way a select works while typing yet renders **empty** after `reset()` —
 * which is exactly why editing a student showed blank Gender, Blood Group,
 * Class, Section and Academic Year, while the Date of Birth beside them (already
 * using Controller) populated correctly.
 *
 * `value ?? ''` matters as well: passing undefined makes MUI treat the field as
 * uncontrolled and warn the first time a real value arrives.
 */
function ControlledSelect({
  name,
  control,
  label,
  children,
  disabled,
  errorMessage,
  onAfterChange,
}: {
  name: string;
  control: Control<StudentFormValues>;
  label: string;
  children: React.ReactNode;
  disabled?: boolean;
  errorMessage?: string;
  onAfterChange?: (value: string) => void;
}) {
  return (
    <Controller
      name={name as never}
      control={control}
      render={({ field }) => (
        <TextField
          select
          fullWidth
          label={label}
          disabled={disabled}
          error={!!errorMessage}
          helperText={errorMessage}
          {...field}
          value={(field.value as string | undefined) ?? ''}
          onChange={(event) => {
            field.onChange(event);
            onAfterChange?.(event.target.value);
          }}
        >
          {children}
        </TextField>
      )}
    />
  );
}

export function StudentFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id && id !== 'new';
  const studentId = isEdit ? Number(id) : undefined;
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [loadingInitial, setLoadingInitial] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);
  const [admissionNumber, setAdmissionNumber] = useState<string | null>(null);

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [years, setYears] = useState<AcademicYear[]>([]);

  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);

  const [guardianDeleteIndex, setGuardianDeleteIndex] = useState<number | null>(null);
  const [savingGuardianIndex, setSavingGuardianIndex] = useState<number | null>(null);

  const isFirstClassEffect = useRef(true);

  const {
    register,
    control,
    handleSubmit,
    watch,
    reset,
    setValue,
    getValues,
    trigger,
    formState: { errors },
  } = useForm<StudentFormValues>({
    // Guardian rules apply on create only; on edit they are saved through their
    // own endpoints and must not gate this form.
    resolver: yupResolver(makeStudentSchema(isEdit)) as any,
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      bloodGroup: '',
      religion: '',
      category: '',
      address: '',
      city: '',
      state: '',
      pincode: '',
      classId: '',
      sectionId: '',
      academicYearId: '',
      rollNumber: '',
      guardians: [emptyGuardian],
    },
  });

  // `keyName: 'fieldKey'` avoids RHF's default 'id' key colliding with the
  // guardian's own domain `id` (used to tell an existing guardian from a new one).
  const { fields, append, remove } = useFieldArray({ control, name: 'guardians', keyName: 'fieldKey' });
  const watchedClassId = watch('classId');

  // Load lookups (classes, academic years) once.
  useEffect(() => {
    classesApi
      .list({ size: 200, sort: 'className,asc' })
      .then((res) => setClasses(res.data.content))
      .catch(() => enqueueSnackbar('Could not load classes.', { variant: 'error' }));
    academicYearsApi
      .list()
      .then((res) => {
        setYears(res.data);
        if (!isEdit) {
          const current = res.data.find((y) => y.isCurrent);
          if (current) setValue('academicYearId', String(current.id));
        }
      })
      .catch(() => enqueueSnackbar('Could not load academic years.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Cascade sections whenever the selected class changes; skip clearing the
  // section on the very first run so hydrated edit data isn't wiped out.
  useEffect(() => {
    if (!watchedClassId) {
      setSections([]);
      return;
    }
    classesApi
      .listSections(Number(watchedClassId))
      .then((res) => {
        setSections(res.data);
        if (isFirstClassEffect.current) {
          isFirstClassEffect.current = false;
        } else {
          setValue('sectionId', '');
        }
      })
      .catch(() => enqueueSnackbar('Could not load sections for that class.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [watchedClassId]);

  // Hydrate the form when editing an existing student.
  useEffect(() => {
    if (!isEdit || !studentId) return;
    (async () => {
      try {
        const res = await studentsApi.getById(studentId);
        const student = res.data;
        setAdmissionNumber(student.admissionNumber);
        setPhotoPreview(student.photoUrl);
        reset({
          firstName: student.firstName ?? '',
          lastName: student.lastName ?? '',
          email: student.email ?? '',
          phone: student.phone ?? '',
          gender: student.gender,
          dateOfBirth: dayjs(student.dateOfBirth) as any,
          bloodGroup: student.bloodGroup ?? '',
          religion: student.religion ?? '',
          category: student.category ?? '',
          address: student.address ?? '',
          city: student.city ?? '',
          state: student.state ?? '',
          pincode: student.pincode ?? '',
          classId: String(student.classId),
          sectionId: String(student.sectionId),
          academicYearId: String(student.academicYearId),
          rollNumber: student.rollNumber,
          admissionDate: dayjs(student.admissionDate) as any,
          guardians:
            student.guardians && student.guardians.length > 0
              ? student.guardians.map((g) => ({
                  id: g.id,
                  name: g.name,
                  relation: g.relation,
                  occupation: g.occupation ?? '',
                  phone: g.phone,
                  email: g.email ?? '',
                  address: g.address ?? '',
                  isPrimary: g.isPrimary,
                }))
              : [emptyGuardian],
        });
      } catch (err: any) {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this student.', {
          variant: 'error',
        });
      } finally {
        setLoadingInitial(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isEdit, studentId]);

  const handlePrimaryChange = (index: number) => {
    fields.forEach((_, i) => setValue(`guardians.${i}.isPrimary`, i === index));
  };

  const handlePhotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Show the local file immediately so the avatar responds while the upload is
    // still in flight. Replaced with the server URL once it lands.
    const localPreview = URL.createObjectURL(file);
    setPhotoFile(file);
    setPhotoPreview(localPreview);

    // Reset the input so re-picking the same file fires onChange again — without
    // this, a failed upload cannot be retried by selecting the same photo.
    e.target.value = '';

    if (!isEdit || !studentId) return;

    setUploadingPhoto(true);
    try {
      const res = await studentsApi.uploadPhoto(studentId, file);
      if (res.data?.photoUrl) {
        setPhotoPreview(res.data.photoUrl);
        // The blob is no longer displayed; release it rather than leaking one
        // object URL per photo change for the life of the page.
        URL.revokeObjectURL(localPreview);
        // Uploaded already, so onSubmit must not upload it a second time.
        setPhotoFile(null);
      }
      enqueueSnackbar('Photo updated.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(
        err?.response?.data?.message ?? 'Could not upload photo. Check the file is a JPG/PNG under 2MB.',
        { variant: 'error' },
      );
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleSaveGuardianRow = async (index: number) => {
    if (!studentId) return;
    const valid = await trigger(`guardians.${index}`);
    if (!valid) return;
    const row = getValues(`guardians.${index}`);
    const payload: GuardianPayload = {
      name: row.name,
      relation: row.relation,
      occupation: row.occupation || undefined,
      phone: row.phone,
      email: row.email || undefined,
      address: row.address || undefined,
      isPrimary: !!row.isPrimary,
    };
    setSavingGuardianIndex(index);
    try {
      if (row.id) {
        await studentsApi.updateGuardian(studentId, row.id, payload);
      } else {
        const created = await studentsApi.addGuardian(studentId, payload);
        setValue(`guardians.${index}.id`, created.data.id);
      }
      enqueueSnackbar('Guardian saved.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this guardian.', {
        variant: 'error',
      });
    } finally {
      setSavingGuardianIndex(null);
    }
  };

  const handleDeleteGuardianRow = async (index: number) => {
    const row = getValues(`guardians.${index}`);
    if (isEdit && studentId && row.id) {
      try {
        await studentsApi.removeGuardian(studentId, row.id);
        enqueueSnackbar('Guardian removed.', { variant: 'success' });
      } catch (err: any) {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not remove this guardian.', {
          variant: 'error',
        });
        setGuardianDeleteIndex(null);
        return;
      }
    }
    remove(index);
    setGuardianDeleteIndex(null);
  };

  /**
   * Runs when validation blocks the submit.
   *
   * Without this, `handleSubmit` simply does not call `onSubmit` and the button
   * looks broken — the failing field is often scrolled out of view, or on a
   * guardian row the user never touched. Naming the fields and scrolling to the
   * first one turns a dead button into an ordinary correctable error.
   */
  const onInvalid = (formErrors: Record<string, unknown>) => {
    const labels: Record<string, string> = {
      firstName: 'First name',
      lastName: 'Last name',
      email: 'Email',
      phone: 'Phone',
      gender: 'Gender',
      dateOfBirth: 'Date of birth',
      address: 'Address',
      city: 'City',
      state: 'State',
      pincode: 'Pincode',
      classId: 'Class',
      sectionId: 'Section',
      academicYearId: 'Academic year',
      rollNumber: 'Roll number',
      admissionDate: 'Admission date',
      guardians: 'Guardians',
    };

    const failed = Object.keys(formErrors).map((key) => labels[key] ?? key);
    enqueueSnackbar(
      failed.length
        ? `Please check: ${failed.join(', ')}`
        : 'Some fields need attention before saving.',
      { variant: 'warning' },
    );

    const firstField = Object.keys(formErrors)[0];
    if (firstField) {
      document
        .querySelector(`[name="${firstField}"]`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  const onSubmit = async (values: StudentFormValues) => {
    setSubmitting(true);
    try {
      const payload: StudentPayload = {
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email || undefined,
        phone: values.phone || undefined,
        classId: Number(values.classId),
        sectionId: Number(values.sectionId),
        academicYearId: Number(values.academicYearId),
        rollNumber: values.rollNumber,
        admissionDate: dayjs(values.admissionDate).format('YYYY-MM-DD'),
        dateOfBirth: dayjs(values.dateOfBirth).format('YYYY-MM-DD'),
        gender: values.gender,
        bloodGroup: values.bloodGroup || undefined,
        religion: values.religion || undefined,
        category: values.category || undefined,
        address: values.address,
        city: values.city,
        state: values.state,
        pincode: values.pincode || undefined,
      };

      if (isEdit && studentId) {
        await studentsApi.update(studentId, payload);
        enqueueSnackbar('Student updated successfully.', { variant: 'success' });
        navigate(`/app/students/${studentId}`);
      } else {
        payload.guardians = values.guardians.map(
          (g): GuardianPayload => ({
            name: g.name,
            relation: g.relation,
            occupation: g.occupation || undefined,
            phone: g.phone,
            email: g.email || undefined,
            address: g.address || undefined,
            isPrimary: !!g.isPrimary,
          }),
        );
        const created = await studentsApi.create(payload);
        if (photoFile) {
          try {
            await studentsApi.uploadPhoto(created.data.id, photoFile);
          } catch {
            enqueueSnackbar('Student was created, but the photo upload failed.', { variant: 'warning' });
          }
        }
        enqueueSnackbar('Student admitted successfully.', { variant: 'success' });
        navigate(`/app/students/${created.data.id}`);
      }
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this student. Please try again.', {
        variant: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingInitial) return <PageLoader label="Loading student..." />;

  return (
    <Box>
      <PageHeader
        title={isEdit ? 'Edit Student' : 'Add Student'}
        subtitle={isEdit ? 'Update this student’s record' : 'Admit a new student with guardian and academic details'}
        breadcrumbs={[
          { label: 'Dashboard', to: '/app/dashboard' },
          { label: 'Students', to: '/app/students' },
          { label: isEdit ? 'Edit' : 'Add' },
        ]}
      />

      <Box component="form" onSubmit={handleSubmit(onSubmit, onInvalid)} noValidate>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent sx={{ textAlign: 'center', py: 4 }}>
                <Badge
                  overlap="circular"
                  anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                  badgeContent={
                    <Tooltip title="Upload photo">
                      <IconButton
                        component="label"
                        size="small"
                        sx={{ bgcolor: 'primary.main', color: 'primary.contrastText', '&:hover': { bgcolor: 'primary.dark' } }}
                      >
                        {uploadingPhoto ? (
                          <CircularProgress size={16} color="inherit" />
                        ) : (
                          <PhotoCameraOutlinedIcon fontSize="small" />
                        )}
                        <input hidden type="file" accept="image/*" onChange={handlePhotoChange} />
                      </IconButton>
                    </Tooltip>
                  }
                >
                  <Avatar src={photoPreview ?? undefined} sx={{ width: 100, height: 100, fontSize: 32, mx: 'auto' }}>
                    {getValues('firstName')?.[0] ?? <CloudUploadOutlinedIcon />}
                  </Avatar>
                </Badge>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  {isEdit ? 'Photo updates immediately when selected.' : 'Photo uploads after the student is created.'}
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card>
              <CardHeader title="Personal Information" />
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
                    <ControlledSelect
                      name="gender"
                      control={control}
                      label="Gender"
                      errorMessage={errors.gender?.message as string | undefined}
                    >
                      <MenuItem value="">Select gender</MenuItem>
                      <MenuItem value="MALE">Male</MenuItem>
                      <MenuItem value="FEMALE">Female</MenuItem>
                      <MenuItem value="OTHER">Other</MenuItem>
                    </ControlledSelect>
                  </Grid>
                  <Grid item xs={12} sm={6}>
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
                  <Grid item xs={12} sm={6}>
                    <ControlledSelect name="bloodGroup" control={control} label="Blood Group">
                      <MenuItem value="">Not specified</MenuItem>
                      {BLOOD_GROUP_OPTIONS.map((bg) => (
                        <MenuItem key={bg} value={bg}>
                          {bg}
                        </MenuItem>
                      ))}
                    </ControlledSelect>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Religion" fullWidth {...register('religion')} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Category" fullWidth {...register('category')} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Email (optional)"
                      fullWidth
                      {...register('email')}
                      error={!!errors.email}
                      helperText={errors.email?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Phone (optional)"
                      fullWidth
                      {...register('phone')}
                      error={!!errors.phone}
                      helperText={errors.phone?.message}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Card>
              <CardHeader
                title="Guardian(s)"
                subheader="At least one guardian is required; mark exactly one as primary."
                action={
                  <Button
                    size="small"
                    startIcon={<AddOutlinedIcon />}
                    onClick={() => append({ ...emptyGuardian, isPrimary: fields.length === 0 })}
                  >
                    Add Guardian
                  </Button>
                }
              />
              <CardContent>
                {typeof errors.guardians?.message === 'string' && (
                  <Typography variant="body2" color="error" sx={{ mb: 2 }}>
                    {errors.guardians.message}
                  </Typography>
                )}
                <Stack spacing={2} divider={<Divider />}>
                  {fields.map((field, index) => (
                    <Grid container spacing={2} key={field.fieldKey} alignItems="flex-start">
                      <Grid item xs={12} sm={6} md={3}>
                        <TextField
                          label="Name"
                          fullWidth
                          size="small"
                          {...register(`guardians.${index}.name`)}
                          error={!!errors.guardians?.[index]?.name}
                          helperText={errors.guardians?.[index]?.name?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6} md={2}>
                        <ControlledSelect
                          name={`guardians.${index}.relation`}
                          control={control}
                          label="Relation"
                          errorMessage={errors.guardians?.[index]?.relation?.message as string | undefined}
                        >
                          <MenuItem value="">Select</MenuItem>
                          {RELATION_OPTIONS.map((r) => (
                            <MenuItem key={r} value={r}>
                              {r}
                            </MenuItem>
                          ))}
                        </ControlledSelect>
                      </Grid>
                      <Grid item xs={12} sm={6} md={2}>
                        <TextField label="Occupation" fullWidth size="small" {...register(`guardians.${index}.occupation`)} />
                      </Grid>
                      <Grid item xs={12} sm={6} md={2}>
                        <TextField
                          label="Phone"
                          fullWidth
                          size="small"
                          {...register(`guardians.${index}.phone`)}
                          error={!!errors.guardians?.[index]?.phone}
                          helperText={errors.guardians?.[index]?.phone?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6} md={3}>
                        <TextField
                          label="Email"
                          fullWidth
                          size="small"
                          {...register(`guardians.${index}.email`)}
                          error={!!errors.guardians?.[index]?.email}
                          helperText={errors.guardians?.[index]?.email?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={8}>
                        <TextField label="Address" fullWidth size="small" {...register(`guardians.${index}.address`)} />
                      </Grid>
                      <Grid item xs={6} sm={2}>
                        <Controller
                          name={`guardians.${index}.isPrimary`}
                          control={control}
                          render={({ field: primaryField }) => (
                            <FormControlLabel
                              control={
                                <Checkbox
                                  checked={!!primaryField.value}
                                  onChange={() => handlePrimaryChange(index)}
                                />
                              }
                              label="Primary"
                            />
                          )}
                        />
                      </Grid>
                      <Grid item xs={6} sm={2} sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                        {isEdit && (
                          <Tooltip title="Save guardian">
                            <span>
                              <IconButton
                                size="small"
                                color="primary"
                                onClick={() => handleSaveGuardianRow(index)}
                                disabled={savingGuardianIndex === index}
                              >
                                {savingGuardianIndex === index ? (
                                  <CircularProgress size={18} />
                                ) : (
                                  <SaveOutlinedIcon fontSize="small" />
                                )}
                              </IconButton>
                            </span>
                          </Tooltip>
                        )}
                        <Tooltip title="Remove guardian">
                          <span>
                            <IconButton
                              size="small"
                              onClick={() =>
                                isEdit ? setGuardianDeleteIndex(index) : handleDeleteGuardianRow(index)
                              }
                              disabled={fields.length === 1 && !isEdit}
                            >
                              <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </Grid>
                    </Grid>
                  ))}
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Address" />
              <CardContent>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      label="Address"
                      fullWidth
                      multiline
                      minRows={2}
                      {...register('address')}
                      error={!!errors.address}
                      helperText={errors.address?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="City"
                      fullWidth
                      {...register('city')}
                      error={!!errors.city}
                      helperText={errors.city?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="State"
                      fullWidth
                      {...register('state')}
                      error={!!errors.state}
                      helperText={errors.state?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Pincode"
                      fullWidth
                      {...register('pincode')}
                      error={!!errors.pincode}
                      helperText={errors.pincode?.message}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Academic" />
              <CardContent>
                <Grid container spacing={2}>
                  {isEdit && (
                    <Grid item xs={12}>
                      <TextField
                        label="Admission Number"
                        fullWidth
                        value={admissionNumber ?? ''}
                        disabled
                        helperText="Auto-generated on admission"
                      />
                    </Grid>
                  )}
                  <Grid item xs={12} sm={6}>
                    <ControlledSelect
                      name="classId"
                      control={control}
                      label="Class"
                      errorMessage={errors.classId?.message as string | undefined}
                    >
                      <MenuItem value="">Select a class</MenuItem>
                      {classes.map((cls) => (
                        <MenuItem key={cls.id} value={String(cls.id)}>
                          {cls.className}
                        </MenuItem>
                      ))}
                    </ControlledSelect>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <ControlledSelect
                      name="sectionId"
                      control={control}
                      label="Section"
                      disabled={!watchedClassId}
                      errorMessage={errors.sectionId?.message as string | undefined}
                    >
                      <MenuItem value="">Select a section</MenuItem>
                      {sections.map((sec) => (
                        <MenuItem key={sec.id} value={String(sec.id)}>
                          {sec.sectionName}
                        </MenuItem>
                      ))}
                    </ControlledSelect>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <ControlledSelect
                      name="academicYearId"
                      control={control}
                      label="Academic Year"
                      errorMessage={errors.academicYearId?.message as string | undefined}
                    >
                      <MenuItem value="">Select an academic year</MenuItem>
                      {years.map((year) => (
                        <MenuItem key={year.id} value={String(year.id)}>
                          {year.yearName}
                          {year.isCurrent ? ' (current)' : ''}
                        </MenuItem>
                      ))}
                    </ControlledSelect>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Roll Number"
                      fullWidth
                      {...register('rollNumber')}
                      error={!!errors.rollNumber}
                      helperText={errors.rollNumber?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="admissionDate"
                      control={control}
                      render={({ field }) => (
                        <DatePicker
                          label="Admission Date"
                          value={field.value ?? null}
                          onChange={(value) => field.onChange(value)}
                          slotProps={{
                            textField: {
                              fullWidth: true,
                              error: !!errors.admissionDate,
                              helperText: errors.admissionDate?.message as string | undefined,
                            },
                          }}
                        />
                      )}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Stack direction="row" justifyContent="flex-end" spacing={1.5} sx={{ mt: 3 }}>
          <Button variant="outlined" color="inherit" onClick={() => navigate('/app/students')} disabled={submitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : <SaveOutlinedIcon />}
            disabled={submitting}
          >
            {isEdit ? 'Save Changes' : 'Admit Student'}
          </Button>
        </Stack>
      </Box>

      <ConfirmDialog
        open={guardianDeleteIndex !== null}
        title="Remove guardian"
        message="Are you sure you want to remove this guardian?"
        confirmLabel="Remove"
        destructive
        onConfirm={() => guardianDeleteIndex !== null && handleDeleteGuardianRow(guardianDeleteIndex)}
        onCancel={() => setGuardianDeleteIndex(null)}
      />
    </Box>
  );
}

export default StudentFormPage;
