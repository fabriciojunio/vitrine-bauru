package br.com.vitrinebauru.cadastro.api;

import br.com.vitrinebauru.cadastro.aplicacao.ExportarDadosPessoais;
import br.com.vitrinebauru.cadastro.aplicacao.SolicitarExclusao;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Os direitos do titular, com endereço próprio.
 *
 * <p>Estar na API, e não só num texto de política de privacidade, é a
 * diferença entre cumprir a LGPD e dizer que cumpre. São dois direitos aqui:
 * ver o que a plataforma guarda e mandar apagar.
 */
@RestController
@RequestMapping("/api/cadastro/privacidade")
@Tag(name = "Privacidade", description = "Direitos do titular dos dados (LGPD)")
public class PrivacidadeController {

    private final ExportarDadosPessoais exportar;
    private final SolicitarExclusao solicitarExclusao;

    public PrivacidadeController(ExportarDadosPessoais exportar, SolicitarExclusao solicitarExclusao) {
        this.exportar = exportar;
        this.solicitarExclusao = solicitarExclusao;
    }

    @GetMapping("/meus-dados")
    @Operation(summary = "Baixar tudo que a plataforma guarda sobre você")
    public ResponseEntity<ExportarDadosPessoais.Arquivo> meusDados(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        var arquivo = exportar.executar(autenticado.id());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("meus-dados-vitrine-bauru.json").build().toString())
                .body(arquivo);
    }

    @DeleteMapping("/minha-conta")
    @Operation(summary = "Pedir a exclusão da conta e de todos os dados")
    public SolicitarExclusao.Recibo excluir(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        if (autenticado.empreendedorId() == null) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Contas da SEDECON são excluídas pela administração, e não por aqui.");
        }
        return solicitarExclusao.executar(autenticado.empreendedorId(), autenticado.id());
    }
}
