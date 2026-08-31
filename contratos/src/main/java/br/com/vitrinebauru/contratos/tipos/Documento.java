package br.com.vitrinebauru.contratos.tipos;

/**
 * CPF ou CNPJ do empreendedor, já validado.
 *
 * <p>Aceita os dois porque o público da SEDECON tem os dois: o MEI formalizado
 * tem CNPJ, e o artesão que ainda não se formalizou só tem CPF. Exigir CNPJ de
 * todo mundo excluiria justamente quem a plataforma existe para alcançar.
 *
 * <p>O CNPJ alfanumérico, que a Receita passou a emitir em julho de 2026, é
 * tratado aqui desde o início. A regra do dígito verificador continua sendo o
 * módulo 11; o que muda é que cada caractere vale o código ASCII menos 48, o
 * que faz "0" continuar valendo 0 e "A" passar a valer 17. Os CNPJ antigos,
 * só com números, caem no mesmo cálculo e continuam válidos.
 */
public record Documento(String valor, Tipo tipo) {

    public enum Tipo {
        CPF,
        CNPJ
    }

    private static final int[] PESOS_CPF_PRIMEIRO = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CPF_SEGUNDO = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Documento {
        if (valor == null || tipo == null) {
            throw new DocumentoInvalido("Documento sem valor ou sem tipo");
        }
    }

    public static Documento de(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new DocumentoInvalido("Informe o CPF ou o CNPJ");
        }
        String limpo = bruto.replaceAll("[^0-9A-Za-z]", "").toUpperCase();

        return switch (limpo.length()) {
            case 11 -> criarCpf(limpo);
            case 14 -> criarCnpj(limpo);
            default -> throw new DocumentoInvalido(
                    "O documento precisa ter 11 digitos (CPF) ou 14 caracteres (CNPJ)");
        };
    }

    private static Documento criarCpf(String limpo) {
        if (!limpo.matches("\\d{11}")) {
            throw new DocumentoInvalido("CPF so pode ter numeros");
        }
        if (todosIguais(limpo)) {
            throw new DocumentoInvalido("CPF invalido");
        }
        int primeiro = digito(limpo.substring(0, 9), PESOS_CPF_PRIMEIRO);
        int segundo = digito(limpo.substring(0, 10), PESOS_CPF_SEGUNDO);
        if (limpo.charAt(9) - '0' != primeiro || limpo.charAt(10) - '0' != segundo) {
            throw new DocumentoInvalido("CPF invalido");
        }
        return new Documento(limpo, Tipo.CPF);
    }

    private static Documento criarCnpj(String limpo) {
        if (!limpo.substring(0, 12).matches("[0-9A-Z]{12}") || !limpo.substring(12).matches("\\d{2}")) {
            throw new DocumentoInvalido(
                    "CNPJ precisa de 12 caracteres alfanumericos seguidos de 2 digitos verificadores");
        }
        if (todosIguais(limpo)) {
            throw new DocumentoInvalido("CNPJ invalido");
        }
        int primeiro = digito(limpo.substring(0, 12), PESOS_CNPJ_PRIMEIRO);
        int segundo = digito(limpo.substring(0, 13), PESOS_CNPJ_SEGUNDO);
        if (limpo.charAt(12) - '0' != primeiro || limpo.charAt(13) - '0' != segundo) {
            throw new DocumentoInvalido("CNPJ invalido");
        }
        return new Documento(limpo, Tipo.CNPJ);
    }

    /** Módulo 11 sobre o valor ASCII do caractere menos 48. */
    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - 48) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(String valor) {
        return valor.chars().distinct().count() == 1;
    }

    public boolean ehCnpj() {
        return tipo == Tipo.CNPJ;
    }

    public String formatado() {
        if (tipo == Tipo.CPF) {
            return valor.substring(0, 3) + "." + valor.substring(3, 6) + "."
                    + valor.substring(6, 9) + "-" + valor.substring(9);
        }
        return valor.substring(0, 2) + "." + valor.substring(2, 5) + "." + valor.substring(5, 8)
                + "/" + valor.substring(8, 12) + "-" + valor.substring(12);
    }

    /**
     * Versão para tela e para log. O documento inteiro só aparece para quem é
     * dono dele; a moderação da SEDECON precisa reconhecer o cadastro, não
     * colecionar CPF alheio.
     */
    public String mascarado() {
        if (tipo == Tipo.CPF) {
            return "***." + valor.substring(3, 6) + "." + valor.substring(6, 9) + "-**";
        }
        return "**." + valor.substring(2, 5) + "." + valor.substring(5, 8) + "/****-**";
    }

    public static class DocumentoInvalido extends IllegalArgumentException {
        public DocumentoInvalido(String mensagem) {
            super(mensagem);
        }
    }
}
