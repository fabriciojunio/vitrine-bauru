import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { chamar, ErroDaApi } from '@/lib/api';
import type { IndicadoresDoEmpreendedor, MinhaLoja } from '@/lib/tipos';
import { mascararCep, mascararTelefone } from '@/lib/formato';
import {
  AreaDeTexto,
  Aviso,
  Botao,
  Campo,
  Carregando,
  Selecao,
  TituloDeSecao,
} from '@/componentes/Basicos';

/**
 * O painel do empreendedor.
 *
 * <p>A primeira coisa da tela é a situação do cadastro, e não um gráfico. Quem
 * entra aqui tem uma pergunta só na cabeça: "minha loja já está no ar?". Se a
 * resposta for não, a segunda pergunta é "o que falta?", e é isso que o bloco
 * de situação responde, inclusive com o motivo escrito pela análise quando o
 * cadastro foi recusado.
 */
export function Painel() {
  const [parametros] = useSearchParams();
  const [loja, definirLoja] = useState<MinhaLoja | null>(null);
  const [numeros, definirNumeros] = useState<IndicadoresDoEmpreendedor | null>(null);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState<string | null>(null);
  const [editando, definirEditando] = useState(false);

  const recarregar = useCallback(() => {
    definirCarregando(true);
    Promise.all([
      chamar<MinhaLoja>('/api/cadastro/minha-loja'),
      chamar<IndicadoresDoEmpreendedor>('/api/cadastro/minha-loja/indicadores').catch(() => null),
    ])
      .then(([minhaLoja, indicadores]) => {
        definirLoja(minhaLoja);
        definirNumeros(indicadores);
      })
      .catch((falha: ErroDaApi) => definirErro(falha.message))
      .finally(() => definirCarregando(false));
  }, []);

  useEffect(recarregar, [recarregar]);

  if (carregando) {
    return <Carregando />;
  }

  if (erro || !loja) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <Aviso tipo="erro" titulo="Não deu para abrir o seu painel">
          {erro ?? 'Tente entrar de novo.'}
        </Aviso>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {parametros.get('novo') === '1' && (
        <Aviso tipo="certo" titulo="Cadastro enviado" className="mb-6">
          Agora é com a SEDECON: eles conferem os dados e avisam por e-mail. Enquanto isso, você
          já pode cadastrar seus produtos, para a loja estrear com o catálogo pronto.
        </Aviso>
      )}

      <SituacaoDoCadastro loja={loja} aoReenviar={recarregar} />

      <div className="grid gap-5 sm:grid-cols-3 my-8">
        <Numero titulo="Produtos no catálogo" valor={numeros?.produtos ?? 0} />
        <Numero titulo="Contatos recebidos" valor={numeros?.contatosNoTotal ?? 0} />
        <Numero titulo="Contatos nos últimos 30 dias" valor={numeros?.contatosNosUltimos30Dias ?? 0} />
      </div>

      <div className="flex flex-wrap gap-3 mb-10">
        <Link to="/painel/produtos" className="botao botao-principal">
          Cuidar dos meus produtos
        </Link>
        {loja.apareceNaVitrine && (
          <Link to={`/loja/${loja.apelidoNaUrl}`} className="botao botao-neutro">
            Ver minha loja como o cliente vê
          </Link>
        )}
        <Link to="/privacidade" className="botao botao-texto">
          Meus dados e privacidade
        </Link>
      </div>

      <TituloDeSecao descricao="Estes dados aparecem na sua página pública.">
        Dados da loja
      </TituloDeSecao>

      {editando ? (
        <FormularioDoPerfil
          loja={loja}
          aoSalvar={(atualizada) => {
            definirLoja(atualizada);
            definirEditando(false);
          }}
          aoCancelar={() => definirEditando(false)}
        />
      ) : (
        <div className="quadro p-5">
          <dl className="grid gap-4 sm:grid-cols-2">
            <Linha titulo="Nome do negócio" valor={loja.nomeDoNegocio} />
            <Linha titulo="Categoria" valor={loja.categoriaPrincipal} />
            <Linha titulo="Bairro" valor={loja.bairro} />
            <Linha titulo="WhatsApp" valor={loja.telefoneWhatsapp} />
            <Linha titulo="CPF ou CNPJ" valor={loja.documento} />
            <Linha titulo="Endereço da loja" valor={`/loja/${loja.apelidoNaUrl}`} />
            <div className="sm:col-span-2">
              <dt className="font-semibold text-sm">O que você faz</dt>
              <dd className="text-tinta-suave">{loja.descricao ?? 'Ainda não preenchido.'}</dd>
            </div>
          </dl>

          <Botao variante="neutro" className="mt-5" onClick={() => definirEditando(true)}>
            Alterar meus dados
          </Botao>
        </div>
      )}
    </div>
  );
}

