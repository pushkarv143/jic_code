import Chip from '@mui/material/Chip';
import type { ChipProps } from '@mui/material/Chip';

const COLOR_MAP: Record<string, ChipProps['color']> = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  RESIGNED: 'warning',
  TERMINATED: 'error',
  ALUMNI: 'info',
  TRANSFERRED: 'warning',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  // Attendance statuses (student_attendance / teacher_attendance).
  PRESENT: 'success',
  ABSENT: 'error',
  LATE: 'warning',
  HALF_DAY: 'info',
  LEAVE: 'secondary',
  // Fee statuses (student_fees).
  PAID: 'success',
  UNPAID: 'warning',
  PARTIAL: 'info',
  OVERDUE: 'error',
};

export interface StatusChipProps {
  status: string;
  size?: ChipProps['size'];
}

/** Consistently-coloured chip for the ACTIVE/INACTIVE/ALUMNI/... enum statuses used across modules. */
export function StatusChip({ status, size = 'small' }: StatusChipProps) {
  const label = status
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
  return <Chip label={label} color={COLOR_MAP[status] ?? 'default'} size={size} variant="outlined" />;
}

export default StatusChip;
