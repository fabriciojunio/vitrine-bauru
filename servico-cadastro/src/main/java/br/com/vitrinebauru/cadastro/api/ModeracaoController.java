package br.com.vitrinebauru.cadastro.api;

import br.com.vitrinebauru.cadastro.aplicacao.ConsultarIndicadores;
import br.com.vitrinebauru.cadastro.aplicacao.ModerarCadastro;
import br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.AuditoriaRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import br.com.vitrinebauru.plataforma.web.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * O lado da SEDECON.
 *
 * <p>Tudo aqui exige o papel de administrador, declarado endereço por
 * endereço. A configuração de segurança já protege o caminho inteiro; a
 * anotação repete a regra de propósito, porque uma mudança futura no mapa de
 * rotas não pode abrir a moderação sem que alguém tenha que apagar uma linha
 * dizendo explicitamente quem pode entrar.
 */
@RestController
@RequestMapping("/api/cadastro/moderacao")
@PreAuthorize("hasRole('ADMIN_SEDECON')")
@Tag(name = "Moderação", description = "Fila de análise e painel da SEDECON")
public class ModeracaoController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 50;

    private final ModerarCadastro moderarCadastro;
    private final ConsultarIndicadores indicadores;
    private final EmpreendedorRepository empreendedores;
    private final AuditoriaRepository auditoria;
    private final Clock relogio;

    public ModeracaoController(ModerarCadastro moderarCadastro, ConsultarIndicadores indicadores,
                               EmpreendedorRepository empreendedores, AuditoriaRepository auditoria,
                               Clock relogio) {
        this.moderarCadastro = moderarCadastro;
        this.indicadores = indicadores;
        this.empreendedores = empreendedores;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @GetMapping("/fila")
    @Operation(summary = "Cadastros esperando análise, do mais antigo para o mais novo")
    public Pagina<Respostas.CadastroParaAnalise> fila(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {

        var agora = relogio.instant();
        var paginacao = PageRequest.of(Math.max(pagina, 0), limitar(tamanho));

        return Pagina.de(empreendedores.filaDeModeracao(paginacao),
                empreendedor -> Respostas.CadastroParaAnalise.de(empreendedor, agora));
    }

    @GetMapping("/cadastros")
    @Operation(summary = "Cadastros por situação")
    public Pagina<Respostas.CadastroParaAnalise> porSituacao(
            @RequestParam String situacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {

        var agora = relogio.instant();
        var status = StatusDoCadastro.valueOf(situacao.toUpperCase());
        var paginacao = PageRequest.of(Math.max(pagina, 0), limitar(tamanho));

        return Pagina.de(empreendedores.findByStatusOrderByCriadoEmAsc(status, paginacao),
                empreendedor -> Respostas.CadastroParaAnalise.de(empreendedor, agora));
    }

    @PostMapping("/{empreendedorId}/aprovar")
    @Operation(summary = "Aprovar o cadastro e colocar a loja no ar")
    public ResponseEntity<Void> aprovar(@PathVariable UUID empreendedorId,
                                        @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        moderarCadastro.aprovar(empreendedorId, autenticado.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{empreendedorId}/rejeitar")
    @Operation(summary = "Recusar o cadastro, com motivo que vai por e-mail")
    public ResponseEntity<Void> rejeitar(@PathVariable UUID empreendedorId,
                                         @Valid @RequestBody Requisicoes.Motivo pedido,
                                         @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        moderarCadastro.rejeitar(empreendedorId, autenticado.id(), pedido.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{empreendedorId}/suspender")
    @Operation(summary = "Tirar a loja do ar")
    public ResponseEntity<Void> suspender(@PathVariable UUID empreendedorId,
                                          @Valid @RequestBody Requisicoes.Motivo pedido,
                                          @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        moderarCadastro.suspender(empreendedorId, autenticado.id(), pedido.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{empreendedorId}/reativar")
    @Operation(summary = "Devolver ao ar uma loja suspensa")
    public ResponseEntity<Void> reativar(@PathVariable UUID empreendedorId,
                                         @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        moderarCadastro.reativar(empreendedorId, autenticado.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Painel de impacto e engajamento")
    public ConsultarIndicadores.Painel painel() {
        return indicadores.executar();
    }

    @GetMapping("/auditoria")
    @Operation(summary = "Histórico de quem fez o quê")
    public Pagina<Map<String, Object>> historico(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "30") int tamanho) {

        var paginacao = PageRequest.of(Math.max(pagina, 0), limitar(tamanho));

        return Pagina.de(auditoria.findAllByOrderByOcorridoEmDesc(paginacao), registro -> {
            var linha = new java.util.LinkedHashMap<String, Object>();
            linha.put("acao", registro.acao());
            linha.put("entidade", registro.entidade());
            linha.put("entidadeId", registro.entidadeId());
            linha.put("detalhe", registro.detalhe());
            linha.put("quando", registro.ocorridoEm());
            return linha;
        });
    }

    /** Teto de página: sem isso, {@code ?tamanho=100000} vira negação de serviço gratuita. */
    private int limitar(int tamanho) {
        return Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_DA_PAGINA);
    }
}
