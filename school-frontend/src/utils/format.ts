import type { Role, Student } from '@/types';

/** Converts SCREAMING_SNAKE role names into readable labels, e.g. VICE_PRINCIPAL -> "Vice Principal". */
export function formatRoleLabel(role: Role | string): string {
  return role
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

export function formatCurrencyINR(value: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat('en-IN').format(value);
}

/**
 * Students without a login account have no first/last name (schema-wise `user_id`
 * is nullable). Falls back to the primary guardian's name so the UI never renders
 * a literal "null null" for that case.
 */
export function getStudentDisplayName(
  student: Pick<Student, 'firstName' | 'lastName'> & { primaryGuardianName?: string | null }
): string {
  const full = [student.firstName, student.lastName].filter(Boolean).join(' ').trim();
  if (full) return full;
  return student.primaryGuardianName ? `${student.primaryGuardianName} (Guardian)` : 'Unnamed Student';
}

export function getStudentInitials(student: Pick<Student, 'firstName' | 'lastName'>): string {
  const initials = `${student.firstName?.[0] ?? ''}${student.lastName?.[0] ?? ''}`.toUpperCase();
  return initials || '?';
}
