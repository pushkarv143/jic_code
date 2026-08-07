import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Avatar from '@mui/material/Avatar';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Rating from '@mui/material/Rating';

const FACULTY = [
  { name: 'Dr. Kavita Rao', subject: 'Principal · Physics', experience: '22 yrs', color: '#2b3a8f' },
  { name: 'Mr. Arjun Sethi', subject: 'Vice Principal · Mathematics', experience: '18 yrs', color: '#eb6834' },
  { name: 'Ms. Neha Kapoor', subject: 'Academic Coordinator · English', experience: '15 yrs', color: '#1baf7a' },
  { name: 'Mr. Ramesh Iyer', subject: 'Senior Mathematics Teacher', experience: '14 yrs', color: '#4a3aa7' },
  { name: 'Ms. Priyanka Joshi', subject: 'Senior Science Teacher', experience: '12 yrs', color: '#e34948' },
  { name: 'Mr. Vikram Nair', subject: 'Computer Science Teacher', experience: '9 yrs', color: '#0288d1' },
  { name: 'Ms. Sunita Menon', subject: 'Social Science Teacher', experience: '11 yrs', color: '#eda100' },
  { name: 'Mr. Farhan Ali', subject: 'Physical Education Head', experience: '10 yrs', color: '#e87ba4' },
];

export function FacultyPage() {
  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            Our People
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            Meet our dedicated faculty
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 560, mt: 2 }}>
            150+ experienced educators committed to bringing out the best in every student.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Grid container spacing={3}>
          {FACULTY.map((person) => (
            <Grid item xs={12} sm={6} md={3} key={person.name}>
              <Card sx={{ height: '100%', textAlign: 'center' }}>
                <CardContent>
                  <Avatar
                    sx={{
                      width: 72,
                      height: 72,
                      mx: 'auto',
                      mb: 2,
                      bgcolor: person.color,
                      fontSize: 24,
                      fontWeight: 700,
                    }}
                  >
                    {person.name
                      .split(' ')
                      .filter((w) => w[0] === w[0].toUpperCase() && !w.includes('.'))
                      .map((w) => w[0])
                      .slice(0, 2)
                      .join('')}
                  </Avatar>
                  <Typography variant="subtitle1" fontWeight={700}>
                    {person.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    {person.subject}
                  </Typography>
                  <Stack direction="row" justifyContent="center" spacing={1} alignItems="center">
                    <Chip label={`${person.experience} experience`} size="small" variant="outlined" />
                  </Stack>
                  <Rating value={5} readOnly size="small" sx={{ mt: 1.5 }} />
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
}

export default FacultyPage;
