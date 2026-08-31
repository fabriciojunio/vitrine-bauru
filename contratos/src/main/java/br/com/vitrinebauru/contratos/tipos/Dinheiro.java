package br.com.vitrinebauru.contratos.tipos;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Preço em centavos.
 *
 * <p>Centavos em {@code long}, e não {@code double}: preço somado em ponto
 * flutuante vira 24,299999999999997 na tela do consumidor. A formatação
 * também é feita a mão em vez de sair do {@code NumberFormat} porque o
 * formatador do Java devolve espaço não separável depois do "R$", que
 * atravessa o JSON e aparece como caractere estranho em alguns navegadores.
 */
public record Dinheiro(long centavos) implements Comparable<Dinheiro> {

    public static final Dinheiro ZERO = new Dinheiro(0);

    public Dinheiro {
        if (centavos < 0) {
            throw new DinheiroInvalido("Preço não pode ser negativo");
        }
    }

    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(centavos);
    }

    public static Dinheiro deReais(String reais) {
        if (reais == null || reais.isBlank()) {
            throw new DinheiroInvalido("Informe o valor");
        }
        String limpo = reais.replace("R$", "").trim().replace(".", "").replace(",", ".");
        try {
            BigDecimal valor = new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
            return new Dinheiro(valor.movePointRight(2).longValueExact());
        } catch (NumberFormatException | ArithmeticException e) {
            throw new DinheiroInvalido("Valor inválido: " + reais);
        }
    }

    public Dinheiro mais(Dinheiro outro) {
        return new Dinheiro(centavos + outro.centavos);
    }

    public String formatado() {
        long reais = centavos / 100;
        long resto = centavos % 100;
        StringBuilder inteiro = new StringBuilder(String.valueOf(reais));
        for (int posicao = inteiro.length() - 3; posicao > 0; posicao -= 3) {
            inteiro.insert(posicao, '.');
        }
        return "R$ " + inteiro + "," + (resto < 10 ? "0" + resto : resto);
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return Long.compare(centavos, outro.centavos);
    }

    public static class DinheiroInvalido extends IllegalArgumentException {
        public DinheiroInvalido(String mensagem) {
            super(mensagem);
        }
    }
}
