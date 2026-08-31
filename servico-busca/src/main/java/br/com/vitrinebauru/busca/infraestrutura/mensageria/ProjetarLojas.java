package br.com.vitrinebauru.busca.infraestrutura.mensageria;

import br.com.vitrinebauru.busca.dominio.LojaNaVitrine;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.LojaRepository;
import br.com.vitrinebauru.busca.infraestrutura.persistencia.ProdutoDaVitrineRepository;
import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.CadastroRejeitado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.EmpreendedorReativado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.PerfilAtualizado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Constrói a vitrine a partir dos eventos do cadastro.
 *
 * <p>É aqui que a moderação da SEDECON vira efeito visível: aprovado entra,
 * suspenso sai, perfil alterado atualiza. A loja é gravada já no cadastro,
 * invisível, para que a aprovação seja só trocar uma coluna, e não montar a
 * projeção inteira no momento em que o analista clica.
 *
 * <p>Quando a loja muda, os produtos dela precisam mudar junto: cada produto
 * carrega o nome da loja e o bairro para a busca funcionar numa consulta só.
 */
@Component
public class ProjetarLojas implements ConsumidorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(ProjetarLojas.class);

    private final LojaRepository lojas;
    private final ProdutoDaVitrineRepository produtos;
    private final Clock relogio;

    public ProjetarLojas(LojaRepository lojas, ProdutoDaVitrineRepository produtos, Clock relogio) {
        this.lojas = lojas;
        this.produtos = produtos;
        this.relogio = relogio;
    }

    @Override
    public String nome() {
        return "busca-projecao-de-lojas";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.EMPREENDEDORES);
    }

    @Override
    public void consumir(Evento evento) {
        var agora = relogio.instant();

        switch (evento) {
            case EmpreendedorCadastrado cadastrado -> gravar(
                    cadastrado.empreendedorId(), cadastrado.nomeDoNegocio(), cadastrado.apelidoNaUrl(),
                    cadastrado.descricao(), cadastrado.categoriaPrincipal(), cadastrado.bairro(),
                    cadastrado.telefoneWhatsapp(), false, agora);

            case PerfilAtualizado atualizado -> gravar(
                    atualizado.empreendedorId(), atualizado.nomeDoNegocio(), atualizado.apelidoNaUrl(),
                    atualizado.descricao(), atualizado.categoriaPrincipal(), atualizado.bairro(),
                    atualizado.telefoneWhatsapp(), null, agora);

            case CadastroAprovado aprovado -> trocarVisibilidade(aprovado.empreendedorId(), true, agora);
            case EmpreendedorReativado reativado -> trocarVisibilidade(reativado.empreendedorId(), true, agora);
            case EmpreendedorSuspenso suspenso -> trocarVisibilidade(suspenso.empreendedorId(), false, agora);
            case CadastroRejeitado rejeitado -> trocarVisibilidade(rejeitado.empreendedorId(), false, agora);

            default -> {
                // Outros eventos do tópico não mudam a vitrine.
            }
        }
    }

    /**
     * @param visível {@code null} mantém a visibilidade que a loja já tinha.
     *                Alterar o perfil não pode colocar no ar quem está
     *                suspenso, nem tirar do ar quem está aprovado.
     */
    private void gravar(UUID id, String nomeDoNegocio, String apelido, String descricao,
                        String categoria, String bairro, String telefone,
                        Boolean visivel, Instant agora) {
        var loja = lojas.findById(id).orElseGet(() -> LojaNaVitrine.nova(
                id, nomeDoNegocio, apelido, descricao, categoria, bairro, telefone,
                visivel != null && visivel, agora));

        loja.atualizar(nomeDoNegocio, apelido, descricao, categoria, bairro, telefone,
                loja.fotoDeCapaUrl(), agora);

        if (visivel != null) {
            if (visivel) {
                loja.mostrar(agora);
            } else {
                loja.esconder(agora);
            }
        }

        lojas.save(loja);
        espalharNosProdutos(loja, agora);
    }

    private void trocarVisibilidade(UUID id, boolean visivel, Instant agora) {
        lojas.findById(id).ifPresentOrElse(loja -> {
            if (visivel) {
                loja.mostrar(agora);
            } else {
                loja.esconder(agora);
            }
            lojas.save(loja);
            espalharNosProdutos(loja, agora);
        }, () -> log.warn("Evento de visibilidade para loja {} que a busca ainda nao conhece", id));
    }

    private void espalharNosProdutos(LojaNaVitrine loja, Instant agora) {
        produtos.findByEmpreendedorId(loja.id()).forEach(produto -> {
            produto.atualizarDaLoja(loja.nomeDoNegocio(), loja.apelidoNaUrl(), loja.bairro(),
                    loja.visivel(), agora);
            produtos.save(produto);
        });
    }
}
