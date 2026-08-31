import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useSessao } from '@/lib/sessao';
import { Arco } from './Basicos';

/**
 * Cabeçalho, rodapé e a moldura da página.
 *
 * O cabeçalho muda conforme quem está logado, e não conforme a rota: o
 * empreendedor vê o caminho para a própria loja, a SEDECON vê a fila de
 * moderação, e quem não entrou vê só o convite para cadastrar. Menos itens na
 * tela é mais importante aqui do que em qualquer outro sistema, porque parte
 * do público mal usa computador.
 */

function LinkDoMenu({ para, children }: { para: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={para}
      className={({ isActive }) =>
        `px-2 py-1 font-semibold underline-offset-4 ${
          isActive ? 'underline decoration-2 decoration-terracota' : 'hover:underline'
        }`
      }
    >
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
    <header className="border-b-2 border-tinta bg-papel-claro">
      <div className="faixa-de-arcos text-tinta" aria-hidden="true" />

      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <Arco className="text-terracota" />
          <span className="font-display text-xl sm:text-2xl font-bold leading-none">
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
          className={`${
            menuAberto ? 'flex' : 'hidden'
          } sm:flex absolute sm:static top-20 left-0 right-0 sm:top-auto flex-col sm:flex-row
             items-stretch sm:items-center gap-2 sm:gap-4 bg-papel-claro sm:bg-transparent
             border-b-2 sm:border-0 border-tinta p-4 sm:p-0 z-20`}
        >
          <LinkDoMenu para="/">Vitrine</LinkDoMenu>
          <LinkDoMenu para="/lojas">Lojas</LinkDoMenu>

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
    <footer className="mt-16 border-t-2 border-tinta bg-papel-fundo">
      <div className="faixa-de-arcos text-tinta rotate-180" aria-hidden="true" />

      <div className="max-w-6xl mx-auto px-4 py-8 grid gap-6 sm:grid-cols-3 text-sm">
        <div>
          <p className="font-display text-lg font-bold">Vitrine Bauru</p>
          <p className="text-tinta-suave mt-1">
            A vitrine digital dos pequenos empreendedores atendidos pela SEDECON. Quem produz
            aqui, vende aqui.
          </p>
        </div>

        <div>
          <p className="font-semibold">SEDECON</p>
          <p className="text-tinta-suave mt-1">
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
          <p className="font-semibold">A plataforma</p>
          <ul className="text-tinta-suave mt-1 space-y-1">
            <li>
              <Link to="/privacidade" className="underline underline-offset-2">
                Privacidade e seus dados
              </Link>
            </li>
            <li>
              <Link to="/cadastrar" className="underline underline-offset-2">
                Cadastrar meu negócio
              </Link>
            </li>
            <li>
              <Link to="/sobre" className="underline underline-offset-2">
                Sobre o projeto
              </Link>
            </li>
          </ul>
        </div>
      </div>

      <p className="text-center text-xs text-concreto pb-6 px-4">
        Projeto de extensão universitária do Unisagrado em parceria com a SEDECON. A negociação e
        a entrega acontecem direto entre consumidor e empreendedor.
      </p>
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
    <div className="bg-mostarda border-b-2 border-tinta px-4 py-2 text-center text-sm font-semibold">
      Ambiente de demonstração: as lojas, os produtos e os telefones desta página são fictícios.
    </div>
  );
}
