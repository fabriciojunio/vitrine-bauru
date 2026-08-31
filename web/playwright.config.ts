import { defineConfig, devices } from '@playwright/test';

/**
 * Testes de ponta a ponta, com navegador de verdade.
 *
 * <p>Rodam contra o sistema inteiro no ar: o servidor único (os quatro
 * serviços num processo, com PostgreSQL de verdade) e o frontend servido pelo
 * Vite. O que se prova aqui é o que nenhum teste de unidade prova: que a
 * pessoa consegue usar.
 *
 * <p>Duas telas são testadas em celular de 360px, que é o tamanho em que a
 * maioria dos consumidores vai abrir a vitrine, e onde a maioria dos problemas
 * de interface aparece.
 *
 * <p>O servidor não é iniciado por aqui de propósito: subir a aplicação Java
 * leva mais de meio minuto e depende do banco preparado. O script
 * `scripts/subir-ambiente-e2e.sh` cuida disso, e o Playwright só reaproveita
 * o que já estiver no ar.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',

  use: {
    baseURL: process.env.E2E_URL ?? 'http://localhost:5173',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // As chamadas diretas de API dos testes passam pelo mesmo repasse do
    // Vite que o navegador usa, entao nao ha CORS nem endereco duplicado.
    extraHTTPHeaders: { Accept: 'application/json' },
  },

  projects: [
    {
      name: 'computador',
      use: { ...devices['Desktop Chrome'] },
      // O arquivo de celular tem asserções que só fazem sentido em tela
      // estreita (menu sanfonado, alvo de toque), e falharia aqui.
      testIgnore: /celular\.spec\.ts/,
    },
    {
      name: 'celular',
      use: { ...devices['Pixel 5'] },
      testMatch: /celular\.spec\.ts/,
    },
  ],

  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
