package br.com.vitrinebauru.plataforma.mensageria;

import java.util.Map;

/**
 * De onde sai o endereço do tópico na hora de publicar.
 *
 * <p>No Kafka o produtor publica pelo nome do tópico. No SNS, publica pelo
 * ARN, que só se conhece depois de criar o tópico e inclui o número da conta.
 * Montar esse endereço na mão exigiria a conta e a região espalhadas pelo
 * código; guardar o que a criação devolveu evita as duas coisas.
 *
 * <p>Criar tópico no SNS é idempotente: chamar de novo para um nome que já
 * existe devolve o mesmo ARN em vez de dar erro. Por isso a subida de cada
 * serviço pode criar todos os tópicos sem coordenação com os outros três.
 */
public class ArnDosTopicos {

    private final Map<String, String> porTopico;

    public ArnDosTopicos(Map<String, String> porTopico) {
        this.porTopico = Map.copyOf(porTopico);
    }

    public String de(String topico) {
        String arn = porTopico.get(topico);
        if (arn == null) {
            throw new IllegalArgumentException(
                    "Tópico sem ARN conhecido: " + topico + ". Foi declarado em Topicos?");
        }
        return arn;
    }

    Map<String, String> todos() {
        return porTopico;
    }
}
