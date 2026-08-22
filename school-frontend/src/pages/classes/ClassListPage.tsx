import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import classesApi from '@/api/classesApi';
import teachersApi from '@/api/teachersApi';
import studentsApi from '@/api/studentsApi';
import academicYearsApi from '@/api/academicYearsApi';
import type {
  AcademicYear,
  ClassOfficialRole,
  ClassTeacherAvailability,
  SchoolClass,
  Student,
  Teacher,
} from '@/types';
import { useAccess } from '@/access/AccessProvider';
import ClassFormDialog from './components/ClassFormDialog';
import AcademicYearManagerDialog from './components/AcademicYearManagerDialog';
import ClassTeacherCell from './components/ClassTeacherCell';
import ClassOfficialCell from './components/ClassOfficialCell';

/**
 * Class directory: filter by academic year, add/edit/delete classes, drill into a
 * class for subjects/mapping/timetable.
 *
 * <p>Readable by teachers, editable only by whoever holds the matching permission.
 * The page previously rendered every control to everyone who could reach it, so a
 * class teacher was shown an editable class-teacher dropdown, an Add Class button
 * and row delete actions — all of which the API answers with 403. Nothing was
 * exposed by that, but every one of those controls was a dead end, and the most
 * prominent of them looked like the authority to appoint a colleague.
 */
