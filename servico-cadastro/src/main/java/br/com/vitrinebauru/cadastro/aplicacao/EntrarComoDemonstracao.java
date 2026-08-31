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
 * Entrada em um clique na demonstração.
 *
 * <p>Existe para quem vai avaliar o sistema (a professora, a SEDECON, alguém
 * lendo o portfólio) não precisar inventar cadastro para ver como é por
 * dentro. São dois papéis, e a diferença entre eles é o ponto: um vê a própria
 * loja, o outro vê a fila de moderação da cidade inteira.
 *
 * <p>Fora do modo demonstração, este caminho não existe. Responde 404, e não
 * 403: quem procura por endereço de entrada sem senha não precisa saber que
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
