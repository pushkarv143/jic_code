import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { PaletteMode } from '@mui/material';

const THEME_KEY = 'sms-theme-mode';

function loadThemeMode(): PaletteMode {
  const stored = localStorage.getItem(THEME_KEY);
  if (stored === 'light' || stored === 'dark') return stored;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export interface UiState {
  themeMode: PaletteMode;
  sidebarOpen: boolean;
  sidebarCollapsed: boolean;
  globalLoading: boolean;
}

const initialState: UiState = {
  themeMode: loadThemeMode(),
  sidebarOpen: false,
  sidebarCollapsed: false,
  globalLoading: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setThemeMode: (state, action: PayloadAction<PaletteMode>) => {
      state.themeMode = action.payload;
      localStorage.setItem(THEME_KEY, action.payload);
    },
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    toggleSidebarCollapsed: (state) => {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },
    setGlobalLoading: (state, action: PayloadAction<boolean>) => {
      state.globalLoading = action.payload;
    },
  },
});

export const {
  setThemeMode,
  toggleSidebar,
  setSidebarOpen,
  toggleSidebarCollapsed,
  setGlobalLoading,
} = uiSlice.actions;
export default uiSlice.reducer;
