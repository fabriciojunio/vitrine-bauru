import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useSessao } from '@/lib/sessao';
import { Arco } from './Basicos';

/**
 * Cabeçalho, rodapé e a moldura da página.
 *
 * <p>A barra de topo é verde institucional porque a plataforma leva o nome da
 * SEDECON: o primeiro segundo de leitura precisa dizer "isto é da prefeitura",
 * e não "isto é mais um site de vendas". O terracota fica reservado para a
 * única ação que interessa ali, que é cadastrar o negócio.
 *
 * <p>O menu muda conforme quem está logado, e não conforme a rota. Menos item
 * na tela importa mais aqui do que em qualquer outro sistema, porque parte do
 * público mal usa computador.
 */

function LinkDoMenu({ para, children }: { para: string; children: React.ReactNode }) {
  return (
    <NavLink to={para} className="link-do-menu">
      {children}
    </NavLink>
  );
}

export function Cabecalho() {
  const { usuario, sair } = useSessao();
  const navegar = useNavigate();
  const [menuAberto, setMenuAberto] = useState(false);

  const encerrar = async () => {
    await sair();
    navegar('/');
  };

  return (
    <header className="bg-selo text-papel border-b-4 border-tinta relative z-30">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center gap-2.5 shrink-0 text-mostarda">
          <Arco />
          <span className="font-display text-xl sm:text-2xl font-bold leading-none text-papel-claro">
            Vitrine Bauru
          </span>
        </Link>

        <button
          className="botao botao-neutro sm:hidden px-3"
          aria-expanded={menuAberto}
          aria-controls="menu-principal"
          onClick={() => setMenuAberto((aberto) => !aberto)}
        >
          Menu
        </button>

        <nav
          id="menu-principal"
          className={`${menuAberto ? 'flex' : 'hidden'} sm:flex absolute sm:static top-full left-0
             right-0 flex-col sm:flex-row items-stretch sm:items-center gap-3 sm:gap-5
             bg-selo-escuro sm:bg-transparent border-b-4 sm:border-0 border-tinta p-4 sm:p-0 z-30`}
        >
          <LinkDoMenu para="/">Vitrine</LinkDoMenu>
          <LinkDoMenu para="/lojas">Lojas</LinkDoMenu>
          <LinkDoMenu para="/sobre">Sobre</LinkDoMenu>

          {!usuario && (
            <>
              <LinkDoMenu para="/entrar">Entrar</LinkDoMenu>
              <Link to="/cadastrar" className="botao botao-principal">
                Quero vender
              </Link>
            </>
          )}

          {usuario?.papel === 'EMPREENDEDOR' && (
            <>
              <LinkDoMenu para="/painel">Minha loja</LinkDoMenu>
              <button className="botao botao-neutro" onClick={encerrar}>
                Sair
              </button>
            </>
          )}

          {usuario?.papel === 'ADMIN_SEDECON' && (
            <>
              <LinkDoMenu para="/sedecon">Moderação</LinkDoMenu>
              <LinkDoMenu para="/sedecon/indicadores">Indicadores</LinkDoMenu>
              <button className="botao botao-neutro" onClick={encerrar}>
                Sair
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

export function Rodape() {
  return (
    <footer className="mt-16 bg-selo-escuro text-papel border-t-4 border-tinta">
      <div className="arcada-faixa h-14" aria-hidden="true" />

      <div className="max-w-6xl mx-auto px-4 pb-10 grid gap-8 sm:grid-cols-3 text-sm">
        <div>
          <div className="flex items-center gap-2 text-mostarda mb-2">
            <Arco />
            <span className="font-display text-lg font-bold text-papel-claro">Vitrine Bauru</span>
          </div>
          <p className="text-papel/80 leading-relaxed">
            A vitrine digital dos pequenos empreendedores atendidos pela SEDECON. Quem produz
            aqui, vende aqui.
          </p>
        </div>

        <div>
          <p className="font-semibold text-mostarda uppercase tracking-wide text-xs mb-2">
            SEDECON
          </p>
          <p className="text-papel/80 leading-relaxed">
            Casa do Empreendedor
            <br />
            Av. Duque de Caxias, 16-55, Vila Cardia
            <br />
            (14) 3227-7819
            <br />
            Segunda a sexta, das 8h às 17h
          </p>
        </div>

        <div>
          <p className="font-semibold text-mostarda uppercase tracking-wide text-xs mb-2">
            A plataforma
          </p>
          <ul className="space-y-1.5 text-papel/90">
            <li>
              <Link to="/privacidade" className="underline underline-offset-2 hover:text-mostarda">
                Privacidade e seus dados
              </Link>
            </li>
            <li>
              <Link to="/cadastrar" className="underline underline-offset-2 hover:text-mostarda">
                Cadastrar meu negócio
              </Link>
            </li>
            <li>
              <Link to="/lojas" className="underline underline-offset-2 hover:text-mostarda">
                Todas as lojas
              </Link>
            </li>
            <li>
              <Link to="/sobre" className="underline underline-offset-2 hover:text-mostarda">
                Sobre o projeto
              </Link>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-papel/20">
        <p className="max-w-6xl mx-auto text-center text-xs text-papel/70 py-4 px-4">
          Projeto de extensão universitária do Unisagrado em parceria com a SEDECON. A negociação
          e a entrega acontecem direto entre consumidor e empreendedor.
        </p>
      </div>
    </footer>
  );
}

/**
 * Faixa que aparece quando o ambiente é de demonstração.
 *
 * Existe para ninguém confundir a vitrine de teste com a de verdade: são lojas
 * fictícias, e alguém precisaria descobrir isso antes de tentar comprar um bolo
 * que não existe.
 */
export function FaixaDeDemonstracao({ ativa }: { ativa: boolean }) {
  if (!ativa) {
    return null;
  }

  return (
    <div className="bg-mostarda border-b-2 border-tinta px-4 py-1.5 text-center text-sm font-semibold">
      Ambiente de demonstração: as lojas, os produtos e os telefones desta página são fictícios.
    </div>
  );
}
