package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.CadastroRejeitado;
import br.com.vitrinebauru.contratos.EmpreendedorReativado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.texto.Sanitizador;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * As quatro decisões que a SEDECON toma sobre um cadastro.
 *
 * <p>Ficam na mesma classe porque são o mesmo ato administrativo com quatro
 * resultados: carregar o cadastro, mudar o estado, gravar quem decidiu, contar
 * ao resto do sistema. Separar em quatro classes triplicaria essa moldura sem
 * separar nenhuma responsabilidade de verdade.
 *
 * <p>Toda decisão vira evento. É o evento que tira a loja do ar, manda o
 * e-mail e atualiza a busca; sem ele, o administrador clicaria em suspender,
 * veria a tela dizer que suspendeu, e a loja continuaria aparecendo para o
 * consumidor.
 */
@Component
public class ModerarCadastro {

    private final EmpreendedorRepository empreendedores;
    private final UsuarioRepository usuarios;
    private final RegistroDeSaida outbox;
    private final Sanitizador sanitizador;
    private final Auditor auditor;
    private final Clock relogio;

    public ModerarCadastro(EmpreendedorRepository empreendedores, UsuarioRepository usuarios,
                           RegistroDeSaida outbox, Sanitizador sanitizador,
                           Auditor auditor, Clock relogio) {
        this.empreendedores = empreendedores;
        this.usuarios = usuarios;
        this.outbox = outbox;
        this.sanitizador = sanitizador;
        this.auditor = auditor;
        this.relogio = relogio;
    }

    @Transactional
    public void aprovar(UUID empreendedorId, UUID moderador) {
        var empreendedor = carregar(empreendedorId);
        var dono = donoDe(empreendedor);
        var agora = relogio.instant();

        empreendedor.aprovar(moderador, agora);

        outbox.gravar(Topicos.EMPREENDEDORES, new CadastroAprovado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedor.id(), moderador, empreendedor.nomeDoNegocio(),
                dono.email(), dono.nome()));

        auditor.registrar(moderador, "cadastro_aprovado", "empreendedor", empreendedor.id(),
                empreendedor.nomeDoNegocio());
    }

    @Transactional
    public void rejeitar(UUID empreendedorId, UUID moderador, String motivo) {
        var empreendedor = carregar(empreendedorId);
        var dono = donoDe(empreendedor);
        var agora = relogio.instant();
        String motivoLimpo = sanitizador.limpar(motivo);

        empreendedor.rejeitar(moderador, motivoLimpo, agora);

        outbox.gravar(Topicos.EMPREENDEDORES, new CadastroRejeitado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedor.id(), moderador, empreendedor.motivoDaModeracao(),
                empreendedor.nomeDoNegocio(), dono.email(), dono.nome()));

        auditor.registrar(moderador, "cadastro_rejeitado", "empreendedor", empreendedor.id(),
                empreendedor.motivoDaModeracao());
    }

    @Transactional
    public void suspender(UUID empreendedorId, UUID moderador, String motivo) {
        var empreendedor = carregar(empreendedorId);
        var dono = donoDe(empreendedor);
        var agora = relogio.instant();
        String motivoLimpo = sanitizador.limpar(motivo);

        empreendedor.suspender(moderador, motivoLimpo, agora);

        outbox.gravar(Topicos.EMPREENDEDORES, new EmpreendedorSuspenso(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedor.id(), moderador, empreendedor.motivoDaModeracao(),
                empreendedor.nomeDoNegocio(), dono.email(), dono.nome()));

        auditor.registrar(moderador, "empreendedor_suspenso", "empreendedor", empreendedor.id(),
                empreendedor.motivoDaModeracao());
    }

    @Transactional
    public void reativar(UUID empreendedorId, UUID moderador) {
        var empreendedor = carregar(empreendedorId);
        var dono = donoDe(empreendedor);
        var agora = relogio.instant();

        empreendedor.reativar(moderador, agora);

        outbox.gravar(Topicos.EMPREENDEDORES, new EmpreendedorReativado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedor.id(), moderador, empreendedor.nomeDoNegocio(),
                dono.email(), dono.nome()));

        auditor.registrar(moderador, "empreendedor_reativado", "empreendedor", empreendedor.id(),
                empreendedor.nomeDoNegocio());
    }

    private Empreendedor carregar(UUID empreendedorId) {
        return empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Cadastro não encontrado."));
    }

    private Usuario donoDe(Empreendedor empreendedor) {
        return usuarios.findById(empreendedor.usuarioId()).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("A conta dona deste cadastro não existe mais."));
    }
}
