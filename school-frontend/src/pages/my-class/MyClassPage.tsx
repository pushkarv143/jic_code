import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import ClassOutlinedIcon from '@mui/icons-material/ClassOutlined';
import MilitaryTechOutlinedIcon from '@mui/icons-material/MilitaryTechOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import myClassApi from '@/api/myClassApi';
import { useAccess } from '@/access/AccessProvider';
import { getStudentDisplayName } from '@/utils/format';
import useDebouncedValue from '@/hooks/useDebouncedValue';
import type { ClassOfficial, Student } from '@/types';
import MyClassStudentDialog from './components/MyClassStudentDialog';
import MyClassOfficials from './components/MyClassOfficials';

/**
 * The class teacher's own section.
 *
 * <p>Exists because the whole-school Classes screen answers a different question.
 * That one is a directory a teacher may read; this one is the single section they
 * are accountable for and may actually change. Keeping them apart is what let the
 * write controls move off the shared screen — see ClassListPage, where the
 * class-teacher dropdown is now read-only for anyone without SECTION_MANAGE.
 *
 * <p>Every request here is scoped server-side by the homeroom assignment: no class
 * or section id is sent, so there is nothing for this page to get wrong.
 */
export function MyClassPage() {
  const { enqueueSnackbar } = useSnackbar();
  const { homeroom, can, isClassTeacherOfOwnSection, settled } = useAccess();

  const canManageRoster = can('MY_CLASS_ROSTER_MANAGE');

  const [tab, setTab] = useState(0);
  const [rows, setRows] = useState<Student[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 350);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 25 });

  const [officials, setOfficials] = useState<ClassOfficial[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Student | null>(null);

  const loadRoster = useCallback(async () => {
    setLoading(true);
    try {
      const page = await myClassApi.getStudents({
        search: debouncedSearch || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
      });
      setRows(page.content);
      setRowCount(page.totalElements);
    } catch {
      enqueueSnackbar('Could not load your class roster.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, paginationModel.page, paginationModel.pageSize, enqueueSnackbar]);

  const loadOfficials = useCallback(async () => {
    try {
      setOfficials(await myClassApi.getOfficials());
    } catch {
      // Non-fatal: the roster is the point of this page, and the posts panel
      // renders its own empty state. A failure here should not blank the screen.
      setOfficials([]);
    }
  }, []);

  useEffect(() => {
    if (!isClassTeacherOfOwnSection) return;
    void loadRoster();
  }, [isClassTeacherOfOwnSection, loadRoster]);

  useEffect(() => {
    if (!isClassTeacherOfOwnSection) return;
    void loadOfficials();
  }, [isClassTeacherOfOwnSection, loadOfficials]);

  const columns = useMemo<GridColDef<Student>[]>(
    () => [
      { field: 'rollNumber', headerName: 'Roll', width: 80 },
      {
        field: 'name',
        headerName: 'Student',
        flex: 1,
        minWidth: 180,
        valueGetter: (_value, row) => getStudentDisplayName(row),
      },
      { field: 'admissionNumber', headerName: 'Admission No.', width: 140 },
      { field: 'gender', headerName: 'Gender', width: 100 },
      { field: 'dateOfBirth', headerName: 'Date of Birth', width: 130 },
      {
        field: 'primaryGuardianName',
        headerName: 'Guardian',
        flex: 1,
        minWidth: 150,
        valueGetter: (_value, row) => row.primaryGuardianName ?? '—',
      },
      { field: 'phone', headerName: 'Phone', width: 140 },
      ...(canManageRoster
        ? [
            {
              field: 'actions',
              headerName: 'Actions',
              width: 90,
              sortable: false,
              filterable: false,
              renderCell: (params) => (
                <Tooltip title="Edit student">
                  <IconButton
                    size="small"
                    onClick={() => {
                      setEditing(params.row);
                      setDialogOpen(true);
                    }}
                  >
                    <EditOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              ),
            } as GridColDef<Student>,
          ]
        : []),
    ],
    [canManageRoster],
  );

  // Wait for /me/access before deciding: rendering the "no class" message during
  // the first fetch would flash a wrong answer at every homeroom teacher on load.
  if (!settled) {
    return <PageLoader />;
  }

  /**
   * Reached by a user who holds MY_CLASS_VIEW but no assignment — 27 of the 44
   * CLASS_TEACHER role holders on the seeded database. The route guard hides the
   * menu entry, but a bookmarked URL still lands here, so it explains itself
   * rather than showing an empty grid or a bare 403.
   */
  if (!isClassTeacherOfOwnSection || !homeroom) {
    return (
      <Box>
        <PageHeader
          title="My Class"
          breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Class' }]}
        />
        <Alert severity="info">
          You are not currently the class teacher of any section. This page shows the roster for the
          section you are assigned to as class teacher — ask the office to assign you one and it will
          appear here.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`${homeroom.className} — My Class`}
        subtitle={
          homeroom.academicYear
            ? `Class teacher for ${homeroom.academicYear}`
            : 'Your class as class teacher'
        }
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Class' }]}
        /*
         * No "Add Student" here. Admitting a pupil creates a login, an admission
         * number and a guardian record and decides which class they join — the
         * office's job, not a class teacher's, even for their own class. The API
         * has no route for it either; STUDENT_CREATE is held by no teaching role.
         * A class teacher edits the records of students already on their roster.
         */
        action={undefined}
      />

      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<GroupsOutlinedIcon />} label="Students" value={homeroom.studentCount} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<ClassOutlinedIcon />} label="Class" value={homeroom.className} color="info" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            icon={<MilitaryTechOutlinedIcon />}
            label="Posts filled"
            value={officials.length}
            color="success"
          />
        </Grid>
      </Grid>

      <Tabs value={tab} onChange={(_e, next) => setTab(next)} sx={{ mb: 2 }}>
        <Tab label="Students" />
        <Tab label="Class Posts" />
      </Tabs>

      {tab === 0 && (
        <>
          <Card sx={{ mb: 2.5 }}>
            <CardContent>
              <Stack direction="row" spacing={2} alignItems="center">
                <TextField
                  size="small"
                  label="Search students"
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setPaginationModel((prev) => ({ ...prev, page: 0 }));
                  }}
                  sx={{ minWidth: 260 }}
                />
                <Typography variant="body2" color="text.secondary">
                  {rowCount} student{rowCount === 1 ? '' : 's'} on your roster
                </Typography>
              </Stack>
            </CardContent>
          </Card>

          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            paginationMode="server"
            rowCount={rowCount}
            paginationModel={paginationModel}
            onPaginationModelChange={setPaginationModel}
            emptyTitle="No students yet"
            emptyDescription="Students are enrolled by the office. Once they are, they appear here."
            mobileVisibleFields={['rollNumber', 'name']}
          />
        </>
      )}

      {tab === 1 && (
        <MyClassOfficials
          officials={officials}
          students={rows}
          onChanged={() => {
            void loadOfficials();
          }}
        />
      )}

      <MyClassStudentDialog
        open={dialogOpen}
        student={editing}
        className={homeroom.className}
        onClose={() => setDialogOpen(false)}
        onSaved={() => {
          setDialogOpen(false);
          void loadRoster();
        }}
      />
    </Box>
  );
}

export default MyClassPage;
