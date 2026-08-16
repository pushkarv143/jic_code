import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import Stepper from '@mui/material/Stepper';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import { authApi } from '@/api/authApi';
import {
  otpDestinationSchema,
  otpCodeSchema,
  resetPasswordSchema,
  type OtpDestinationFormValues,
  type OtpCodeFormValues,
  type ResetPasswordFormValues,
} from './authSchemas';

const STEPS = ['Your details', 'Enter code', 'New password'];

/**
 * Password recovery by one-time passcode.
 *
 * Replaces a flow that emailed a reset link. The code works the same in a browser
 * and on the phone app, where the link never did — it pointed back at this site,
 * which an Android user cannot usefully open.
 *
 * Step 1 always reports the same thing whether or not the address is registered,
 * so this page must never render anything that would distinguish the two.
 */
export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [step, setStep] = useState(0);
  const [destination, setDestination] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [resendIn, setResendIn] = useState(0);

  const emailForm = useForm<OtpDestinationFormValues>({ resolver: yupResolver(otpDestinationSchema) });
  const codeForm = useForm<OtpCodeFormValues>({ resolver: yupResolver(otpCodeSchema) });
  const passwordForm = useForm<ResetPasswordFormValues>({ resolver: yupResolver(resetPasswordSchema) });

  // Counts the resend button back down to enabled. Mirrors the server's own
  // cooldown so the button is disabled rather than returning a guaranteed 429.
  useEffect(() => {
    if (resendIn <= 0) return undefined;
    const timer = setTimeout(() => setResendIn((seconds) => seconds - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendIn]);

  const sendCode = useCallback(
    async (address: string) => {
      setSubmitting(true);
      try {
        const result = await authApi.requestOtp({ destination: address, purpose: 'PASSWORD_RESET' });
        setDestination(address);
        setResendIn(result.resendAfterSeconds);
        setStep(1);
        enqueueSnackbar('If an account matches, a code has been sent.', { variant: 'info' });
      } catch (err: any) {
        // Throttling is the one failure worth showing: it is about the request
        // rate, not about the account, so it discloses nothing.
        enqueueSnackbar(
          err?.response?.data?.message ?? 'Could not send a code right now. Please try again.',
          { variant: 'error' },
        );
      } finally {
        setSubmitting(false);
      }
    },
    [enqueueSnackbar],
  );

  const onEmailSubmit = (values: OtpDestinationFormValues) => sendCode(values.destination);

  const onCodeSubmit = async (values: OtpCodeFormValues) => {
    setSubmitting(true);
    try {
      const result = await authApi.verifyOtp({
        destination,
        purpose: 'PASSWORD_RESET',
        code: values.code,
      });
      setResetToken(result.resetToken ?? '');
      setStep(2);
    } catch (err: any) {
      codeForm.setError('code', {
        message: err?.response?.data?.message ?? 'That code is incorrect or has expired.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const onPasswordSubmit = async (values: ResetPasswordFormValues) => {
    setSubmitting(true);
    try {
      await authApi.resetPassword({ token: resetToken, newPassword: values.newPassword });
      setResetToken('');
      enqueueSnackbar('Password reset. Sign in with your new password.', { variant: 'success' });
      navigate('/login');
    } catch (err: any) {
      enqueueSnackbar(
        err?.response?.data?.message ?? 'Could not reset your password. Please start again.',
        { variant: 'error' },
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={800} gutterBottom>
        Forgot your password?
      </Typography>

      <Stepper activeStep={step} sx={{ my: 3 }}>
        {STEPS.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {step === 0 && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Enter the email address or mobile number on your account and we&apos;ll send you a
            6-digit code.
          </Typography>
          <Alert severity="info" sx={{ mb: 2 }}>
            For your security we&apos;ll always show the same confirmation, whether or not it is
            registered.
          </Alert>
          <Box component="form" onSubmit={emailForm.handleSubmit(onEmailSubmit)} noValidate>
            <TextField
              label="Email or mobile number"
              fullWidth
              margin="normal"
              autoFocus
              {...emailForm.register('destination')}
              error={!!emailForm.formState.errors.destination}
              helperText={emailForm.formState.errors.destination?.message}
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
              {submitting ? 'Sending...' : 'Send Code'}
            </Button>
          </Box>
        </>
      )}

      {step === 1 && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Enter the 6-digit code we sent to <strong>{destination}</strong>. It expires in 5
            minutes — check your spam folder if it hasn&apos;t arrived.
          </Typography>
          <Box component="form" onSubmit={codeForm.handleSubmit(onCodeSubmit)} noValidate>
            <TextField
              label="6-digit code"
              fullWidth
              margin="normal"
              autoFocus
              inputProps={{ inputMode: 'numeric', maxLength: 6 }}
              {...codeForm.register('code')}
              error={!!codeForm.formState.errors.code}
              helperText={codeForm.formState.errors.code?.message}
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
              {submitting ? 'Verifying...' : 'Verify Code'}
            </Button>
            <Button
              fullWidth
              sx={{ mt: 1 }}
              disabled={resendIn > 0 || submitting}
              onClick={() => sendCode(destination)}
            >
              {resendIn > 0 ? `Resend in ${resendIn}s` : 'Resend code'}
            </Button>
          </Box>
        </>
      )}

      {step === 2 && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Choose a new password for your account.
          </Typography>
          <Box component="form" onSubmit={passwordForm.handleSubmit(onPasswordSubmit)} noValidate>
            <TextField
              label="New Password"
              type="password"
              fullWidth
              margin="normal"
              autoFocus
              {...passwordForm.register('newPassword')}
              error={!!passwordForm.formState.errors.newPassword}
              helperText={passwordForm.formState.errors.newPassword?.message}
            />
            <TextField
              label="Confirm New Password"
              type="password"
              fullWidth
              margin="normal"
              {...passwordForm.register('confirmPassword')}
              error={!!passwordForm.formState.errors.confirmPassword}
              helperText={passwordForm.formState.errors.confirmPassword?.message}
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
              {submitting ? 'Saving...' : 'Set New Password'}
            </Button>
          </Box>
        </>
      )}

      <Typography variant="body2" align="center" sx={{ mt: 3 }}>
        <Link component={RouterLink} to="/login" underline="hover">
          <ArrowBackIcon sx={{ fontSize: 14, verticalAlign: 'middle', mr: 0.5 }} />
          Back to Sign In
        </Link>
      </Typography>
    </Box>
  );
}

export default ForgotPasswordPage;
