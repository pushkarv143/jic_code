import { useState } from 'react';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import type { ClassOfficial, ClassOfficialRole, Student } from '@/types';
import { getStudentDisplayName } from '@/utils/format';

export interface ClassOfficialCellProps {
  classId: number;
  role: ClassOfficialRole;
  /** Current holders of every post in this class; this cell picks out its own. */
  officials: ClassOfficial[];
  /** Active students of this class, or undefined until they have been fetched. */
  students?: Student[];
  /** Loads this class's students the first time the dropdown is opened. */
  onRequestStudents: () => void;
  onAppoint: (studentId: number) => Promise<void>;
  onVacate: (officialId: number) => Promise<void>;
  disabled?: boolean;
}

const VACANT = '';

/** Head Boy needs a male student and Head Girl a female one; Monitor is open. */
const REQUIRED_GENDER: Partial<Record<ClassOfficialRole, 'MALE' | 'FEMALE'>> = {
  HEAD_BOY: 'MALE',
  HEAD_GIRL: 'FEMALE',
};

const ROLE_LABELS: Record<ClassOfficialRole, string> = {
  HEAD_BOY: 'Head Boy',
  HEAD_GIRL: 'Head Girl',
  MONITOR: 'Monitor',
  SPORTS_CAPTAIN: 'Sports Captain',
  CULTURAL_SECRETARY: 'Cultural Secretary',
};

/**
 * A single dropdown for one class-level post.
 *
 * <p>Students are fetched per class only when a dropdown is first opened. Loading
 * them for every row up front would mean ten extra requests — and roughly three
 * hundred students — on a screen where most rows are never touched.
 */
export function ClassOfficialCell({
  role,
  officials,
  students,
  onRequestStudents,
  onAppoint,
  onVacate,
  disabled,
}: ClassOfficialCellProps) {
  const [saving, setSaving] = useState(false);

  const current = officials.find((o) => o.role === role) ?? null;
  const required = REQUIRED_GENDER[role];

  // Mirrors the server's rule so a mismatch cannot be picked. The server still
  // enforces it; this only keeps the list from offering an option it would reject.
  const options = (students ?? []).filter((s) => !required || s.gender === required);

  // A student holds at most one post, so anyone already holding a different one
  // is unavailable here. The holder of *this* post stays selectable so the
  // current value renders and the grid can re-save the row unchanged.
  const heldElsewhere = new Map<number, string>();
  officials.forEach((official) => {
    if (official.role !== role && official.studentId != null) {
      heldElsewhere.set(official.studentId, ROLE_LABELS[official.role] ?? official.role);
    }
  });

  const handleChange = async (raw: string) => {
    setSaving(true);
    try {
      if (raw === VACANT) {
        if (current) await onVacate(current.id);
      } else {
        await onAppoint(Number(raw));
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Stack direction="row" spacing={0.75} alignItems="center" sx={{ width: '100%' }}>
      <Select
        size="small"
        fullWidth
        displayEmpty
        disabled={disabled || saving}
        value={current?.studentId != null ? String(current.studentId) : VACANT}
        onOpen={onRequestStudents}
        onChange={(e) => handleChange(e.target.value)}
        sx={{ fontSize: '0.78rem', '& .MuiSelect-select': { py: 0.5 } }}
        renderValue={(value) =>
          value === VACANT ? (
            <Typography variant="caption" color="text.disabled">
              Vacant
            </Typography>
          ) : (
            <Typography variant="caption" noWrap>
              {current?.studentName ?? value}
            </Typography>
          )
        }
      >
        <MenuItem value={VACANT}>
          <em>Vacant</em>
        </MenuItem>
        {students === undefined && (
          <MenuItem disabled>
            <em>Loading students...</em>
          </MenuItem>
        )}
        {students !== undefined && options.length === 0 && (
          <MenuItem disabled>
            <em>{required ? `No ${required.toLowerCase()} students in this class` : 'No students'}</em>
          </MenuItem>
        )}
        {options.map((student) => {
          const otherPost = heldElsewhere.get(student.id);
          return (
            <MenuItem key={student.id} value={String(student.id)} disabled={!!otherPost}>
              {getStudentDisplayName(student)}
              {student.rollNumber ? ` (Roll ${student.rollNumber})` : ''}
              {otherPost && (
                <Typography variant="caption" color="text.disabled" sx={{ ml: 1 }}>
                  — already {otherPost}
                </Typography>
              )}
            </MenuItem>
          );
        })}
      </Select>
      {saving && (
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <CircularProgress size={14} />
        </Box>
      )}
    </Stack>
  );
}

export default ClassOfficialCell;
