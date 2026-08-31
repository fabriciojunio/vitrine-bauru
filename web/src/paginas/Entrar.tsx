import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { chamar, ErroDaApi } from '@/lib/api';
import { useSessao } from '@/lib/sessao';
import { Arco, Aviso, Botao, Campo } from '@/componentes/Basicos';

/**
 * A tela de entrar, com a demonstração em um clique.
 *
 * <p>Os dois botões de demonstração só aparecem quando o ambiente diz que ela
 * está ligada, e quem responde isso é o servidor. Assim a mesma imagem da
 * aplicação sobe em demonstração e em produção sem nenhuma diferença de
 * código: em produção o endereço simplesmente não existe, e os botões somem.
 *
 * <p>Eles existem porque as pessoas que vão avaliar este sistema (a professora,
 * a SEDECON, quem abrir o portfólio) não têm cadastro nem deveriam precisar
 * inventar um. Entrar como empreendedor e entrar como SEDECON mostram os dois
 * lados do produto, que é o que interessa ver.
 */
export function Entrar() {
  const { entrar, entrarNaDemonstracao, usuario } = useSessao();
  const navegar = useNavigate();

  const [email, definirEmail] = useState('');
  const [senha, definirSenha] = useState('');
  const [erro, definirErro] = useState<string | null>(null);
  const [enviando, definirEnviando] = useState(false);
  const [demonstracaoAtiva, definirDemonstracaoAtiva] = useState(false);

  useEffect(() => {
    chamar<{ ativa: boolean }>('/api/cadastro/auth/demonstracao', { semAutenticacao: true })
      .then((resposta) => definirDemonstracaoAtiva(resposta.ativa))
      .catch(() => definirDemonstracaoAtiva(false));
  }, []);

  useEffect(() => {
    if (usuario) {
      navegar(usuario.papel === 'ADMIN_SEDECON' ? '/sedecon' : '/painel', { replace: true });
    }
  }, [usuario, navegar]);

  const enviar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    definirErro(null);
    definirEnviando(true);

    try {
      const logado = await entrar(email, senha);
      navegar(logado.papel === 'ADMIN_SEDECON' ? '/sedecon' : '/painel');
    } catch (falha) {
      definirErro(falha instanceof ErroDaApi ? falha.message : 'Não foi possível entrar agora.');
    } finally {
      definirEnviando(false);
    }
  };

  const entrarComoDemonstracao = async (papel: 'empreendedor' | 'sedecon') => {
    definirErro(null);
    definirEnviando(true);
    try {
      const logado = await entrarNaDemonstracao(papel);
      navegar(logado.papel === 'ADMIN_SEDECON' ? '/sedecon' : '/painel');
    } catch (falha) {
      definirErro(
        falha instanceof ErroDaApi
          ? falha.message
          : 'A demonstração não respondeu. Tente de novo em instantes.',
      );
    } finally {
      definirEnviando(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-10 grid gap-8 md:grid-cols-2 items-start">
      <div className="quadro carimbo p-6">
        <Arco className="text-terracota mb-2" />
        <h1 className="text-3xl">Entrar na sua loja</h1>
        <p className="text-tinta-suave mt-1">
          É a mesma conta que você criou no cadastro do seu negócio.
        </p>

        <form className="mt-6 flex flex-col gap-4" onSubmit={enviar} noValidate>
          {erro && (
            <Aviso tipo="erro" titulo="Não foi possível entrar">
              {erro}
            </Aviso>
          )}

          <Campo
            etiqueta="E-mail"
            type="email"
            autoComplete="email"
            inputMode="email"
            value={email}
            onChange={(evento) => definirEmail(evento.target.value)}
            required
          />

          <Campo
            etiqueta="Senha"
            type="password"
            autoComplete="current-password"
            value={senha}
            onChange={(evento) => definirSenha(evento.target.value)}
            required
          />

          <Botao type="submit" carregando={enviando}>
            Entrar
          </Botao>
        </form>

        <p className="mt-5 text-sm text-tinta-suave">
          Ainda não tem cadastro?{' '}
          <Link to="/cadastrar" className="underline underline-offset-2 font-semibold">
            Cadastre seu negócio
          </Link>
          . É de graça e leva poucos minutos.
        </p>
      </div>

      {demonstracaoAtiva ? (
        <div className="quadro carimbo p-6 bg-mostarda-claro">
          <h2 className="text-2xl">Conhecer sem cadastro</h2>
          <p className="text-tinta-suave mt-1">
            Este é um ambiente de demonstração, com lojas fictícias. Entre pelos dois lados para
            ver como o sistema funciona por dentro.
          </p>

          <div className="mt-5 flex flex-col gap-3">
            <Botao
              variante="principal"
              onClick={() => entrarComoDemonstracao('empreendedor')}
              carregando={enviando}
            >
              Entrar como empreendedora
            </Botao>
            <p className="text-sm text-tinta-suave -mt-1">
              Você vê a loja Doces da Lourdes: catálogo, fotos, situação do cadastro e quantos
              contatos ela recebeu.
            </p>

            <Botao
              variante="selo"
              onClick={() => entrarComoDemonstracao('sedecon')}
              carregando={enviando}
            >
              Entrar como SEDECON
            </Botao>
            <p className="text-sm text-tinta-suave -mt-1">
              Você vê a fila de moderação, aprova ou recusa cadastro e acompanha os indicadores de
              impacto da plataforma.
            </p>
          </div>

          <p className="mt-5 text-xs text-concreto">
            Nada aqui é dado real: nomes, telefones e documentos foram inventados para a
            demonstração. Os dados são recriados periodicamente.
          </p>
        </div>
      ) : (
        <div className="quadro p-6 bg-papel-fundo">
          <h2 className="text-2xl">Primeira vez por aqui?</h2>
          <p className="text-tinta-suave mt-2">
            Se você tem um negócio em Bauru, pode cadastrar de graça. A SEDECON confere os dados
            e sua loja entra na vitrine.
          </p>
          <Link to="/cadastrar" className="botao botao-principal mt-4">
            Cadastrar meu negócio
          </Link>
        </div>
      )}

      {/* Fecha a página com o que resolve o problema de quem não conseguiu
          entrar. Sem isto sobra um vazio embaixo dos dois cartões, e quem
          travou no login fica sem saída. */}
      <div className="md:col-span-2 quadro carimbo-leve p-6 bg-selo-claro">
        <h2 className="text-xl">Não consegue entrar?</h2>
        <div className="grid gap-6 sm:grid-cols-3 mt-3 text-sm text-tinta-suave">
          <div>
            <p className="font-semibold text-tinta">Esqueceu a senha</p>
            <p className="mt-1">
              Procure a Casa do Empreendedor levando um documento. A SEDECON reencaminha o acesso.
            </p>
          </div>
          <div>
            <p className="font-semibold text-tinta">Conta bloqueada</p>
            <p className="mt-1">
              Depois de cinco tentativas erradas, a conta trava por quinze minutos. Espere e
              tente de novo.
            </p>
          </div>
          <div>
            <p className="font-semibold text-tinta">Atendimento</p>
            <p className="mt-1">
              Av. Duque de Caxias, 16-55, Vila Cardia. (14) 3227-7819. De segunda a sexta, das 8h
              às 17h.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
