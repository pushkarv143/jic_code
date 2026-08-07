import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import { useState } from 'react';
import { alpha } from '@mui/material/styles';
import ChildCareOutlinedIcon from '@mui/icons-material/ChildCareOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined';

const STAGES = [
  {
    label: 'Early Years',
    range: 'Nursery - UKG',
    icon: <ChildCareOutlinedIcon />,
    description: 'Play-based learning focused on motor skills, language and social development.',
    subjects: ['Phonics & Language', 'Numeracy Foundations', 'Art & Craft', 'Music & Movement'],
  },
  {
    label: 'Primary',
    range: 'Class 1 - 5',
    icon: <MenuBookOutlinedIcon />,
    description: 'Building strong fundamentals in literacy, numeracy and scientific curiosity.',
    subjects: ['English', 'Mathematics', 'Environmental Science', 'Second Language', 'Computer Basics'],
  },
  {
    label: 'Middle School',
    range: 'Class 6 - 8',
    icon: <CalculateOutlinedIcon />,
    description: 'Deeper subject exploration with project-based and experiential learning.',
    subjects: ['English', 'Mathematics', 'Science', 'Social Science', 'Second Language', 'Computer Science'],
  },
  {
    label: 'Senior Secondary',
    range: 'Class 9 - 12',
    icon: <ScienceOutlinedIcon />,
    description: 'Stream-based specialization preparing students for board exams and beyond.',
    subjects: ['Science (PCM/PCB)', 'Commerce', 'Humanities', 'Computer Science', 'Physical Education'],
  },
];

export function AcademicsPage() {
  const [tab, setTab] = useState(0);
  const stage = STAGES[tab];

  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            Academics
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            A CBSE curriculum built for every stage of learning
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 560, mt: 2 }}>
            From early years to senior secondary, our curriculum balances academic rigor with
            creativity and real-world skills.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Tabs
          value={tab}
          onChange={(_e, value) => setTab(value)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ mb: 4 }}
        >
          {STAGES.map((s) => (
            <Tab key={s.label} label={s.label} icon={s.icon} iconPosition="start" />
          ))}
        </Tabs>

        <Card>
          <CardContent sx={{ p: { xs: 3, md: 4 } }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="h5" fontWeight={800}>
                {stage.label}
              </Typography>
              <Chip label={stage.range} color="primary" size="small" />
            </Stack>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3, maxWidth: 640 }}>
              {stage.description}
            </Typography>
            <Typography variant="subtitle2" fontWeight={700} gutterBottom>
              Core Subjects
            </Typography>
            <Grid container spacing={1.5}>
              {stage.subjects.map((subject) => (
                <Grid item key={subject}>
                  <Chip
                    label={subject}
                    variant="outlined"
                    sx={{
                      borderColor: (theme) => alpha(theme.palette.primary.main, 0.4),
                      fontWeight: 600,
                    }}
                  />
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>

        <Grid container spacing={3} sx={{ mt: 0.5 }}>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" fontWeight={700} gutterBottom>
                  Assessment Pattern
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Continuous internal assessments, periodic tests, and term examinations aligned
                  with CBSE guidelines, complemented by project work and practical evaluations.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" fontWeight={700} gutterBottom>
                  Co-Curricular Integration
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Every stream includes dedicated periods for sports, arts, and life-skills
                  education to nurture well-rounded individuals.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" fontWeight={700} gutterBottom>
                  Career Guidance
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Dedicated counsellors support students in Classes 9-12 with stream selection,
                  competitive exam prep and career planning.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}

export default AcademicsPage;
