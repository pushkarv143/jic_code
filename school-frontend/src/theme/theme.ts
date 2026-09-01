import { createTheme, type PaletteMode, type ThemeOptions, alpha } from '@mui/material/styles';
import type {} from '@mui/x-data-grid/themeAugmentation';

declare module '@mui/material/styles' {
  interface Palette {
    sidebar: { background: string; color: string };
  }
  interface PaletteOptions {
    sidebar?: { background: string; color: string };
  }
}

const PRIMARY = {
  main: '#2b3a8f',
  light: '#5563b8',
  dark: '#1c2763',
  contrastText: '#ffffff',
};

const SECONDARY = {
  main: '#ffb703',
  light: '#ffcb47',
  dark: '#c88900',
  contrastText: '#1c2763',
};

export type ThemeDensity = 'comfortable' | 'compact';

export const ACCENT_COLOR_SWATCHES: Array<{ label: string; value: string }> = [
  { label: 'Indigo', value: '#2b3a8f' },
  { label: 'Teal', value: '#00897b' },
  { label: 'Purple', value: '#6a1b9a' },
  { label: 'Rose', value: '#c2185b' },
  { label: 'Orange', value: '#ef6c00' },
  { label: 'Green', value: '#2e7d32' },
  { label: 'Blue', value: '#1565c0' },
  { label: 'Slate', value: '#455a64' },
];

export interface ThemeCustomization {
  accentColor?: string;
  density?: ThemeDensity;
}

function getDesignTokens(mode: PaletteMode, accentColor?: string): ThemeOptions {
  const isLight = mode === 'light';
  return {
    palette: {
      mode,
      primary: accentColor ? { main: accentColor } : PRIMARY,
      secondary: SECONDARY,
      success: { main: '#2e7d32' },
      warning: { main: '#ed6c02' },
      error: { main: '#d32f2f' },
      info: { main: '#0288d1' },
      background: {
        default: isLight ? '#f0f2f8' : '#0d1117',
        paper: isLight ? '#ffffff' : '#161d2e',
      },
      text: {
        primary: isLight ? '#1a1f36' : '#e6e9f2',
        secondary: isLight ? '#5a6072' : '#9aa2b8',
      },
      divider: isLight ? 'rgba(26,31,54,0.08)' : 'rgba(230,233,242,0.08)',
      sidebar: {
        background: isLight ? '#1c2763' : '#0b0f1c',
        color: '#e6e9f2',
      },
    },
    shape: { borderRadius: 12 },
    typography: {
      fontFamily: [
        'Inter',
        'Roboto',
        '"Segoe UI"',
        'Helvetica',
        'Arial',
        'sans-serif',
      ].join(','),
      h1: { fontWeight: 800, fontSize: '2.75rem', letterSpacing: '-0.03em' },
      h2: { fontWeight: 800, fontSize: '2.25rem', letterSpacing: '-0.025em' },
      h3: { fontWeight: 700, fontSize: '1.875rem', letterSpacing: '-0.02em' },
      h4: { fontWeight: 700, fontSize: '1.5rem', letterSpacing: '-0.01em' },
      h5: { fontWeight: 700, fontSize: '1.25rem' },
      h6: { fontWeight: 600, fontSize: '1.0625rem' },
      subtitle1: { fontWeight: 600 },
      subtitle2: { fontWeight: 600 },
      button: { fontWeight: 700, textTransform: 'none', letterSpacing: '0.01em' },
    },
  };
}

