import { useMemo } from 'react';
import type { ComponentProps, ReactNode } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Tooltip from '@mui/material/Tooltip';
import useMediaQuery from '@mui/material/useMediaQuery';
import { useTheme } from '@mui/material/styles';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import {
  DataGrid,
  GridToolbarContainer,
  GridToolbarColumnsButton,
  GridToolbarFilterButton,
  GridToolbarDensitySelector,
  GridToolbarQuickFilter,
  type GridColDef,
  type GridRowsProp,
  type GridValidRowModel,
  type GridPaginationModel,
  type GridSortModel,
  type GridRowSelectionModel,
  type DataGridProps,
} from '@mui/x-data-grid';
import EmptyState from './EmptyState';

export interface DataTableProps<T extends GridValidRowModel> {
  rows: GridRowsProp<T>;
  columns: GridColDef<T>[];
  loading?: boolean;
  pageSize?: number;
  onExport?: () => void;
  height?: number;
  checkboxSelection?: boolean;
  /**
   * Per-row height, forwarded to DataGrid. Pass `() => 'auto'` when a cell
   * renders a variable number of controls (the class list stacks one
   * class-teacher dropdown per section) so rows grow to fit instead of clipping.
   */
  getRowHeight?: DataGridProps['getRowHeight'];
  getRowId?: (row: T) => string | number;
  emptyTitle?: string;
  emptyDescription?: string;
  /** Extra actions rendered in the toolbar, left of the Export button (e.g. bulk "Promote"). */
  toolbarExtra?: ReactNode;
  /** When set to 'server', pagination/sorting are driven by the props below instead of client-side. */
  paginationMode?: 'client' | 'server';
  sortingMode?: 'client' | 'server';
  rowCount?: number;
  paginationModel?: GridPaginationModel;
  onPaginationModelChange?: (model: GridPaginationModel) => void;
  sortModel?: GridSortModel;
  onSortModelChange?: (model: GridSortModel) => void;
  rowSelectionModel?: GridRowSelectionModel;
  onRowSelectionModelChange?: (model: GridRowSelectionModel) => void;
  /**
   * Data-column fields to keep visible on phone-width screens (< sm); every other data
   * column starts hidden there (the 'actions' column is always kept). Purely a starting
   * point — still user-toggleable afterwards via the Columns menu. Omit to leave every
   * column visible and rely on the grid's own horizontal scroll on narrow screens.
   */
  mobileVisibleFields?: string[];
}

function ExportableToolbar({
  onExport,
  toolbarExtra,
}: {
  onExport?: () => void;
  toolbarExtra?: ReactNode;
}) {
  return (
    <GridToolbarContainer sx={{ px: 1.5, py: 1, gap: 1 }}>
      <GridToolbarColumnsButton />
      <GridToolbarFilterButton />
      <GridToolbarDensitySelector />
      <Box sx={{ flexGrow: 1 }} />
      <GridToolbarQuickFilter />
      {toolbarExtra}
      {onExport && (
        <Tooltip title="Export the current view">
          <Button
            size="small"
            startIcon={<FileDownloadOutlinedIcon />}
            onClick={onExport}
            variant="outlined"
          >
            Export
          </Button>
        </Tooltip>
      )}
    </GridToolbarContainer>
  );
}

/**
 * Pre-configured MUI X DataGrid wrapper reused by every module's list page.
 * Ships pagination, sorting, column/density/filter toolbar and an optional
 * export button wired to a caller-supplied handler.
 */
export function DataTable<T extends GridValidRowModel>({
  rows,
  columns,
  loading = false,
  pageSize = 10,
  onExport,
  height = 520,
  checkboxSelection = false,
  getRowHeight,
  getRowId,
  emptyTitle,
  emptyDescription,
  toolbarExtra,
  paginationMode = 'client',
  sortingMode = 'client',
  rowCount,
  paginationModel,
  onPaginationModelChange,
  sortModel,
  onSortModelChange,
  rowSelectionModel,
  onRowSelectionModelChange,
  mobileVisibleFields,
}: DataTableProps<T>) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  const initialState = useMemo(() => {
    const state: ComponentProps<typeof DataGrid>['initialState'] = {};
    if (paginationMode === 'client') {
      state.pagination = { paginationModel: { pageSize, page: 0 } };
    }
    if (isMobile && mobileVisibleFields) {
      const columnVisibilityModel: Record<string, boolean> = {};
      columns.forEach((col) => {
        if (col.field !== 'actions' && !mobileVisibleFields.includes(col.field)) {
          columnVisibilityModel[col.field] = false;
        }
      });
      state.columns = { columnVisibilityModel };
    }
    return state;
  }, [pageSize, paginationMode, isMobile, mobileVisibleFields, columns]);

  if (!loading && rows.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <Box sx={{ width: '100%', height }}>
      <DataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        getRowId={getRowId}
        checkboxSelection={checkboxSelection}
        getRowHeight={getRowHeight}
        disableRowSelectionOnClick
        pageSizeOptions={[5, 10, 25, 50]}
        initialState={Object.keys(initialState).length ? initialState : undefined}
        paginationMode={paginationMode}
        sortingMode={sortingMode}
        rowCount={paginationMode === 'server' ? rowCount : undefined}
        paginationModel={paginationModel}
        onPaginationModelChange={onPaginationModelChange}
        sortModel={sortModel}
        onSortModelChange={onSortModelChange}
        rowSelectionModel={rowSelectionModel}
        onRowSelectionModelChange={onRowSelectionModelChange}
        slots={{ toolbar: () => <ExportableToolbar onExport={onExport} toolbarExtra={toolbarExtra} /> }}
        sx={{
          '& .MuiDataGrid-cell:focus': { outline: 'none' },
          '& .MuiDataGrid-cell:focus-within': { outline: 'none' },
        }}
      />
    </Box>
  );
}

export default DataTable;
