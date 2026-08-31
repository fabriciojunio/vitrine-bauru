package br.com.vitrinebauru.contratos.tipos;

/**
 * CEP com oito dígitos.
 *
 * <p>O formato é conferido antes de chamar o ViaCEP. Sem isso a plataforma
 * bate na API pública a cada tecla digitada errada, o que é falta de educação
 * com um serviço gratuito e ainda deixa o campo do formulário piscando sem
 * explicação.
 */
public record Cep(String valor) {

    public Cep {
        if (valor == null || !valor.matches("\\d{8}")) {
            throw new CepInvalido("CEP precisa ter 8 digitos");
        }
    }

    public static Cep de(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new CepInvalido("Informe o CEP");
        }
        return new Cep(bruto.replaceAll("\\D", ""));
    }

    public String formatado() {
        return valor.substring(0, 5) + "-" + valor.substring(5);
    }

    public static class CepInvalido extends IllegalArgumentException {
        public CepInvalido(String mensagem) {
            super(mensagem);
        }
    }
}
