package br.com.vitrinebauru.cadastro.dominio;

import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;

/**
 * O que conta como senha aceitavel aqui.
 *
 * <p>A regra e comprimento e senha obvia, e nao a exigencia classica de
 * maiuscula, numero e simbolo. Isso e escolha, e a escolha tem motivo: quem
 * usa esta plataforma do lado do vendedor e gente com pouca familiaridade
 * digital, e exigir simbolo produz "Senha@123" anotada num papel colado no
 * balcao, que e pior que uma senha longa e sem simbolo. A recomendacao atual
 * do NIST vai na mesma direcao: tamanho minimo, lista de senha conhecida
 * barrada, e nada de regra de composicao.
 */
public final class PoliticaDeSenha {

    public static final int TAMANHO_MINIMO = 8;
    public static final int TAMANHO_MAXIMO = 72;

    /**
     * As mais usadas no Brasil, com as variacoes que a plataforma naturalmente
     * atrai. Lista curta de proposito: ela existe para barrar o obvio, nao
     * para ser um dicionario.
     */
    private static final Set<String> SENHAS_CONHECIDAS = Set.of(
            "12345678", "123456789", "1234567890", "senha123", "password",
            "qwertyui", "11111111", "abcd1234", "12341234", "sedecon123",
            "bauru123", "vitrine123", "mudar123", "senhasenha", "empreendedor");

    private PoliticaDeSenha() {
    }

    public static void exigirValida(String senha, String email, String nome) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "A senha precisa ter pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        // O bcrypt so considera os primeiros 72 bytes. Aceitar mais que isso
        // daria a falsa impressao de que a senha inteira protege alguma coisa.
        if (senha.getBytes().length > TAMANHO_MAXIMO) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "A senha pode ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
        }
        if (senha.isBlank()) {
            throw new ErrosDeNegocio.RegraDeNegocio("A senha não pode ser só espaços.");
        }
        if (SENHAS_CONHECIDAS.contains(senha.toLowerCase())) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Essa senha é usada por muita gente e é fácil de adivinhar. Escolha outra.");
        }
        if (temApenasUmCaractereRepetido(senha)) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Uma senha com o mesmo caractere repetido não protege a sua conta.");
        }
        if (pareceComOsDados(senha, email, nome)) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "A senha não pode ser o seu nome nem o seu e-mail.");
        }
    }

    private static boolean temApenasUmCaractereRepetido(String senha) {
        return senha.chars().distinct().count() == 1;
    }

    private static boolean pareceComOsDados(String senha, String email, String nome) {
        String comparavel = simplificar(senha);
        List<String> proibidos = List.of(
                simplificar(email == null ? "" : email.split("@")[0]),
                simplificar(nome == null ? "" : nome));

        return proibidos.stream()
                .filter(proibido -> proibido.length() >= 4)
                .anyMatch(comparavel::contains);
    }

    private static String simplificar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}
