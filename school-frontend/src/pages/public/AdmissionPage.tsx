import { useState } from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import CircularProgress from '@mui/material/CircularProgress';
import { alpha } from '@mui/material/styles';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import ChecklistOutlinedIcon from '@mui/icons-material/ChecklistOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import admissionApi from '@/api/admissionApi';
import { admissionSchema, CLASS_OPTIONS, type AdmissionFormValues } from './AdmissionPage.schema';

const PROCESS_STEPS = [
  { icon: <DescriptionOutlinedIcon />, title: 'Submit Enquiry', text: 'Fill the admission enquiry form with student and parent details.' },
  { icon: <ChecklistOutlinedIcon />, title: 'Document Verification', text: 'Our admissions team reviews submitted documents and eligibility.' },
  { icon: <EventAvailableOutlinedIcon />, title: 'Interaction & Assessment', text: 'A friendly interaction session is scheduled for the applicant.' },
  { icon: <SchoolOutlinedIcon />, title: 'Confirmation', text: 'Receive an admission offer and complete the fee formalities.' },
];

/** Admission info + a real enquiry form matching the admission_enquiries schema. */
export function AdmissionPage() {
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<AdmissionFormValues>({
    resolver: yupResolver(admissionSchema),
  });

  const onSubmit = async (values: AdmissionFormValues) => {
    setSubmitting(true);
    try {
      await admissionApi.submitEnquiry({
        studentName: values.studentName,
        parentName: values.parentName,
        phone: values.phone,
        email: values.email,
        classApplying: values.classApplying,
        dob: dayjs(values.dob).format('YYYY-MM-DD'),
        address: values.address,
      });
      setSubmitted(true);
      enqueueSnackbar('Admission enquiry submitted successfully!', { variant: 'success' });
      reset();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not submit your enquiry. Please try again.', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            Admissions 2026-2027
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            Begin your child&apos;s Greenwood journey
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 560, mt: 2 }}>
            Admissions are open for Nursery through Class 12. Submit an enquiry below and our
            admissions team will guide you through every step.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Box sx={{ textAlign: 'center', mb: 5 }}>
          <Typography variant="h4" fontWeight={800}>
            Our Admission Process
          </Typography>
        </Box>
        <Grid container spacing={3} sx={{ mb: 2 }}>
          {PROCESS_STEPS.map((step, idx) => (
            <Grid item xs={12} sm={6} md={3} key={step.title}>
              <Card sx={{ height: '100%', textAlign: 'center' }}>
                <CardContent>
                  <Chip label={`Step ${idx + 1}`} size="small" color="secondary" sx={{ mb: 1.5 }} />
                  <Box
                    sx={{
                      width: 52,
                      height: 52,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      bgcolor: (theme) => alpha(theme.palette.primary.main, 0.1),
                      color: 'primary.main',
                      mx: 'auto',
                      mb: 1.5,
                    }}
                  >
                    {step.icon}
                  </Box>
                  <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                    {step.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {step.text}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      <Divider />

      <Container maxWidth="md" sx={{ py: { xs: 6, md: 8 } }}>
        <Card>
          <CardContent sx={{ p: { xs: 3, md: 5 } }}>
            {submitted ? (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <CheckCircleOutlineIcon color="success" sx={{ fontSize: 64, mb: 2 }} />
                <Typography variant="h5" fontWeight={800} gutterBottom>
                  Enquiry Received!
                </Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 3, maxWidth: 440, mx: 'auto' }}>
                  Thank you for your interest in Greenwood International School. Our admissions team
                  will contact you within 2 business days.
                </Typography>
                <Button variant="outlined" onClick={() => setSubmitted(false)}>
                  Submit Another Enquiry
                </Button>
              </Box>
            ) : (
              <>
                <Typography variant="h5" fontWeight={700} gutterBottom>
                  Admission Enquiry Form
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  All fields are required. We&apos;ll use these details to get in touch with you.
                </Typography>
                <LocalizationProvider dateAdapter={AdapterDayjs}>
                  <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          label="Student's Full Name"
                          fullWidth
                          {...register('studentName')}
                          error={!!errors.studentName}
                          helperText={errors.studentName?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          label="Parent / Guardian's Name"
                          fullWidth
                          {...register('parentName')}
                          error={!!errors.parentName}
                          helperText={errors.parentName?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          label="Phone Number"
                          fullWidth
                          {...register('phone')}
                          error={!!errors.phone}
                          helperText={errors.phone?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          label="Email Address"
                          fullWidth
                          {...register('email')}
                          error={!!errors.email}
                          helperText={errors.email?.message}
                        />
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          select
                          label="Class Applying For"
                          fullWidth
                          defaultValue=""
                          {...register('classApplying')}
                          error={!!errors.classApplying}
                          helperText={errors.classApplying?.message}
                        >
                          {CLASS_OPTIONS.map((option) => (
                            <MenuItem key={option} value={option}>
                              {option}
                            </MenuItem>
                          ))}
                        </TextField>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Controller
                          name="dob"
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
                                  error: !!errors.dob,
                                  helperText: errors.dob?.message as string | undefined,
                                },
                              }}
                            />
                          )}
                        />
                      </Grid>
                      <Grid item xs={12}>
                        <TextField
                          label="Residential Address"
                          fullWidth
                          multiline
                          minRows={2}
                          {...register('address')}
                          error={!!errors.address}
                          helperText={errors.address?.message}
                        />
                      </Grid>
                    </Grid>
                    <Stack direction="row" justifyContent="flex-end" sx={{ mt: 3 }}>
                      <Button
                        type="submit"
                        variant="contained"
                        size="large"
                        disabled={submitting}
                        startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : <SendOutlinedIcon />}
                      >
                        {submitting ? 'Submitting...' : 'Submit Enquiry'}
                      </Button>
                    </Stack>
                  </Box>
                </LocalizationProvider>
              </>
            )}
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}

export default AdmissionPage;
