package br.com.vitrinebauru.notificacoes.api;

import br.com.vitrinebauru.notificacoes.dominio.Notificacao;
import br.com.vitrinebauru.notificacoes.infraestrutura.envio.EnviadorDeEmail;
import br.com.vitrinebauru.notificacoes.infraestrutura.persistencia.NotificacaoRepository;
import br.com.vitrinebauru.plataforma.web.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Consulta do que foi enviado, só para a SEDECON.
 *
 * <p>Serve para responder "o empreendedor foi avisado?" sem abrir o banco. O
 * corpo do e-mail não vai nesta resposta: quem administra a plataforma precisa
 * saber que a mensagem saiu, e não ler a correspondência das pessoas.
 */
@RestController
@RequestMapping("/api/notificacoes")
@PreAuthorize("hasRole('ADMIN_SEDECON')")
@Tag(name = "Notificações", description = "Histórico de e-mails enviados")
public class NotificacaoController {

    private final NotificacaoRepository notificacoes;
    private final EnviadorDeEmail enviador;

    public NotificacaoController(NotificacaoRepository notificacoes, EnviadorDeEmail enviador) {
        this.notificacoes = notificacoes;
        this.enviador = enviador;
    }

    @GetMapping
    @Operation(summary = "Últimos e-mails, do mais novo para o mais antigo")
    public Pagina<Resposta> listar(@RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "30") int tamanho) {

        var paginacao = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return Pagina.de(notificacoes.findAllByOrderByCriadaEmDesc(paginacao), Resposta::de);
    }

    @GetMapping("/situacao")
    @Operation(summary = "Como o envio está configurado e quantos e-mails estão na fila")
    public Map<String, Object> situacao() {
        return Map.of(
                "envio", enviador.descricao(),
                "naFila", notificacoes.countByEnviadaEmIsNull());
    }

    public record Resposta(UUID id, UUID empreendedorId, String tipo, String assunto,
                           String destinatario, Instant criadaEm, Instant enviadaEm,
                           int tentativas, String ultimoErro) {

        static Resposta de(Notificacao notificacao) {
            return new Resposta(
                    notificacao.id(),
                    notificacao.empreendedorId(),
                    notificacao.tipo().name(),
                    notificacao.assunto(),
                    mascarar(notificacao.destinatario()),
                    notificacao.criadaEm(),
                    notificacao.enviadaEm(),
                    notificacao.tentativas(),
                    notificacao.ultimoErro());
        }

        private static String mascarar(String email) {
            int arroba = email.indexOf('@');
            return arroba <= 1 ? "***" : email.charAt(0) + "***" + email.substring(arroba);
        }
    }
}
