package br.com.vitrinebauru.catalogo.infraestrutura.mensageria;

import br.com.vitrinebauru.catalogo.dominio.EmpreendedorConhecido;
import br.com.vitrinebauru.catalogo.infraestrutura.persistencia.EmpreendedorConhecidoRepository;
import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.CadastroRejeitado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.EmpreendedorReativado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.PerfilAtualizado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/**
 * Aprende quem existe e quem pode publicar.
 *
 * <p>O empreendedor entra aqui ja no cadastro, e nao so na aprovacao. E de
 * proposito: enquanto espera a analise da SEDECON ele pode montar o catalogo,
 * e assim, no minuto em que for aprovado, a loja aparece na vitrine com
 * produto dentro. Se so pudesse cadastrar produto depois de aprovado, toda
 * loja nova estrearia vazia.
 *
 * <p>Suspenso e excluido perdem o direito de publicar, mas os produtos ficam:
 * suspensao costuma ser temporaria, e apagar catalogo de quem foi suspenso por
 * engano seria um estrago sem volta.
 */
@Component
public class RegistrarEmpreendedoresConhecidos implements ConsumidorDeEventos {

    private final EmpreendedorConhecidoRepository conhecidos;
    private final Clock relogio;

    public RegistrarEmpreendedoresConhecidos(EmpreendedorConhecidoRepository conhecidos,
                                   Clock relogio) {
        this.conhecidos = conhecidos;
        this.relogio = relogio;
    }

    @Override
    public String nome() {
        return "catalogo-empreendedores";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.EMPREENDEDORES);
    }

    @Override
    public void consumir(Evento evento) {
        switch (evento) {
            case EmpreendedorCadastrado cadastrado ->
                    registrar(cadastrado.empreendedorId(), cadastrado.nomeDoNegocio(), true);
            case CadastroAprovado aprovado ->
                    registrar(aprovado.empreendedorId(), aprovado.nomeDoNegocio(), true);
            case EmpreendedorReativado reativado ->
                    registrar(reativado.empreendedorId(), reativado.nomeDoNegocio(), true);
            case PerfilAtualizado atualizado ->
                    conhecidos.findById(atualizado.empreendedorId()).ifPresent(conhecido ->
                            conhecido.atualizar(atualizado.nomeDoNegocio(), conhecido.podePublicar(),
                                    relogio.instant()));
            case CadastroRejeitado rejeitado ->
                    registrar(rejeitado.empreendedorId(), rejeitado.nomeDoNegocio(), false);
            case EmpreendedorSuspenso suspenso ->
                    registrar(suspenso.empreendedorId(), suspenso.nomeDoNegocio(), false);
            default -> {
                // Evento de outro assunto no mesmo topico nao muda nada aqui.
            }
        }
    }

    private void registrar(UUID empreendedorId, String nomeDoNegocio, boolean podePublicar) {
        var agora = relogio.instant();
        conhecidos.findById(empreendedorId)
                .ifPresentOrElse(
                        conhecido -> conhecido.atualizar(nomeDoNegocio, podePublicar, agora),
                        () -> conhecidos.save(new EmpreendedorConhecido(
                                empreendedorId, nomeDoNegocio, podePublicar, agora)));
    }
}
