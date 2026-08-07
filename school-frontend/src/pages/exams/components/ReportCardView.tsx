import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import Avatar from '@mui/material/Avatar';
import Chip from '@mui/material/Chip';
import type { ReportCard } from '@/types';

export interface ReportCardViewProps {
  report: ReportCard;
  photoUrl?: string | null;
  printAreaId?: string;
}

/** Printable report-card layout: school header, student info + photo, subject-wise marks table, overall grade/remarks. */
export function ReportCardView({ report, photoUrl, printAreaId = 'report-card-print-area' }: ReportCardViewProps) {
  const pass = report.overallPercentage >= 40;

  return (
    <Card
      id={printAreaId}
      variant="outlined"
      sx={{ maxWidth: 720, mx: 'auto', '@media print': { boxShadow: 'none', border: 'none' } }}
    >
      <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Typography variant="h5" fontWeight={700}>
            Greenwood International School
          </Typography>
          <Typography variant="body2" color="text.secondary">
            School Address, City, State — PIN
          </Typography>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mt: 2, textTransform: 'uppercase', letterSpacing: 1 }}>
            Report Card — {report.examName}
          </Typography>
        </Box>

        <Divider sx={{ mb: 2 }} />

        <Grid container spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <Grid item xs={photoUrl ? 9 : 12}>
            <Grid container spacing={1.5}>
              <Grid item xs={6}>
                <Typography variant="caption" color="text.secondary">
                  Student Name
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {report.studentName}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="caption" color="text.secondary">
                  Class / Section
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {report.className} - {report.sectionName}
                </Typography>
              </Grid>
            </Grid>
          </Grid>
          {photoUrl && (
            <Grid item xs={3} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Avatar src={photoUrl} variant="rounded" sx={{ width: 64, height: 64 }} />
            </Grid>
          )}
        </Grid>

        <Divider sx={{ mb: 2 }} />

        <Table size="small" sx={{ mb: 2 }}>
          <TableHead>
            <TableRow>
              <TableCell>Subject</TableCell>
              <TableCell align="right">Marks Obtained</TableCell>
              <TableCell align="right">Max Marks</TableCell>
              <TableCell align="right">Grade</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {report.subjects.map((s) => (
              <TableRow key={s.subjectName}>
                <TableCell>{s.subjectName}</TableCell>
                <TableCell align="right">{s.marksObtained}</TableCell>
                <TableCell align="right">{s.maxMarks}</TableCell>
                <TableCell align="right">{s.gradeName ?? '-'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <Divider sx={{ mb: 2 }} />

        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={6} sm={3}>
            <Typography variant="caption" color="text.secondary">
              Total Obtained
            </Typography>
            <Typography variant="body1" fontWeight={700}>
              {report.totalObtained} / {report.totalMax}
            </Typography>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Typography variant="caption" color="text.secondary">
              Percentage
            </Typography>
            <Typography variant="body1" fontWeight={700}>
              {report.overallPercentage.toFixed(1)}%
            </Typography>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Typography variant="caption" color="text.secondary">
              Overall Grade
            </Typography>
            <Typography variant="body1" fontWeight={700}>
              {report.overallGrade}
            </Typography>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Typography variant="caption" color="text.secondary" display="block">
              Result
            </Typography>
            <Chip size="small" label={pass ? 'Pass' : 'Needs Improvement'} color={pass ? 'success' : 'error'} variant="outlined" />
          </Grid>
        </Grid>

        <Divider sx={{ mb: 2 }} />

        <Typography variant="caption" color="text.secondary" display="block" textAlign="center">
          This is a system-generated report card and does not require a signature.
        </Typography>
      </CardContent>
    </Card>
  );
}

export default ReportCardView;
