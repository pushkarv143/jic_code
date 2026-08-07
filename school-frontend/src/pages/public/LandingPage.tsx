import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Avatar from '@mui/material/Avatar';
import Stack from '@mui/material/Stack';
import Chip from '@mui/material/Chip';
import Rating from '@mui/material/Rating';
import { alpha } from '@mui/material/styles';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import EmojiEventsOutlinedIcon from '@mui/icons-material/EmojiEventsOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import SportsSoccerOutlinedIcon from '@mui/icons-material/SportsSoccerOutlined';
import ComputerOutlinedIcon from '@mui/icons-material/ComputerOutlined';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import FormatQuoteIcon from '@mui/icons-material/FormatQuote';

const STATS = [
  { label: 'Years of Excellence', value: '27+' },
  { label: 'Students Enrolled', value: '2,500+' },
  { label: 'Expert Faculty', value: '150+' },
  { label: 'Board Exam Pass Rate', value: '99.2%' },
];

const FEATURES = [
  {
    icon: <ScienceOutlinedIcon />,
    title: 'Modern Science Labs',
    description: 'Fully-equipped physics, chemistry and biology labs for hands-on learning.',
  },
  {
    icon: <ComputerOutlinedIcon />,
    title: 'Digital Classrooms',
    description: 'Smart boards and computer labs integrated into everyday teaching.',
  },
  {
    icon: <MenuBookOutlinedIcon />,
    title: 'Extensive Library',
    description: 'Over 15,000 titles across fiction, reference and competitive exam prep.',
  },
  {
    icon: <SportsSoccerOutlinedIcon />,
    title: 'Sports & Fitness',
    description: 'Dedicated grounds for athletics, football, basketball and swimming.',
  },
  {
    icon: <PaletteOutlinedIcon />,
    title: 'Arts & Culture',
    description: 'Music, dance and visual arts programs to nurture creative expression.',
  },
  {
    icon: <EmojiEventsOutlinedIcon />,
    title: 'Award-Winning Faculty',
    description: 'Experienced educators recognized at state and national levels.',
  },
];

const TESTIMONIALS = [
  {
    name: 'Anita Deshmukh',
    role: 'Parent, Class 6',
    quote:
      'Greenwood has given my daughter the confidence and curiosity to explore everything from robotics to theatre. The teachers truly care.',
  },
  {
    name: 'Rohan Mehta',
    role: 'Alumnus, Batch of 2019',
    quote:
      'The foundation I built at Greenwood — academically and personally — is something I carry with me through engineering college.',
  },
  {
    name: 'Priya Nair',
    role: 'Parent, Class 10',
    quote:
      'Transparent communication, a caring administration, and a genuine focus on every child. We couldn’t have asked for more.',
  },
];

