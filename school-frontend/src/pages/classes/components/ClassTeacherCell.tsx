import { useState } from 'react';
import Stack from '@mui/material/Stack';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import type { Section, Teacher } from '@/types';

export interface ClassTeacherCellProps {
  sections: Section[];
  teachers: Teacher[];
  /**
   * Where each already-assigned teacher's homeroom is, keyed by teacher id.
   * A teacher can head only one section, so anyone in here is unavailable
   * everywhere except the section they already hold.
   */
  takenBy: Record<number, { sectionId: number; label: string }>;
  /** Resolves once the change has been persisted, so the row can refresh. */
  onAssign: (sectionId: number, teacherId: number | null) => Promise<void>;
  disabled?: boolean;
}

const UNASSIGNED = '';

/**
 * One class-teacher dropdown per section, stacked.
 *
 * <p>A class teacher belongs to a section rather than to a class — a class here
 * has three — so a single dropdown on the class row would have to pick one
 * section arbitrarily or overwrite all three with the same teacher. Stacking one
 * control per section keeps each assignment addressed to the section it actually
 * applies to.
 */
export function ClassTeacherCell({
  sections,
  teachers,
  takenBy,
  onAssign,
  disabled,
}: ClassTeacherCellProps) {
  // Keyed by section so two dropdowns in the same row spin independently.
  const [savingSectionId, setSavingSectionId] = useState<number | null>(null);

  if (sections.length === 0) {
    return (
      <Typography variant="caption" color="text.secondary">
        No sections
      </Typography>
    );
  }

  const handleChange = async (sectionId: number, raw: string) => {
    setSavingSectionId(sectionId);
    try {
      await onAssign(sectionId, raw === UNASSIGNED ? null : Number(raw));
    } finally {
      setSavingSectionId(null);
    }
  };

  return (
    <Stack spacing={0.5} sx={{ py: 0.75, width: '100%' }}>
      {sections.map((section) => (
        <Stack key={section.id} direction="row" spacing={0.75} alignItems="center">
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ minWidth: 18, fontWeight: 700 }}
          >
            {section.sectionName}
          </Typography>
          <Select
            size="small"
            fullWidth
            displayEmpty
            disabled={disabled || savingSectionId === section.id}
            value={section.classTeacherId != null ? String(section.classTeacherId) : UNASSIGNED}
            onChange={(e) => handleChange(section.id, e.target.value)}
            sx={{ fontSize: '0.78rem', '& .MuiSelect-select': { py: 0.5 } }}
            renderValue={(value) => {
              if (value === UNASSIGNED) {
                return (
                  <Typography variant="caption" color="text.disabled">
                    Unassigned
                  </Typography>
                );
              }
              // Prefer the name the server already resolved: the teacher may not be
              // in the dropdown list if it was truncated by paging.
              const fallback = teachers.find((t) => String(t.id) === value);
              return (
                <Typography variant="caption" noWrap>
                  {section.classTeacherName ??
                    [fallback?.firstName, fallback?.lastName].filter(Boolean).join(' ') ??
                    value}
                </Typography>
              );
            }}
          >
            <MenuItem value={UNASSIGNED}>
              <em>Unassigned</em>
            </MenuItem>
            {teachers.map((teacher) => {
              // Unavailable everywhere except the section they already head, so
              // the current value still renders and can be re-saved.
              const taken = takenBy[teacher.id];
              const unavailable = !!taken && taken.sectionId !== section.id;
              return (
                <MenuItem key={teacher.id} value={String(teacher.id)} disabled={unavailable}>
                  {[teacher.firstName, teacher.lastName].filter(Boolean).join(' ')}
                  {teacher.employeeId ? ` (${teacher.employeeId})` : ''}
                  {unavailable && (
                    <Typography variant="caption" color="text.disabled" sx={{ ml: 1 }}>
                      — already {taken.label}
                    </Typography>
                  )}
                </MenuItem>
              );
            })}
          </Select>
          {savingSectionId === section.id && (
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <CircularProgress size={14} />
            </Box>
          )}
        </Stack>
      ))}
    </Stack>
  );
}

export default ClassTeacherCell;
