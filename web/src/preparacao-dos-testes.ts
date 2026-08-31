import '@testing-library/jest-dom/vitest';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

/**
 * Preparação comum dos testes de tela.
 *
 * O `fetch` é substituído por um espião em cada teste que precisa dele. Teste
 * de interface que chama a rede de verdade é teste que passa na máquina de
 * quem escreveu e falha no CI, e que ainda fica lento sem motivo.
 */

/**
 * O Node 25 passou a expor um `localStorage` próprio, que aqui chega sem os
 * métodos e sobrepõe o do jsdom. Sem esta substituição, tudo que depende de
 * sessão falha por um detalhe de ambiente que não tem nada a ver com o que
 * está sendo testado.
 */
function instalarArmazenamentoDeTeste(): void {
  const precisaDeSubstituto =
    typeof globalThis.localStorage === 'undefined' ||
    typeof globalThis.localStorage.getItem !== 'function';

  if (!precisaDeSubstituto) {
    return;
  }

  const dados = new Map<string, string>();

  const substituto: Storage = {
    get length() {
      return dados.size;
    },
    clear: () => dados.clear(),
    getItem: (chave: string) => dados.get(chave) ?? null,
    key: (indice: number) => Array.from(dados.keys())[indice] ?? null,
    removeItem: (chave: string) => {
      dados.delete(chave);
    },
    setItem: (chave: string, valor: string) => {
      dados.set(chave, String(valor));
    },
  };

  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    writable: true,
    value: substituto,
  });
}

instalarArmazenamentoDeTeste();

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  instalarArmazenamentoDeTeste();
  localStorage.clear();
});

/** Resposta pronta, para os testes não repetirem a montagem do `fetch`. */
export function respostaComJson(corpo: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'Content-Type': 'application/json' }),
    json: async () => corpo,
    text: async () => JSON.stringify(corpo),
  } as Response;
}
