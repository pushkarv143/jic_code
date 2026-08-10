import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import Link from '@mui/material/Link';
import Divider from '@mui/material/Divider';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import StatusChip from '@/components/common/StatusChip';
import assignmentsApi from '@/api/assignmentsApi';
import classesApi from '@/api/classesApi';
import { useAppSelector } from '@/store/hooks';
import type { Assignment, AssignmentSubmission, Role, SchoolClass, Section, Subject } from '@/types';
import type { AssignmentPayload } from '@/api/assignmentsApi';
import AssignmentFormDialog from './components/AssignmentFormDialog';
import SubmissionsDialog from './components/SubmissionsDialog';
import SubmitAssignmentDialog from './components/SubmitAssignmentDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'TEACHER', 'CLASS_TEACHER'];

/** Management view for TEACHER/CLASS_TEACHER/admin roles: filterable DataTable, add/edit/delete, view submissions. */
function ManagementAssignmentsView() {
  const { enqueueSnackbar } = useSnackbar();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');
  const [subjectId, setSubjectId] = useState<number | ''>('');

  const [rows, setRows] = useState<Assignment[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Assignment | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Assignment | null>(null);
  const [submissionsTarget, setSubmissionsTarget] = useState<Assignment | null>(null);

  useEffect(() => {
    classesApi.list({ size: 200, sort: 'className,asc' }).then((res) => setClasses(res.data.content)).catch(() => undefined);
  }, []);

  useEffect(() => {
    setSectionId('');
    setSubjectId('');
    if (!classId) {
      setSections([]);
      setSubjects([]);
      return;
    }
    classesApi.listSections(classId as number).then((res) => setSections(res.data)).catch(() => undefined);
    classesApi.listSubjects(classId as number).then((res) => setSubjects(res.data)).catch(() => undefined);
  }, [classId]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await assignmentsApi.list({
        classId: classId || undefined,
        sectionId: sectionId || undefined,
        subjectId: subjectId || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'dueDate,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load assignments.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [classId, sectionId, subjectId, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [classId, sectionId, subjectId]);

  const handleSave = async (values: AssignmentPayload) => {
    setSaving(true);
    try {
      if (editing) {
        await assignmentsApi.update(editing.id, values);
        enqueueSnackbar('Assignment updated.', { variant: 'success' });
      } else {
        await assignmentsApi.create(values);
        enqueueSnackbar('Assignment added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this assignment.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await assignmentsApi.remove(deleteTarget.id);
      enqueueSnackbar('Assignment deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this assignment.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<Assignment>[] = useMemo(
    () => [
      { field: 'title', headerName: 'Title', flex: 1.2, minWidth: 180 },
      { field: 'subjectName', headerName: 'Subject', flex: 0.8, minWidth: 120, valueGetter: (_v, row) => row.subjectName ?? `#${row.subjectId}` },
      {
        field: 'classSection',
        headerName: 'Class',
        width: 120,
        valueGetter: (_v, row) => `${row.className ?? row.classId}${row.sectionName ? `-${row.sectionName}` : ''}`,
      },
      { field: 'assignedDate', headerName: 'Assigned', width: 120, valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-') },
      { field: 'dueDate', headerName: 'Due', width: 120, valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-') },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 150,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View submissions">
              <IconButton size="small" onClick={() => setSubmissionsTarget(params.row)}>
                <GroupsOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => { setEditing(params.row); setFormOpen(true); }}>
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Delete">
              <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <PageHeader
        title="Assignments"
        subtitle="Create assignments, track submissions and grade student work"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Assignments' }]}
        action={
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
            Add Assignment
          </Button>
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField select fullWidth size="small" label="Class" value={classId} onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}>
                <MenuItem value="">All classes</MenuItem>
                {classes.map((c) => (
                  <MenuItem key={c.id} value={c.id}>{c.className}</MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <TextField select fullWidth size="small" label="Section" disabled={!classId} value={sectionId} onChange={(e) => setSectionId(e.target.value === '' ? '' : Number(e.target.value))}>
                <MenuItem value="">All sections</MenuItem>
                {sections.map((s) => (
                  <MenuItem key={s.id} value={s.id}>{s.sectionName}</MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <TextField select fullWidth size="small" label="Subject" disabled={!classId} value={subjectId} onChange={(e) => setSubjectId(e.target.value === '' ? '' : Number(e.target.value))}>
                <MenuItem value="">All subjects</MenuItem>
                {subjects.map((s) => (
                  <MenuItem key={s.id} value={s.id}>{s.subjectName}</MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          mobileVisibleFields={['title', 'dueDate']}
          emptyTitle="No assignments found"
          emptyDescription="Add an assignment to get started."
        />
      </Card>

      <AssignmentFormDialog
        open={formOpen}
        editing={editing}
        classes={classes}
        saving={saving}
        onClose={() => { setFormOpen(false); setEditing(null); }}
        onSubmit={handleSave}
      />

      <SubmissionsDialog open={!!submissionsTarget} assignment={submissionsTarget} onClose={() => setSubmissionsTarget(null)} />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete assignment"
        message={`Delete "${deleteTarget?.title ?? ''}"? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

interface StudentAssignmentRow {
  assignment: Assignment;
  submission: AssignmentSubmission | null;
}

function submissionStatusChip(row: StudentAssignmentRow) {
  if (row.submission) {
    return <StatusChip status={row.submission.status} />;
  }
  const overdue = dayjs(row.assignment.dueDate).isBefore(dayjs(), 'day');
  return <Chip size="small" variant="outlined" color={overdue ? 'error' : 'warning'} label={overdue ? 'Overdue' : 'Not Submitted'} />;
}

/** STUDENT card-list view: assignment details + submission status, submit button, marks/feedback once graded. */
function StudentAssignmentsView() {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState<StudentAssignmentRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [submitTarget, setSubmitTarget] = useState<Assignment | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setLoaded(false);
    try {
      const res = await assignmentsApi.list({ size: 100, sort: 'dueDate,desc' });
      const withSubmissions = await Promise.all(
        res.data.content.map(async (a) => {
          const subRes = await assignmentsApi.getMySubmission(a.id).catch(() => ({ data: null }));
          return { assignment: a, submission: subRes.data };
        }),
      );
      setRows(withSubmissions);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load your assignments.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (file: File) => {
    if (!submitTarget) return;
    setSubmitting(true);
    try {
      await assignmentsApi.submit(submitTarget.id, file);
      enqueueSnackbar('Assignment submitted.', { variant: 'success' });
      setSubmitTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not submit this assignment.', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Assignments"
        subtitle="View assignments for your class and submit your work"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Assignments' }]}
      />

      {loading ? (
        <PageLoader label="Loading assignments..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No assignments yet" description="Your teachers haven't posted any assignments yet." />
      ) : (
        <Grid container spacing={2.5}>
          {rows.map((row) => (
            <Grid item xs={12} sm={6} md={4} key={row.assignment.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" sx={{ mb: 1 }}>
                    <Typography variant="subtitle1" fontWeight={700}>
                      {row.assignment.title}
                    </Typography>
                    {submissionStatusChip(row)}
                  </Stack>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    {row.assignment.subjectName ?? `Subject #${row.assignment.subjectId}`}
                  </Typography>
                  {row.assignment.description && (
                    <Typography variant="body2" sx={{ mb: 1.5 }}>
                      {row.assignment.description}
                    </Typography>
                  )}
                  <Typography variant="caption" color="text.secondary" display="block">
                    Due {dayjs(row.assignment.dueDate).format('DD MMM YYYY')}
                  </Typography>
                  {row.assignment.fileUrl && (
                    <Link href={row.assignment.fileUrl} target="_blank" rel="noopener noreferrer" variant="body2" sx={{ display: 'inline-block', mt: 1 }}>
                      Download assignment
                    </Link>
                  )}

                  {row.submission?.status === 'GRADED' && (
                    <>
                      <Divider sx={{ my: 1.5 }} />
                      <Stack direction="row" spacing={1} alignItems="center">
                        <AssignmentTurnedInOutlinedIcon color="success" fontSize="small" />
                        <Typography variant="body2" fontWeight={700}>
                          Marks: {row.submission.marksObtained}
                        </Typography>
                      </Stack>
                      {row.submission.feedback && (
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                          "{row.submission.feedback}"
                        </Typography>
                      )}
                    </>
                  )}
                </CardContent>
                <Box sx={{ p: 2, pt: 0 }}>
                  {row.submission ? (
                    <Button fullWidth variant="outlined" disabled>
                      {row.submission.status === 'GRADED' ? 'Graded' : 'Submitted'}
                    </Button>
                  ) : (
                    <Button fullWidth variant="contained" onClick={() => setSubmitTarget(row.assignment)}>
                      Submit
                    </Button>
                  )}
                </Box>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <SubmitAssignmentDialog
        open={!!submitTarget}
        assignment={submitTarget}
        saving={submitting}
        onClose={() => setSubmitTarget(null)}
        onSubmit={handleSubmit}
      />
    </Box>
  );
}

/** Branches by role: TEACHER/CLASS_TEACHER/admin get the management table, STUDENT gets a card list + submit workflow. */
export function AssignmentListPage() {
  const role = useAppSelector((state) => state.auth.user?.role);
  const isManagement = !!role && WRITE_ROLES.includes(role);
  return isManagement ? <ManagementAssignmentsView /> : <StudentAssignmentsView />;
}

export default AssignmentListPage;
