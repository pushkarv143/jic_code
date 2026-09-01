import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Breadcrumbs from '@mui/material/Breadcrumbs';
import Link from '@mui/material/Link';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import { Link as RouterLink } from 'react-router-dom';
import type { ReactNode } from 'react';

export interface BreadcrumbItem {
  label: string;
  to?: string;
}

export interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumbs?: BreadcrumbItem[];
  action?: ReactNode;
}

export function PageHeader({ title, subtitle, breadcrumbs, action }: PageHeaderProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: { xs: 'column', sm: 'row' },
        alignItems: { xs: 'flex-start', sm: 'center' },
        justifyContent: 'space-between',
        gap: 2,
        mb: 3.5,
      }}
    >
      <Box>
        {breadcrumbs && breadcrumbs.length > 0 && (
          <Breadcrumbs
            separator={<NavigateNextIcon sx={{ fontSize: 14, opacity: 0.5 }} />}
            sx={{ mb: 0.75, '& .MuiBreadcrumbs-ol': { flexWrap: 'nowrap' } }}
          >
            {breadcrumbs.map((crumb, idx) =>
              crumb.to && idx !== breadcrumbs.length - 1 ? (
                <Link
                  key={crumb.label}
                  component={RouterLink}
                  to={crumb.to}
                  underline="hover"
                  color="text.secondary"
                  sx={{ fontSize: '0.78rem', fontWeight: 500, '&:hover': { color: 'primary.main' } }}
                >
                  {crumb.label}
                </Link>
              ) : (
                <Typography key={crumb.label} color="text.primary" sx={{ fontSize: '0.78rem', fontWeight: 600 }}>
                  {crumb.label}
                </Typography>
              ),
            )}
          </Breadcrumbs>
        )}
        <Typography variant="h4" component="h1" sx={{ fontWeight: 800, letterSpacing: '-0.02em' }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, fontWeight: 500 }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {action && (
        <Box sx={{ flexShrink: 0, display: 'flex', gap: 1 }}>
          {action}
        </Box>
      )}
    </Box>
  );
}

export default PageHeader;
