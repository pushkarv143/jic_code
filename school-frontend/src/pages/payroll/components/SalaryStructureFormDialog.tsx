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
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { Controller, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import teachersApi from '@/api/teachersApi';
import staffApi from '@/api/staffApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import type { PayrollEmployeeType, SalaryStructure, Staff, Teacher } from '@/types';
import type { SalaryStructurePayload } from '@/api/payrollApi';

interface EmployeeOption {
  employeeId: number;
  label: string;
}

const schema = yup.object({
  employeeType: yup.mixed<PayrollEmployeeType>().oneOf(['TEACHER', 'STAFF']).required('Employee type is required'),
  basicSalary: yup
    .string()
    .required('Basic salary is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
  hra: yup
    .string()
    .required('HRA is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
  da: yup
    .string()
    .required('DA is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
  otherAllowances: yup
    .string()
    .required('Other allowances is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
  pfPercentage: yup
    .string()
    .required('PF % is required')
    .test('num', 'Enter a valid percentage', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
  esiPercentage: yup
    .string()
    .required('ESI % is required')
    .test('num', 'Enter a valid percentage', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
});
type FormValues = yup.InferType<typeof schema>;

export interface SalaryStructureFormDialogProps {
  open: boolean;
  editing: SalaryStructure | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: SalaryStructurePayload) => void;
}

/**
 * Add/edit dialog for a salary structure. `employeeId` is the person's `users.id`
 * (not the teachers/staff table id) per the schema contract. Searches teachers or
 * the read-only staff directory depending on the selected employee type.
 */
export function SalaryStructureFormDialog({ open, editing, saving, onClose, onSubmit }: SalaryStructureFormDialogProps) {
  const [employeeQuery, setEmployeeQuery] = useState('');
  const debouncedQuery = useDebouncedValue(employeeQuery, 400);
  const [teacherOptions, setTeacherOptions] = useState<Teacher[]>([]);
  const [staffOptions, setStaffOptions] = useState<Staff[]>([]);
  const [selectedEmployee, setSelectedEmployee] = useState<EmployeeOption | null>(null);
  const [searching, setSearching] = useState(false);

  const {
    control,
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: {
      employeeType: 'TEACHER',
      basicSalary: '',
      hra: '',
      da: '',
      otherAllowances: '',
      pfPercentage: '',
      esiPercentage: '',
    },
  });

  const employeeType = watch('employeeType');

  useEffect(() => {
    if (open) {
      reset({
        employeeType: editing?.employeeType ?? 'TEACHER',
        basicSalary: editing ? String(editing.basicSalary) : '',
        hra: editing ? String(editing.hra) : '',
        da: editing ? String(editing.da) : '',
        otherAllowances: editing ? String(editing.otherAllowances) : '',
        pfPercentage: editing ? String(editing.pfPercentage) : '',
        esiPercentage: editing ? String(editing.esiPercentage) : '',
      });
      setSelectedEmployee(
        editing ? { employeeId: editing.employeeId, label: editing.employeeName ?? `Employee #${editing.employeeId}` } : null,
      );
      setTeacherOptions([]);
      setStaffOptions([]);
      setEmployeeQuery('');
    }
  }, [open, editing, reset]);

  useEffect(() => {
    if (!debouncedQuery) {
      setTeacherOptions([]);
      setStaffOptions([]);
      return;
    }
    setSearching(true);
    if (employeeType === 'TEACHER') {
      teachersApi
        .list({ search: debouncedQuery, size: 20 })
        .then((res) => setTeacherOptions(res.data.content))
        .catch(() => undefined)
        .finally(() => setSearching(false));
    } else {
      staffApi
        .list({ search: debouncedQuery, size: 20 })
        .then((res) => setStaffOptions(res.data.content))
        .catch(() => undefined)
        .finally(() => setSearching(false));
    }
  }, [debouncedQuery, employeeType]);

  const teacherOptionList: EmployeeOption[] =
    employeeType === 'TEACHER'
      ? teacherOptions.map((t) => ({
          employeeId: t.userId,
          label: `${t.firstName ?? t.user?.firstName ?? ''} ${t.lastName ?? t.user?.lastName ?? ''}`.trim() + ` (${t.employeeId})`,
        }))
      : staffOptions
          .filter((s) => s.userId !== null)
          .map((s) => ({
            employeeId: s.userId as number,
            label: `${s.firstName ?? ''} ${s.lastName ?? ''}`.trim() + ` (${s.employeeId})`,
          }));

  const submit = (values: FormValues) => {
    if (!editing && !selectedEmployee) return;
    onSubmit({
      employeeId: editing ? editing.employeeId : (selectedEmployee as EmployeeOption).employeeId,
      employeeType: values.employeeType,
      basicSalary: Number(values.basicSalary),
      hra: Number(values.hra),
      da: Number(values.da),
      otherAllowances: Number(values.otherAllowances),
      pfPercentage: Number(values.pfPercentage),
      esiPercentage: Number(values.esiPercentage),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Salary Structure' : 'Add Salary Structure'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <Controller
                name="employeeType"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Employee Type"
                    fullWidth
                    disabled={!!editing}
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value as PayrollEmployeeType)}
                    error={!!errors.employeeType}
                    helperText={errors.employeeType?.message}
                  >
                    <MenuItem value="TEACHER">Teacher</MenuItem>
                    <MenuItem value="STAFF">Staff</MenuItem>
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              {editing ? (
                <TextField label="Employee" fullWidth disabled value={selectedEmployee?.label ?? ''} />
              ) : (
                <Autocomplete
                  options={teacherOptionList}
                  loading={searching}
                  value={selectedEmployee}
                  isOptionEqualToValue={(o, v) => o.employeeId === v.employeeId}
                  onChange={(_e, value) => setSelectedEmployee(value)}
                  onInputChange={(_e, value) => setEmployeeQuery(value)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Employee"
                      placeholder="Search teacher by name..."
                      error={!editing && !selectedEmployee}
                      helperText={!editing && !selectedEmployee ? 'Search and select an employee' : undefined}
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
              )}
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Basic Salary (INR)"
                fullWidth
                {...register('basicSalary')}
                error={!!errors.basicSalary}
                helperText={errors.basicSalary?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="HRA (INR)" fullWidth {...register('hra')} error={!!errors.hra} helperText={errors.hra?.message} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="DA (INR)" fullWidth {...register('da')} error={!!errors.da} helperText={errors.da?.message} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Other Allowances (INR)"
                fullWidth
                {...register('otherAllowances')}
                error={!!errors.otherAllowances}
                helperText={errors.otherAllowances?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="PF (%)"
                fullWidth
                {...register('pfPercentage')}
                error={!!errors.pfPercentage}
                helperText={errors.pfPercentage?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="ESI (%)"
                fullWidth
                {...register('esiPercentage')}
                error={!!errors.esiPercentage}
                helperText={errors.esiPercentage?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary">
                Net salary is computed by the server as basic + HRA + DA + other allowances, less PF/ESI deductions,
                each time payroll is generated for a month.
              </Typography>
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
            disabled={saving || (!editing && !selectedEmployee)}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default SalaryStructureFormDialog;
