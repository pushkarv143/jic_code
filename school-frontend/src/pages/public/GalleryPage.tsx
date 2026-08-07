import { useState } from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Chip from '@mui/material/Chip';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import SportsSoccerOutlinedIcon from '@mui/icons-material/SportsSoccerOutlined';
import TheaterComedyOutlinedIcon from '@mui/icons-material/TheaterComedyOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import CelebrationOutlinedIcon from '@mui/icons-material/CelebrationOutlined';
import ComputerOutlinedIcon from '@mui/icons-material/ComputerOutlined';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';

const GRADIENTS = [
  'linear-gradient(135deg,#2b3a8f,#5563b8)',
  'linear-gradient(135deg,#eb6834,#ffb703)',
  'linear-gradient(135deg,#1baf7a,#57d9a8)',
  'linear-gradient(135deg,#4a3aa7,#8a7fe0)',
  'linear-gradient(135deg,#e34948,#f08a89)',
  'linear-gradient(135deg,#0288d1,#5fc1ff)',
];

const PHOTOS = [
  { icon: <ScienceOutlinedIcon />, title: 'Science Fair 2026', category: 'Academics' },
  { icon: <SportsSoccerOutlinedIcon />, title: 'Annual Sports Meet', category: 'Sports' },
  { icon: <TheaterComedyOutlinedIcon />, title: 'Drama Club Showcase', category: 'Arts' },
  { icon: <MenuBookOutlinedIcon />, title: 'Library Reading Week', category: 'Academics' },
  { icon: <CelebrationOutlinedIcon />, title: 'Founder’s Day Celebration', category: 'Events' },
  { icon: <ComputerOutlinedIcon />, title: 'Robotics Workshop', category: 'Academics' },
  { icon: <PaletteOutlinedIcon />, title: 'Inter-House Art Competition', category: 'Arts' },
  { icon: <GroupsOutlinedIcon />, title: 'Graduation Ceremony', category: 'Events' },
  { icon: <SportsSoccerOutlinedIcon />, title: 'Inter-School Football Cup', category: 'Sports' },
  { icon: <ScienceOutlinedIcon />, title: 'Chemistry Lab Session', category: 'Academics' },
  { icon: <CelebrationOutlinedIcon />, title: 'Republic Day Function', category: 'Events' },
  { icon: <TheaterComedyOutlinedIcon />, title: 'Annual Day Performance', category: 'Arts' },
];

const CATEGORIES = ['All', 'Academics', 'Sports', 'Arts', 'Events'];

/** Responsive photo grid using gradient + icon placeholders (no external image hotlinking). */
export function GalleryPage() {
  const [tab, setTab] = useState('All');
  const filtered = tab === 'All' ? PHOTOS : PHOTOS.filter((p) => p.category === tab);

  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            Campus Life
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            Photo Gallery
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 560, mt: 2 }}>
            A glimpse into everyday life, celebrations and achievements at Greenwood.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Tabs
          value={tab}
          onChange={(_e, value) => setTab(value)}
          sx={{ mb: 4 }}
          variant="scrollable"
          scrollButtons="auto"
        >
          {CATEGORIES.map((cat) => (
            <Tab key={cat} value={cat} label={cat} />
          ))}
        </Tabs>

        <Grid container spacing={2.5}>
          {filtered.map((photo, idx) => (
            <Grid item xs={12} sm={6} md={4} key={photo.title}>
              <Box
                sx={{
                  borderRadius: 3,
                  overflow: 'hidden',
                  position: 'relative',
                  height: 220,
                  background: GRADIENTS[idx % GRADIENTS.length],
                  display: 'flex',
                  alignItems: 'flex-end',
                  color: '#fff',
                }}
              >
                <Box
                  sx={{
                    position: 'absolute',
                    top: 12,
                    right: 12,
                    fontSize: 40,
                    opacity: 0.35,
                    '& svg': { fontSize: 40 },
                  }}
                >
                  {photo.icon}
                </Box>
                <Box sx={{ p: 2, width: '100%', background: 'linear-gradient(to top, rgba(0,0,0,0.55), transparent)' }}>
                  <Chip
                    label={photo.category}
                    size="small"
                    sx={{ mb: 1, bgcolor: 'rgba(255,255,255,0.2)', color: '#fff', fontWeight: 700 }}
                  />
                  <Typography variant="subtitle1" fontWeight={700}>
                    {photo.title}
                  </Typography>
                </Box>
              </Box>
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
}

export default GalleryPage;
