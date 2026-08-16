import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import { authApi } from '@/api/authApi';
import { useAppDispatch } from '@/store/hooks';
import { setCredentials } from '@/store/authSlice';
import {
  forgotPasswordSchema,
  otpCodeSchema,
  type ForgotPasswordFormValues,
  type OtpCodeFormValues,
} from './authSchemas';

/**
 * Signing in with an emailed code instead of a password.
 *
 * Offered alongside password sign-in, not instead of it — it exists for the
 * parents and students who mostly cannot recall a password set once at admission.
 *
 * It is not a weaker door than the one already there: anyone who can read the code
 * could equally have used "forgot password" to take the account. Both are gated on
 * the same mailbox.
 */
export function OtpLoginPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();

  const [codeSent, setCodeSent] = useState(false);
  const [destination, setDestination] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [resendIn, setResendIn] = useState(0);

  const emailForm = useForm<ForgotPasswordFormValues>({ resolver: yupResolver(forgotPasswordSchema) });
  const codeForm = useForm<OtpCodeFormValues>({ resolver: yupResolver(otpCodeSchema) });

  useEffect(() => {
    if (resendIn <= 0) return undefined;
    const timer = setTimeout(() => setResendIn((seconds) => seconds - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendIn]);

  const sendCode = useCallback(
    async (address: string) => {
      setSubmitting(true);
      try {
        const result = await authApi.requestOtp({ destination: address, purpose: 'LOGIN' });
        setDestination(address);
        setResendIn(result.resendAfterSeconds);
        setCodeSent(true);
        enqueueSnackbar('If an account matches, a code has been sent.', { variant: 'info' });
      } catch (err: any) {
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

  const onEmailSubmit = (values: ForgotPasswordFormValues) => sendCode(values.email);

  const onCodeSubmit = async (values: OtpCodeFormValues) => {
    setSubmitting(true);
    try {
      const result = await authApi.verifyOtp({ destination, purpose: 'LOGIN', code: values.code });
      if (!result.auth) {
        // The API contract says LOGIN carries a token pair; if it ever does not,
        // failing loudly beats navigating into the app with no session.
        throw new Error('Signed in but no session was returned.');
      }
      // Identical to the password path, so refresh and logout need no special case.
      dispatch(setCredentials(result.auth));
      enqueueSnackbar(`Welcome back, ${result.auth.user.firstName}!`, { variant: 'success' });
      navigate('/app/dashboard', { replace: true });
    } catch (err: any) {
      codeForm.setError('code', {
        message: err?.response?.data?.message ?? 'That code is incorrect or has expired.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={800} gutterBottom>
        Sign in with a code
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        We&apos;ll email a 6-digit code to the address on your account. No password needed.
      </Typography>

      {!codeSent ? (
        <>
          <Alert severity="info" sx={{ mb: 2 }}>
            For your security we&apos;ll always show the same confirmation, whether or not the email
            is registered.
          </Alert>
          <Box component="form" onSubmit={emailForm.handleSubmit(onEmailSubmit)} noValidate>
            <TextField
              label="Email Address"
              fullWidth
              margin="normal"
              autoFocus
              {...emailForm.register('email')}
              error={!!emailForm.formState.errors.email}
              helperText={emailForm.formState.errors.email?.message}
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
      ) : (
        <Box component="form" onSubmit={codeForm.handleSubmit(onCodeSubmit)} noValidate>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Enter the code sent to <strong>{destination}</strong>. It expires in 5 minutes.
          </Typography>
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
            {submitting ? 'Signing in...' : 'Sign In'}
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
      )}

      <Typography variant="body2" align="center" sx={{ mt: 3 }}>
        <Link component={RouterLink} to="/login" underline="hover">
          <ArrowBackIcon sx={{ fontSize: 14, verticalAlign: 'middle', mr: 0.5 }} />
          Sign in with a password instead
        </Link>
      </Typography>
    </Box>
  );
}

export default OtpLoginPage;
