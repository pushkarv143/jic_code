import { useRef, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Link from '@mui/material/Link';
import CircularProgress from '@mui/material/CircularProgress';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import studentsApi from '@/api/studentsApi';
import type { StudentDocument } from '@/types';

const DOCUMENT_TYPES = [
  'BIRTH_CERTIFICATE',
  'AADHAR_CARD',
  'TRANSFER_CERTIFICATE',
  'MARKSHEET',
  'PHOTO',
  'ADDRESS_PROOF',
  'OTHER',
];

export interface DocumentsTabProps {
  studentId: number;
  documents: StudentDocument[];
  onChanged: (documents: StudentDocument[]) => void;
}

/** Documents tab: list with type + download link, upload new document (multipart file + documentType). */
export function DocumentsTab({ studentId, documents, onChanged }: DocumentsTabProps) {
  const { enqueueSnackbar } = useSnackbar();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [documentType, setDocumentType] = useState('');
  const [uploading, setUploading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<StudentDocument | null>(null);

  const handleUpload = async (file: File) => {
    if (!documentType) {
      enqueueSnackbar('Please select a document type before uploading.', { variant: 'warning' });
      return;
    }
    setUploading(true);
    try {
      const res = await studentsApi.uploadDocument(studentId, file, documentType);
      onChanged([...documents, res.data]);
      enqueueSnackbar('Document uploaded.', { variant: 'success' });
      setDocumentType('');
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not upload this document.', { variant: 'error' });
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await studentsApi.removeDocument(studentId, deleteTarget.id);
      onChanged(documents.filter((d) => d.id !== deleteTarget.id));
      enqueueSnackbar('Document removed.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not remove this document.', { variant: 'error' });
    } finally {
      setDeleteTarget(null);
    }
  };

  return (
    <Box>
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            Upload a new document
          </Typography>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={5}>
              <TextField
                select
                label="Document Type"
                fullWidth
                size="small"
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
              >
                <MenuItem value="">Select a type</MenuItem>
                {DOCUMENT_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t.replace(/_/g, ' ')}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={7}>
              <Button
                component="label"
                variant="outlined"
                startIcon={uploading ? <CircularProgress size={16} /> : <CloudUploadOutlinedIcon />}
                disabled={uploading}
              >
                Choose File & Upload
                <input
                  ref={fileInputRef}
                  hidden
                  type="file"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleUpload(file);
                  }}
                />
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {documents.length === 0 ? (
        <EmptyState title="No documents uploaded" description="Upload admission or identity documents using the form above." />
      ) : (
        <Grid container spacing={2}>
          {documents.map((doc) => (
            <Grid item xs={12} sm={6} md={4} key={doc.id}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" spacing={1.5} alignItems="flex-start">
                    <DescriptionOutlinedIcon color="primary" />
                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                      <Typography variant="subtitle2" fontWeight={700}>
                        {doc.documentType.replace(/_/g, ' ')}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Uploaded {dayjs(doc.uploadedAt).format('DD MMM YYYY')}
                      </Typography>
                      <Link href={doc.fileUrl} target="_blank" rel="noopener noreferrer" variant="body2">
                        Download
                      </Link>
                    </Box>
                    <IconButton size="small" onClick={() => setDeleteTarget(doc)}>
                      <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                    </IconButton>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete document"
        message={`Delete "${deleteTarget?.documentType.replace(/_/g, ' ')}"? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default DocumentsTab;
