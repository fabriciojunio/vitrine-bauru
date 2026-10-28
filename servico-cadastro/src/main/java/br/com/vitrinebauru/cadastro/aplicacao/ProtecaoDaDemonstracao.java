package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.infraestrutura.config.PropriedadesDaDemonstracao;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guarda a loja em que o botão "entrar como empreendedor" abre a sessão.
 *
 * <p>Essa loja é de todo mundo que visita a demonstração ao mesmo tempo. Um
 * visitante que pede a exclusão dela, ou que entra como SEDECON e a suspende,
 * tira a entrada do ar para quem vier depois, até o reinício da madrugada.
 * As outras lojas semeadas e as cadastradas na hora continuam livres para
 * testar exclusão e suspensão.
 *
 * <p>Fora do modo demonstração não faz nada.
 */
@Component
public class ProtecaoDaDemonstracao {

    private final PropriedadesDaDemonstracao propriedades;
    private final UsuarioRepository usuarios;

    public ProtecaoDaDemonstracao(PropriedadesDaDemonstracao propriedades, UsuarioRepository usuarios) {
        this.propriedades = propriedades;
        this.usuarios = usuarios;
    }

    public void exigirQueNaoSejaALojaDeEntrada(UUID usuarioId) {
        if (!propriedades.ativo()) {
            return;
        }
        boolean lojaDeEntrada = usuarios.findById(usuarioId)
                .map(usuario -> usuario.email().equals(EntrarComoDemonstracao.emailDaLojaDeEntrada()))
                .orElse(false);
        if (lojaDeEntrada) {
            throw new ErrosDeNegocio.Proibido(
                    "Na demonstração, a loja de entrada não pode ser excluída nem suspensa. "
                            + "Cadastre uma loja nova para testar isso.");
        }
    }
}
