package br.com.vitrinebauru.contratos.tipos;

/**
 * Telefone brasileiro com DDD.
 *
 * <p>Existe por causa de um detalhe do fluxo principal: o botão de contato
 * monta um link {@code wa.me}, e link torto não dá erro, só leva o consumidor
 * para uma conversa vazia com um número que não existe. Validar aqui, no
 * cadastro, é mais barato que descobrir depois pelo empreendedor reclamando
 * que ninguém procura ele.
 */
public record Telefone(String ddd, String numero) {

    public Telefone {
        if (ddd == null || !ddd.matches("\\d{2}")) {
            throw new TelefoneInvalido("DDD precisa ter 2 digitos");
        }
        if (Integer.parseInt(ddd) < 11) {
            throw new TelefoneInvalido("DDD inexistente no Brasil");
        }
        if (numero == null || !numero.matches("\\d{8,9}")) {
            throw new TelefoneInvalido("Numero precisa ter 8 ou 9 digitos");
        }
        if (numero.length() == 9 && numero.charAt(0) != '9') {
            throw new TelefoneInvalido("Celular com 9 digitos precisa comecar com 9");
        }
    }

    public static Telefone de(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new TelefoneInvalido("Informe o telefone");
        }
        String digitos = bruto.replaceAll("\\D", "");
        if (digitos.startsWith("55") && digitos.length() > 11) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new TelefoneInvalido("Telefone precisa ter DDD mais 8 ou 9 digitos");
        }
        return new Telefone(digitos.substring(0, 2), digitos.substring(2));
    }

    public boolean ehCelular() {
        return numero.length() == 9;
    }

    public String somenteDigitos() {
        return ddd + numero;
    }

    /** Formato que o link do WhatsApp espera: código do país colado no número. */
    public String paraWhatsapp() {
        return "55" + somenteDigitos();
    }

    public String formatado() {
        String parte = ehCelular()
                ? numero.substring(0, 5) + "-" + numero.substring(5)
                : numero.substring(0, 4) + "-" + numero.substring(4);
        return "(" + ddd + ") " + parte;
    }

    public static class TelefoneInvalido extends IllegalArgumentException {
        public TelefoneInvalido(String mensagem) {
            super(mensagem);
        }
    }
}
