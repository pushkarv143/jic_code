import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import Switch from '@mui/material/Switch';
import FormControlLabel from '@mui/material/FormControlLabel';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import CircularProgress from '@mui/material/CircularProgress';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import { useSnackbar } from 'notistack';
import studyMaterialsApi, { type StudyMaterialPayload } from '@/api/studyMaterialsApi';
import classesApi from '@/api/classesApi';
import type { MaterialType, SchoolClass, Section, StudyMaterial, Subject } from '@/types';
import { usePermissions } from '@/hooks/usePermissions';

const MATERIAL_TYPES: MaterialType[] = ['NOTES', 'PRESENTATION', 'WORKSHEET', 'REFERENCE', 'VIDEO', 'OTHER'];

export interface StudyMaterialFormDialogProps {
  open: boolean;
  /** Null to create, a material to edit. */
  editing: StudyMaterial | null;
  onClose: () => void;
  onSaved: () => void;
}

type SourceMode = 'file' | 'link';

export function StudyMaterialFormDialog({ open, editing, onClose, onSaved }: StudyMaterialFormDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  // Only management may share with a whole class; the backend refuses a null
  // section for anyone else, so the option is hidden rather than offered and refused.
  const { isManagement } = usePermissions();

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);

  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');
  const [subjectId, setSubjectId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [materialType, setMaterialType] = useState<MaterialType>('NOTES');
  const [published, setPublished] = useState(true);
  const [sourceMode, setSourceMode] = useState<SourceMode>('file');
  const [externalUrl, setExternalUrl] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    classesApi
      .list()
      .then((res) => setClasses(res.data.content))
      .catch(() => setClasses([]));
  }, [open]);

  // Reset the form whenever the dialog opens, so a previous edit never bleeds
  // into the next create.
  useEffect(() => {
    if (!open) return;
    setError(null);
    setFile(null);
    if (editing) {
      setClassId(editing.classId);
      setSectionId(editing.sectionId ?? '');
      setSubjectId(editing.subjectId);
      setTitle(editing.title);
      setDescription(editing.description ?? '');
      setMaterialType(editing.materialType);
      setPublished(editing.published);
      setExternalUrl(editing.externalUrl ?? '');
      setSourceMode(editing.externalUrl ? 'link' : 'file');
    } else {
      setClassId('');
      setSectionId('');
      setSubjectId('');
      setTitle('');
      setDescription('');
      setMaterialType('NOTES');
      setPublished(true);
      setExternalUrl('');
      setSourceMode('file');
    }
  }, [open, editing]);

  useEffect(() => {
    if (!classId) {
      setSections([]);
      setSubjects([]);
      return;
    }
    classesApi
      .listSections(Number(classId))
      .then((res) => setSections(res.data))
      .catch(() => setSections([]));
    classesApi
      .listSubjects(Number(classId))
      .then((res) => setSubjects(res.data))
      .catch(() => setSubjects([]));
  }, [classId]);

  const handleSave = async () => {
    setError(null);

    if (!classId || !subjectId || !title.trim()) {
      setError('Class, subject and title are required.');
      return;
    }
    if (!isManagement && !sectionId) {
      setError('Pick a section. Only management can share with every section of a class.');
      return;
    }
    // Mirrors the backend's either-or rule so the user is told before a round trip.
    const keepingExistingFile = Boolean(editing?.fileUrl) && sourceMode === 'file' && !file;
    if (sourceMode === 'link' && !externalUrl.trim()) {
      setError('Enter the link to the resource.');
      return;
    }
    if (sourceMode === 'file' && !file && !keepingExistingFile) {
      setError('Choose a file to upload, or switch to a link.');
      return;
    }

    const payload: StudyMaterialPayload = {
      classId: Number(classId),
      sectionId: sectionId === '' ? null : Number(sectionId),
      subjectId: Number(subjectId),
      title: title.trim(),
      description: description.trim() || undefined,
      materialType,
      externalUrl: sourceMode === 'link' ? externalUrl.trim() : undefined,
      published,
    };

    setSaving(true);
    try {
      if (editing) {
        await studyMaterialsApi.update(editing.id, payload, sourceMode === 'file' ? file : null);
      } else {
        await studyMaterialsApi.create(payload, sourceMode === 'file' ? file : null);
      }
      enqueueSnackbar(editing ? 'Study material updated' : 'Study material shared', { variant: 'success' });
      onSaved();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Could not save the study material.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit study material' : 'Share study material'}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}

          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                fullWidth
                size="small"
                label="Class"
                value={classId}
                onChange={(e) => {
                  setClassId(e.target.value === '' ? '' : Number(e.target.value));
                  setSectionId('');
                  setSubjectId('');
                }}
              >
                {classes.map((cls) => (
                  <MenuItem key={cls.id} value={cls.id}>
                    {cls.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                select
                fullWidth
                size="small"
                label="Section"
                value={sectionId}
                disabled={!classId}
                onChange={(e) => setSectionId(e.target.value === '' ? '' : Number(e.target.value))}
                helperText={isManagement ? 'Leave blank to share with every section' : 'Required'}
              >
                {isManagement && <MenuItem value="">All sections</MenuItem>}
                {sections.map((section) => (
                  <MenuItem key={section.id} value={section.id}>
                    {section.sectionName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                select
                fullWidth
                size="small"
                label="Subject"
                value={subjectId}
                disabled={!classId}
                onChange={(e) => setSubjectId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                {subjects.map((subject) => (
                  <MenuItem key={subject.id} value={subject.id}>
                    {subject.subjectName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                select
                fullWidth
                size="small"
                label="Type"
                value={materialType}
                onChange={(e) => setMaterialType(e.target.value as MaterialType)}
              >
                {MATERIAL_TYPES.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                label="Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                minRows={2}
                size="small"
                label="Description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </Grid>
          </Grid>

          <Stack spacing={1}>
            <Typography variant="subtitle2" fontWeight={700}>
              Resource
            </Typography>
            <ToggleButtonGroup
              exclusive
              size="small"
              value={sourceMode}
              onChange={(_, next: SourceMode | null) => next && setSourceMode(next)}
            >
              <ToggleButton value="file">Upload a file</ToggleButton>
              <ToggleButton value="link">Link to a resource</ToggleButton>
            </ToggleButtonGroup>

            {sourceMode === 'file' ? (
              <Stack direction="row" spacing={1} alignItems="center">
                <Button component="label" variant="outlined" size="small" startIcon={<UploadFileOutlinedIcon />}>
                  Choose file
                  <input hidden type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
                </Button>
                <Typography variant="body2" color="text.secondary">
                  {file?.name ?? (editing?.fileUrl ? 'Keeping the current file' : 'No file chosen')}
                </Typography>
              </Stack>
            ) : (
              <TextField
                fullWidth
                size="small"
                label="Link"
                placeholder="https://..."
                value={externalUrl}
                onChange={(e) => setExternalUrl(e.target.value)}
              />
            )}
          </Stack>

          <FormControlLabel
            control={<Switch checked={published} onChange={(e) => setPublished(e.target.checked)} />}
            label={published ? 'Published - visible to students' : 'Draft - hidden from students'}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSave}
          disabled={saving}
          startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          {editing ? 'Save changes' : 'Share'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default StudyMaterialFormDialog;
