import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { chamar, sessaoGuardada } from './api';
import type { Sessao, UsuarioLogado } from './tipos';

/**
 * Quem está logado, para o resto da aplicação.
 *
 * Na primeira carga, se existe um token de renovação guardado, o contexto
 * tenta reconstruir a sessão antes de decidir o que mostrar. Sem isso, quem
 * atualizasse a página do painel seria jogado para a tela de login por um
 * instante e voltaria, o que parece defeito mesmo funcionando.
 */

interface ContextoDaSessao {
  usuario: UsuarioLogado | null;
  carregando: boolean;
  entrar(email: string, senha: string): Promise<UsuarioLogado>;
  entrarNaDemonstracao(papel: 'empreendedor' | 'sedecon'): Promise<UsuarioLogado>;
  sair(): Promise<void>;
}

const Contexto = createContext<ContextoDaSessao | null>(null);

export function ProvedorDaSessao({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(null);
  const [carregando, setCarregando] = useState(true);

  const guardar = useCallback((sessao: Sessao) => {
    sessaoGuardada.guardarAcesso(sessao.tokenDeAcesso);
    sessaoGuardada.guardarRenovacao(sessao.tokenDeRenovacao);
    setUsuario(sessao.usuario);
    return sessao.usuario;
  }, []);

  useEffect(() => {
    const renovacao = sessaoGuardada.renovacao();
    if (!renovacao) {
      setCarregando(false);
      return;
    }

    chamar<Sessao>('/api/cadastro/auth/renovar', {
      metodo: 'POST',
      corpo: { tokenDeRenovacao: renovacao },
      semAutenticacao: true,
    })
      .then(guardar)
      .catch(() => sessaoGuardada.limpar())
      .finally(() => setCarregando(false));
  }, [guardar]);

  const entrar = useCallback(
    async (email: string, senha: string) => {
      const sessao = await chamar<Sessao>('/api/cadastro/auth/login', {
        metodo: 'POST',
        corpo: { email, senha },
        semAutenticacao: true,
      });
      return guardar(sessao);
    },
    [guardar],
  );

  const entrarNaDemonstracao = useCallback(
    async (papel: 'empreendedor' | 'sedecon') => {
      const sessao = await chamar<Sessao>('/api/cadastro/auth/demonstracao', {
        metodo: 'POST',
        corpo: { papel },
        semAutenticacao: true,
      });
      return guardar(sessao);
    },
    [guardar],
  );

  const sair = useCallback(async () => {
    const renovacao = sessaoGuardada.renovacao();
    try {
      await chamar<void>('/api/cadastro/auth/sair', {
        metodo: 'POST',
        corpo: { tokenDeRenovacao: renovacao ?? '' },
      });
    } catch {
      // Sair precisa funcionar mesmo com o servidor fora do ar: o que importa
      // para quem clicou é o token sumir deste navegador.
    }
    sessaoGuardada.limpar();
    setUsuario(null);
  }, []);

  const valor = useMemo(
    () => ({ usuario, carregando, entrar, entrarNaDemonstracao, sair }),
    [usuario, carregando, entrar, entrarNaDemonstracao, sair],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useSessao(): ContextoDaSessao {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useSessao precisa estar dentro do ProvedorDaSessao');
  }
  return contexto;
}
