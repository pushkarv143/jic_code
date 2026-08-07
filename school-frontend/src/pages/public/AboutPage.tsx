import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Avatar from '@mui/material/Avatar';
import Divider from '@mui/material/Divider';
import { alpha } from '@mui/material/styles';
import FlagOutlinedIcon from '@mui/icons-material/FlagOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import FavoriteBorderOutlinedIcon from '@mui/icons-material/FavoriteBorderOutlined';
import HistoryEduOutlinedIcon from '@mui/icons-material/HistoryEduOutlined';

const VALUES = [
  { icon: <FlagOutlinedIcon />, title: 'Our Mission', text: 'To provide holistic, values-driven education that empowers every student to reach their full potential.' },
  { icon: <VisibilityOutlinedIcon />, title: 'Our Vision', text: 'To be a leading institution recognized for academic excellence, innovation, and character development.' },
  { icon: <FavoriteBorderOutlinedIcon />, title: 'Our Values', text: 'Integrity, curiosity, respect, and resilience form the foundation of everything we teach.' },
];

const MILESTONES = [
  { year: '1998', text: 'Greenwood International School founded with 120 students across two houses.' },
  { year: '2005', text: 'CBSE affiliation granted; new science and computer labs inaugurated.' },
  { year: '2012', text: 'Senior secondary wing launched with commerce, science and humanities streams.' },
  { year: '2018', text: 'Smart classrooms and digital library introduced campus-wide.' },
  { year: '2026', text: 'Serving over 2,500 students with a faculty of 150+ educators.' },
];

const LEADERSHIP = [
  { name: 'Dr. Kavita Rao', role: 'Principal', bio: '22 years in academic leadership, PhD in Education.' },
  { name: 'Mr. Arjun Sethi', role: 'Vice Principal', bio: 'Specialist in curriculum design and student wellbeing.' },
  { name: 'Ms. Neha Kapoor', role: 'Academic Coordinator', bio: 'Leads CBSE curriculum planning across all grades.' },
];

export function AboutPage() {
  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            About Us
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1, maxWidth: 640 }}>
            27 years of nurturing curious, confident learners.
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 640, mt: 2 }}>
            Greenwood International School has grown from a two-classroom beginning into one of
            the region&apos;s most respected CBSE-affiliated institutions.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Grid container spacing={3}>
          {VALUES.map((v) => (
            <Grid item xs={12} md={4} key={v.title}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Box
                    sx={{
                      width: 52,
                      height: 52,
                      borderRadius: 2.5,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      bgcolor: (theme) => alpha(theme.palette.primary.main, 0.1),
                      color: 'primary.main',
                      mb: 2,
                    }}
                  >
                    {v.icon}
                  </Box>
                  <Typography variant="h6" fontWeight={700} gutterBottom>
                    {v.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {v.text}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      <Container maxWidth="lg" sx={{ pb: { xs: 6, md: 8 } }}>
        <Box sx={{ textAlign: 'center', mb: 5 }}>
          <HistoryEduOutlinedIcon color="primary" sx={{ fontSize: 40, mb: 1 }} />
          <Typography variant="h4" fontWeight={800}>
            Our Journey
          </Typography>
        </Box>
        <Grid container spacing={0}>
          {MILESTONES.map((m, idx) => (
            <Grid item xs={12} key={m.year}>
              <Box sx={{ display: 'flex', gap: 3 }}>
                <Box sx={{ width: 80, flexShrink: 0, textAlign: 'right' }}>
                  <Typography variant="h6" fontWeight={800} color="primary.main">
                    {m.year}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: 'secondary.main', mt: 0.5 }} />
                  {idx < MILESTONES.length - 1 && <Box sx={{ width: 2, flexGrow: 1, bgcolor: 'divider', my: 0.5 }} />}
                </Box>
                <Box sx={{ pb: 4 }}>
                  <Typography variant="body1" color="text.secondary">
                    {m.text}
                  </Typography>
                </Box>
              </Box>
            </Grid>
          ))}
        </Grid>
      </Container>

      <Divider />

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Box sx={{ textAlign: 'center', mb: 5 }}>
          <Typography variant="h4" fontWeight={800}>
            School Leadership
          </Typography>
        </Box>
        <Grid container spacing={3}>
          {LEADERSHIP.map((person) => (
            <Grid item xs={12} sm={6} md={4} key={person.name}>
              <Card sx={{ height: '100%', textAlign: 'center', p: 2 }}>
                <CardContent>
                  <Avatar sx={{ width: 72, height: 72, mx: 'auto', mb: 2, bgcolor: 'primary.main', fontSize: 26 }}>
                    {person.name.split(' ').map((w) => w[0]).slice(0, 2).join('')}
                  </Avatar>
                  <Typography variant="subtitle1" fontWeight={700}>
                    {person.name}
                  </Typography>
                  <Typography variant="body2" color="primary.main" fontWeight={600} gutterBottom>
                    {person.role}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {person.bio}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
}

export default AboutPage;
