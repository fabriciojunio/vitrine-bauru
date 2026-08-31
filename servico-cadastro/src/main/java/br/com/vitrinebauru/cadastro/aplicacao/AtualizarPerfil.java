package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.contratos.PerfilAtualizado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.tipos.Cep;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.texto.Sanitizador;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * O empreendedor mexendo na propria vitrine.
 *
 * <p>Alterar o perfil nao devolve o cadastro para a fila de moderacao. E uma
 * decisao consciente: mandar de volta para analise a cada correcao de horario
 * de funcionamento tiraria a loja do ar por dias e ensinaria o empreendedor a
 * nunca mais atualizar nada. O que protege contra abuso e a auditoria, que
 * guarda toda alteracao, e a suspensao, que e imediata.
 *
 * <p>Trocar o nome do negocio nao muda o endereco da loja. Quem ja imprimiu o
 * link no cartao de visita continua com um link que funciona.
 */
@Component
public class AtualizarPerfil {

    private final EmpreendedorRepository empreendedores;
    private final RegistroDeSaida outbox;
    private final Sanitizador sanitizador;
    private final Auditor auditor;
    private final Clock relogio;

    public AtualizarPerfil(EmpreendedorRepository empreendedores, RegistroDeSaida outbox,
                           Sanitizador sanitizador, Auditor auditor, Clock relogio) {
        this.empreendedores = empreendedores;
        this.outbox = outbox;
        this.sanitizador = sanitizador;
        this.auditor = auditor;
        this.relogio = relogio;
    }

    @Transactional
    public Empreendedor executar(UUID empreendedorId, UUID autor, Pedido pedido) {
        var empreendedor = empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Cadastro não encontrado."));

        Telefone telefone = Telefone.de(pedido.telefoneWhatsapp());
        if (!telefone.ehCelular()) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Informe um celular com WhatsApp. É por ele que o cliente vai falar com você.");
        }

        String cep = pedido.cep() == null || pedido.cep().isBlank()
                ? null
                : Cep.de(pedido.cep()).valor();

        var agora = relogio.instant();
        empreendedor.atualizarPerfil(
                sanitizador.limpar(pedido.nomeDoNegocio()),
                sanitizador.limpar(pedido.descricao()),
                pedido.categoriaPrincipal(),
                pedido.bairro(),
                cep,
                telefone,
                agora);

        publicarPerfil(empreendedor, agora);
        auditor.registrar(autor, "perfil_atualizado", "empreendedor", empreendedor.id(),
                empreendedor.nomeDoNegocio());
        return empreendedor;
    }

    @Transactional
    public Empreendedor trocarFotoDeCapa(UUID empreendedorId, UUID autor, String url) {
        var empreendedor = empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Cadastro não encontrado."));

        var agora = relogio.instant();
        empreendedor.trocarFotoDeCapa(url, agora);

        publicarPerfil(empreendedor, agora);
        auditor.registrar(autor, "foto_de_capa_trocada", "empreendedor", empreendedor.id(), url);
        return empreendedor;
    }

    /**
     * Um cadastro rejeitado que foi corrigido volta para a fila por decisao do
     * proprio empreendedor, e nao automaticamente ao salvar: salvar sem querer
     * reenviar acontece, e reenviar sem ter corrigido nada so faz a fila da
     * SEDECON crescer.
     */
    @Transactional
    public void reenviarParaAnalise(UUID empreendedorId, UUID autor) {
        var empreendedor = empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Cadastro não encontrado."));

        empreendedor.reenviarParaAnalise(relogio.instant());
        auditor.registrar(autor, "cadastro_reenviado", "empreendedor", empreendedor.id(), null);
    }

    private void publicarPerfil(Empreendedor empreendedor, java.time.Instant agora) {
        outbox.gravar(Topicos.EMPREENDEDORES, new PerfilAtualizado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedor.id(),
                empreendedor.nomeDoNegocio(),
                empreendedor.apelidoNaUrl(),
                empreendedor.descricao(),
                empreendedor.categoriaPrincipal(),
                empreendedor.bairro(),
                empreendedor.telefoneWhatsapp(),
                empreendedor.fotoDeCapaUrl()));
    }

    public record Pedido(
            String nomeDoNegocio,
            String descricao,
            String categoriaPrincipal,
            String bairro,
            String cep,
            String telefoneWhatsapp) {
    }
}
