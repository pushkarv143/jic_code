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
import CircularProgress from '@mui/material/CircularProgress';
import Chip from '@mui/material/Chip';
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import dayjs, { type Dayjs } from 'dayjs';
import { useSnackbar } from 'notistack';
import classesApi from '@/api/classesApi';
import type { Assignment, SchoolClass, Section, Subject } from '@/types';
import type { AssignmentPayload } from '@/api/assignmentsApi';

export interface AssignmentFormDialogProps {
  open: boolean;
  editing: Assignment | null;
  classes: SchoolClass[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: AssignmentPayload) => void;
}

interface FormValues {
  title: string;
  description: string;
  classId: number | '';
  sectionId: number | '';
  subjectId: number | '';
  assignedDate: Dayjs | null;
  dueDate: Dayjs | null;
}

/** Create/edit dialog for an assignment: title/description, class -> section/subject cascade, dates, optional file (multipart). */
export function AssignmentFormDialog({ open, editing, classes, saving, onClose, onSubmit }: AssignmentFormDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [sections, setSections] = useState<Section[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [file, setFile] = useState<File | null>(null);

  const { control, register, handleSubmit, watch, setValue, reset } = useForm<FormValues>({
    defaultValues: {
      title: '',
      description: '',
      classId: '',
      sectionId: '',
      subjectId: '',
      assignedDate: dayjs(),
      dueDate: dayjs().add(7, 'day'),
    },
  });

  const classId = watch('classId');

  useEffect(() => {
    if (open) {
      reset({
        title: editing?.title ?? '',
        description: editing?.description ?? '',
        classId: editing?.classId ?? '',
        sectionId: editing?.sectionId ?? '',
        subjectId: editing?.subjectId ?? '',
        assignedDate: editing ? dayjs(editing.assignedDate) : dayjs(),
        dueDate: editing ? dayjs(editing.dueDate) : dayjs().add(7, 'day'),
      });
      setFile(null);
    }
  }, [open, editing, reset]);

  useEffect(() => {
    if (!classId) {
      setSections([]);
      setSubjects([]);
      return;
    }
    classesApi.listSections(classId as number).then((res) => setSections(res.data)).catch(() => undefined);
    classesApi.listSubjects(classId as number).then((res) => setSubjects(res.data)).catch(() => undefined);
  }, [classId]);

  const submit = (values: FormValues) => {
    if (!values.classId || !values.sectionId || !values.subjectId || !values.assignedDate || !values.dueDate) {
      enqueueSnackbar('Please fill in class, section, subject and both dates.', { variant: 'warning' });
      return;
    }
    onSubmit({
      title: values.title,
      description: values.description || undefined,
      classId: values.classId as number,
      sectionId: values.sectionId as number,
      subjectId: values.subjectId as number,
      assignedDate: dayjs(values.assignedDate).format('YYYY-MM-DD'),
      dueDate: dayjs(values.dueDate).format('YYYY-MM-DD'),
      file,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Assignment' : 'Add Assignment'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField label="Title" fullWidth autoFocus {...register('title', { required: true })} />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Description" fullWidth multiline minRows={2} {...register('description')} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="classId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Class"
                    fullWidth
                    value={field.value}
                    onChange={(e) => {
                      const v = e.target.value === '' ? '' : Number(e.target.value);
                      field.onChange(v);
                      setValue('sectionId', '');
                      setValue('subjectId', '');
                    }}
                  >
                    <MenuItem value="">Select a class</MenuItem>
                    {classes.map((c) => (
                      <MenuItem key={c.id} value={c.id}>
                        {c.className}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="sectionId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Section"
                    fullWidth
                    disabled={!classId}
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">Select a section</MenuItem>
                    {sections.map((s) => (
                      <MenuItem key={s.id} value={s.id}>
                        {s.sectionName}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="subjectId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Subject"
                    fullWidth
                    disabled={!classId}
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">Select a subject</MenuItem>
                    {subjects.map((s) => (
                      <MenuItem key={s.id} value={s.id}>
                        {s.subjectName}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="assignedDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Assigned Date"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="dueDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Due Date"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
                {file ? 'Change Attachment' : 'Attach File (optional)'}
                <input hidden type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              </Button>
              {file && <Chip size="small" label={file.name} onDelete={() => setFile(null)} sx={{ ml: 1.5 }} />}
              {!file && editing?.fileUrl && (
                <Chip size="small" variant="outlined" label="Existing attachment kept unless replaced" sx={{ ml: 1.5 }} />
              )}
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default AssignmentFormDialog;
