import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InputAdornment from '@mui/material/InputAdornment';
import Tooltip from '@mui/material/Tooltip';
import Pagination from '@mui/material/Pagination';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNew';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import studyMaterialsApi from '@/api/studyMaterialsApi';
import type { MaterialType, StudyMaterial } from '@/types';
import { usePermissions } from '@/hooks/usePermissions';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import StudyMaterialFormDialog from './components/StudyMaterialFormDialog';

const TYPE_FILTERS: Array<{ label: string; value: MaterialType | '' }> = [
  { label: 'All', value: '' },
  { label: 'Notes', value: 'NOTES' },
  { label: 'Slides', value: 'PRESENTATION' },
  { label: 'Worksheets', value: 'WORKSHEET' },
  { label: 'Reference', value: 'REFERENCE' },
  { label: 'Video', value: 'VIDEO' },
];

const PAGE_SIZE = 12;

/**
 * The study material library. One page for every role — the backend decides which
 * rows come back (a teacher sees the classes they teach including their own drafts,
 * a student only published materials for their own class/section), and each row
 * carries `canManage` so the edit/delete controls appear only where they apply.
 */
export function StudyMaterialsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const { can } = usePermissions();
  const canUpload = can('MATERIAL_MANAGE');

  const [materials, setMaterials] = useState<StudyMaterial[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<MaterialType | ''>('');
  const debouncedSearch = useDebouncedValue(search, 400);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<StudyMaterial | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<StudyMaterial | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await studyMaterialsApi.list({
        page,
        size: PAGE_SIZE,
        search: debouncedSearch || undefined,
        materialType: typeFilter || undefined,
      });
      setMaterials(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch {
      enqueueSnackbar('Could not load study materials.', { variant: 'error' });
      setMaterials([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch, typeFilter, enqueueSnackbar]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, typeFilter]);

  const handleOpen = async (material: StudyMaterial) => {
    try {
      // Goes back to the server rather than using the URL already in hand: the
      // download endpoint re-runs the visibility check, so a material revoked
      // since the list was fetched is refused rather than silently opened.
      const res = await studyMaterialsApi.resolveDownload(material.id);
      const target = res.data.externalUrl ?? res.data.fileUrl;
      if (!target) {
        enqueueSnackbar('This material has no attached file or link.', { variant: 'warning' });
        return;
      }
      window.open(target, '_blank', 'noopener,noreferrer');
    } catch {
      enqueueSnackbar('You no longer have access to this material.', { variant: 'error' });
      void load();
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await studyMaterialsApi.remove(deleteTarget.id);
      enqueueSnackbar('Study material deleted', { variant: 'success' });
      setDeleteTarget(null);
      void load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete the material.', {
        variant: 'error',
      });
    } finally {
      setDeleting(false);
    }
  };

  const emptyDescription = useMemo(
    () =>
      canUpload
        ? 'Nothing shared yet for the classes you teach. Use "Share material" to add the first one.'
        : 'Your teachers have not shared any material yet. Check back after your next class.',
    [canUpload],
  );

  return (
    <Box>
      <PageHeader
        title="Study Materials"
        subtitle="Notes, slides and worksheets shared with your classes"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Study Materials' }]}
        action={
          canUpload ? (
            <Button
              variant="contained"
              startIcon={<AddOutlinedIcon />}
              onClick={() => {
                setEditing(null);
                setDialogOpen(true);
              }}
            >
              Share material
            </Button>
          ) : undefined
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                size="small"
                placeholder="Search by title or description"
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
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Type"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value as MaterialType | '')}
              >
                {TYPE_FILTERS.map((option) => (
                  <MenuItem key={option.label} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {loading ? (
        <PageLoader />
      ) : materials.length === 0 ? (
        <EmptyState title="No study materials found" description={emptyDescription} />
      ) : (
        <>
          <Grid container spacing={2}>
            {materials.map((material) => (
              <Grid item xs={12} sm={6} md={4} key={material.id}>
                <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <CardContent sx={{ flexGrow: 1 }}>
                    <Stack direction="row" spacing={1} alignItems="flex-start" justifyContent="space-between">
                      <Stack direction="row" spacing={1} alignItems="center">
                        <DescriptionOutlinedIcon color="primary" fontSize="small" />
                        <Typography variant="subtitle1" fontWeight={700}>
                          {material.title}
                        </Typography>
                      </Stack>
                      {!material.published && <Chip size="small" color="warning" label="Draft" />}
                    </Stack>

                    {material.description && (
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        {material.description}
                      </Typography>
                    )}

                    <Stack direction="row" spacing={0.75} sx={{ mt: 1.5 }} flexWrap="wrap" useFlexGap>
                      <Chip size="small" label={material.materialType.toLowerCase()} />
                      {material.subjectName && (
                        <Chip size="small" variant="outlined" label={material.subjectName} />
                      )}
                      <Chip
                        size="small"
                        variant="outlined"
                        label={
                          material.sectionName
                            ? `${material.className ?? ''} ${material.sectionName}`.trim()
                            : `${material.className ?? 'Class'} - all sections`
                        }
                      />
                    </Stack>

                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>
                      {material.teacherName ? `Shared by ${material.teacherName}` : 'Shared by the school'}
                      {material.createdAt ? ` on ${dayjs(material.createdAt).format('DD MMM YYYY')}` : ''}
                    </Typography>
                  </CardContent>

                  <Stack direction="row" spacing={0.5} sx={{ px: 2, pb: 1.5 }} alignItems="center">
                    <Button
                      size="small"
                      startIcon={material.externalUrl ? <OpenInNewOutlinedIcon /> : <DownloadOutlinedIcon />}
                      onClick={() => handleOpen(material)}
                    >
                      {material.externalUrl ? 'Open' : 'Download'}
                    </Button>
                    <Box sx={{ flexGrow: 1 }} />
                    {/* canManage comes from the backend's ownership rule, so a
                        teacher sees these only on their own uploads. */}
                    {material.canManage && (
                      <Tooltip title="Edit">
                        <IconButton
                          size="small"
                          onClick={() => {
                            setEditing(material);
                            setDialogOpen(true);
                          }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    {material.canManage && (
                      <Tooltip title="Delete">
                        <IconButton size="small" onClick={() => setDeleteTarget(material)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </Stack>
                </Card>
              </Grid>
            ))}
          </Grid>

          {totalPages > 1 && (
            <Stack alignItems="center" sx={{ mt: 3 }}>
              <Pagination
                count={totalPages}
                page={page + 1}
                onChange={(_, next) => setPage(next - 1)}
                color="primary"
              />
            </Stack>
          )}
        </>
      )}

      <StudyMaterialFormDialog
        open={dialogOpen}
        editing={editing}
        onClose={() => setDialogOpen(false)}
        onSaved={() => {
          setDialogOpen(false);
          void load();
        }}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete study material"
        message={`Delete "${deleteTarget?.title ?? 'this material'}"? Students will no longer see it.`}
        confirmLabel="Delete"
        destructive
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default StudyMaterialsPage;
