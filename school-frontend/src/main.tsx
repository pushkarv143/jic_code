import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import { store } from '@/store/store';
import App from './App';
import './index.css';

// The API returns bare times as "HH:mm:ss" (teacher check-in/out, exam schedule
// slots, transport pickup/drop). dayjs core ignores the format argument and hands
// the string to `new Date()`, which rejects a time with no date — so
// `dayjs('09:30:00', 'HH:mm:ss')` silently yields an *invalid* dayjs rather than
// throwing. It is still truthy, so the value survives every `? :` guard and is
// only noticed when `.format()` renders the literal string "Invalid Date" and the
// round-trip POST is rejected. Registered here, once, so every parse site works.
dayjs.extend(customParseFormat);

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element not found');
}

createRoot(rootElement).render(
  <StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </StrictMode>,
);
