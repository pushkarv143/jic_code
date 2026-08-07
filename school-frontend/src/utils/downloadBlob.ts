/**
 * Triggers a browser download for a `Blob` obtained from an axios request made
 * with `{ responseType: 'blob' }` (PDF/Excel/JSON binary downloads). Shared by
 * every module that downloads a server-generated file instead of building one
 * client-side (see utils/csvExport.ts for the client-side CSV case).
 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export default downloadBlob;
