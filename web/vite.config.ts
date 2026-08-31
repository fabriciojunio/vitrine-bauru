/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwind from '@tailwindcss/vite';
import path from 'node:path';

export default defineConfig({
  plugins: [react(), tailwind()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 5173,
    // Em desenvolvimento o navegador fala só com o Vite, que repassa para o
    // back-end. Sem isso seria preciso liberar CORS em desenvolvimento, e CORS
    // aberto em desenvolvimento tem o costume de vazar para produção.
    proxy: {
      '/api': {
        target: process.env.VITE_API_ALVO ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // Mapa de código em produção entrega o fonte original para quem abrir o
    // inspetor. Não há ganho que compense.
    sourcemap: false,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/preparacao-dos-testes.ts',
    // O Playwright cuida do que precisa de navegador de verdade; o Vitest
    // roda só o que dá para verificar sem servidor no ar.
    exclude: ['node_modules/**', 'e2e/**'],
  },
});
