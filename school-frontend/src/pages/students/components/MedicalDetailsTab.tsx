import { useState } from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import { useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import studentsApi, { type MedicalDetailsPayload } from '@/api/studentsApi';
import type { MedicalDetails } from '@/types';

export interface MedicalDetailsTabProps {
  studentId: number;
  medicalDetails: MedicalDetails | null;
  onChanged: (details: MedicalDetails) => void;
}

interface MedicalFormValues {
  heightCm: string;
  weightKg: string;
  allergies: string;
  medicalConditions: string;
  doctorName: string;
  doctorContact: string;
}

/** Medical Details tab: simple view/edit form over GET/PUT /students/{id}/medical-details. */
export function MedicalDetailsTab({ studentId, medicalDetails, onChanged }: MedicalDetailsTabProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [saving, setSaving] = useState(false);

  const { register, handleSubmit } = useForm<MedicalFormValues>({
    defaultValues: {
      heightCm: medicalDetails?.heightCm != null ? String(medicalDetails.heightCm) : '',
      weightKg: medicalDetails?.weightKg != null ? String(medicalDetails.weightKg) : '',
      allergies: medicalDetails?.allergies ?? '',
      medicalConditions: medicalDetails?.medicalConditions ?? '',
      doctorName: medicalDetails?.doctorName ?? '',
      doctorContact: medicalDetails?.doctorContact ?? '',
    },
  });

  const onSubmit = async (values: MedicalFormValues) => {
    setSaving(true);
    const payload: MedicalDetailsPayload = {
      heightCm: values.heightCm ? Number(values.heightCm) : null,
      weightKg: values.weightKg ? Number(values.weightKg) : null,
      allergies: values.allergies || null,
      medicalConditions: values.medicalConditions || null,
      doctorName: values.doctorName || null,
      doctorContact: values.doctorContact || null,
    };
    try {
      const res = await studentsApi.updateMedicalDetails(studentId, payload);
      onChanged(res.data);
      enqueueSnackbar('Medical details saved.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save medical details.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ maxWidth: 640 }}>
      <Grid container spacing={2}>
        <Grid item xs={6}>
          <TextField label="Height (cm)" type="number" fullWidth {...register('heightCm')} />
        </Grid>
        <Grid item xs={6}>
          <TextField label="Weight (kg)" type="number" fullWidth {...register('weightKg')} />
        </Grid>
        <Grid item xs={12}>
          <TextField label="Allergies" fullWidth multiline minRows={2} {...register('allergies')} />
        </Grid>
        <Grid item xs={12}>
          <TextField label="Medical Conditions" fullWidth multiline minRows={2} {...register('medicalConditions')} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField label="Doctor Name" fullWidth {...register('doctorName')} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField label="Doctor Contact" fullWidth {...register('doctorContact')} />
        </Grid>
      </Grid>
      <Stack direction="row" justifyContent="flex-end" sx={{ mt: 2.5 }}>
        <Button
          type="submit"
          variant="contained"
          disabled={saving}
          startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveOutlinedIcon />}
        >
          Save Medical Details
        </Button>
      </Stack>
    </Box>
  );
}

export default MedicalDetailsTab;
