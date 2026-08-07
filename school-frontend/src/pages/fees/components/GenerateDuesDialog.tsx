import { useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useSnackbar } from 'notistack';
import feesApi from '@/api/feesApi';
import type { AcademicYear, FeeStructure, SchoolClass } from '@/types';
import { formatCurrencyINR } from '@/utils/format';

export interface GenerateDuesDialogProps {
  open: boolean;
  classes: SchoolClass[];
  years: AcademicYear[];
  onClose: () => void;
  onGenerated: () => void;
}

/** "Generate Dues" workflow: pick class + academic year, choose which fee structures to apply, then POST /student-fees/generate. */
export function GenerateDuesDialog({ open, classes, years, onClose, onGenerated }: GenerateDuesDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [classId, setClassId] = useState<number | ''>('');
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');
  const [structures, setStructures] = useState<FeeStructure[]>([]);
  const [loadingStructures, setLoadingStructures] = useState(false);
  const [selected, setSelected] = useState<number[]>([]);
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    if (!open) {
      setClassId('');
      setAcademicYearId('');
      setStructures([]);
      setSelected([]);
    }
  }, [open]);

  useEffect(() => {
    if (!classId || !academicYearId) {
      setStructures([]);
      setSelected([]);
      return;
    }
    setLoadingStructures(true);
    feesApi.feeStructures
      .list({ classId: classId as number, academicYearId: academicYearId as number })
      .then((res) => {
        setStructures(res.data);
        setSelected(res.data.map((s) => s.id));
      })
      .catch(() => enqueueSnackbar('Could not load fee structures for that class/year.', { variant: 'error' }))
      .finally(() => setLoadingStructures(false));
  }, [classId, academicYearId, enqueueSnackbar]);

  const toggle = (id: number) => {
    setSelected((prev) => (prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id]));
  };

  const total = useMemo(
    () => structures.filter((s) => selected.includes(s.id)).reduce((sum, s) => sum + s.amount, 0),
    [structures, selected],
  );

  const handleGenerate = async () => {
    if (!classId || !academicYearId || selected.length === 0) return;
    setGenerating(true);
    try {
      const res = await feesApi.studentFees.generate({
        classId: classId as number,
        academicYearId: academicYearId as number,
        feeStructureIds: selected,
      });
      enqueueSnackbar(
        `Dues generated for ${res.data.generatedCount} student fee record${res.data.generatedCount === 1 ? '' : 's'}` +
          (res.data.skippedCount ? ` (${res.data.skippedCount} skipped — already existed).` : '.'),
        { variant: 'success' },
      );
      onGenerated();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not generate dues.', { variant: 'error' });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Generate Dues</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.5 }}>
          <Grid item xs={12} sm={6}>
            <TextField
              select
              fullWidth
              label="Class"
              value={classId}
              onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">Select a class</MenuItem>
              {classes.map((c) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.className}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              select
              fullWidth
              label="Academic Year"
              value={academicYearId}
              onChange={(e) => setAcademicYearId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">Select a year</MenuItem>
              {years.map((y) => (
                <MenuItem key={y.id} value={y.id}>
                  {y.yearName}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12}>
            {loadingStructures ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                <CircularProgress size={24} />
              </Box>
            ) : !classId || !academicYearId ? (
              <Typography variant="body2" color="text.secondary">
                Select a class and academic year to see applicable fee structures.
              </Typography>
            ) : structures.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No fee structures are defined for this class and academic year yet.
              </Typography>
            ) : (
              <>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  Fee structures to apply
                </Typography>
                <FormGroup>
                  {structures.map((s) => (
                    <FormControlLabel
                      key={s.id}
                      control={<Checkbox checked={selected.includes(s.id)} onChange={() => toggle(s.id)} />}
                      label={`${s.feeCategoryName ?? `Category #${s.feeCategoryId}`} — ${formatCurrencyINR(s.amount)}`}
                    />
                  ))}
                </FormGroup>
                <Typography variant="body2" sx={{ mt: 1 }} fontWeight={600}>
                  Total per student: {formatCurrencyINR(total)}
                </Typography>
              </>
            )}
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={generating}>
          Cancel
        </Button>
        <Button
          variant="contained"
          disabled={generating || selected.length === 0}
          startIcon={generating ? <CircularProgress size={16} color="inherit" /> : undefined}
          onClick={handleGenerate}
        >
          Generate
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default GenerateDuesDialog;