export function ClassListPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  /*
   * CLASS_MANAGE covers creating, editing and deleting a class.
   * SECTION_MANAGE covers the class-teacher assignment, which is a section-level
   * change (sections.class_teacher_id) even though the class grid is where it is
   * offered — these are the same two grants SectionController and ClassController
   * enforce, so the buttons now appear exactly when the request would succeed.
   */
  const { can } = useAccess();
  const canManageClasses = can('CLASS_MANAGE');
  const canAssignClassTeacher = can('SECTION_MANAGE');
  // Appointing head boy/girl/monitor across any class. The homeroom-scoped
  // equivalent lives in My Class and needs only MY_CLASS_OFFICIALS_MANAGE.
  const canManageOfficials = can('CLASS_MANAGE');

  const [rows, setRows] = useState<SchoolClass[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SchoolClass | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<SchoolClass | null>(null);
  const [yearManagerOpen, setYearManagerOpen] = useState(false);

  // Teachers are the same list for every row, so they are fetched once.
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  // Students are per class and only needed once a post dropdown is opened, so
  // they are fetched lazily and cached here. Loading them for all ten rows up
  // front would be ten requests and ~300 students for a screen where most rows
  // are never touched.
  const [studentsByClass, setStudentsByClass] = useState<Record<number, Student[]>>({});
  const [teacherAvailability, setTeacherAvailability] = useState<ClassTeacherAvailability[]>([]);
  const requestedStudentsRef = useRef<Set<number>>(new Set());

  const loadYears = useCallback(() => {
    academicYearsApi
      .list()
      .then((res) => setYears(res.data))
      .catch(() => enqueueSnackbar('Could not load academic years.', { variant: 'error' }));
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadYears();
  }, [loadYears]);

  useEffect(() => {
    teachersApi
      .list({ size: 500, sort: 'employeeId,asc' })
      .then((res) => setTeachers(res.data.content))
      .catch(() => enqueueSnackbar('Could not load teachers for assignment.', { variant: 'error' }));
  }, [enqueueSnackbar]);

  /**
   * Which teachers already have a homeroom, school-wide.
   *
   * <p>Fetched separately from the page rather than derived from the rows on
   * screen: a teacher heading a section in some other class would look free
   * otherwise, and the save would fail with a rule the admin could not see.
   */
  const loadTeacherAvailability = useCallback(() => {
    classesApi
      .getClassTeacherAvailability()
      .then((res) => setTeacherAvailability(res.data))
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    loadTeacherAvailability();
  }, [loadTeacherAvailability]);

  const takenBy = useMemo(() => {
    const map: Record<number, { sectionId: number; label: string }> = {};
    teacherAvailability.forEach((entry) => {
      map[entry.teacherId] = {
        sectionId: entry.sectionId,
        label: `${entry.className ?? 'class'} - ${entry.sectionName ?? ''}`.trim(),
      };
    });
    return map;
  }, [teacherAvailability]);

  const requestStudents = useCallback(
    (classId: number) => {
      // Guarded by a ref rather than by studentsByClass: a dropdown can be opened
      // again before the first response lands, and keying off the state map would
      // fire the same request twice.
      if (requestedStudentsRef.current.has(classId)) return;
      requestedStudentsRef.current.add(classId);

      studentsApi
        .list({ classId, size: 200, status: 'ACTIVE' })
        .then((res) => setStudentsByClass((prev) => ({ ...prev, [classId]: res.data.content })))
        .catch(() => {
          // Let a later open retry rather than leaving the dropdown stuck on
          // "Loading students...".
          requestedStudentsRef.current.delete(classId);
          enqueueSnackbar('Could not load students for that class.', { variant: 'error' });
        });
    },
    [enqueueSnackbar],
  );

  const loadClasses = useCallback(async () => {
    setLoading(true);
    try {
      const res = await classesApi.list({
        academicYearId: academicYearId || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'className,asc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load classes.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [academicYearId, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    loadClasses();
  }, [loadClasses]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [academicYearId]);

  const handleSave = async (values: { className: string; academicYearId: number }) => {
    setSaving(true);
    try {
      if (editing) {
        await classesApi.update(editing.id, values);
        enqueueSnackbar('Class updated.', { variant: 'success' });
      } else {
        await classesApi.create(values);
        enqueueSnackbar('Class added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      loadClasses();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this class.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await classesApi.remove(deleteTarget.id);
      enqueueSnackbar('Class deleted.', { variant: 'success' });
      setDeleteTarget(null);
      loadClasses();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this class.', { variant: 'error' });
    }
  };

  /**
   * Applies one assignment and reloads the page.
   *
   * <p>Reloading rather than patching the row in place: appointing a post-holder
   * ends the sitting holder's tenure server-side, and assigning a class teacher
   * can promote that teacher's role. Neither is visible from the response of the
   * call that caused it, so the authoritative state comes from a refetch.
   */
  const applyAssignment = useCallback(
    async (action: () => Promise<unknown>, successMessage: string) => {
      try {
        await action();
        enqueueSnackbar(successMessage, { variant: 'success' });
        await loadClasses();
        // A class-teacher change alters who is free school-wide, so the
        // availability map has to move with it.
        loadTeacherAvailability();
      } catch (err: any) {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not save that assignment.', {
          variant: 'error',
        });
      }
    },
    [enqueueSnackbar, loadClasses, loadTeacherAvailability],
  );

  const columns: GridColDef<SchoolClass>[] = useMemo(
    () => [
      { field: 'className', headerName: 'Class', flex: 1, minWidth: 130 },
      { field: 'academicYearName', headerName: 'Academic Year', width: 130 },
      { field: 'studentCount', headerName: 'Students', width: 90, valueGetter: (_v, row) => row.studentCount ?? '-' },
      {
        field: 'classTeacher',
        headerName: 'Class Teacher',
        width: 230,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <ClassTeacherCell
            sections={params.row.sections ?? []}
            teachers={teachers}
            takenBy={takenBy}
            // The reported bug: without this the dropdown was live for every role
            // that can read the page, and saving returned 403.
            disabled={!canAssignClassTeacher}
            onAssign={(sectionId, teacherId) =>
              applyAssignment(
                () => classesApi.assignClassTeacher(sectionId, teacherId as number),
                teacherId === null ? 'Class teacher cleared.' : 'Class teacher assigned.',
              )
            }
          />
        ),
      },
      ...(['HEAD_BOY', 'HEAD_GIRL', 'MONITOR'] as ClassOfficialRole[]).map((role) => ({
        field: role,
        headerName: role === 'HEAD_BOY' ? 'Head Boy' : role === 'HEAD_GIRL' ? 'Head Girl' : 'Monitor',
        width: 170,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <ClassOfficialCell
            classId={params.row.id}
            role={role}
            // Same bug class as the class-teacher dropdown: appointing school-wide
            // is management-only (ClassOfficialController's WRITE_ROLES), so for a
            // teacher these were live selects that always returned 403. A class
            // teacher appoints posts for their own section under My Class instead.
            disabled={!canManageOfficials}
            officials={params.row.officials ?? []}
            students={studentsByClass[params.row.id]}
            onRequestStudents={() => requestStudents(params.row.id)}
            onAppoint={(studentId) =>
              applyAssignment(
                () => classesApi.appointOfficial(params.row.id, { studentId, role }),
                'Appointed.',
              )
            }
            onVacate={(officialId) =>
              applyAssignment(
                () => classesApi.endOfficial(params.row.id, officialId),
                'Post is now vacant.',
              )
            }
          />
        ),
      })) as GridColDef<SchoolClass>[],
      {
        field: 'actions',
        headerName: 'Actions',
        width: 160,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            {/* Open stays available to everyone who can read the page — the detail
                screen is where a teacher reads the timetable and subject mapping. */}
            <Tooltip title="Open">
              <IconButton size="small" onClick={() => navigate(`/app/classes/${params.row.id}`)}>
                <ArrowForwardOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {canManageClasses && (
              <>
                <Tooltip title="Edit">
                  <IconButton
                    size="small"
                    onClick={() => {
                      setEditing(params.row);
                      setFormOpen(true);
                    }}
                  >
                    <EditOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Delete">
                  <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                    <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                  </IconButton>
                </Tooltip>
              </>
            )}
          </Stack>
        ),
      },
    ],
    [
      navigate,
      teachers,
      takenBy,
      studentsByClass,
      requestStudents,
      applyAssignment,
      canManageClasses,
      canAssignClassTeacher,
      canManageOfficials,
    ],
  );

  return (
    <Box>
      <PageHeader
        title="Classes & Subjects"
        subtitle="Manage classes, subjects, teacher mapping and timetables"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Classes & Subjects' }]}
        action={
          // Both of these write. Rendered only for a caller who may actually use
          // them, rather than shown to every reader and refused by the API.
          canManageClasses ? (
            <Stack direction="row" spacing={1.5}>
              <Button variant="outlined" startIcon={<CalendarMonthOutlinedIcon />} onClick={() => setYearManagerOpen(true)}>
                Academic Years
              </Button>
              <Button
                variant="contained"
                startIcon={<AddOutlinedIcon />}
                onClick={() => {
                  setEditing(null);
                  setFormOpen(true);
                }}
              >
                Add Class
              </Button>
            </Stack>
          ) : undefined
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Academic Year"
                value={academicYearId}
                onChange={(e) => setAcademicYearId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All academic years</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y.id} value={y.id}>
                    {y.yearName}
                    {y.isCurrent ? ' (current)' : ''}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            {years.find((y) => y.isCurrent) && (
              <Grid item>
                <Chip
                  size="small"
                  color="success"
                  variant="outlined"
                  label={`Current year: ${years.find((y) => y.isCurrent)?.yearName}`}
                />
              </Grid>
            )}
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
          getRowHeight={() => "auto"}
          mobileVisibleFields={["className", "studentCount", "classTeacher"]}
          emptyTitle="No classes found"
          emptyDescription="Add a class to get started."
        />
      </Card>

      <ClassFormDialog
        open={formOpen}
        editing={editing}
        years={years}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete class"
        message={`Are you sure you want to delete ${deleteTarget?.className ?? ''}? This may fail if it still has subjects or students.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <AcademicYearManagerDialog
        open={yearManagerOpen}
        onClose={() => setYearManagerOpen(false)}
        onChanged={loadYears}
      />
    </Box>
  );
}

export default ClassListPage;
