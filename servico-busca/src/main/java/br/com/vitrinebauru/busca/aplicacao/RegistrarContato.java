package br.com.vitrinebauru.busca.aplicacao;

import br.com.vitrinebauru.busca.infraestrutura.persistencia.LojaRepository;
import br.com.vitrinebauru.contratos.CanalDeContato;
import br.com.vitrinebauru.contratos.ContatoIniciado;
import br.com.vitrinebauru.contratos.OrigemDoContato;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;

/**
 * Registra o clique em "falar no WhatsApp" e monta o link.
 *
 * <p>Este é o fim do funil do produto. A plataforma não fecha venda: ela leva
 * o consumidor até a conversa. Contar quantas vezes isso acontece é a única
 * medida honesta de impacto que a SEDECON pode levar para a apresentação
 * final, e é o objetivo específico número 5 do projeto.
 *
 * <p>O link é montado aqui, e não no navegador, para a mensagem inicial ser
 * sempre a mesma e o formato do número não depender de código de tela. O
 * empreendedor recebe uma mensagem que já diz de onde veio o contato, o que
 * ajuda ele a perceber que a vitrine está funcionando.
 *
 * <p>Nada do consumidor é guardado: sem IP, sem cookie, sem identificador de
 * sessão. Da para contar sem rastrear.
 */
@Component
public class RegistrarContato {

    private final LojaRepository lojas;
    private final RegistroDeSaida outbox;
    private final Clock relogio;
    private final String nomeDaPlataforma;

    public RegistrarContato(LojaRepository lojas, RegistroDeSaida outbox, Clock relogio,
                            @Value("${vitrine.nome:Vitrine Bauru}") String nomeDaPlataforma) {
        this.lojas = lojas;
        this.outbox = outbox;
        this.relogio = relogio;
        this.nomeDaPlataforma = nomeDaPlataforma;
    }

    @Transactional
    public Resultado executar(UUID empreendedorId, UUID produtoId, String nomeDoProduto,
                              CanalDeContato canal, OrigemDoContato origem) {
        var loja = lojas.findById(empreendedorId)
                .filter(encontrada -> encontrada.visivel())
                .orElseThrow(() -> new ErrosDeNegocio.NaoEncontrado("Essa loja não está disponível."));

        var agora = relogio.instant();

        outbox.gravar(Topicos.CONTATOS, new ContatoIniciado(
                UUID.randomUUID(), Correlacao.atual(), agora,
                empreendedorId, produtoId, canal, origem));

        return new Resultado(montarLink(loja.telefoneWhatsapp(), nomeDoProduto), loja.nomeDoNegocio());
    }

    private String montarLink(String telefone, String nomeDoProduto) {
        String mensagem = nomeDoProduto == null || nomeDoProduto.isBlank()
                ? "Olá! Vi a sua loja no " + nomeDaPlataforma + " e queria saber mais."
                : "Olá! Vi o " + nomeDoProduto + " no " + nomeDaPlataforma + " e queria saber mais.";

        return "https://wa.me/" + Telefone.de(telefone).paraWhatsapp()
                + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
    }

    public record Resultado(String linkDoWhatsapp, String nomeDoNegocio) {
    }
}
