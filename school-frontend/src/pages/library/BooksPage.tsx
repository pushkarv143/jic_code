import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InputAdornment from '@mui/material/InputAdornment';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import type { GridColDef, GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import libraryApi from '@/api/libraryApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { exportRowsToCsv } from '@/utils/csvExport';
import { formatCurrencyINR } from '@/utils/format';
import type { Book, BookCategory } from '@/types';
import BookFormDialog from './components/BookFormDialog';
import CategoryManagerDialog from './components/CategoryManagerDialog';

/** Server-paginated book catalog: search + category filter, CRUD, CSV export. */
export function BooksPage() {
  const { enqueueSnackbar } = useSnackbar();

  const [rows, setRows] = useState<Book[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<BookCategory[]>([]);
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 400);

  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'title', sort: 'asc' }]);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Book | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Book | null>(null);
  const [categoryManagerOpen, setCategoryManagerOpen] = useState(false);

  const loadCategories = useCallback(() => {
    libraryApi.bookCategories
      .list()
      .then((res) => setCategories(res.data))
      .catch(() => enqueueSnackbar('Could not load book categories.', { variant: 'error' }));
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadBooks = useCallback(async () => {
    setLoading(true);
    try {
      const sort = sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
      const res = await libraryApi.books.list({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        search: debouncedSearch || undefined,
        categoryId: categoryId || undefined,
        sort,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load books. Please try again.', {
        variant: 'error',
      });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, sortModel, debouncedSearch, categoryId, enqueueSnackbar]);

  useEffect(() => {
    loadBooks();
  }, [loadBooks]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [debouncedSearch, categoryId]);

  const handleSave = async (values: Parameters<typeof libraryApi.books.create>[0]) => {
    setSaving(true);
    try {
      if (editing) {
        await libraryApi.books.update(editing.id, values);
        enqueueSnackbar('Book updated.', { variant: 'success' });
      } else {
        await libraryApi.books.create(values);
        enqueueSnackbar('Book added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      loadBooks();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this book.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await libraryApi.books.remove(deleteTarget.id);
      enqueueSnackbar('Book deleted.', { variant: 'success' });
      setDeleteTarget(null);
      loadBooks();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — it may have active issues.', {
        variant: 'error',
      });
      setDeleteTarget(null);
    }
  };

  const handleExport = () => {
    exportRowsToCsv(
      rows as unknown as Record<string, unknown>[],
      [
        { field: 'title', header: 'Title' },
        { field: 'author', header: 'Author' },
        { field: 'isbn', header: 'ISBN' },
        { field: 'categoryName', header: 'Category' },
        { field: 'publisher', header: 'Publisher' },
        { field: 'totalCopies', header: 'Total Copies' },
        { field: 'availableCopies', header: 'Available Copies' },
        { field: 'rackNumber', header: 'Rack No.' },
        { field: 'price', header: 'Price' },
      ],
      `books-page-${paginationModel.page + 1}`,
    );
  };

  const columns: GridColDef<Book>[] = useMemo(
    () => [
      { field: 'title', headerName: 'Title', flex: 1.3, minWidth: 200 },
      { field: 'author', headerName: 'Author', flex: 0.9, minWidth: 140 },
      { field: 'isbn', headerName: 'ISBN', width: 130 },
      { field: 'categoryName', headerName: 'Category', flex: 0.8, minWidth: 120, valueGetter: (_v, row) => row.categoryName ?? `#${row.categoryId}` },
      {
        field: 'copies',
        headerName: 'Copies',
        width: 130,
        sortable: false,
        renderCell: (params) => (
          <Chip
            size="small"
            label={`${params.row.availableCopies}/${params.row.totalCopies} available`}
            color={params.row.availableCopies > 0 ? 'success' : 'default'}
            variant="outlined"
          />
        ),
      },
      { field: 'rackNumber', headerName: 'Rack', width: 90, valueGetter: (_v, row) => row.rackNumber ?? '-' },
      {
        field: 'price',
        headerName: 'Price',
        width: 110,
        valueFormatter: (value) => (value ? formatCurrencyINR(Number(value)) : '-'),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
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
          </Stack>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                size="small"
                placeholder="Search by title, author or ISBN"
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
                label="Category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All categories</MenuItem>
                {categories.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={6}>
              <Stack direction="row" spacing={1.5} justifyContent={{ xs: 'flex-start', md: 'flex-end' }}>
                <Button variant="outlined" startIcon={<CategoryOutlinedIcon />} onClick={() => setCategoryManagerOpen(true)}>
                  Categories
                </Button>
                <Button
                  variant="contained"
                  startIcon={<AddOutlinedIcon />}
                  onClick={() => {
                    setEditing(null);
                    setFormOpen(true);
                  }}
                >
                  Add Book
                </Button>
              </Stack>
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
          sortingMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          sortModel={sortModel}
          onSortModelChange={setSortModel}
          onExport={handleExport}
          emptyTitle="No books found"
          emptyDescription="Try adjusting the filters, or add a new book to the catalog."
          mobileVisibleFields={['title', 'copies']}
        />
      </Card>

      <BookFormDialog
        open={formOpen}
        editing={editing}
        categories={categories}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete book"
        message={`Delete "${deleteTarget?.title ?? ''}"? This may fail if it has active issues.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <CategoryManagerDialog
        open={categoryManagerOpen}
        onClose={() => setCategoryManagerOpen(false)}
        onChanged={loadCategories}
      />
    </Box>
  );
}

export default BooksPage;
