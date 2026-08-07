import { useCallback, useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Menu from '@mui/material/Menu';
import InputAdornment from '@mui/material/InputAdornment';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Avatar from '@mui/material/Avatar';
import CircularProgress from '@mui/material/CircularProgress';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { getStudentDisplayName, getStudentInitials } from '@/utils/format';
import TrendingUpOutlinedIcon from '@mui/icons-material/TrendingUpOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import type { GridColDef, GridPaginationModel, GridRowSelectionModel, GridSortModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import studentsApi from '@/api/studentsApi';
import classesApi from '@/api/classesApi';
import type { ImportResult, SchoolClass, Section, Student, StudentStatus } from '@/types';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { exportRowsToCsv } from '@/utils/csvExport';
import { downloadBlob } from '@/utils/downloadBlob';
import PromoteDialog from './components/PromoteDialog';
import ImportResultDialog from './components/ImportResultDialog';

const STATUS_OPTIONS: Array<{ label: string; value: StudentStatus | '' }> = [
  { label: 'All', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Alumni', value: 'ALUMNI' },
  { label: 'Transferred', value: 'TRANSFERRED' },
];

/** Server-paginated student directory: filter by class/section/status, search, export, promote in bulk. */
export function StudentListPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [rows, setRows] = useState<Student[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');
  const [status, setStatus] = useState<StudentStatus | ''>('');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 400);

  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'admissionNumber', sort: 'asc' }]);
  const [selectionModel, setSelectionModel] = useState<GridRowSelectionModel>([]);

  const [deleteTarget, setDeleteTarget] = useState<Student | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [promoteOpen, setPromoteOpen] = useState(false);

  const [exportMenuAnchor, setExportMenuAnchor] = useState<HTMLElement | null>(null);
  const [exportingExcel, setExportingExcel] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    classesApi
      .list({ size: 200, sort: 'className,asc' })
      .then((res) => setClasses(res.data.content))
      .catch(() => enqueueSnackbar('Could not load classes for filtering.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!classId) {
      setSections([]);
      setSectionId('');
      return;
    }
    classesApi
      .listSections(classId as number)
      .then((res) => setSections(res.data))
      .catch(() => enqueueSnackbar('Could not load sections for that class.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId]);

  const loadStudents = useCallback(async () => {
    setLoading(true);
    try {
      const sort = sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
      const res = await studentsApi.list({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        search: debouncedSearch || undefined,
        classId: classId || undefined,
        sectionId: sectionId || undefined,
        status: status || undefined,
        sort,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load students. Please try again.', {
        variant: 'error',
      });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, sortModel, debouncedSearch, classId, sectionId, status, enqueueSnackbar]);

  useEffect(() => {
    loadStudents();
  }, [loadStudents]);

  // Reset to page 0 whenever a filter changes so results aren't confusingly empty.
  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [debouncedSearch, classId, sectionId, status]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await studentsApi.remove(deleteTarget.id);
      enqueueSnackbar('Student deleted.', { variant: 'success' });
      setDeleteTarget(null);
      loadStudents();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this student.', {
        variant: 'error',
      });
    } finally {
      setDeleting(false);
    }
  };

  const handleExportCsv = () => {
    setExportMenuAnchor(null);
    exportRowsToCsv(
      rows as unknown as Record<string, unknown>[],
      [
        { field: 'admissionNumber', header: 'Admission No.' },
        { field: 'firstName', header: 'First Name' },
        { field: 'lastName', header: 'Last Name' },
        { field: 'className', header: 'Class' },
        { field: 'sectionName', header: 'Section' },
        { field: 'rollNumber', header: 'Roll No.' },
        { field: 'gender', header: 'Gender' },
        { field: 'phone', header: 'Phone' },
        { field: 'status', header: 'Status' },
      ],
      `students-page-${paginationModel.page + 1}`,
    );
  };

  const handleExportExcel = async () => {
    setExportMenuAnchor(null);
    setExportingExcel(true);
    try {
      const blob = await studentsApi.exportExcel({
        classId: classId || undefined,
        sectionId: sectionId || undefined,
        status: status || undefined,
      });
      downloadBlob(blob, 'students-export.xlsx');
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not export students to Excel.', { variant: 'error' });
    } finally {
      setExportingExcel(false);
    }
  };

  const handleImportClick = () => fileInputRef.current?.click();

  const handleFileSelected = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setImporting(true);
    try {
      const res = await studentsApi.importExcel(file);
      setImportResult(res.data);
      loadStudents();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not import the Excel file.', { variant: 'error' });
    } finally {
      setImporting(false);
    }
  };

  const columns: GridColDef<Student>[] = useMemo(
    () => [
      {
        field: 'name',
        headerName: 'Student',
        flex: 1.4,
        minWidth: 220,
        sortable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={1.25} alignItems="center" sx={{ height: '100%' }}>
            <Avatar src={params.row.photoUrl ?? undefined} sx={{ width: 32, height: 32 }}>
              {getStudentInitials(params.row)}
            </Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Box sx={{ fontWeight: 600, fontSize: '0.85rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {getStudentDisplayName(params.row)}
              </Box>
              <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{params.row.admissionNumber}</Box>
            </Box>
          </Stack>
        ),
      },
      {
        field: 'className',
        headerName: 'Class / Section',
        flex: 0.9,
        minWidth: 130,
        valueGetter: (_value, row) =>
          row.className ? `${row.className}${row.sectionName ? ` - ${row.sectionName}` : ''}` : '-',
      },
      { field: 'rollNumber', headerName: 'Roll No.', width: 100 },
      { field: 'gender', headerName: 'Gender', width: 100 },
      { field: 'phone', headerName: 'Phone', width: 130, sortable: false },
      {
        field: 'status',
        headerName: 'Status',
        width: 130,
        renderCell: (params) => <StatusChip status={params.row.status} />,
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 140,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View profile">
              <IconButton size="small" onClick={() => navigate(`/app/students/${params.row.id}`)}>
                <VisibilityOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => navigate(`/app/students/${params.row.id}/edit`)}>
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
    [navigate],
  );

  const selectedIds = selectionModel as number[];

  return (
    <Box>
      <PageHeader
        title="Students"
        subtitle="Browse, search and manage every enrolled student"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Students' }]}
        action={
          <Button
            variant="contained"
            startIcon={<AddOutlinedIcon />}
            onClick={() => navigate('/app/students/new')}
          >
            Add Student
          </Button>
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                fullWidth
                size="small"
                placeholder="Search by name or admission no."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchOutlinedIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                select
                fullWidth
                size="small"
                label="Class"
                value={classId}
                onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All classes</MenuItem>
                {classes.map((cls) => (
                  <MenuItem key={cls.id} value={cls.id}>
                    {cls.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                select
                fullWidth
                size="small"
                label="Section"
                value={sectionId}
                disabled={!classId}
                onChange={(e) => setSectionId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All sections</MenuItem>
                {sections.map((sec) => (
                  <MenuItem key={sec.id} value={sec.id}>
                    {sec.sectionName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={5}>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {STATUS_OPTIONS.map((opt) => (
                  <Chip
                    key={opt.label}
                    label={opt.label}
                    size="small"
                    color={status === opt.value && opt.value !== '' ? 'primary' : 'default'}
                    variant={status === opt.value ? 'filled' : 'outlined'}
                    onClick={() => setStatus(opt.value)}
                  />
                ))}
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {selectedIds.length > 0 && (
        <Box sx={{ mb: 1.5 }}>
          <Button
            variant="outlined"
            size="small"
            startIcon={<TrendingUpOutlinedIcon />}
            onClick={() => setPromoteOpen(true)}
          >
            Promote {selectedIds.length} selected
          </Button>
        </Box>
      )}

      <input ref={fileInputRef} type="file" accept=".xlsx" hidden onChange={handleFileSelected} />

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          checkboxSelection
          paginationMode="server"
          sortingMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          sortModel={sortModel}
          onSortModelChange={setSortModel}
          rowSelectionModel={selectionModel}
          onRowSelectionModelChange={setSelectionModel}
          toolbarExtra={
            <Stack direction="row" spacing={1}>
              <Button
                size="small"
                variant="outlined"
                startIcon={importing ? <CircularProgress size={14} color="inherit" /> : <FileUploadOutlinedIcon />}
                onClick={handleImportClick}
                disabled={importing}
              >
                Import Excel
              </Button>
              <Button
                size="small"
                variant="outlined"
                startIcon={exportingExcel ? <CircularProgress size={14} color="inherit" /> : <FileDownloadOutlinedIcon />}
                endIcon={<ArrowDropDownIcon />}
                onClick={(e) => setExportMenuAnchor(e.currentTarget)}
                disabled={exportingExcel}
              >
                Export
              </Button>
              <Menu anchorEl={exportMenuAnchor} open={!!exportMenuAnchor} onClose={() => setExportMenuAnchor(null)}>
                <MenuItem onClick={handleExportCsv}>Export CSV</MenuItem>
                <MenuItem onClick={handleExportExcel}>Export Excel (Server)</MenuItem>
              </Menu>
            </Stack>
          }
          emptyTitle="No students found"
          emptyDescription="Try adjusting the filters, or add a new student to get started."
        />
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete student"
        message={`Are you sure you want to delete ${
          deleteTarget ? getStudentDisplayName(deleteTarget) : 'this student'
        }? This action cannot be undone.`}
        confirmLabel="Delete"
        destructive
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <PromoteDialog
        open={promoteOpen}
        studentIds={selectedIds}
        onClose={() => setPromoteOpen(false)}
        onPromoted={() => {
          setPromoteOpen(false);
          setSelectionModel([]);
          loadStudents();
        }}
      />

      <ImportResultDialog open={!!importResult} result={importResult} onClose={() => setImportResult(null)} />
    </Box>
  );
}

export default StudentListPage;
