import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BASE_DA_API, chamar, ErroDaApi, sessaoGuardada } from '@/lib/api';
import { useSessao } from '@/lib/sessao';
import { Aviso, Botao, TituloDeSecao } from '@/componentes/Basicos';

/**
 * Privacidade, com os dois direitos que a LGPD garante ao titular
 * funcionando de verdade: ver o que a plataforma guarda e mandar apagar.
 *
 * <p>Estar aqui, com botão que funciona, é a diferença entre cumprir a lei e
 * escrever que cumpre. A exclusão avisa que leva tempo porque leva mesmo: são
 * quatro serviços apagando a parte deles e confirmando de volta.
 */
export function Privacidade() {
  const { usuario, sair } = useSessao();
  const navegar = useNavigate();

  const [excluindo, definirExcluindo] = useState(false);
  const [confirmando, definirConfirmando] = useState(false);
  const [erro, definirErro] = useState<string | null>(null);
  const [recibo, definirRecibo] = useState<string | null>(null);

  const baixarMeusDados = () => {
    // Abre em aba nova com o token na consulta? Não: o token não pode ir para
    // a barra de endereço, onde fica no histórico. A busca é feita pelo
    // próprio cliente, com cabeçalho, e o arquivo é montado aqui.
    chamar<unknown>('/api/cadastro/privacidade/meus-dados')
      .then((dados) => {
        const conteudo = new Blob([JSON.stringify(dados, null, 2)], {
          type: 'application/json',
        });
        const endereco = URL.createObjectURL(conteudo);
        const link = document.createElement('a');
        link.href = endereco;
        link.download = 'meus-dados-vitrine-bauru.json';
        link.click();
        URL.revokeObjectURL(endereco);
      })
      .catch((falha: ErroDaApi) => definirErro(falha.message));
  };

  const excluirConta = async () => {
    definirExcluindo(true);
    definirErro(null);
    try {
      const resposta = await chamar<{ protocolo: string; prazoLimite: string }>(
        '/api/cadastro/privacidade/minha-conta',
        { metodo: 'DELETE' },
      );
      definirRecibo(resposta.protocolo);
      sessaoGuardada.limpar();
      await sair();
    } catch (falha) {
      definirErro(falha instanceof ErroDaApi ? falha.message : 'Não foi possível concluir.');
    } finally {
      definirExcluindo(false);
    }
  };

  if (recibo) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <Aviso tipo="certo" titulo="Pedido de exclusão registrado">
          <p>
            Sua loja saiu da vitrine agora. O apagamento dos dados nos demais serviços acontece em
            seguida e costuma levar segundos.
          </p>
          <p className="mt-2">
            Guarde o número do protocolo: <strong>{recibo}</strong>
          </p>
        </Aviso>
        <Botao className="mt-6" onClick={() => navegar('/')}>
          Voltar para a vitrine
        </Botao>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <TituloDeSecao descricao="O que a plataforma guarda, por que guarda, e o que você pode fazer com isso.">
        Privacidade e seus dados
      </TituloDeSecao>

      <div className="quadro p-5 flex flex-col gap-4">
        <section>
          <h3 className="font-display text-xl font-bold">O que guardamos</h3>
          <p className="text-tinta-suave mt-1">
            De quem vende: nome, e-mail, senha protegida por bcrypt, CPF ou CNPJ, telefone, bairro
            e os dados do negócio. O documento é usado pela SEDECON para conferir o cadastro, e
            aparece mascarado para quem modera.
          </p>
          <p className="text-tinta-suave mt-2">
            De quem compra: nada. Não há conta, não há cookie de rastreamento e não há registro
            de quem clicou. O contato pelo WhatsApp é contado sem qualquer identificação de quem
            clicou.
          </p>
        </section>

        <section>
          <h3 className="font-display text-xl font-bold">Com quem compartilhamos</h3>
          <p className="text-tinta-suave mt-1">
            Com ninguém. O CNPJ é conferido na base pública da Receita Federal, e o e-mail de
            aviso é enviado por um provedor de e-mail transacional. Não há publicidade, não há
            venda de dados e não há ferramenta de análise de terceiro nesta página.
          </p>
        </section>

        <section>
          <h3 className="font-display text-xl font-bold">Por quanto tempo</h3>
          <p className="text-tinta-suave mt-1">
            Enquanto o cadastro existir. Ao pedir exclusão, os dados que identificam você são
            apagados dos quatro serviços; fica apenas o registro de auditoria dos atos da SEDECON,
            sem os seus dados pessoais, porque quem responde por uma aprovação é quem aprovou.
          </p>
        </section>
      </div>

      {usuario ? (
        <div className="mt-8 flex flex-col gap-5">
          {erro && <Aviso tipo="erro">{erro}</Aviso>}

          <div className="quadro p-5">
            <h3 className="font-display text-xl font-bold">Baixar meus dados</h3>
            <p className="text-tinta-suave mt-1 mb-3">
              Um arquivo com tudo que o serviço de cadastro guarda sobre você.
            </p>
            <Botao variante="neutro" onClick={baixarMeusDados}>
              Baixar em arquivo
            </Botao>
          </div>

          <div className="quadro p-5 bg-terracota-claro">
            <h3 className="font-display text-xl font-bold">Excluir minha conta</h3>
            <p className="text-tinta-suave mt-1 mb-3">
              Sua loja sai da vitrine na hora e todos os seus dados são apagados. Não dá para
              desfazer.
            </p>

            {confirmando ? (
              <div className="flex flex-wrap gap-3">
                <Botao variante="principal" carregando={excluindo} onClick={excluirConta}>
                  Confirmo, pode excluir
                </Botao>
                <Botao variante="neutro" onClick={() => definirConfirmando(false)}>
                  Não, voltar
                </Botao>
              </div>
            ) : (
              <Botao variante="neutro" onClick={() => definirConfirmando(true)}>
                Quero excluir minha conta
              </Botao>
            )}
          </div>
        </div>
      ) : (
        <p className="mt-8 text-tinta-suave">
          Para baixar ou apagar seus dados, entre na sua conta. Se você não consegue entrar,
          procure a Casa do Empreendedor: Av. Duque de Caxias, 16-55, Vila Cardia,
          (14) 3227-7819.
        </p>
      )}

      <p className="mt-8 text-xs text-concreto">
        Endereço da API usado por esta página: <code>{BASE_DA_API || 'mesma origem'}</code>
      </p>
    </div>
  );
}
