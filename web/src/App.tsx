import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { chamar } from '@/lib/api';
import { useSessao } from '@/lib/sessao';
import { Cabecalho, FaixaDeDemonstracao, Rodape } from '@/componentes/Estrutura';
import { Carregando } from '@/componentes/Basicos';
import { Vitrine } from '@/paginas/Vitrine';
import { Lojas } from '@/paginas/Lojas';
import { PaginaDaLoja } from '@/paginas/PaginaDaLoja';
import { Entrar } from '@/paginas/Entrar';
import { Cadastrar } from '@/paginas/Cadastrar';
import { Painel } from '@/paginas/Painel';
import { MeusProdutos } from '@/paginas/MeusProdutos';
import { Moderacao } from '@/paginas/Moderacao';
import { Indicadores } from '@/paginas/Indicadores';
import { Privacidade } from '@/paginas/Privacidade';
import { NaoEncontrada, Sobre } from '@/paginas/Sobre';

/**
 * O mapa de telas.
 *
 * <p>A proteção de rota aqui é conveniência, não segurança: ela evita mostrar
 * uma tela que vai falhar. Quem de fato barra acesso é o back-end, que confere
 * o token e o papel em toda requisição. Confiar em rota protegida no navegador
 * é confiar em código que roda na máquina de quem está tentando entrar.
 */

function RotaProtegida({
  children,
  papel,
}: {
  children: ReactNode;
  papel?: 'EMPREENDEDOR' | 'ADMIN_SEDECON';
}) {
  const { usuario, carregando } = useSessao();
  const local = useLocation();

  if (carregando) {
    return <Carregando texto="Abrindo sua conta…" />;
  }

  if (!usuario) {
    return <Navigate to="/entrar" state={{ de: local.pathname }} replace />;
  }

  if (papel && usuario.papel !== papel) {
    return <Navigate to={usuario.papel === 'ADMIN_SEDECON' ? '/sedecon' : '/painel'} replace />;
  }

  return <>{children}</>;
}

/** Sobe ao topo a cada troca de tela: sem isso, a busca abre no meio da grade. */
function SubirAoTrocarDePagina() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}

export function App() {
  const [demonstracao, definirDemonstracao] = useState(false);

  useEffect(() => {
    chamar<{ ativa: boolean }>('/api/cadastro/auth/demonstracao', { semAutenticacao: true })
      .then((resposta) => definirDemonstracao(resposta.ativa))
      .catch(() => definirDemonstracao(false));
  }, []);

  return (
    <div className="min-h-screen flex flex-col">
      <SubirAoTrocarDePagina />
      <FaixaDeDemonstracao ativa={demonstracao} />
      <Cabecalho />

      <main className="grow">
        <Routes>
          <Route path="/" element={<Vitrine />} />
          <Route path="/lojas" element={<Lojas />} />
          <Route path="/loja/:apelido" element={<PaginaDaLoja />} />
          <Route path="/entrar" element={<Entrar />} />
          <Route path="/cadastrar" element={<Cadastrar />} />
          <Route path="/sobre" element={<Sobre />} />
          <Route path="/privacidade" element={<Privacidade />} />

          <Route
            path="/painel"
            element={
              <RotaProtegida papel="EMPREENDEDOR">
                <Painel />
              </RotaProtegida>
            }
          />
          <Route
            path="/painel/produtos"
            element={
              <RotaProtegida papel="EMPREENDEDOR">
                <MeusProdutos />
              </RotaProtegida>
            }
          />

          <Route
            path="/sedecon"
            element={
              <RotaProtegida papel="ADMIN_SEDECON">
                <Moderacao />
              </RotaProtegida>
            }
          />
          <Route
            path="/sedecon/indicadores"
            element={
              <RotaProtegida papel="ADMIN_SEDECON">
                <Indicadores />
              </RotaProtegida>
            }
          />

          <Route path="*" element={<NaoEncontrada />} />
        </Routes>
      </main>

      <Rodape />
    </div>
  );
}
