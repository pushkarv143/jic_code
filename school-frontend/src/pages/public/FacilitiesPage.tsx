import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import { alpha } from '@mui/material/styles';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import ComputerOutlinedIcon from '@mui/icons-material/ComputerOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import SportsSoccerOutlinedIcon from '@mui/icons-material/SportsSoccerOutlined';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import LocalHospitalOutlinedIcon from '@mui/icons-material/LocalHospitalOutlined';
import RestaurantOutlinedIcon from '@mui/icons-material/RestaurantOutlined';
import ApartmentOutlinedIcon from '@mui/icons-material/ApartmentOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';

const FACILITIES = [
  {
    icon: <ScienceOutlinedIcon />,
    title: 'Science Laboratories',
    points: ['Separate Physics, Chemistry & Biology labs', 'Modern instrumentation and safety equipment', 'Dedicated lab assistants'],
  },
  {
    icon: <ComputerOutlinedIcon />,
    title: 'Computer & Robotics Lab',
    points: ['1:1 systems ratio in senior classes', 'Robotics and coding electives', 'High-speed campus network'],
  },
  {
    icon: <MenuBookOutlinedIcon />,
    title: 'Library & Resource Centre',
    points: ['15,000+ titles across genres', 'Digital catalog and e-books', 'Quiet reading zones'],
  },
  {
    icon: <SportsSoccerOutlinedIcon />,
    title: 'Sports Complex',
    points: ['Football & cricket grounds', 'Indoor badminton & basketball courts', 'Swimming pool with certified coaches'],
  },
  {
    icon: <DirectionsBusOutlinedIcon />,
    title: 'Transport',
    points: ['GPS-enabled bus fleet', 'Trained drivers and attendants', 'Coverage across the city'],
  },
  {
    icon: <LocalHospitalOutlinedIcon />,
    title: 'Health & Wellness',
    points: ['On-campus medical room', 'Qualified nurse on duty', 'Regular health check-up camps'],
  },
  {
    icon: <RestaurantOutlinedIcon />,
    title: 'Cafeteria',
    points: ['Hygienic, nutritious meals', 'Allergy-aware menu options', 'Fresh produce sourced daily'],
  },
  {
    icon: <ApartmentOutlinedIcon />,
    title: 'Hostel Facilities',
    points: ['Separate boys & girls hostels', 'Warden-supervised residential care', '24x7 security and CCTV monitoring'],
  },
];

export function FacilitiesPage() {
  return (
    <Box>
      <Box sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main" fontWeight={700}>
            Campus
          </Typography>
          <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>
            World-class facilities for well-rounded growth
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 560, mt: 2 }}>
            From science labs to sports grounds, every facility is designed with student safety
            and learning in mind.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Grid container spacing={3}>
          {FACILITIES.map((facility) => (
            <Grid item xs={12} sm={6} md={4} key={facility.title}>
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
                    {facility.icon}
                  </Box>
                  <Typography variant="h6" fontWeight={700} gutterBottom>
                    {facility.title}
                  </Typography>
                  <List dense disablePadding>
                    {facility.points.map((point) => (
                      <ListItem key={point} disableGutters sx={{ py: 0.25 }}>
                        <ListItemIcon sx={{ minWidth: 28 }}>
                          <CheckCircleOutlineIcon fontSize="small" color="success" />
                        </ListItemIcon>
                        <ListItemText primary={point} primaryTypographyProps={{ variant: 'body2' }} />
                      </ListItem>
                    ))}
                  </List>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
}

export default FacilitiesPage;
