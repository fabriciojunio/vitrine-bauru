/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Endereço do back-end. Vazio em desenvolvimento, quando o próprio Vite faz
   * o repasse; preenchido no build da Vercel, onde o front e a API ficam em
   * domínios diferentes.
   */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
