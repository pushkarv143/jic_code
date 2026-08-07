import { useState } from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import { alpha } from '@mui/material/styles';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import { contactSchema, type ContactFormValues } from './ContactPage.schema';

const CONTACT_DETAILS = [
  { icon: <LocationOnOutlinedIcon />, title: 'Address', text: '142 Lakeview Avenue, Sector 21, Greenwood City, 500081' },
  { icon: <PhoneOutlinedIcon />, title: 'Phone', text: '+91 98765 43210' },
  { icon: <EmailOutlinedIcon />, title: 'Email', text: 'info@greenwoodschool.edu' },
  { icon: <AccessTimeOutlinedIcon />, title: 'Office Hours', text: 'Mon - Sat, 8:00 AM - 4:00 PM' },
];

/** Real contact form (RHF + Yup) that simulates a submission (no backend endpoint yet) and toasts success. */
export function ContactPage() {
  const [submitting, setSubmitting] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ContactFormValues>({ resolver: yupResolver(contactSchema) });

  const onSubmit = async (_values: ContactFormValues) => {
    setSubmitting(true);
    // No backend endpoint for general contact enquiries yet — simulate network latency.
    await new Promise((resolve) => setTimeout(resolve, 900));
    setSubmitting(false);
    enqueueSnackbar('Thanks for reaching out! Our team will get back to you within 2 business days.', {
      variant: 'success',
    });
    reset();
  };

  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            Get in Touch
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            We&apos;d love to hear from you
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 560, mt: 2 }}>
            Questions about admissions, academics or campus visits? Send us a message and our team
            will respond promptly.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Grid container spacing={4}>
          <Grid item xs={12} md={4}>
            <Stack spacing={2.5}>
              {CONTACT_DETAILS.map((item) => (
                <Card key={item.title}>
                  <CardContent sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
                    <Box
                      sx={{
                        width: 44,
                        height: 44,
                        borderRadius: 2,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        bgcolor: (theme) => alpha(theme.palette.primary.main, 0.1),
                        color: 'primary.main',
                        flexShrink: 0,
                      }}
                    >
                      {item.icon}
                    </Box>
                    <Box>
                      <Typography variant="subtitle2" fontWeight={700}>
                        {item.title}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {item.text}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card>
              <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                <Typography variant="h5" fontWeight={700} gutterBottom>
                  Send us a message
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Fill out the form below and we&apos;ll get in touch as soon as possible.
                </Typography>
                <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Full Name"
                        fullWidth
                        {...register('name')}
                        error={!!errors.name}
                        helperText={errors.name?.message}
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
                    <Grid item xs={12}>
                      <TextField
                        label="Email Address"
                        fullWidth
                        {...register('email')}
                        error={!!errors.email}
                        helperText={errors.email?.message}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="Subject"
                        fullWidth
                        {...register('subject')}
                        error={!!errors.subject}
                        helperText={errors.subject?.message}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="Message"
                        fullWidth
                        multiline
                        minRows={4}
                        {...register('message')}
                        error={!!errors.message}
                        helperText={errors.message?.message}
                      />
                    </Grid>
                  </Grid>
                  <Button
                    type="submit"
                    variant="contained"
                    size="large"
                    disabled={submitting}
                    startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : <SendOutlinedIcon />}
                    sx={{ mt: 3 }}
                  >
                    {submitting ? 'Sending...' : 'Send Message'}
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}

export default ContactPage;