export function getTheme(mode: PaletteMode, customization: ThemeCustomization = {}) {
  const { accentColor, density = 'comfortable' } = customization;
  const compact = density === 'compact';
  const tokens = getDesignTokens(mode, accentColor);
  const isLight = mode === 'light';
  const primaryMain = accentColor ?? PRIMARY.main;

  return createTheme({
    ...tokens,
    spacing: compact ? 6 : 8,
    components: {
      /* ---- Base ---- */
      MuiCssBaseline: {
        styleOverrides: {
          body: { scrollbarGutter: 'stable' },
        },
      },

      /* ---- Buttons ---- */
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: {
            borderRadius: 10,
            paddingTop: compact ? 6 : 9,
            paddingBottom: compact ? 6 : 9,
            paddingLeft: 20,
            paddingRight: 20,
            transition: 'all 0.18s cubic-bezier(0.4,0,0.2,1)',
          },
          containedPrimary: {
            background: `linear-gradient(135deg, ${primaryMain} 0%, ${isLight ? '#3a4bb0' : '#2b3a8f'} 100%)`,
            boxShadow: `0 4px 14px ${alpha(primaryMain, 0.3)}`,
            '&:hover': {
              boxShadow: `0 6px 20px ${alpha(primaryMain, 0.42)}`,
              background: `linear-gradient(135deg, ${primaryMain} 20%, ${isLight ? '#4a5bc0' : '#3a4bb0'} 100%)`,
            },
          },
          sizeLarge: {
            paddingTop: 12,
            paddingBottom: 12,
            fontSize: '1rem',
          },
          outlinedPrimary: {
            borderColor: alpha(primaryMain, 0.5),
            '&:hover': {
              borderColor: primaryMain,
              backgroundColor: alpha(primaryMain, 0.06),
            },
          },
        },
      },
      MuiIconButton: {
        styleOverrides: {
          root: {
            borderRadius: 10,
            transition: 'background-color 0.15s ease, transform 0.15s ease',
            '&:hover': { transform: 'scale(1.05)' },
          },
        },
      },

      /* ---- Cards & Paper ---- */
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 16,
            border: isLight
              ? '1px solid rgba(26,31,54,0.06)'
              : '1px solid rgba(230,233,242,0.07)',
            boxShadow: isLight
              ? '0 1px 3px rgba(23,29,58,0.04), 0 4px 16px rgba(23,29,58,0.07)'
              : '0 1px 3px rgba(0,0,0,0.2), 0 4px 16px rgba(0,0,0,0.35)',
            backgroundImage: 'none',
            transition: 'box-shadow 0.2s ease, transform 0.2s ease',
            '&:hover': isLight
              ? { boxShadow: '0 2px 8px rgba(23,29,58,0.06), 0 8px 24px rgba(23,29,58,0.1)' }
              : { boxShadow: '0 2px 8px rgba(0,0,0,0.28), 0 8px 24px rgba(0,0,0,0.45)' },
          },
        },
      },
      MuiCardContent: {
        styleOverrides: {
          root: compact ? { padding: 14, '&:last-child': { paddingBottom: 14 } } : { padding: 20, '&:last-child': { paddingBottom: 20 } },
        },
      },
      MuiCardHeader: {
        styleOverrides: {
          root: { paddingBottom: 0 },
          title: { fontSize: '1.05rem', fontWeight: 700 },
          subheader: { fontSize: '0.8rem', marginTop: 2 },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: { backgroundImage: 'none' },
          rounded: { borderRadius: 16 },
          elevation1: {
            boxShadow: isLight
              ? '0 1px 3px rgba(23,29,58,0.04), 0 4px 12px rgba(23,29,58,0.07)'
              : '0 1px 3px rgba(0,0,0,0.2), 0 4px 12px rgba(0,0,0,0.35)',
          },
          elevation2: {
            boxShadow: isLight
              ? '0 2px 6px rgba(23,29,58,0.06), 0 8px 24px rgba(23,29,58,0.1)'
              : '0 2px 6px rgba(0,0,0,0.25), 0 8px 24px rgba(0,0,0,0.4)',
          },
          elevation3: {
            boxShadow: isLight
              ? '0 4px 12px rgba(23,29,58,0.08), 0 16px 40px rgba(23,29,58,0.12)'
              : '0 4px 12px rgba(0,0,0,0.3), 0 16px 40px rgba(0,0,0,0.5)',
          },
        },
      },

      /* ---- App Bar ---- */
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: isLight ? 'rgba(255,255,255,0.88)' : 'rgba(22,29,46,0.88)',
            backdropFilter: 'blur(12px)',
            WebkitBackdropFilter: 'blur(12px)',
            color: isLight ? '#1a1f36' : '#e6e9f2',
            boxShadow: isLight
              ? '0 1px 0 rgba(23,29,58,0.07), 0 2px 8px rgba(23,29,58,0.04)'
              : '0 1px 0 rgba(230,233,242,0.07), 0 2px 8px rgba(0,0,0,0.25)',
          },
        },
      },

      /* ---- Drawer / Sidebar ---- */
      MuiDrawer: {
        styleOverrides: { paper: { border: 'none' } },
      },

      /* ---- Dialogs ---- */
      MuiDialog: {
        styleOverrides: {
          paper: ({ theme: t }) => ({
            borderRadius: 20,
            [t.breakpoints.down('sm')]: {
              margin: 0,
              width: '100%',
              maxWidth: '100%',
              height: '100%',
              maxHeight: '100%',
              borderRadius: 0,
            },
          }),
        },
      },
      MuiDialogTitle: {
        styleOverrides: {
          root: { fontWeight: 700, fontSize: '1.125rem', paddingBottom: 8 },
        },
      },
      MuiDialogContent: {
        styleOverrides: { root: { paddingTop: '8px !important' } },
      },

      /* ---- Input fields ---- */
      MuiTextField: {
        defaultProps: { variant: 'outlined' },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: 10,
            transition: 'box-shadow 0.2s ease',
            '&:hover .MuiOutlinedInput-notchedOutline': {
              borderColor: alpha(primaryMain, 0.5),
            },
            '&.Mui-focused': {
              boxShadow: `0 0 0 3px ${alpha(primaryMain, 0.12)}`,
            },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
              borderColor: primaryMain,
              borderWidth: '1.5px',
            },
          },
        },
      },
      MuiInputLabel: {
        styleOverrides: {
          root: { fontWeight: 500 },
        },
      },

      /* ---- Select ---- */
      MuiSelect: {
        styleOverrides: {
          root: { borderRadius: 10 },
        },
      },

      /* ---- Chips ---- */
      MuiChip: {
        styleOverrides: {
          root: {
            fontWeight: 600,
            borderRadius: 8,
            fontSize: '0.78rem',
          },
          sizeSmall: {
            height: 22,
            fontSize: '0.72rem',
          },
        },
      },

      /* ---- Tables ---- */
      MuiTableCell: {
        styleOverrides: {
          root: compact ? { paddingTop: 6, paddingBottom: 6 } : undefined,
          head: {
            fontWeight: 700,
            backgroundColor: isLight ? '#f0f2f8' : '#111726',
            fontSize: '0.78rem',
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            color: isLight ? '#5a6072' : '#9aa2b8',
          },
        },
      },
      MuiTableRow: {
        styleOverrides: {
          root: {
            transition: 'background-color 0.12s ease',
            '&:hover': {
              backgroundColor: isLight ? 'rgba(43,58,143,0.035)' : 'rgba(255,183,3,0.05)',
            },
          },
        },
      },

      /* ---- Data Grid ---- */
      MuiDataGrid: {
        styleOverrides: {
          root: {
            border: 'none',
            '--DataGrid-rowBorderColor': isLight
              ? 'rgba(26,31,54,0.06)'
              : 'rgba(230,233,242,0.07)',
          },
          columnHeaders: {
            backgroundColor: isLight ? '#f0f2f8' : '#111726',
            fontWeight: 700,
            fontSize: '0.78rem',
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
          },
          row: {
            transition: 'background-color 0.12s ease',
            '&:hover': {
              backgroundColor: isLight ? 'rgba(43,58,143,0.035)' : 'rgba(255,183,3,0.05)',
            },
          },
        },
      },

      /* ---- Tabs ---- */
      MuiTabs: {
        styleOverrides: {
          root: { minHeight: 44 },
          indicator: {
            height: 3,
            borderRadius: '3px 3px 0 0',
            background: `linear-gradient(90deg, ${primaryMain}, ${alpha(primaryMain, 0.6)})`,
          },
        },
      },
      MuiTab: {
        styleOverrides: {
          root: {
            minHeight: 44,
            fontWeight: 600,
            fontSize: '0.875rem',
            textTransform: 'none',
            letterSpacing: 0,
            transition: 'color 0.15s ease',
          },
        },
      },

      /* ---- Alerts ---- */
      MuiAlert: {
        styleOverrides: {
          root: {
            borderRadius: 12,
            fontWeight: 500,
            border: '1px solid',
          },
          standardSuccess: {
            borderColor: alpha('#2e7d32', 0.25),
            backgroundColor: alpha('#2e7d32', isLight ? 0.07 : 0.14),
          },
          standardError: {
            borderColor: alpha('#d32f2f', 0.25),
            backgroundColor: alpha('#d32f2f', isLight ? 0.07 : 0.14),
          },
          standardWarning: {
            borderColor: alpha('#ed6c02', 0.25),
            backgroundColor: alpha('#ed6c02', isLight ? 0.07 : 0.14),
          },
          standardInfo: {
            borderColor: alpha('#0288d1', 0.25),
            backgroundColor: alpha('#0288d1', isLight ? 0.07 : 0.14),
          },
          icon: { alignItems: 'center' },
        },
      },

      /* ---- LinearProgress ---- */
      MuiLinearProgress: {
        styleOverrides: {
          root: { borderRadius: 100, height: 6, overflow: 'hidden' },
          bar: { borderRadius: 100 },
          barColorPrimary: {
            background: `linear-gradient(90deg, ${primaryMain}, ${alpha(primaryMain, 0.7)})`,
          },
        },
      },

      /* ---- Skeleton ---- */
      MuiSkeleton: {
        styleOverrides: {
          root: { borderRadius: 8 },
          wave: {
            '&::after': {
              background: `linear-gradient(90deg, transparent, ${isLight ? 'rgba(0,0,0,0.06)' : 'rgba(255,255,255,0.08)'}, transparent)`,
            },
          },
        },
      },

      /* ---- Avatar ---- */
      MuiAvatar: {
        styleOverrides: {
          root: {
            fontWeight: 700,
            fontSize: '0.9rem',
          },
          colorDefault: {
            background: `linear-gradient(135deg, ${primaryMain}, ${alpha(primaryMain, 0.7)})`,
            color: '#fff',
          },
        },
      },

      /* ---- Badge ---- */
      MuiBadge: {
        styleOverrides: {
          badge: { fontWeight: 700, fontSize: '0.65rem' },
        },
      },

      /* ---- Menu ---- */
      MuiMenu: {
        styleOverrides: {
          paper: {
            borderRadius: 14,
            border: isLight ? '1px solid rgba(26,31,54,0.08)' : '1px solid rgba(230,233,242,0.08)',
            boxShadow: isLight
              ? '0 4px 12px rgba(23,29,58,0.08), 0 16px 40px rgba(23,29,58,0.12)'
              : '0 4px 12px rgba(0,0,0,0.3), 0 16px 40px rgba(0,0,0,0.5)',
          },
          list: { padding: '6px' },
        },
      },
      MuiMenuItem: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            fontSize: '0.875rem',
            fontWeight: 500,
            margin: '1px 0',
            padding: '7px 10px',
            '&.Mui-selected': {
              backgroundColor: alpha(primaryMain, 0.1),
              fontWeight: 700,
              '&:hover': { backgroundColor: alpha(primaryMain, 0.15) },
            },
          },
        },
      },

      /* ---- Tooltip ---- */
      MuiTooltip: {
        defaultProps: { arrow: true },
        styleOverrides: {
          tooltip: {
            fontSize: '0.75rem',
            fontWeight: 600,
            borderRadius: 8,
            padding: '5px 10px',
            backgroundColor: isLight ? '#1a1f36' : '#e6e9f2',
            color: isLight ? '#fff' : '#1a1f36',
          },
          arrow: {
            color: isLight ? '#1a1f36' : '#e6e9f2',
          },
        },
      },

      /* ---- List ---- */
      MuiListItemButton: {
        styleOverrides: {
          root: {
            borderRadius: 10,
            transition: 'background-color 0.15s ease',
          },
        },
      },

      /* ---- Snackbar ---- */
      MuiSnackbarContent: {
        styleOverrides: {
          root: { borderRadius: 12 },
        },
      },
    },
  });
}

export default getTheme;
