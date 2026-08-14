import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

export interface AppFooterProps {
  /**
   * 'contained' sits at the bottom of a scrolling page and carries a top border —
   * used inside the dashboard shell. 'bare' drops the border and background for
   * places that already sit on a coloured panel, such as the auth and marketing
   * layouts.
   */
  variant?: 'contained' | 'bare';
}

/** The attribution line itself, kept in one place so every surface agrees. */
export const COPYRIGHT_TEXT = 'All rights reserved by Pushkar Verma';

/**
 * Site-wide attribution footer.
 *
 * Rendered by every layout — dashboard, auth and public — rather than by
 * individual pages, so it appears on every route including error and 403 screens
 * without each new page having to remember it.
 */
export function AppFooter({ variant = 'contained' }: AppFooterProps) {
  const year = new Date().getFullYear();

  return (
    <Box
      component="footer"
      sx={{
        // Deliberately slim: this strip is on every page, so any height it takes
        // is taken from the content area on all of them.
        py: 0.5,
        px: 1.5,
        textAlign: 'center',
        ...(variant === 'contained' && {
          mt: 'auto',
          borderTop: '1px solid',
          borderColor: 'divider',
        }),
      }}
    >
      <Typography
        component="span"
        color={variant === 'bare' ? 'inherit' : 'text.secondary'}
        sx={{ fontSize: '0.68rem', lineHeight: 1.6, opacity: 0.7 }}
      >
        © {year} {COPYRIGHT_TEXT}
      </Typography>
    </Box>
  );
}

export default AppFooter;
