import { createTheme, type PaletteMode, type ThemeOptions } from '@mui/material/styles';
// Augments the MUI Theme['components'] type with MuiDataGrid so we can style
// the DataGrid centrally alongside every other component override.
import type {} from '@mui/x-data-grid/themeAugmentation';

declare module '@mui/material/styles' {
  interface Palette {
    sidebar: {
      background: string;
      color: string;
    };
  }
  interface PaletteOptions {
    sidebar?: {
      background: string;
      color: string;
    };
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

/** Density mode driven by the Theme Customizer (ProfileMenu) — 'compact' tightens paddings app-wide. */
export type ThemeDensity = 'comfortable' | 'compact';

/** Curated accent-color swatches offered by the Theme Customizer. Passing `undefined` keeps the default indigo. */
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
  /** Hex color overriding the default indigo primary; MUI auto-derives light/dark/contrastText from `main`. */
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
        default: isLight ? '#f4f6fb' : '#0f1420',
        paper: isLight ? '#ffffff' : '#161d2e',
      },
      text: {
        primary: isLight ? '#1a1f36' : '#e6e9f2',
        secondary: isLight ? '#5a6072' : '#9aa2b8',
      },
      divider: isLight ? 'rgba(26,31,54,0.09)' : 'rgba(230,233,242,0.09)',
      sidebar: {
        background: isLight ? '#1c2763' : '#0b0f1c',
        color: '#e6e9f2',
      },
    },
    shape: {
      borderRadius: 10,
    },
    typography: {
      fontFamily: [
        'Inter',
        'Roboto',
        '"Segoe UI"',
        'Helvetica',
        'Arial',
        'sans-serif',
      ].join(','),
      h1: { fontWeight: 700, fontSize: '2.75rem', letterSpacing: '-0.02em' },
      h2: { fontWeight: 700, fontSize: '2.25rem', letterSpacing: '-0.02em' },
      h3: { fontWeight: 700, fontSize: '1.875rem', letterSpacing: '-0.01em' },
      h4: { fontWeight: 700, fontSize: '1.5rem' },
      h5: { fontWeight: 600, fontSize: '1.25rem' },
      h6: { fontWeight: 600, fontSize: '1.0625rem' },
      subtitle1: { fontWeight: 500 },
      subtitle2: { fontWeight: 500 },
      button: { fontWeight: 600, textTransform: 'none' },
    },
  };
}

export function getTheme(mode: PaletteMode, customization: ThemeCustomization = {}) {
  const { accentColor, density = 'comfortable' } = customization;
  const compact = density === 'compact';
  const tokens = getDesignTokens(mode, accentColor);
  const isLight = mode === 'light';

  return createTheme({
    ...tokens,
    spacing: compact ? 6 : 8,
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            scrollbarGutter: 'stable',
          },
        },
      },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: {
            borderRadius: 8,
            paddingTop: compact ? 5 : 8,
            paddingBottom: compact ? 5 : 8,
            paddingLeft: 18,
            paddingRight: 18,
          },
          containedPrimary: {
            boxShadow: '0 4px 14px rgba(43,58,143,0.25)',
            '&:hover': {
              boxShadow: '0 6px 18px rgba(43,58,143,0.32)',
            },
          },
          sizeLarge: {
            paddingTop: 12,
            paddingBottom: 12,
            fontSize: '1rem',
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 14,
            border: isLight
              ? '1px solid rgba(26,31,54,0.07)'
              : '1px solid rgba(230,233,242,0.08)',
            boxShadow: isLight
              ? '0 2px 10px rgba(23,29,58,0.06)'
              : '0 2px 10px rgba(0,0,0,0.35)',
            backgroundImage: 'none',
          },
        },
      },
      MuiCardContent: {
        styleOverrides: {
          root: compact ? { padding: 14, '&:last-child': { paddingBottom: 14 } } : undefined,
        },
      },
      MuiCardHeader: {
        styleOverrides: {
          title: { fontSize: '1.05rem', fontWeight: 600 },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: { backgroundImage: 'none' },
          rounded: { borderRadius: 14 },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: isLight ? '#ffffff' : '#161d2e',
            color: isLight ? '#1a1f36' : '#e6e9f2',
            boxShadow: isLight
              ? '0 1px 2px rgba(23,29,58,0.06)'
              : '0 1px 2px rgba(0,0,0,0.4)',
          },
        },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            border: 'none',
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 600 },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: compact ? { paddingTop: 6, paddingBottom: 6 } : undefined,
          head: {
            fontWeight: 700,
            backgroundColor: isLight ? '#f4f6fb' : '#111726',
          },
        },
      },
      MuiDataGrid: {
        styleOverrides: {
          root: {
            border: 'none',
            '--DataGrid-rowBorderColor': isLight
              ? 'rgba(26,31,54,0.07)'
              : 'rgba(230,233,242,0.08)',
          },
          columnHeaders: {
            backgroundColor: isLight ? '#f4f6fb' : '#111726',
            fontWeight: 700,
          },
        },
      },
      MuiTooltip: {
        styleOverrides: {
          tooltip: {
            fontSize: '0.75rem',
          },
        },
      },
    },
  });
}

export default getTheme;
