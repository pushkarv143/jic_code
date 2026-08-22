import { SnackbarProvider } from 'notistack';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { ThemeModeProvider } from '@/theme/ThemeModeProvider';
import { LanguageProvider } from '@/i18n/LanguageProvider';
import { AccessProvider } from '@/access/AccessProvider';
import AppRouter from '@/routes/AppRouter';

/**
 * App root: theme (Redux-backed), language (i18n scaffolding), date localization,
 * toast provider, live authorization state, and the router.
 *
 * AccessProvider sits inside SnackbarProvider (it has no toasts of its own, but
 * screens that call `refresh()` do) and outside AppRouter, which needs the
 * permissions and module list to decide what to route and what to show in the nav.
 */
function App() {
  return (
    <ThemeModeProvider>
      <LanguageProvider>
        <LocalizationProvider dateAdapter={AdapterDayjs}>
          <SnackbarProvider
            maxSnack={3}
            anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
            autoHideDuration={4000}
          >
            <AccessProvider>
              <AppRouter />
            </AccessProvider>
          </SnackbarProvider>
        </LocalizationProvider>
      </LanguageProvider>
    </ThemeModeProvider>
  );
}

export default App;
