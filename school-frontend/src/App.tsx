import { SnackbarProvider } from 'notistack';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { ThemeModeProvider } from '@/theme/ThemeModeProvider';
import { LanguageProvider } from '@/i18n/LanguageProvider';
import AppRouter from '@/routes/AppRouter';

/** App root: theme (Redux-backed), language (i18n scaffolding), date localization, toast provider, and the router. */
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
            <AppRouter />
          </SnackbarProvider>
        </LocalizationProvider>
      </LanguageProvider>
    </ThemeModeProvider>
  );
}

export default App;
