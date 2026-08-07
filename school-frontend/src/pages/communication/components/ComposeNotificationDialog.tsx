import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Autocomplete from '@mui/material/Autocomplete';
import RadioGroup from '@mui/material/RadioGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Radio from '@mui/material/Radio';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import studentsApi from '@/api/studentsApi';
import teachersApi from '@/api/teachersApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { formatRoleLabel } from '@/utils/format';
import type { NotificationChannel, Role, SendNotificationPayload } from '@/types';

const ROLES: Role[] = [
  'SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'TEACHER', 'CLASS_TEACHER',
  'ACCOUNTANT', 'LIBRARIAN', 'RECEPTIONIST', 'STUDENT', 'PARENT', 'SECURITY_GUARD',
];

interface RecipientOption {
  userId: number;
  label: string;
}

const schema = yup.object({
  type: yup.mixed<NotificationChannel>().oneOf(['SMS', 'EMAIL', 'PUSH', 'IN_APP']).required(),
  subject: yup.string().required('Subject is required').max(200),
  message: yup.string().required('Message is required').max(2000),
});
type FormValues = yup.InferType<typeof schema>;

export interface ComposeNotificationDialogProps {
  open: boolean;
  sending: boolean;
  onClose: () => void;
  onSubmit: (values: SendNotificationPayload) => void;
}

/**
 * Compose dialog for admin roles: recipient mode toggle (specific user vs. a whole role),
 * type/subject/message, POST /notifications/send. There is no single "search all users"
 * endpoint in the contract yet (ASSUMPTION/GAP - reconcile with backend), so the specific-user
 * search combines Teacher + Student results client-side and only lists people with a login
 * account (a non-null `users.id`).
 */
export function ComposeNotificationDialog({ open, sending, onClose, onSubmit }: ComposeNotificationDialogProps) {
  const [recipientMode, setRecipientMode] = useState<'user' | 'role'>('role');
  const [targetRole, setTargetRole] = useState<Role | ''>('');
  const [recipientQuery, setRecipientQuery] = useState('');
  const debouncedQuery = useDebouncedValue(recipientQuery, 400);
  const [recipientOptions, setRecipientOptions] = useState<RecipientOption[]>([]);
  const [selectedRecipient, setSelectedRecipient] = useState<RecipientOption | null>(null);
  const [searching, setSearching] = useState(false);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { type: 'IN_APP', subject: '', message: '' },
  });

  useEffect(() => {
    if (open) {
      reset({ type: 'IN_APP', subject: '', message: '' });
      setRecipientMode('role');
      setTargetRole('');
      setSelectedRecipient(null);
      setRecipientQuery('');
      setRecipientOptions([]);
    }
  }, [open, reset]);

  useEffect(() => {
    if (recipientMode !== 'user' || !debouncedQuery) {
      setRecipientOptions([]);
      return;
    }
    setSearching(true);
    Promise.all([
      teachersApi.list({ search: debouncedQuery, size: 10 }).catch(() => ({ data: { content: [] } })),
      studentsApi.list({ search: debouncedQuery, size: 10 }).catch(() => ({ data: { content: [] } })),
    ])
      .then(([teacherRes, studentRes]) => {
        const teacherOptions: RecipientOption[] = teacherRes.data.content.map((t) => ({
          userId: t.userId,
          label: `${t.firstName ?? t.user?.firstName ?? ''} ${t.lastName ?? t.user?.lastName ?? ''}`.trim() + ' (Teacher)',
        }));
        const studentOptions: RecipientOption[] = studentRes.data.content
          .filter((s) => s.userId !== null)
          .map((s) => ({
            userId: s.userId as number,
            label: `${s.firstName ?? ''} ${s.lastName ?? ''}`.trim() + ` (Student, ${s.admissionNumber})`,
          }));
        setRecipientOptions([...teacherOptions, ...studentOptions]);
      })
      .finally(() => setSearching(false));
  }, [debouncedQuery, recipientMode]);

  const submit = (values: FormValues) => {
    if (recipientMode === 'user' && !selectedRecipient) return;
    if (recipientMode === 'role' && !targetRole) return;
    onSubmit({
      recipientId: recipientMode === 'user' ? selectedRecipient?.userId : undefined,
      targetRole: recipientMode === 'role' ? (targetRole as Role) : undefined,
      type: values.type,
      subject: values.subject.trim(),
      message: values.message.trim(),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Compose Notification</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <RadioGroup
                row
                value={recipientMode}
                onChange={(e) => {
                  setRecipientMode(e.target.value as 'user' | 'role');
                  setSelectedRecipient(null);
                  setTargetRole('');
                }}
              >
                <FormControlLabel value="role" control={<Radio />} label="Send to a role" />
                <FormControlLabel value="user" control={<Radio />} label="Send to a specific user" />
              </RadioGroup>
            </Grid>

            {recipientMode === 'role' ? (
              <Grid item xs={12}>
                <TextField
                  select
                  fullWidth
                  label="Target Role"
                  value={targetRole}
                  onChange={(e) => setTargetRole(e.target.value as Role | '')}
                  error={!targetRole}
                  helperText={!targetRole ? 'Select the role that should receive this notification' : undefined}
                >
                  <MenuItem value="">Select a role</MenuItem>
                  {ROLES.map((r) => (
                    <MenuItem key={r} value={r}>
                      {formatRoleLabel(r)}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            ) : (
              <Grid item xs={12}>
                <Autocomplete
                  options={recipientOptions}
                  loading={searching}
                  value={selectedRecipient}
                  isOptionEqualToValue={(o, v) => o.userId === v.userId}
                  onChange={(_e, value) => setSelectedRecipient(value)}
                  onInputChange={(_e, value) => setRecipientQuery(value)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Recipient"
                      placeholder="Search teacher or student by name..."
                      error={!selectedRecipient}
                      helperText={!selectedRecipient ? 'Search and select a recipient' : undefined}
                      InputProps={{
                        ...params.InputProps,
                        endAdornment: (
                          <>
                            {searching ? <CircularProgress size={16} /> : null}
                            {params.InputProps.endAdornment}
                          </>
                        ),
                      }}
                    />
                  )}
                />
              </Grid>
            )}

            <Grid item xs={12} sm={6}>
              <Controller
                name="type"
                control={control}
                render={({ field }) => (
                  <TextField select label="Channel" fullWidth value={field.value} onChange={(e) => field.onChange(e.target.value as NotificationChannel)}>
                    <MenuItem value="IN_APP">In-App</MenuItem>
                    <MenuItem value="EMAIL">Email</MenuItem>
                    <MenuItem value="SMS">SMS</MenuItem>
                    <MenuItem value="PUSH">Push</MenuItem>
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
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
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={sending}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={sending || (recipientMode === 'user' ? !selectedRecipient : !targetRole)}
            startIcon={sending ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Send
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default ComposeNotificationDialog;
