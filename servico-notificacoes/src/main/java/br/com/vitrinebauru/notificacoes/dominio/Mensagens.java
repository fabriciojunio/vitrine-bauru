package br.com.vitrinebauru.notificacoes.dominio;

/**
 * O texto dos e-mails.
 *
 * <p>Escritos como uma pessoa escreveria para outra, e não como sistema
 * escreve para usuário. Quem recebe é um empreendedor que muitas vezes tem
 * pouca familiaridade digital, e "Sua solicitação foi processada com sucesso"
 * não diz o que aconteceu nem o que fazer em seguida.
 *
 * <p>Três regras seguidas em todos: dizer o que aconteceu na primeira linha,
 * dizer o próximo passo, e assinar como SEDECON, que é a instituição em que a
 * pessoa confia.
 *
 * <p>Texto puro, sem HTML. E-mail com marcação exige tratar cliente antigo,
 * imagem bloqueada e filtro de spam mais agressivo, sem melhorar nada do que
 * precisa ser dito aqui.
 */
public final class Mensagens {

    private static final String ASSINATURA = """

            ---
            Vitrine Bauru
            Uma iniciativa da SEDECON, a Secretaria de Desenvolvimento Econômico de Bauru.
            Casa do Empreendedor: Av. Duque de Caxias, 16-55, Vila Cardia.""";

    private Mensagens() {
    }

    public static Conteudo boasVindas(String nome, String nomeDoNegocio) {
        return new Conteudo(
                "Recebemos o cadastro da " + nomeDoNegocio,
                """
                Olá, %s.

                Recebemos o cadastro da %s na Vitrine Bauru. Agora ele está na fila de \
                análise da SEDECON, que confere os dados antes de a loja aparecer para o \
                público.

                Enquanto espera, você já pode entrar na plataforma e cadastrar seus \
                produtos. Assim, no momento em que o cadastro for aprovado, sua loja \
                estreia com o catálogo pronto.

                Se algum dado estiver errado, é só corrigir pelo painel.%s"""
                        .formatted(nome, nomeDoNegocio, ASSINATURA));
    }

    public static Conteudo aprovado(String nome, String nomeDoNegocio) {
        return new Conteudo(
                "Sua loja está no ar: " + nomeDoNegocio,
                """
                Olá, %s.

                A SEDECON aprovou o cadastro da %s. A partir de agora sua loja aparece na \
                busca da Vitrine Bauru e qualquer pessoa da cidade pode encontrar seus \
                produtos.

                Duas coisas ajudam a aparecer mais: cadastrar foto nos produtos e manter \
                marcado o que está disponível. Quem procura no celular decide pela foto.

                O contato chega direto no seu WhatsApp, sem passar pela plataforma.%s"""
                        .formatted(nome, nomeDoNegocio, ASSINATURA));
    }

    public static Conteudo rejeitado(String nome, String nomeDoNegocio, String motivo) {
        return new Conteudo(
                "Sobre o cadastro da " + nomeDoNegocio,
                """
                Olá, %s.

                A SEDECON analisou o cadastro da %s e ele ainda não pôde ser aprovado.

                Motivo informado pela análise:
                %s

                Isso não encerra o seu cadastro. Entre no painel, corrija o que foi \
                apontado e envie de novo para análise. Se tiver dúvida sobre o que \
                corrigir, a Casa do Empreendedor atende de segunda a sexta, das 8h às \
                17h.%s"""
                        .formatted(nome, nomeDoNegocio, motivo, ASSINATURA));
    }

    public static Conteudo suspenso(String nome, String nomeDoNegocio, String motivo) {
        return new Conteudo(
                "Sua loja foi suspensa temporariamente: " + nomeDoNegocio,
                """
                Olá, %s.

                A loja %s foi suspensa e não está aparecendo na busca da Vitrine Bauru \
                neste momento.

                Motivo informado:
                %s

                Seus produtos continuam salvos. Para entender a suspensão ou pedir \
                revisão, procure a SEDECON na Casa do Empreendedor.%s"""
                        .formatted(nome, nomeDoNegocio, motivo, ASSINATURA));
    }

    public record Conteudo(String assunto, String corpo) {
    }
}
