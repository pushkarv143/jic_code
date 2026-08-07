import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import type { ImportResult } from '@/types';

export interface ImportResultDialogProps {
  open: boolean;
  result: ImportResult | null;
  onClose: () => void;
}

/** Shows the outcome of POST /students/import/excel: imported count + a table of skipped rows and reasons. */
export function ImportResultDialog({ open, result, onClose }: ImportResultDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Import Results</DialogTitle>
      <DialogContent>
        {result && (
          <Box sx={{ mt: 1 }}>
            <Alert severity={result.skippedRows.length === 0 ? 'success' : 'info'} sx={{ mb: 2 }}>
              {result.importedCount} student{result.importedCount === 1 ? '' : 's'} imported successfully.
              {result.skippedRows.length > 0 &&
                ` ${result.skippedRows.length} row${result.skippedRows.length === 1 ? '' : 's'} skipped.`}
            </Alert>

            {result.skippedRows.length > 0 && (
              <>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Skipped Rows
                </Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell width={110}>Row Number</TableCell>
                      <TableCell>Reason</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {result.skippedRows.map((row) => (
                      <TableRow key={row.rowNumber}>
                        <TableCell>{row.rowNumber}</TableCell>
                        <TableCell>{row.reason}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button variant="contained" onClick={onClose}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default ImportResultDialog;
