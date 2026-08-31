import { Link } from 'react-router-dom';
import { TituloDeSecao } from '@/componentes/Basicos';

/**
 * A página que explica de onde o projeto veio.
 *
 * <p>Existe porque a plataforma leva o nome da SEDECON e pede CPF de gente de
 * verdade. Quem chega precisa conseguir descobrir, em um clique, quem está por
 * trás disso e por quê.
 */
export function Sobre() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <TituloDeSecao descricao="De onde veio, para quem é e o que a plataforma faz (e o que não faz).">
        Sobre a Vitrine Bauru
      </TituloDeSecao>

      <div className="flex flex-col gap-6 text-tinta-suave leading-relaxed">
        <p>
          A Vitrine Bauru nasceu de um projeto de extensão universitária do Unisagrado, em
          parceria com a SEDECON, a Secretaria de Desenvolvimento Econômico de Bauru. A
          secretaria já atende pequenos empreendedores pela Casa do Empreendedor, pelo Banco do
          Povo e pelo Emprega Bauru; o que faltava era uma vitrine comum, para o trabalho desse
          pessoal alcançar quem mora a dez quadras de distância e não sabe que eles existem.
        </p>

        <div className="quadro p-5 bg-papel-fundo">
          <h3 className="font-display text-xl font-bold text-tinta">O que a plataforma faz</h3>
          <ul className="mt-2 list-disc pl-5 space-y-1">
            <li>Mostra produtos e serviços de pequenos negócios de Bauru.</li>
            <li>Deixa qualquer pessoa procurar por palavra, bairro ou categoria, sem cadastro.</li>
            <li>Leva o consumidor direto ao WhatsApp do empreendedor.</li>
            <li>Permite que a SEDECON confira cada cadastro antes de a loja entrar no ar.</li>
          </ul>
        </div>

        <div className="quadro p-5">
          <h3 className="font-display text-xl font-bold text-tinta">O que ela não faz</h3>
          <ul className="mt-2 list-disc pl-5 space-y-1">
            <li>Não processa pagamento. A negociação acontece entre as duas pessoas.</li>
            <li>Não cobra taxa, comissão nem mensalidade de ninguém.</li>
            <li>Não entrega, não intermedeia e não garante a compra.</li>
            <li>Não guarda nada sobre quem só está olhando a vitrine.</li>
          </ul>
        </div>

        <p>
          O nome vem do que a plataforma é: uma vitrine. O desenho da interface vem do Calçadão da
          Batista de Carvalho, fechado ao trânsito em 1992 e coberto por setenta arcos de ferro,
          que é onde o comércio de rua de Bauru acontece desde antes de existir internet na
          cidade. O arco aparece nos títulos e nas fotos por isso.
        </p>

        <div className="quadro p-5 bg-selo-claro">
          <h3 className="font-display text-xl font-bold text-tinta">Precisa de ajuda?</h3>
          <p className="mt-2">
            A Casa do Empreendedor atende de segunda a sexta, das 8h às 17h, na Av. Duque de
            Caxias, 16-55, Vila Cardia. Telefone (14) 3227-7819. Se você tem um negócio e não
            consegue se cadastrar sozinho, é lá que ajudam a fazer.
          </p>
          <Link to="/cadastrar" className="botao botao-principal mt-4">
            Cadastrar meu negócio
          </Link>
        </div>
      </div>
    </div>
  );
}

export function NaoEncontrada() {
  return (
    <div className="max-w-2xl mx-auto px-4 py-20 text-center">
      <p className="font-display text-6xl font-bold text-terracota">404</p>
      <h1 className="text-3xl mt-2">Esta página não existe</h1>
      <p className="text-tinta-suave mt-2">
        Pode ser um link antigo, ou uma loja que saiu da plataforma.
      </p>
      <Link to="/" className="botao botao-principal mt-6">
        Voltar para a vitrine
      </Link>
    </div>
  );
}
