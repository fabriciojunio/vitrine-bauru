package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.SessaoDeRenovacao;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.SessaoDeRenovacaoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Troca um token de renovação por um par novo.
 *
 * <p>O token antigo é queimado na troca. Isso cria uma detecção barata de
 * roubo de sessão: se o token queimado voltar a aparecer, ou alguém copiou a
 * sessão, ou a cópia legítima ficou para trás. Nos dois casos o certo é
 * derrubar todas as sessões do usuário e obrigar login de novo, porque manter
 * a sessão viva na dúvida beneficia quem roubou.
 */
@Component
public class RenovarSessao {

    private static final Logger log = LoggerFactory.getLogger(RenovarSessao.class);

    private static final String MENSAGEM_GENERICA = "Sua sessão expirou. Entre de novo.";

    private final SessaoDeRenovacaoRepository sessoesSalvas;
    private final UsuarioRepository usuarios;
    private final EmpreendedorRepository empreendedores;
    private final Sessoes sessoes;
    private final RegistroDeSeguranca registroDeSeguranca;
    private final Clock relogio;

    public RenovarSessao(SessaoDeRenovacaoRepository sessoesSalvas, UsuarioRepository usuarios,
                         EmpreendedorRepository empreendedores, Sessoes sessoes,
                         RegistroDeSeguranca registroDeSeguranca, Clock relogio) {
        this.sessoesSalvas = sessoesSalvas;
        this.usuarios = usuarios;
        this.empreendedores = empreendedores;
        this.sessoes = sessoes;
        this.registroDeSeguranca = registroDeSeguranca;
        this.relogio = relogio;
    }

    @Transactional
    public Sessoes.Aberta executar(String tokenDeRenovacao) {
        if (tokenDeRenovacao == null || tokenDeRenovacao.isBlank()) {
            throw new ErrosDeNegocio.NaoAutenticado(MENSAGEM_GENERICA);
        }

        SessaoDeRenovacao sessao = sessoesSalvas.findByHashDoToken(sessoes.resumo(tokenDeRenovacao))
                .orElseThrow(() -> new ErrosDeNegocio.NaoAutenticado(MENSAGEM_GENERICA));

        var agora = relogio.instant();

        if (sessao.jaFoiUsada()) {
            // Transação própria, senão a exceção lançada em seguida desfaz a
            // revogação e a sessão roubada continua valendo.
            int derrubadas = registroDeSeguranca.derrubarTodasAsSessoes(sessao.usuarioId(), sessao.id());
            log.warn("Token de renovacao reutilizado pelo usuario {}. {} sessoes revogadas.",
                    sessao.usuarioId(), derrubadas);
            throw new ErrosDeNegocio.NaoAutenticado(
                    "Detectamos uso indevido da sua sessão. Por segurança, entre de novo.");
        }

        if (!sessao.estaValida(agora)) {
            throw new ErrosDeNegocio.NaoAutenticado(MENSAGEM_GENERICA);
        }

        var usuario = usuarios.findById(sessao.usuarioId())
                .orElseThrow(() -> new ErrosDeNegocio.NaoAutenticado(MENSAGEM_GENERICA));
        usuario.exigirAtiva();

        UUID empreendedorId = empreendedores.findByUsuarioId(usuario.id())
                .map(empreendedor -> empreendedor.id())
                .orElse(null);

        Sessoes.Aberta nova = sessoes.abrir(usuario, empreendedorId);
        sessao.usar(nova.sessaoId(), agora);

        return nova;
    }
}
