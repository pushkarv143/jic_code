import { useCallback, useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import Link from '@mui/material/Link';
import Divider from '@mui/material/Divider';
import Pagination from '@mui/material/Pagination';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import noticesApi from '@/api/noticesApi';
import { useAppSelector } from '@/store/hooks';
import { formatRoleLabel } from '@/utils/format';
import type { Notice, Role } from '@/types';
import type { NoticePayload } from '@/api/noticesApi';
import NoticeFormDialog from './components/NoticeFormDialog';

const ADMIN_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
const PAGE_SIZE = 9;

/** A feed of notice cards, scoped server-side by role. Admin roles can add/edit/delete. */
export function NoticeBoardPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const canWrite = !!user && ADMIN_ROLES.includes(user.role);

  const [rows, setRows] = useState<Notice[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Notice | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Notice | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await noticesApi.list({ page, size: PAGE_SIZE, sort: 'publishedAt,desc' });
      setRows(res.data.content);
      setTotalPages(res.data.totalPages);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load notices.', { variant: 'error' });
      setRows([]);
      setLoaded(true);
    } finally {
      setLoading(false);
    }
  }, [page, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: NoticePayload) => {
    setSaving(true);
    try {
      if (editing) {
        await noticesApi.update(editing.id, values);
        enqueueSnackbar('Notice updated.', { variant: 'success' });
      } else {
        await noticesApi.create(values);
        enqueueSnackbar('Notice published.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      setPage(0);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this notice.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await noticesApi.remove(deleteTarget.id);
      enqueueSnackbar('Notice deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this notice.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Notice Board"
        subtitle="Announcements and circulars"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Notice Board' }]}
        action={
          canWrite && (
            <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
              Add Notice
            </Button>
          )
        }
      />

      {loading ? (
        <PageLoader label="Loading notices..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No notices yet" description="There are no announcements to show right now." />
      ) : (
        <>
          <Grid container spacing={2.5}>
            {rows.map((notice) => {
              const expired = notice.expiryDate ? dayjs(notice.expiryDate).isBefore(dayjs(), 'day') : false;
              return (
                <Grid item xs={12} sm={6} md={4} key={notice.id}>
                  <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', opacity: expired ? 0.6 : 1 }}>
                    <CardContent sx={{ flexGrow: 1 }}>
                      <Stack direction="row" spacing={1.5} alignItems="flex-start" sx={{ mb: 1.5 }}>
                        <Box sx={{ width: 40, height: 40, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'action.hover', color: 'primary.main', flexShrink: 0 }}>
                          <CampaignOutlinedIcon />
                        </Box>
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                          <Typography variant="subtitle1" fontWeight={700}>
                            {notice.title}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {notice.publishedByName ?? 'School Admin'} · {dayjs(notice.publishedAt).format('DD MMM YYYY')}
                          </Typography>
                        </Box>
                        {canWrite && (
                          <Stack direction="row" spacing={0.25}>
                            <Tooltip title="Edit">
                              <IconButton size="small" onClick={() => { setEditing(notice); setFormOpen(true); }}>
                                <EditOutlinedIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Delete">
                              <IconButton size="small" onClick={() => setDeleteTarget(notice)}>
                                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                              </IconButton>
                            </Tooltip>
                          </Stack>
                        )}
                      </Stack>
                      <Typography variant="body2" sx={{ mb: 1.5, whiteSpace: 'pre-wrap' }}>
                        {notice.description}
                      </Typography>
                      <Divider sx={{ mb: 1.5 }} />
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                        {notice.targetRole && (
                          <Chip size="small" variant="outlined" color="primary" label={formatRoleLabel(notice.targetRole)} />
                        )}
                        {notice.expiryDate && (
                          <Chip size="small" variant="outlined" color={expired ? 'default' : 'warning'} label={`${expired ? 'Expired' : 'Expires'} ${dayjs(notice.expiryDate).format('DD MMM YYYY')}`} />
                        )}
                        {notice.attachmentUrl && (
                          <Link href={notice.attachmentUrl} target="_blank" rel="noopener noreferrer" variant="body2" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
                            <AttachFileOutlinedIcon fontSize="inherit" /> Attachment
                          </Link>
                        )}
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>

          {totalPages > 1 && (
            <Stack direction="row" justifyContent="center" sx={{ mt: 3 }}>
              <Pagination count={totalPages} page={page + 1} onChange={(_e, value) => setPage(value - 1)} color="primary" />
            </Stack>
          )}
        </>
      )}

      <NoticeFormDialog
        open={formOpen}
        editing={editing}
        saving={saving}
        onClose={() => { setFormOpen(false); setEditing(null); }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete notice"
        message={`Delete "${deleteTarget?.title ?? ''}"? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default NoticeBoardPage;
