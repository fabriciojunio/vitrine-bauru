import { StrictMode } from 'react';
import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { chamar, sessaoGuardada } from './api';
import { ProvedorDaSessao, useSessao } from './sessao';

function resposta(corpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const sessaoNova = {
  tokenDeAcesso: 'acesso-novo',
  tokenDeRenovacao: 'renovacao-nova',
  expiraEm: '2026-11-02T20:00:00Z',
  usuario: {
    id: 'u1',
    nome: 'Lourdes',
    email: 'lourdes@exemplo.com',
    papel: 'EMPREENDEDOR',
    empreendedorId: 'e1',
  },
};

function QuemEsta() {
  const { usuario, carregando } = useSessao();
  return <p>{carregando ? 'carregando' : (usuario?.nome ?? 'ninguém')}</p>;
}

function renovacoes(espiao: ReturnType<typeof vi.fn>) {
  return espiao.mock.calls.filter((chamada) => String(chamada[0]).includes('/auth/renovar'));
}

describe('sessão reconstruída ao abrir a página', () => {
  beforeEach(() => {
    sessaoGuardada.limpar();
  });

  // O StrictMode roda o efeito de montagem duas vezes em desenvolvimento. Com
  // duas renovações simultâneas do mesmo token, a segunda chega ao servidor
  // como reuso, e ele derruba todas as sessões da conta por suspeita de roubo.
  it('faz uma renovação só, mesmo com o efeito rodando duas vezes', async () => {
    sessaoGuardada.guardarRenovacao('renovacao-guardada');
    const espiao = vi.fn().mockImplementation(async () => resposta(sessaoNova));
    vi.stubGlobal('fetch', espiao);

    render(
      <StrictMode>
        <ProvedorDaSessao>
          <QuemEsta />
        </ProvedorDaSessao>
      </StrictMode>,
    );

    expect(await screen.findByText('Lourdes')).toBeInTheDocument();
    expect(renovacoes(espiao)).toHaveLength(1);
    expect(sessaoGuardada.renovacao()).toBe('renovacao-nova');
  });

  it('divide a renovação com uma chamada que recebeu 401 ao mesmo tempo', async () => {
    sessaoGuardada.guardarAcesso('acesso-vencido');
    sessaoGuardada.guardarRenovacao('renovacao-guardada');
    const espiao = vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
      if (url.includes('/auth/renovar')) {
        return resposta(sessaoNova);
      }
      const autorizacao = (init?.headers as Record<string, string> | undefined)?.Authorization;
      return autorizacao === 'Bearer acesso-novo'
        ? resposta({ nomeDoNegocio: 'Doces da Lourdes' })
        : resposta({ detail: 'Sessão expirada' }, 401);
    });
    vi.stubGlobal('fetch', espiao);

    render(
      <ProvedorDaSessao>
        <QuemEsta />
      </ProvedorDaSessao>,
    );
    const loja = await chamar<{ nomeDoNegocio: string }>('/api/cadastro/minha-loja');

    expect(loja.nomeDoNegocio).toBe('Doces da Lourdes');
    expect(await screen.findByText('Lourdes')).toBeInTheDocument();
    expect(renovacoes(espiao)).toHaveLength(1);
  });

  it('sem token guardado, não chama o servidor e mostra ninguém logado', async () => {
    const espiao = vi.fn();
    vi.stubGlobal('fetch', espiao);

    render(
      <ProvedorDaSessao>
        <QuemEsta />
      </ProvedorDaSessao>,
    );

    expect(await screen.findByText('ninguém')).toBeInTheDocument();
    expect(espiao).not.toHaveBeenCalled();
  });

  it('com o token recusado, limpa a sessão e mostra ninguém logado', async () => {
    sessaoGuardada.guardarRenovacao('renovacao-queimada');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta({ detail: 'Sessão inválida' }, 401)));

    render(
      <ProvedorDaSessao>
        <QuemEsta />
      </ProvedorDaSessao>,
    );

    expect(await screen.findByText('ninguém')).toBeInTheDocument();
    expect(sessaoGuardada.renovacao()).toBeNull();
  });
});