function Linha({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <div>
      <dt className="font-semibold text-sm">{titulo}</dt>
      <dd className="text-tinta-suave">{valor}</dd>
    </div>
  );
}

function Numero({ titulo, valor }: { titulo: string; valor: number }) {
  return (
    <div className="quadro placa-leve p-4">
      <p className="font-display text-4xl font-bold">{valor}</p>
      <p className="text-sm text-tinta-suave mt-1">{titulo}</p>
    </div>
  );
}

function SituacaoDoCadastro({ loja, aoReenviar }: { loja: MinhaLoja; aoReenviar(): void }) {
  const [enviando, definirEnviando] = useState(false);

  if (loja.situacao === 'APROVADO') {
    return (
      <Aviso tipo="certo" titulo="Sua loja está no ar">
        Qualquer pessoa consegue encontrar seus produtos na busca. O endereço da sua loja é{' '}
        <strong>/loja/{loja.apelidoNaUrl}</strong>, e você pode mandar esse link direto para os
        seus clientes.
      </Aviso>
    );
  }

  if (loja.situacao === 'PENDENTE') {
    return (
      <Aviso tipo="atencao" titulo="Seu cadastro está na fila da SEDECON">
        A análise costuma levar alguns dias. Aproveite para cadastrar seus produtos e mandar as
        fotos: assim que for aprovado, sua loja aparece completa.
      </Aviso>
    );
  }

  if (loja.situacao === 'REJEITADO') {
    return (
      <Aviso tipo="erro" titulo="A SEDECON pediu uma correção">
        <p className="mb-2">{loja.motivoDaModeracao}</p>
        <p className="mb-3">
          Corrija o que foi apontado nos seus dados e envie de novo para análise. Se tiver dúvida,
          a Casa do Empreendedor atende de segunda a sexta, das 8h às 17h.
        </p>
        <Botao
          variante="neutro"
          carregando={enviando}
          onClick={async () => {
            definirEnviando(true);
            try {
              await chamar('/api/cadastro/minha-loja/reenviar', { metodo: 'POST' });
              aoReenviar();
            } finally {
              definirEnviando(false);
            }
          }}
        >
          Já corrigi, enviar de novo
        </Botao>
      </Aviso>
    );
  }

  if (loja.situacao === 'SUSPENSO') {
    return (
      <Aviso tipo="erro" titulo="Sua loja está suspensa">
        <p>{loja.motivoDaModeracao}</p>
        <p className="mt-2">
          Seus produtos continuam salvos. Para pedir revisão, procure a SEDECON na Casa do
          Empreendedor.
        </p>
      </Aviso>
    );
  }

  return (
    <Aviso tipo="atencao" titulo="Cadastro encerrado">
      Este cadastro foi excluído a pedido do titular.
    </Aviso>
  );
}

