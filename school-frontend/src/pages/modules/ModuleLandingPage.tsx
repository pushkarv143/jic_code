import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardActionArea from '@mui/material/CardActionArea';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import { alpha, useTheme } from '@mui/material/styles';
import PageHeader, { type BreadcrumbItem } from '@/components/common/PageHeader';

export interface ModuleSection {
  icon: ReactNode;
  title: string;
  description: string;
  badge?: string;
}

export interface ModuleLandingPageProps {
  title: string;
  subtitle: string;
  breadcrumbs: BreadcrumbItem[];
  sections: ModuleSection[];
  action?: ReactNode;
}

/**
 * Shared "module landing" page: header + breadcrumb + a card grid of sub-sections.
 * Every /app/* business module (Students, Teachers, Fees, ...) renders one of
 * these in round 1; later rounds replace individual cards with real CRUD screens.
 */
export function ModuleLandingPage({
  title,
  subtitle,
  breadcrumbs,
  sections,
  action,
}: ModuleLandingPageProps) {
  const theme = useTheme();

  return (
    <Box>
      <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs} action={action} />
      <Grid container spacing={2.5}>
        {sections.map((section) => (
          <Grid item xs={12} sm={6} lg={4} key={section.title}>
            <Card sx={{ height: '100%' }}>
              <CardActionArea sx={{ height: '100%', p: 0.5 }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
                    <Box
                      sx={{
                        width: 48,
                        height: 48,
                        borderRadius: 2.5,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'light' ? 0.1 : 0.18),
                        color: 'primary.main',
                        mb: 1.5,
                      }}
                    >
                      {section.icon}
                    </Box>
                    {section.badge && (
                      <Chip label={section.badge} size="small" color="secondary" variant="outlined" />
                    )}
                  </Box>
                  <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                    {section.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {section.description}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

export default ModuleLandingPage;
