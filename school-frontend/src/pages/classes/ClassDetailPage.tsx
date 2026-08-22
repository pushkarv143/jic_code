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
import { useAccess } from '@/access/AccessProvider';
import type { ClassOverview, SchoolClass, Section, Subject, Teacher } from '@/types';
import SectionFormDialog from './components/SectionFormDialog';
import SubjectFormDialog from './components/SubjectFormDialog';
import TeacherMappingTab from './components/TeacherMappingTab';
import ClassOverviewTab from './components/ClassOverviewTab';
import ClassOfficialsTab from './components/ClassOfficialsTab';
import ClassTimetableTab from './components/ClassTimetableTab';

// "Sections" is now "Class Setup": the school runs one section per class, so the
// tab shows that section's room, capacity and class teacher without naming it a
// section or offering to add another. The tab positions are unchanged so the
// index checks below keep lining up.
const TABS = ['Overview', 'Class Setup', 'Subjects', 'Teachers & Timetable', 'Officials', 'Week Grid'] as const;

// Assigning periods is the office's job, and /api/v1/timetable/classes/** is
// management-only, so a teacher opening this tab would only reach a 403. They read
// their own week on My Timetable instead.
const TIMETABLE_TAB_INDEX = TABS.indexOf('Week Grid');

/** Class detail: room/capacity and class teacher, subjects, and a subject x teacher mapping grid. */
export function ClassDetailPage() {
  const { id } = useParams<{ id: string }>();
  const classId = Number(id);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  /*
   * Write gates for this screen, matched to what the API actually enforces:
   *
   *   SECTION_MANAGE - room/capacity and the class-teacher assignment, both of
   *                    which write to `sections`
   *   SUBJECT_MANAGE - adding, editing and deleting a class's subjects
   *
   * Read access is deliberately wider than write here: a teacher needs to see
   * who the class teacher is and which subjects the class runs. So the controls
   * are rendered disabled rather than removed where the value itself is the
   * information — see the class-teacher Autocomplete below.
   */
  const { can } = useAccess();
  const canManageSection = can('SECTION_MANAGE');
  const canManageSubjects = can('SUBJECT_MANAGE');
  // The whole-week grid writes the timetable, so it is hidden without the grant
  // rather than shown read-only — it is an editor, not a view.
  const canManageTimetable = can('TIMETABLE_MANAGE');

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

  // Update only. The section is created with the class, and the backend refuses a
  // second one, so there is no create branch left to reach from here.
  const handleSaveSection = async (values: { sectionName: string; roomNumber?: string; capacity?: number }) => {
    if (!editingSection) return;
    setSavingSection(true);
    try {
      await classesApi.updateSection(editingSection.id, values);
      enqueueSnackbar('Room and capacity updated.', { variant: 'success' });
      setSectionDialogOpen(false);
      setEditingSection(null);
      const res = await classesApi.listSections(classId);
      setSections(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save these details.', { variant: 'error' });
    } finally {
      setSavingSection(false);
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
          { label: 'Classes & Subjects', to: '/app/classes' },
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
          {TABS.map((t, index) => (
            <Tab
              key={t}
              label={t}
              // Kept mounted (rather than filtered out) so every tab keeps the index
              // the content checks below use.
              sx={index === TIMETABLE_TAB_INDEX && !canManageTimetable ? { display: 'none' } : undefined}
            />
          ))}
        </Tabs>
        <CardContent>
          {/* One section per class, so this reads as the class's own room, capacity
              and class teacher. No add or delete: the section is created with the
              class and there is never a second one to manage. */}
          {tab === 1 && (
            <Box>
              {sections.length === 0 ? (
                <EmptyState
                  title="This class has no section record"
                  description="Every class is created with one. Contact an administrator if this class is missing it — students cannot be enrolled until it exists."
                />
              ) : (
                <Grid container spacing={2}>
                  {sections.slice(0, 1).map((sec) => (
                    <Grid item xs={12} sm={8} md={6} key={sec.id}>
                      <Card variant="outlined">
                        <CardContent>
                          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                            <Box>
                              <Typography variant="subtitle1" fontWeight={700}>
                                Room &amp; Capacity
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                Room {sec.roomNumber ?? '-'} · Capacity {sec.capacity ?? '-'}
                              </Typography>
                            </Box>
                            {canManageSection && (
                              <Tooltip title="Edit room and capacity">
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
                            )}
                          </Stack>
                          <Box sx={{ mt: 2 }}>
                            {/*
                              Disabled rather than hidden, and `readOnly` so the
                              text cannot be typed into or the popup opened: who
                              the class teacher is, is information a teacher
                              legitimately needs on this screen. Removing the
                              field would take the answer away with the control.
                              Assigning one writes sections.class_teacher_id and
                              is refused by the API without SECTION_MANAGE.
                            */}
                            <Autocomplete
                              size="small"
                              disabled={!canManageSection}
                              readOnly={!canManageSection}
                              options={teacherOptions}
                              getOptionLabel={(o) => o.label}
                              value={teacherOptions.find((t) => t.id === sec.classTeacherId) ?? null}
                              onChange={(_e, value) => handleAssignClassTeacher(sec, value)}
                              isOptionEqualToValue={(o, v) => o.id === v.id}
                              renderInput={(params) => (
                                <TextField
                                  {...params}
                                  label="Class Teacher"
                                  placeholder={canManageSection ? 'Search teacher...' : undefined}
                                  helperText={canManageSection ? undefined : 'Only an administrator can change this'}
                                />
                              )}
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
              {canManageSubjects && (
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
              )}
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
                            {canManageSubjects && (
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
                            )}
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

          {tab === 5 && canManageTimetable && (
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