/** Marketing homepage for the fictional Greenwood International School. */
export function LandingPage() {
  return (
    <Box>
      {/* Hero */}
      <Box
        sx={{
          background: (theme) =>
            `linear-gradient(150deg, ${theme.palette.mode === 'light' ? '#1c2763' : '#0b0f1c'} 0%, ${theme.palette.primary.main} 65%, #3a4bb0 100%)`,
          color: '#fff',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <Box
          sx={{
            position: 'absolute',
            width: 520,
            height: 520,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(255,183,3,0.2) 0%, rgba(255,183,3,0) 70%)',
            top: -160,
            right: -160,
          }}
        />
        <Container maxWidth="lg" sx={{ py: { xs: 8, md: 12 }, position: 'relative' }}>
          <Grid container spacing={6} alignItems="center">
            <Grid item xs={12} md={7}>
              <Chip
                label="Admissions open for 2026-2027"
                sx={{ bgcolor: 'rgba(255,183,3,0.16)', color: 'secondary.main', fontWeight: 700, mb: 2 }}
              />
              <Typography variant="h2" sx={{ fontWeight: 800, mb: 2, fontSize: { xs: '2.25rem', md: '3.25rem' } }}>
                Where curious minds become confident leaders.
              </Typography>
              <Typography variant="h6" sx={{ opacity: 0.85, fontWeight: 400, mb: 4, maxWidth: 560 }}>
                Greenwood International School blends rigorous academics, modern facilities and a
                nurturing community to help every student thrive.
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <Button
                  component={RouterLink}
                  to="/admission"
                  variant="contained"
                  color="secondary"
                  size="large"
                  endIcon={<ArrowForwardIcon />}
                >
                  Apply for Admission
                </Button>
                <Button
                  component={RouterLink}
                  to="/academics"
                  variant="outlined"
                  size="large"
                  sx={{ color: '#fff', borderColor: 'rgba(255,255,255,0.5)', '&:hover': { borderColor: '#fff' } }}
                >
                  Explore Academics
                </Button>
              </Stack>
            </Grid>
            <Grid item xs={12} md={5}>
              <Box
                sx={{
                  borderRadius: 4,
                  border: '1px solid rgba(255,255,255,0.18)',
                  bgcolor: 'rgba(255,255,255,0.06)',
                  p: 4,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 2,
                }}
              >
                <SchoolOutlinedIcon sx={{ fontSize: 96, color: 'secondary.main' }} />
                <Typography variant="h6" fontWeight={700} textAlign="center">
                  CBSE Affiliated · Est. 1998
                </Typography>
                <Typography variant="body2" textAlign="center" sx={{ opacity: 0.8 }}>
                  Recognized for academic excellence and holistic student development across the
                  region.
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* Stats strip */}
      <Container maxWidth="lg" sx={{ mt: { xs: -4, md: -5 }, position: 'relative', zIndex: 1 }}>
        <Card sx={{ py: { xs: 2, md: 3 } }}>
          <Grid container>
            {STATS.map((stat, idx) => (
              <Grid
                item
                xs={6}
                sm={3}
                key={stat.label}
                sx={{
                  textAlign: 'center',
                  borderRight: {
                    sm: idx < STATS.length - 1 ? '1px solid' : 'none',
                  },
                  borderColor: 'divider',
                  py: { xs: 1.5, sm: 0 },
                }}
              >
                <Typography variant="h4" fontWeight={800} color="primary.main">
                  {stat.value}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {stat.label}
                </Typography>
              </Grid>
            ))}
          </Grid>
        </Card>
      </Container>

      {/* Features */}
      <Container maxWidth="lg" sx={{ py: { xs: 8, md: 10 } }}>
        <Box sx={{ textAlign: 'center', mb: 6 }}>
          <Typography variant="overline" color="primary.main" fontWeight={700}>
            Why Greenwood
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            Facilities built for growth
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 560, mx: 'auto', mt: 1.5 }}>
            Every corner of our campus is designed to spark curiosity and support every kind of
            learner.
          </Typography>
        </Box>
        <Grid container spacing={3}>
          {FEATURES.map((feature) => (
            <Grid item xs={12} sm={6} md={4} key={feature.title}>
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
                    {feature.icon}
                  </Box>
                  <Typography variant="h6" fontWeight={700} gutterBottom>
                    {feature.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {feature.description}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* Testimonials */}
      <Box sx={{ bgcolor: 'action.hover', py: { xs: 8, md: 10 } }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: 'center', mb: 6 }}>
            <Typography variant="overline" color="primary.main" fontWeight={700}>
              Testimonials
            </Typography>
            <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
              What our community says
            </Typography>
          </Box>
          <Grid container spacing={3}>
            {TESTIMONIALS.map((t) => (
              <Grid item xs={12} md={4} key={t.name}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <FormatQuoteIcon sx={{ color: 'primary.main', opacity: 0.3, fontSize: 40 }} />
                    <Typography variant="body1" sx={{ mb: 3, minHeight: 96 }}>
                      {t.quote}
                    </Typography>
                    <Stack direction="row" spacing={1.5} alignItems="center">
                      <Avatar sx={{ bgcolor: 'primary.main' }}>{t.name.charAt(0)}</Avatar>
                      <Box>
                        <Typography variant="subtitle2" fontWeight={700}>
                          {t.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {t.role}
                        </Typography>
                      </Box>
                    </Stack>
                    <Rating value={5} readOnly size="small" sx={{ mt: 1.5 }} />
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* CTA */}
      <Container maxWidth="lg" sx={{ py: { xs: 8, md: 10 } }}>
        <Card
          sx={{
            p: { xs: 4, md: 6 },
            textAlign: 'center',
            background: (theme) =>
              `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
            color: '#fff',
          }}
        >
          <GroupsOutlinedIcon sx={{ fontSize: 48, color: 'secondary.main', mb: 2 }} />
          <Typography variant="h4" fontWeight={800} gutterBottom>
            Ready to join the Greenwood family?
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.85, maxWidth: 520, mx: 'auto', mb: 3 }}>
            Seats for the 2026-2027 academic year are filling quickly. Start your child&apos;s
            journey with us today.
          </Typography>
          <Button component={RouterLink} to="/admission" variant="contained" color="secondary" size="large">
            Start Your Application
          </Button>
        </Card>
      </Container>
    </Box>
  );
}

export default LandingPage;
