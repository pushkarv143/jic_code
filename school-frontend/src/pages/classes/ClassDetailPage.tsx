import { useCallback, useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import classesApi from '@/api/classesApi';
import teachersApi from '@/api/teachersApi';
import type { ClassOverview, SchoolClass, Section, Subject, Teacher } from '@/types';
import SectionFormDialog from './components/SectionFormDialog';
import SubjectFormDialog from './components/SubjectFormDialog';
import TeacherMappingTab from './components/TeacherMappingTab';
import ClassOverviewTab from './components/ClassOverviewTab';
import ClassOfficialsTab from './components/ClassOfficialsTab';
import ClassTimetableTab from './components/ClassTimetableTab';

const TABS = ['Overview', 'Sections', 'Subjects', 'Teacher Mapping', 'Officials', 'Timetable'] as const;

/** Class detail: sections (with class-teacher assignment), subjects, and a section x subject teacher-mapping grid. */
export function ClassDetailPage() {
  const { id } = useParams<{ id: string }>();
  const classId = Number(id);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [schoolClass, setSchoolClass] = useState<SchoolClass | null>(null);
  const [sections, setSections] = useState<Section[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [overview, setOverview] = useState<ClassOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(0);

  const [sectionDialogOpen, setSectionDialogOpen] = useState(false);
  const [editingSection, setEditingSection] = useState<Section | null>(null);
  const [savingSection, setSavingSection] = useState(false);
  const [deleteSectionTarget, setDeleteSectionTarget] = useState<Section | null>(null);

  const [subjectDialogOpen, setSubjectDialogOpen] = useState(false);
  const [editingSubject, setEditingSubject] = useState<Subject | null>(null);
  const [savingSubject, setSavingSubject] = useState(false);
  const [deleteSubjectTarget, setDeleteSubjectTarget] = useState<Subject | null>(null);

  // Kept separate from loadAll so appointing an official can refresh just the
  // overview — the warnings there include vacant posts — without re-fetching
  // the roster, subjects and the whole teacher list behind it.
  const loadOverview = useCallback(async () => {
    try {
      const res = await classesApi.getOverview(classId);
      setOverview(res.data);
    } catch {
      // Non-fatal: the other tabs still work without the summary, so this stays
      // quiet rather than throwing an error toast over a screen that loaded.
      setOverview(null);
    }
  }, [classId]);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [classRes, sectionsRes, subjectsRes, teachersRes] = await Promise.all([
        classesApi.getById(classId),
        classesApi.listSections(classId),
        classesApi.listSubjects(classId),
        teachersApi.list({ size: 200, sort: 'employeeId,asc' }),
      ]);
      setSchoolClass(classRes.data);
      setSections(sectionsRes.data);
      setSubjects(subjectsRes.data);
      setTeachers(teachersRes.data.content);
      await loadOverview();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this class.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [classId, enqueueSnackbar, loadOverview]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const handleSaveSection = async (values: { sectionName: string; roomNumber?: string; capacity?: number }) => {
    setSavingSection(true);
    try {
      if (editingSection) {
        await classesApi.updateSection(editingSection.id, values);
        enqueueSnackbar('Section updated.', { variant: 'success' });
      } else {
        await classesApi.createSection(classId, values);
        enqueueSnackbar('Section added.', { variant: 'success' });
      }
      setSectionDialogOpen(false);
      setEditingSection(null);
      const res = await classesApi.listSections(classId);
      setSections(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this section.', { variant: 'error' });
    } finally {
      setSavingSection(false);
    }
  };

  const handleDeleteSection = async () => {
    if (!deleteSectionTarget) return;
    try {
      await classesApi.removeSection(deleteSectionTarget.id);
      enqueueSnackbar('Section deleted.', { variant: 'success' });
      setDeleteSectionTarget(null);
      setSections((prev) => prev.filter((s) => s.id !== deleteSectionTarget.id));
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this section.', { variant: 'error' });
    }
  };

  const handleAssignClassTeacher = async (section: Section, teacher: Teacher | null) => {
    try {
      const res = await classesApi.assignClassTeacher(section.id, teacher ? teacher.id : (null as unknown as number));
      setSections((prev) => prev.map((s) => (s.id === section.id ? res.data : s)));
      enqueueSnackbar('Class teacher updated.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not assign a class teacher.', { variant: 'error' });
    }
  };

  const handleSaveSubject = async (values: { subjectName: string; subjectCode: string; isElective: boolean }) => {
    setSavingSubject(true);
    try {
      if (editingSubject) {
        await classesApi.updateSubject(editingSubject.id, values);
        enqueueSnackbar('Subject updated.', { variant: 'success' });
      } else {
        await classesApi.createSubject(classId, values);
        enqueueSnackbar('Subject added.', { variant: 'success' });
      }
      setSubjectDialogOpen(false);
      setEditingSubject(null);
      const res = await classesApi.listSubjects(classId);
      setSubjects(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this subject.', { variant: 'error' });
    } finally {
      setSavingSubject(false);
    }
  };

  const handleDeleteSubject = async () => {
    if (!deleteSubjectTarget) return;
    try {
      await classesApi.removeSubject(deleteSubjectTarget.id);
      enqueueSnackbar('Subject deleted.', { variant: 'success' });
      setDeleteSubjectTarget(null);
      setSubjects((prev) => prev.filter((s) => s.id !== deleteSubjectTarget.id));
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this subject.', { variant: 'error' });
    }
  };

  const teacherOptions = useMemo(
    () => teachers.map((t) => ({ ...t, label: `${t.firstName ?? t.username} ${t.lastName ?? ''}`.trim() })),
    [teachers],
  );

  if (loading) return <PageLoader label="Loading class..." />;
  if (!schoolClass) return <EmptyState title="Class not found" description="This class may have been removed." />;

  return (
    <Box>
      <PageHeader
        title={schoolClass.className}
        subtitle={schoolClass.academicYearName ?? `Academic Year #${schoolClass.academicYearId}`}
        breadcrumbs={[
          { label: 'Dashboard', to: '/app/dashboard' },
          { label: 'Classes & Sections', to: '/app/classes' },
          { label: schoolClass.className },
        ]}
        action={
          <Button variant="outlined" onClick={() => navigate('/app/classes')}>
            Back to Classes
          </Button>
        }
      />

      <Card>
        <Tabs
          value={tab}
          onChange={(_e: SyntheticEvent, v: number) => setTab(v)}
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          {TABS.map((t) => (
            <Tab key={t} label={t} />
          ))}
        </Tabs>
        <CardContent>
          {tab === 1 && (
            <Box>
              <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<AddOutlinedIcon />}
                  onClick={() => {
                    setEditingSection(null);
                    setSectionDialogOpen(true);
                  }}
                >
                  Add Section
                </Button>
              </Stack>
              {sections.length === 0 ? (
                <EmptyState title="No sections yet" description="Add a section to start enrolling students into this class." />
              ) : (
                <Grid container spacing={2}>
                  {sections.map((sec) => (
                    <Grid item xs={12} sm={6} md={4} key={sec.id}>
                      <Card variant="outlined">
                        <CardContent>
                          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                            <Box>
                              <Typography variant="subtitle1" fontWeight={700}>
                                Section {sec.sectionName}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                Room {sec.roomNumber ?? '-'} · Capacity {sec.capacity ?? '-'}
                              </Typography>
                            </Box>
                            <Stack direction="row" spacing={0.5}>
                              <Tooltip title="Edit">
                                <IconButton
                                  size="small"
                                  onClick={() => {
                                    setEditingSection(sec);
                                    setSectionDialogOpen(true);
                                  }}
                                >
                                  <EditOutlinedIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton size="small" onClick={() => setDeleteSectionTarget(sec)}>
                                  <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                                </IconButton>
                              </Tooltip>
                            </Stack>
                          </Stack>
                          <Box sx={{ mt: 2 }}>
                            <Autocomplete
                              size="small"
                              options={teacherOptions}
                              getOptionLabel={(o) => o.label}
                              value={teacherOptions.find((t) => t.id === sec.classTeacherId) ?? null}
                              onChange={(_e, value) => handleAssignClassTeacher(sec, value)}
                              isOptionEqualToValue={(o, v) => o.id === v.id}
                              renderInput={(params) => <TextField {...params} label="Class Teacher" placeholder="Search teacher..." />}
                            />
                          </Box>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </Box>
          )}

          {tab === 2 && (
            <Box>
              <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<AddOutlinedIcon />}
                  onClick={() => {
                    setEditingSubject(null);
                    setSubjectDialogOpen(true);
                  }}
                >
                  Add Subject
                </Button>
              </Stack>
              {subjects.length === 0 ? (
                <EmptyState title="No subjects yet" description="Add a subject taught in this class." />
              ) : (
                <Grid container spacing={2}>
                  {subjects.map((subj) => (
                    <Grid item xs={12} sm={6} md={4} key={subj.id}>
                      <Card variant="outlined">
                        <CardContent>
                          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                            <Box>
                              <Typography variant="subtitle1" fontWeight={700}>
                                {subj.subjectName}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                Code: {subj.subjectCode}
                              </Typography>
                              {subj.isElective && <Chip label="Elective" size="small" sx={{ ml: 1 }} />}
                            </Box>
                            <Stack direction="row" spacing={0.5}>
                              <Tooltip title="Edit">
                                <IconButton
                                  size="small"
                                  onClick={() => {
                                    setEditingSubject(subj);
                                    setSubjectDialogOpen(true);
                                  }}
                                >
                                  <EditOutlinedIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Delete">
                                <IconButton size="small" onClick={() => setDeleteSubjectTarget(subj)}>
                                  <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                                </IconButton>
                              </Tooltip>
                            </Stack>
                          </Stack>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </Box>
          )}

          {tab === 3 && <TeacherMappingTab classId={classId} sections={sections} subjects={subjects} />}

          {tab === 0 && <ClassOverviewTab overview={overview} />}

          {/* The overview's warnings include vacant posts, so an appointment
              refetches it rather than leaving a stale "No current head boy". */}
          {tab === 4 && <ClassOfficialsTab classId={classId} onChanged={loadOverview} />}

          {tab === 5 && (
            <ClassTimetableTab
              classId={classId}
              sections={sections}
              subjects={subjects}
              teachers={teachers}
            />
          )}
        </CardContent>
      </Card>

      <SectionFormDialog
        open={sectionDialogOpen}
        editing={editingSection}
        saving={savingSection}
        onClose={() => {
          setSectionDialogOpen(false);
          setEditingSection(null);
        }}
        onSubmit={handleSaveSection}
      />

      <ConfirmDialog
        open={!!deleteSectionTarget}
        title="Delete section"
        message={`Delete section ${deleteSectionTarget?.sectionName ?? ''}? This may fail if students are enrolled in it.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDeleteSection}
        onCancel={() => setDeleteSectionTarget(null)}
      />

      <SubjectFormDialog
        open={subjectDialogOpen}
        editing={editingSubject}
        saving={savingSubject}
        onClose={() => {
          setSubjectDialogOpen(false);
          setEditingSubject(null);
        }}
        onSubmit={handleSaveSubject}
      />

      <ConfirmDialog
        open={!!deleteSubjectTarget}
        title="Delete subject"
        message={`Delete ${deleteSubjectTarget?.subjectName ?? ''}? This may fail if it's already mapped to a teacher.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDeleteSubject}
        onCancel={() => setDeleteSubjectTarget(null)}
      />
    </Box>
  );
}

export default ClassDetailPage;