function FormularioDoPerfil({
  loja,
  aoSalvar,
  aoCancelar,
}: {
  loja: MinhaLoja;
  aoSalvar(atualizada: MinhaLoja): void;
  aoCancelar(): void;
}) {
  const [bairros, definirBairros] = useState<string[]>([]);
  const [categorias, definirCategorias] = useState<string[]>([]);
  const [campos, definirCampos] = useState({
    nomeDoNegocio: loja.nomeDoNegocio,
    descricao: loja.descricao ?? '',
    categoriaPrincipal: loja.categoriaPrincipal,
    bairro: loja.bairro,
    cep: loja.cep ?? '',
    telefoneWhatsapp: loja.telefoneWhatsapp,
  });
  const [erros, definirErros] = useState<Record<string, string>>({});
  const [erroGeral, definirErroGeral] = useState<string | null>(null);
  const [salvando, definirSalvando] = useState(false);

  useEffect(() => {
    chamar<string[]>('/api/cadastro/bairros').then(definirBairros).catch(() => undefined);
    chamar<string[]>('/api/cadastro/categorias').then(definirCategorias).catch(() => undefined);
  }, []);

  const salvar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    definirErros({});
    definirErroGeral(null);
    definirSalvando(true);

    try {
      const atualizada = await chamar<MinhaLoja>('/api/cadastro/minha-loja', {
        metodo: 'PUT',
        corpo: {
          ...campos,
          telefoneWhatsapp: campos.telefoneWhatsapp.replace(/\D/g, ''),
          cep: campos.cep.replace(/\D/g, ''),
        },
      });
      aoSalvar(atualizada);
    } catch (falha) {
      if (falha instanceof ErroDaApi) {
        definirErros(falha.campos);
        definirErroGeral(Object.keys(falha.campos).length === 0 ? falha.message : null);
      }
    } finally {
      definirSalvando(false);
    }
  };

  return (
    <form className="quadro p-5 flex flex-col gap-4" onSubmit={salvar} noValidate>
      {erroGeral && <Aviso tipo="erro">{erroGeral}</Aviso>}

      <Campo
        etiqueta="Nome do negócio"
        value={campos.nomeDoNegocio}
        erro={erros.nomeDoNegocio}
        onChange={(evento) =>
          definirCampos((atual) => ({ ...atual, nomeDoNegocio: evento.target.value }))
        }
      />

      <AreaDeTexto
        etiqueta="O que você faz"
        rows={4}
        maxLength={600}
        value={campos.descricao}
        erro={erros.descricao}
        onChange={(evento) =>
          definirCampos((atual) => ({ ...atual, descricao: evento.target.value }))
        }
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Selecao
          etiqueta="Categoria"
          opcoes={categorias}
          value={campos.categoriaPrincipal}
          erro={erros.categoriaPrincipal}
          onChange={(evento) =>
            definirCampos((atual) => ({ ...atual, categoriaPrincipal: evento.target.value }))
          }
        />
        <Selecao
          etiqueta="Bairro"
          opcoes={bairros}
          value={campos.bairro}
          erro={erros.bairro}
          onChange={(evento) =>
            definirCampos((atual) => ({ ...atual, bairro: evento.target.value }))
          }
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Campo
          etiqueta="CEP"
          value={campos.cep}
          erro={erros.cep}
          inputMode="numeric"
          onChange={(evento) =>
            definirCampos((atual) => ({ ...atual, cep: mascararCep(evento.target.value) }))
          }
        />
        <Campo
          etiqueta="Celular com WhatsApp"
          value={campos.telefoneWhatsapp}
          erro={erros.telefoneWhatsapp}
          inputMode="tel"
          onChange={(evento) =>
            definirCampos((atual) => ({
              ...atual,
              telefoneWhatsapp: mascararTelefone(evento.target.value),
            }))
          }
        />
      </div>

      <div className="flex gap-3">
        <Botao type="submit" carregando={salvando}>
          Salvar
        </Botao>
        <Botao type="button" variante="neutro" onClick={aoCancelar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}
