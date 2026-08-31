package br.com.vitrinebauru.cadastro.api;

import br.com.vitrinebauru.cadastro.aplicacao.Autenticar;
import br.com.vitrinebauru.cadastro.aplicacao.EncerrarSessao;
import br.com.vitrinebauru.cadastro.aplicacao.EntrarComoDemonstracao;
import br.com.vitrinebauru.cadastro.aplicacao.RenovarSessao;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cadastro/auth")
@Tag(name = "Autenticação", description = "Entrar, renovar e sair")
public class AutenticacaoController {

    private final Autenticar autenticar;
    private final RenovarSessao renovarSessao;
    private final EncerrarSessao encerrarSessao;
    private final EntrarComoDemonstracao entrarComoDemonstracao;
    private final UsuarioRepository usuarios;

    public AutenticacaoController(Autenticar autenticar, RenovarSessao renovarSessao,
                                  EncerrarSessao encerrarSessao,
                                  EntrarComoDemonstracao entrarComoDemonstracao,
                                  UsuarioRepository usuarios) {
        this.autenticar = autenticar;
        this.renovarSessao = renovarSessao;
        this.encerrarSessao = encerrarSessao;
        this.entrarComoDemonstracao = entrarComoDemonstracao;
        this.usuarios = usuarios;
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar com e-mail e senha")
    public Respostas.Sessao entrar(@Valid @RequestBody Requisicoes.Login pedido) {
        var aberta = autenticar.executar(pedido.email(), pedido.senha());
        return montar(aberta);
    }

    @PostMapping("/renovar")
    @Operation(summary = "Trocar o token de renovação por um par novo")
    public Respostas.Sessao renovar(@Valid @RequestBody Requisicoes.Renovacao pedido) {
        return montar(renovarSessao.executar(pedido.tokenDeRenovacao()));
    }

    @PostMapping("/sair")
    @Operation(summary = "Encerrar a sessão")
    public ResponseEntity<Void> sair(@RequestBody(required = false) Requisicoes.Renovacao pedido) {
        encerrarSessao.executar(pedido == null ? null : pedido.tokenDeRenovacao());
        return ResponseEntity.noContent().build();
    }

    /**
     * Entrada da demonstracao. Devolve 404 quando o modo esta desligado, o que
     * e o comportamento do endereco inexistente que ele de fato e naquele
     * ambiente.
     */
    @PostMapping("/demonstracao")
    @Operation(summary = "Entrar na demonstração sem senha")
    public Respostas.Sessao demonstracao(@Valid @RequestBody Requisicoes.Demonstracao pedido) {
        return montar(entrarComoDemonstracao.executar(pedido.papel()));
    }

    @GetMapping("/demonstracao")
    @Operation(summary = "Saber se a demonstração está ligada neste ambiente")
    public Map<String, Object> situacaoDaDemonstracao() {
        return Map.of("ativa", entrarComoDemonstracao.ativa());
    }

    @GetMapping("/eu")
    @Operation(summary = "Dados de quem está logado")
    public Respostas.UsuarioLogado eu(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        if (autenticado == null) {
            throw new ErrosDeNegocio.NaoAutenticado("Faça login para continuar.");
        }
        var usuario = usuarios.findById(autenticado.id()).orElseThrow(() ->
                new ErrosDeNegocio.NaoAutenticado("Sua conta não existe mais."));

        return new Respostas.UsuarioLogado(usuario.id(), usuario.nome(), usuario.email(),
                usuario.papel().name(), autenticado.empreendedorId());
    }

    private Respostas.Sessao montar(br.com.vitrinebauru.cadastro.aplicacao.Sessoes.Aberta aberta) {
        String nome = usuarios.findById(aberta.usuario().id())
                .map(usuario -> usuario.nome())
                .orElse("");
        return Respostas.Sessao.de(aberta, nome);
    }
}
