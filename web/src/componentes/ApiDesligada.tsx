import { Arco } from './Basicos';

/**
 * A tela que aparece quando a API não responde.
 *
 * <p>Existe porque a publicação do front-end e a do back-end são
 * independentes: dá para o site estar no ar antes de o servidor existir, e
 * quem abrir nessa janela merece uma explicação, e não um alerta vermelho
 * dizendo "não foi possível carregar".
 *
 * <p>Também cobre o caso mais comum depois de tudo publicado: a hospedagem
 * gratuita hiberna depois de quinze minutos parada, e a primeira visita do dia
 * espera o servidor acordar.
 */
export function ApiDesligada({ aoTentarDeNovo }: { aoTentarDeNovo?: () => void }) {
  return (
    <div className="max-w-2xl mx-auto px-4 py-16 text-center">
      <Arco className="text-terracota mx-auto mb-3" />

      <h2 className="text-2xl sm:text-3xl">A vitrine ainda não está respondendo</h2>

      <p className="text-tinta-suave mt-3">
        Isso costuma ser uma de duas coisas. A mais comum: o servidor hiberna
        depois de quinze minutos sem acesso, e a primeira visita do dia espera
        de trinta a sessenta segundos até ele acordar. Recarregue a página em
        instantes.
      </p>

      <p className="text-tinta-suave mt-3">
        A outra: esta publicação do site ainda não foi ligada ao endereço da
        API. Quem cuida do projeto resolve isso configurando a variável{' '}
        <code className="font-mono text-sm bg-papel-fundo px-1 border border-linha">
          VITE_API_URL
        </code>
        .
      </p>

      {aoTentarDeNovo && (
        <button className="botao botao-principal mt-6" onClick={aoTentarDeNovo}>
          Tentar de novo
        </button>
      )}

      <p className="text-sm text-concreto mt-8">
        Se você precisa falar com a SEDECON agora, a Casa do Empreendedor atende
        de segunda a sexta, das 8h às 17h, na Av. Duque de Caxias, 16-55, Vila
        Cardia, telefone (14) 3227-7819.
      </p>
    </div>
  );
}
