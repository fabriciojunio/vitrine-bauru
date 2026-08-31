package br.com.vitrinebauru.cadastro.api;

import br.com.vitrinebauru.cadastro.aplicacao.Sessoes;
import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.contratos.tipos.Telefone;

import java.time.Instant;
import java.util.UUID;

/**
 * O que sai pela API.
 *
 * <p>Nenhuma entidade JPA atravessa esta camada. Alem do motivo conhecido
 * (carregamento preguicoso estourando fora da transacao), tem o motivo que
 * importa mais aqui: a entidade tem o documento inteiro e o hash da senha, e
 * um dia alguem acrescenta um campo sensivel a entidade sem lembrar de que ela
 * vira JSON em algum lugar.
 */
public final class Respostas {

    private Respostas() {
    }

    public record Sessao(String tokenDeAcesso, String tokenDeRenovacao, Instant expiraEm,
                         UsuarioLogado usuario) {

        public static Sessao de(Sessoes.Aberta aberta, String nome) {
            return new Sessao(
                    aberta.tokenDeAcesso(),
                    aberta.tokenDeRenovacao(),
                    aberta.acessoExpiraEm(),
                    new UsuarioLogado(
                            aberta.usuario().id(),
                            nome,
                            aberta.usuario().email(),
                            aberta.usuario().papel().name(),
                            aberta.usuario().empreendedorId()));
        }
    }

    public record UsuarioLogado(UUID id, String nome, String email, String papel, UUID empreendedorId) {
    }

    /** Visao do proprio empreendedor sobre a loja dele. */
    public record MinhaLoja(UUID id, String nomeDoNegocio, String apelidoNaUrl, String descricao,
                            String categoriaPrincipal, String bairro, String cep,
                            String telefoneWhatsapp, String documento, String fotoDeCapaUrl,
                            String situacao, String motivoDaModeracao, Instant cadastradoEm,
                            Instant moderadoEm, boolean apareceNaVitrine) {

        public static MinhaLoja de(Empreendedor empreendedor) {
            return new MinhaLoja(
                    empreendedor.id(),
                    empreendedor.nomeDoNegocio(),
                    empreendedor.apelidoNaUrl(),
                    empreendedor.descricao(),
                    empreendedor.categoriaPrincipal(),
                    empreendedor.bairro(),
                    empreendedor.cep(),
                    Telefone.de(empreendedor.telefoneWhatsapp()).formatado(),
                    empreendedor.documentoMascarado(),
                    empreendedor.fotoDeCapaUrl(),
                    empreendedor.status().name(),
                    empreendedor.motivoDaModeracao(),
                    empreendedor.criadoEm(),
                    empreendedor.moderadoEm(),
                    empreendedor.status().apareceNaVitrine());
        }
    }

    /**
     * Visao de quem modera.
     *
     * <p>O documento aparece mascarado tambem aqui. Quem analisa precisa
     * reconhecer o cadastro e conferir se o tipo bate, e nao anotar o CPF de
     * quem se cadastrou. O resultado da consulta a Receita, que e o que de
     * fato ajuda na decisao, vem no campo ao lado.
     */
    public record CadastroParaAnalise(UUID id, String nomeDoNegocio, String descricao,
                                      String categoriaPrincipal, String bairro,
                                      String telefoneWhatsapp, String documento, String tipoDoDocumento,
                                      String situacaoDoDocumento, String situacao,
                                      Instant cadastradoEm, long diasNaFila) {

        public static CadastroParaAnalise de(Empreendedor empreendedor, Instant agora) {
            return new CadastroParaAnalise(
                    empreendedor.id(),
                    empreendedor.nomeDoNegocio(),
                    empreendedor.descricao(),
                    empreendedor.categoriaPrincipal(),
                    empreendedor.bairro(),
                    Telefone.de(empreendedor.telefoneWhatsapp()).formatado(),
                    empreendedor.documentoMascarado(),
                    empreendedor.documentoTipo().name(),
                    empreendedor.situacaoDoDocumento(),
                    empreendedor.status().name(),
                    empreendedor.criadoEm(),
                    java.time.Duration.between(empreendedor.criadoEm(), agora).toDays());
        }
    }

    public record CadastroCriado(UUID empreendedorId, String apelidoNaUrl, String mensagem) {
    }
}
