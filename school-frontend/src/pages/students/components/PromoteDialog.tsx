import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import DialogContentText from '@mui/material/DialogContentText';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import CircularProgress from '@mui/material/CircularProgress';
import { useSnackbar } from 'notistack';
import classesApi from '@/api/classesApi';
import academicYearsApi from '@/api/academicYearsApi';
import studentsApi from '@/api/studentsApi';
import type { AcademicYear, SchoolClass } from '@/types';

export interface PromoteDialogProps {
  open: boolean;
  studentIds: number[];
  onClose: () => void;
  onPromoted: () => void;
}

/** Bulk-promote dialog: pick target class + academic year, calls POST /students/promote. */
export function PromoteDialog({ open, studentIds, onClose, onPromoted }: PromoteDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');
  const [resolvingSection, setResolvingSection] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    setClassId('');
    setSectionId('');
    (async () => {
      try {
        const [classRes, yearRes] = await Promise.all([
          classesApi.list({ size: 200, sort: 'className,asc' }),
          academicYearsApi.list(),
        ]);
        setClasses(classRes.data.content);
        setYears(yearRes.data);
        const current = yearRes.data.find((y) => y.isCurrent);
        setAcademicYearId(current ? current.id : '');
      } catch {
        enqueueSnackbar('Could not load classes / academic years.', { variant: 'error' });
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // Promotion still has to name a target section, but a class has only one, so it
  // is resolved from the chosen class rather than asked for. Promote stays
  // disabled until it arrives, which is what resolvingSection is for.
  useEffect(() => {
    if (!classId) {
      setSectionId('');
      return;
    }
    setResolvingSection(true);
    classesApi
      .listSections(classId as number)
      .then((res) => setSectionId(res.data[0]?.id ?? ''))
      .catch(() => enqueueSnackbar('Could not load the section for that class.', { variant: 'error' }))
      .finally(() => setResolvingSection(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId]);

  const handlePromote = async () => {
    if (!classId || !sectionId || !academicYearId) return;
    setSubmitting(true);
    try {
      await studentsApi.promote({
        studentIds,
        toClassId: classId as number,
        toSectionId: sectionId as number,
        academicYearId: academicYearId as number,
      });
      enqueueSnackbar(`${studentIds.length} student(s) promoted successfully.`, { variant: 'success' });
      onPromoted();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not promote students. Please try again.', {
        variant: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Promote Students</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Promote {studentIds.length} selected student(s) to a new class and academic year.
        </DialogContentText>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField
              select
              label="Target Class"
              fullWidth
              value={classId}
              onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">Select a class</MenuItem>
              {classes.map((cls) => (
                <MenuItem key={cls.id} value={cls.id}>
                  {cls.className}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12}>
            <TextField
              select
              label="Academic Year"
              fullWidth
              value={academicYearId}
              onChange={(e) => setAcademicYearId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">Select an academic year</MenuItem>
              {years.map((year) => (
                <MenuItem key={year.id} value={year.id}>
                  {year.yearName}
                  {year.isCurrent ? ' (current)' : ''}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={submitting}>
          Cancel
        </Button>
        <Button
          onClick={handlePromote}
          variant="contained"
          disabled={!classId || !sectionId || !academicYearId || resolvingSection || submitting}
          startIcon={submitting ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          Promote
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default PromoteDialog;
