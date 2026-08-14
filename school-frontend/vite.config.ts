import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Uploaded photos, documents and study materials are served by the backend
      // from /uploads, and the paths stored in the database are relative
      // ("/uploads/student-id-pictures/<uuid>.jpg"). Without this they resolve
      // against the dev server on :5173 and 404 — the upload succeeds, the
      // success toast appears, and the avatar silently falls back to initials.
      //
      // Not needed in production: .env.production sets VITE_API_BASE_URL=/api/v1,
      // so the app and the API share an origin and nginx serves /uploads itself.
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    // Foundation-stage app is already broad (auth, dashboards, 13 modules);
    // raise the warning limit rather than force a manual chunk split that
    // fights recharts' internal d3 imports. Route-level code-splitting
    // (React.lazy) is the natural next step once real module pages land.
    chunkSizeWarningLimit: 1600,
  },
});
