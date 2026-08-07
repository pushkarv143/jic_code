export interface CsvColumn<T> {
  field: keyof T | string;
  header: string;
  /** Optional value accessor; defaults to reading `row[field]`. */
  value?: (row: T) => string | number | null | undefined;
}

// Escape-sequence form (not a literal character) so linters don't flag this as
// stray irregular whitespace in the source file.
const UTF8_BOM = '\uFEFF';

function toCsvCell(value: unknown): string {
  if (value === null || value === undefined) return '';
  const str = String(value);
  if (/[",\n]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

/**
 * Builds a CSV string from rows + column definitions and triggers a browser
 * download. Pure client-side (no server round-trip) - used by every module's
 * "Export" button to export the current page's visible rows.
 */
export function exportRowsToCsv<T extends Record<string, unknown>>(
  rows: T[],
  columns: CsvColumn<T>[],
  filename: string,
): void {
  const header = columns.map((col) => toCsvCell(col.header)).join(',');
  const lines = rows.map((row) =>
    columns
      .map((col) => toCsvCell(col.value ? col.value(row) : (row as Record<string, unknown>)[col.field as string]))
      .join(','),
  );
  const csv = [header, ...lines].join('\r\n');

  // Prefix a UTF-8 BOM so Excel opens the file without mangling non-ASCII characters.
  const blob = new Blob([UTF8_BOM + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export default exportRowsToCsv;
