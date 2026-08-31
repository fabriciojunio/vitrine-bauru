package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro;
import br.com.vitrinebauru.cadastro.infraestrutura.externo.ConsultaDeDocumento;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.contratos.tipos.Documento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Confere na Receita os CNPJ que entraram na fila, em segundo plano.
 *
 * <p>Fora do caminho do cadastro de proposito. Se a consulta acontecesse
 * durante o cadastro, o empreendedor com internet fraca ficaria olhando um
 * botao girando por causa de uma API de terceiro, e um timeout viraria
 * "não consegui me cadastrar".
 *
 * <p>Tarefa agendada, e nao chamada assincrona: assincrona morre junto com o
 * processo e ninguem fica sabendo. A tarefa pega de onde parou depois de
 * qualquer reinicio, porque o que define o que falta e o estado no banco.
 */
@Component
public class ConferirDocumentos {

    private static final Logger log = LoggerFactory.getLogger(ConferirDocumentos.class);
    private static final int POR_RODADA = 5;

    private final EmpreendedorRepository empreendedores;
    private final ConsultaDeDocumento consulta;
    private final Clock relogio;

    public ConferirDocumentos(EmpreendedorRepository empreendedores,
                              ConsultaDeDocumento consulta, Clock relogio) {
        this.empreendedores = empreendedores;
        this.consulta = consulta;
        this.relogio = relogio;
    }

    @Scheduled(fixedDelayString = "${vitrine.conferencia.intervalo-ms:30000}")
    @Transactional
    public void conferirPendentes() {
        var pendentes = empreendedores
                .findByStatusOrderByCriadoEmAsc(StatusDoCadastro.PENDENTE, PageRequest.of(0, POR_RODADA))
                .getContent();

        for (Empreendedor empreendedor : pendentes) {
            if (empreendedor.documentoConferidoEm() != null) {
                continue;
            }
            conferir(empreendedor);
        }
    }

    private void conferir(Empreendedor empreendedor) {
        var documento = new Documento(empreendedor.documento(), empreendedor.documentoTipo());

        if (!documento.ehCnpj()) {
            // CPF nao tem consulta publica de situacao, e nem deveria ter.
            // Marcar como conferido evita a fila tentar de novo para sempre.
            empreendedor.anotarConferenciaDoDocumento(
                    "CPF: sem consulta automática, conferir na análise", relogio.instant());
            return;
        }

        consulta.consultar(documento).ifPresent(situacao -> {
            String resumo = situacao.situacaoCadastral()
                    + (situacao.razaoSocial() == null ? "" : " (" + situacao.razaoSocial() + ")");
            empreendedor.anotarConferenciaDoDocumento(resumo, relogio.instant());
            log.info("CNPJ do empreendedor {} conferido: {}", empreendedor.id(), resumo);
        });
    }
}
