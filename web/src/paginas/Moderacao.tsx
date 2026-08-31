import { useCallback, useEffect, useState } from 'react';
import { chamar, ErroDaApi } from '@/lib/api';
import type { CadastroParaAnalise, Pagina } from '@/lib/tipos';
import { formatarData, tempoDesde } from '@/lib/formato';
import { Aviso, Botao, Carregando, TituloDeSecao } from '@/componentes/Basicos';

/**
 * A fila de moderação da SEDECON.
 *
 * <p>Esta tela é a razão de o sistema ter moderação, e por isso ela mostra
 * quanto tempo cada cadastro está esperando: sem esse número, a fila é
 * atendida por quem aparece na frente, e quem se cadastrou primeiro espera
 * para sempre.
 *
 * <p>A recusa exige motivo escrito porque o motivo vai inteiro no e-mail do
 * empreendedor. Recusar sem explicar é a forma mais rápida de perder alguém que
 * estava disposto a se formalizar.
 */
export function Moderacao() {
  const [fila, definirFila] = useState<Pagina<CadastroParaAnalise> | null>(null);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState<string | null>(null);
  const [recusando, definirRecusando] = useState<string | null>(null);
  const [motivo, definirMotivo] = useState('');
  const [emAcao, definirEmAcao] = useState<string | null>(null);

  const recarregar = useCallback(() => {
    definirCarregando(true);
    chamar<Pagina<CadastroParaAnalise>>('/api/cadastro/moderacao/fila?tamanho=30')
      .then(definirFila)
      .catch((falha: ErroDaApi) => definirErro(falha.message))
      .finally(() => definirCarregando(false));
  }, []);

  useEffect(recarregar, [recarregar]);

  const aprovar = async (cadastro: CadastroParaAnalise) => {
    definirEmAcao(cadastro.id);
    try {
      await chamar(`/api/cadastro/moderacao/${cadastro.id}/aprovar`, { metodo: 'POST' });
      recarregar();
    } catch (falha) {
      definirErro(falha instanceof ErroDaApi ? falha.message : 'Não foi possível aprovar.');
    } finally {
      definirEmAcao(null);
    }
  };

  const rejeitar = async (cadastro: CadastroParaAnalise) => {
    definirEmAcao(cadastro.id);
    try {
      await chamar(`/api/cadastro/moderacao/${cadastro.id}/rejeitar`, {
        metodo: 'POST',
        corpo: { motivo },
      });
      definirRecusando(null);
      definirMotivo('');
      recarregar();
    } catch (falha) {
      definirErro(falha instanceof ErroDaApi ? falha.message : 'Não foi possível recusar.');
    } finally {
      definirEmAcao(null);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <TituloDeSecao descricao="Cadastros esperando análise, do mais antigo para o mais novo.">
        Fila de moderação
      </TituloDeSecao>

      {erro && (
        <Aviso tipo="erro" className="mb-5">
          {erro}
        </Aviso>
      )}

      {carregando && <Carregando />}

      {!carregando && fila?.conteudo.length === 0 && (
        <Aviso tipo="certo" titulo="Nenhum cadastro esperando">
          A fila está zerada. Assim que alguém se cadastrar, o pedido aparece aqui.
        </Aviso>
      )}

      <div className="flex flex-col gap-5">
        {fila?.conteudo.map((cadastro) => (
          <article key={cadastro.id} className="quadro placa-leve p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h3 className="text-xl">{cadastro.nomeDoNegocio}</h3>
                <p className="text-sm text-concreto">
                  {cadastro.categoriaPrincipal} · {cadastro.bairro}
                </p>
              </div>
              <p
                className={`selo-categoria ${
                  cadastro.diasNaFila >= 7 ? 'bg-sinal' : 'bg-faixa'
                }`}
              >
                esperando {tempoDesde(cadastro.cadastradoEm)}
              </p>
            </div>

            {cadastro.descricao && <p className="mt-3 text-tinta-suave">{cadastro.descricao}</p>}

            <dl className="mt-4 grid gap-3 sm:grid-cols-2 text-sm">
              <div>
                <dt className="font-semibold">Documento</dt>
                <dd className="text-tinta-suave">
                  {cadastro.documento} ({cadastro.tipoDoDocumento})
                </dd>
              </div>
              <div>
                <dt className="font-semibold">Situação na Receita</dt>
                <dd className="text-tinta-suave">
                  {cadastro.situacaoDoDocumento ?? 'Consulta ainda não concluída'}
                </dd>
              </div>
              <div>
                <dt className="font-semibold">WhatsApp</dt>
                <dd className="text-tinta-suave">{cadastro.telefoneWhatsapp}</dd>
              </div>
              <div>
                <dt className="font-semibold">Cadastrado em</dt>
                <dd className="text-tinta-suave">{formatarData(cadastro.cadastradoEm)}</dd>
              </div>
            </dl>

            {recusando === cadastro.id ? (
              <div className="mt-4 flex flex-col gap-3">
                <label className="etiqueta" htmlFor={`motivo-${cadastro.id}`}>
                  Motivo da recusa (vai no e-mail do empreendedor)
                </label>
                <textarea
                  id={`motivo-${cadastro.id}`}
                  className="campo"
                  rows={3}
                  minLength={10}
                  maxLength={400}
                  value={motivo}
                  onChange={(evento) => definirMotivo(evento.target.value)}
                  placeholder="Explique o que precisa ser corrigido para o cadastro ser aprovado."
                />
                <div className="flex gap-3">
                  <Botao
                    variante="principal"
                    disabled={motivo.trim().length < 10}
                    carregando={emAcao === cadastro.id}
                    onClick={() => rejeitar(cadastro)}
                  >
                    Confirmar recusa
                  </Botao>
                  <Botao
                    variante="neutro"
                    onClick={() => {
                      definirRecusando(null);
                      definirMotivo('');
                    }}
                  >
                    Cancelar
                  </Botao>
                </div>
              </div>
            ) : (
              <div className="mt-4 flex flex-wrap gap-3">
                <Botao
                  variante="selo"
                  carregando={emAcao === cadastro.id}
                  onClick={() => aprovar(cadastro)}
                >
                  Aprovar e colocar no ar
                </Botao>
                <Botao variante="neutro" onClick={() => definirRecusando(cadastro.id)}>
                  Recusar com motivo
                </Botao>
              </div>
            )}
          </article>
        ))}
      </div>
    </div>
  );
}
