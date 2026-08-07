import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ThemeProvider, type PaletteMode } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { getTheme, type ThemeDensity } from './theme';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { setThemeMode } from '@/store/uiSlice';

const ACCENT_KEY = 'sms-theme-accent';
const DENSITY_KEY = 'sms-theme-density';

function loadAccentColor(): string | undefined {
  return localStorage.getItem(ACCENT_KEY) || undefined;
}

function loadDensity(): ThemeDensity {
  return localStorage.getItem(DENSITY_KEY) === 'compact' ? 'compact' : 'comfortable';
}

interface ThemeModeContextValue {
  mode: PaletteMode;
  toggleMode: () => void;
  setMode: (mode: PaletteMode) => void;
  /** Theme Customizer state (Round 7) — persisted to localStorage, applied via theme.ts's getTheme(). */
  accentColor: string | undefined;
  setAccentColor: (color: string | undefined) => void;
  density: ThemeDensity;
  setDensity: (density: ThemeDensity) => void;
}

const ThemeModeContext = createContext<ThemeModeContextValue | undefined>(undefined);

/**
 * Wraps the app with an MUI ThemeProvider whose palette mode is sourced from
 * Redux (uiSlice), which itself persists to localStorage. Also owns the
 * lighter-weight Theme Customizer preferences (accent color + density), kept
 * as plain localStorage-backed state rather than Redux since they're purely
 * cosmetic and don't need to be shared across reducers. Exposes useThemeMode()
 * for any component that needs to read or change any of these.
 */
export function ThemeModeProvider({ children }: { children: ReactNode }) {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.themeMode);
  const [accentColor, setAccentColorState] = useState<string | undefined>(loadAccentColor);
  const [density, setDensityState] = useState<ThemeDensity>(loadDensity);

  useEffect(() => {
    localStorage.setItem('sms-theme-mode', mode);
  }, [mode]);

  const theme = useMemo(() => getTheme(mode, { accentColor, density }), [mode, accentColor, density]);

  const value = useMemo<ThemeModeContextValue>(
    () => ({
      mode,
      toggleMode: () => dispatch(setThemeMode(mode === 'light' ? 'dark' : 'light')),
      setMode: (next: PaletteMode) => dispatch(setThemeMode(next)),
      accentColor,
      setAccentColor: (color?: string) => {
        setAccentColorState(color);
        if (color) localStorage.setItem(ACCENT_KEY, color);
        else localStorage.removeItem(ACCENT_KEY);
      },
      density,
      setDensity: (next: ThemeDensity) => {
        setDensityState(next);
        localStorage.setItem(DENSITY_KEY, next);
      },
    }),
    [mode, dispatch, accentColor, density],
  );

  return (
    <ThemeModeContext.Provider value={value}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ThemeModeContext.Provider>
  );
}

export function useThemeMode(): ThemeModeContextValue {
  const ctx = useContext(ThemeModeContext);
  if (!ctx) {
    throw new Error('useThemeMode must be used within a ThemeModeProvider');
  }
  return ctx;
}
