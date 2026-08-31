package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.StatusDoCadastro;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.ContatoRegistradoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.ProdutoDoEmpreendedorRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O painel da SEDECON.
 *
 * <p>Responde as perguntas do objetivo específico número 5 do projeto, que é
 * "avaliar o impacto e o engajamento inicial da plataforma". Sem isto, a
 * resposta na apresentação final seria impressão pessoal.
 *
 * <p>A métrica que importa não é visita: é contato iniciado. A plataforma não
 * fecha venda, então o que ela pode provar é que gerou conversa entre
 * consumidor e empreendedor. E a lista de aprovados sem nenhum produto é a
 * mais acionável de todas, porque é a lista de quem precisa de capacitação.
 */
@Component
public class ConsultarIndicadores {

    private static final Duration JANELA = Duration.ofDays(30);
    private static final int TAMANHO_DO_RANKING = 5;

    private final EmpreendedorRepository empreendedores;
    private final ProdutoDoEmpreendedorRepository produtos;
    private final ContatoRegistradoRepository contatos;
    private final Clock relogio;

    public ConsultarIndicadores(EmpreendedorRepository empreendedores,
                                ProdutoDoEmpreendedorRepository produtos,
                                ContatoRegistradoRepository contatos, Clock relogio) {
        this.empreendedores = empreendedores;
        this.produtos = produtos;
        this.contatos = contatos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Painel executar() {
        var desde = relogio.instant().minus(JANELA);

        long aprovados = empreendedores.countByStatus(StatusDoCadastro.APROVADO);
        long pendentes = empreendedores.countByStatus(StatusDoCadastro.PENDENTE);
        long suspensos = empreendedores.countByStatus(StatusDoCadastro.SUSPENSO);
        long rejeitados = empreendedores.countByStatus(StatusDoCadastro.REJEITADO);

        Map<UUID, String> nomes = empreendedores.findAll().stream()
                .collect(Collectors.toMap(Empreendedor::id, Empreendedor::nomeDoNegocio, (a, b) -> a));

        List<MaisProcurado> ranking = contatos.maisProcurados(desde).stream()
                .limit(TAMANHO_DO_RANKING)
                .map(linha -> new MaisProcurado(
                        linha.getEmpreendedorId(),
                        nomes.getOrDefault(linha.getEmpreendedorId(), "Cadastro removido"),
                        linha.getTotal()))
                .toList();

        List<SemProduto> semProduto = empreendedores.findAll().stream()
                .filter(empreendedor -> empreendedor.status() == StatusDoCadastro.APROVADO)
                .filter(empreendedor -> produtos.countByEmpreendedorId(empreendedor.id()) == 0)
                .sorted(Comparator.comparing(Empreendedor::moderadoEm,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(empreendedor -> new SemProduto(
                        empreendedor.id(), empreendedor.nomeDoNegocio(),
                        empreendedor.bairro(), empreendedor.moderadoEm()))
                .toList();

        return new Painel(
                aprovados,
                pendentes,
                suspensos,
                rejeitados,
                produtos.count(),
                contatos.count(),
                contatos.countByOcorridoEmAfter(desde),
                semProduto.size(),
                porBairro(),
                ranking,
                semProduto);
    }

    /** Distribuição por bairro: mostra onde a plataforma pegou e onde não. */
    private Map<String, Long> porBairro() {
        return empreendedores.findAll().stream()
                .filter(empreendedor -> empreendedor.status() == StatusDoCadastro.APROVADO)
                .collect(Collectors.groupingBy(Empreendedor::bairro, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }

    @Transactional(readOnly = true)
    public PainelDoEmpreendedor doEmpreendedor(UUID empreendedorId) {
        var desde = relogio.instant().minus(JANELA);
        return new PainelDoEmpreendedor(
                produtos.countByEmpreendedorId(empreendedorId),
                contatos.countByEmpreendedorId(empreendedorId),
                contatos.countByEmpreendedorIdAndOcorridoEmAfter(empreendedorId, desde));
    }

    public record Painel(
            long empreendedoresAprovados,
            long cadastrosPendentes,
            long empreendedoresSuspensos,
            long cadastrosRejeitados,
            long produtosPublicados,
            long contatosNoTotal,
            long contatosNosUltimos30Dias,
            long aprovadosSemNenhumProduto,
            Map<String, Long> aprovadosPorBairro,
            List<MaisProcurado> maisProcurados,
            List<SemProduto> precisamDeAjuda) {
    }

    public record MaisProcurado(UUID empreendedorId, String nomeDoNegocio, long contatos) {
    }

    public record SemProduto(UUID empreendedorId, String nomeDoNegocio, String bairro,
                             java.time.Instant aprovadoEm) {
    }

    public record PainelDoEmpreendedor(long produtos, long contatosNoTotal, long contatosNosUltimos30Dias) {
    }

    /** Exposto para o construtor do painel saber o tamanho da janela. */
    public static Duration janela() {
        return JANELA;
    }
}
