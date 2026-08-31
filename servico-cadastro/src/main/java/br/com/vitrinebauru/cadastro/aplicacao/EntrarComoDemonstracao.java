package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.infraestrutura.config.PropriedadesDaDemonstracao;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.demonstracao.DadosDaDemonstracao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Entrada em um clique na demonstracao.
 *
 * <p>Existe para quem vai avaliar o sistema (a professora, a SEDECON, alguem
 * lendo o portfolio) nao precisar inventar cadastro para ver como e por
 * dentro. Sao dois papeis, e a diferenca entre eles e o ponto: um ve a propria
 * loja, o outro ve a fila de moderacao da cidade inteira.
 *
 * <p>Fora do modo demonstracao, este caminho nao existe. Responde 404, e nao
 * 403: quem procura por endereco de entrada sem senha nao precisa saber que
 * ele existiu algum dia.
 */
@Component
public class EntrarComoDemonstracao {

    public static final String PAPEL_EMPREENDEDOR = "empreendedor";
    public static final String PAPEL_SEDECON = "sedecon";

    private final UsuarioRepository usuarios;
    private final EmpreendedorRepository empreendedores;
    private final Sessoes sessoes;
    private final Auditor auditor;
    private final PropriedadesDaDemonstracao propriedades;

    public EntrarComoDemonstracao(UsuarioRepository usuarios, EmpreendedorRepository empreendedores,
                                  Sessoes sessoes, Auditor auditor,
                                  PropriedadesDaDemonstracao propriedades) {
        this.usuarios = usuarios;
        this.empreendedores = empreendedores;
        this.sessoes = sessoes;
        this.auditor = auditor;
        this.propriedades = propriedades;
    }

    public boolean ativa() {
        return propriedades.ativo();
    }

    @Transactional
    public Sessoes.Aberta executar(String papel) {
        if (!propriedades.ativo()) {
            throw new ErrosDeNegocio.NaoEncontrado("Este endereço não existe.");
        }

        String email = switch (papel == null ? "" : papel.toLowerCase()) {
            case PAPEL_SEDECON -> DadosDaDemonstracao.ADMIN_EMAIL;
            case PAPEL_EMPREENDEDOR -> DadosDaDemonstracao.lojas().getFirst().email();
            default -> throw new ErrosDeNegocio.RegraDeNegocio(
                    "Escolha entre entrar como empreendedor ou como SEDECON.");
        };

        var usuario = usuarios.findByEmail(email).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado(
                        "A demonstração ainda não foi preparada. Tente de novo em instantes."));

        UUID empreendedorId = empreendedores.findByUsuarioId(usuario.id())
                .map(empreendedor -> empreendedor.id())
                .orElse(null);

        auditor.registrar(usuario.id(), "entrada_na_demonstracao", "usuario", usuario.id(), papel);
        return sessoes.abrir(usuario, empreendedorId);
    }
}
