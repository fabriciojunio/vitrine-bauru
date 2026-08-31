package br.com.vitrinebauru.contratos;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

/**
 * Bairros aceitos no cadastro.
 *
 * <p>Lista fechada, e não campo livre, por causa da busca: com texto livre, o
 * mesmo bairro vira "Vila Cardia", "vila cardia", "V. Cardia" e "Vl Cardia",
 * e o filtro por bairro, que é uma das três formas de busca do produto,
 * simplesmente para de funcionar. Como a plataforma atende um município só, a
 * lista cabe num arquivo.
 *
 * <p>Acrescentar bairro é acrescentar uma linha aqui. É de propósito que isso
 * exija passar por revisão de código: é o único lugar onde a normalização da
 * busca pode ser quebrada sem ninguém perceber.
 */
public final class BairrosDeBauru {

    private static final List<String> BAIRROS = List.of(
            "Centro",
            "Vila Cardia",
            "Vila Falcão",
            "Vila Universitária",
            "Vila Aviação",
            "Vila Independência",
            "Vila Santa Tereza",
            "Vila Souto",
            "Vila Nova Cidade Universitária",
            "Vila Seabra",
            "Vila Zillo",
            "Jardim Europa",
            "Jardim Estoril",
            "Jardim Brasil",
            "Jardim Bela Vista",
            "Jardim Terra Branca",
            "Jardim Ferraz",
            "Jardim Nasralla",
            "Jardim Redentor",
            "Jardim Colonial",
            "Jardim Godoy",
            "Jardim Contorno",
            "Jardim Panorama",
            "Jardim América",
            "Jardim Ivone",
            "Núcleo Habitacional Mary Dota",
            "Núcleo Habitacional Presidente Geisel",
            "Núcleo Habitacional Edison Bastos Gasparini",
            "Núcleo Habitacional Nobuji Nagasawa",
            "Parque Vista Alegre",
            "Parque São Geraldo",
            "Parque Jaraguá",
            "Parque Residencial das Camélias",
            "Parque Santa Edwiges",
            "Parque Paulistano",
            "Bela Vista",
            "Altos da Cidade",
            "Chácara das Flores",
            "Distrito Industrial",
            "Ferradura Mirim",
            "Beija-Flor",
            "Pousada da Esperança",
            "Tibiriçá",
            "Vila Aeroporto");

    private BairrosDeBauru() {
    }

    public static List<String> todos() {
        return BAIRROS;
    }

    public static boolean existe(String bairro) {
        return normalizado(bairro).isPresent();
    }

    /**
     * Devolve o bairro no formato oficial a partir do que a pessoa digitou.
     * Compara sem acento e sem caixa, porque o empreendedor não tem obrigação
     * de acertar o acento de Tibiriçá para se cadastrar.
     */
    public static Optional<String> normalizado(String bairro) {
        if (bairro == null || bairro.isBlank()) {
            return Optional.empty();
        }
        String procurado = semAcento(bairro);
        return BAIRROS.stream()
                .filter(oficial -> semAcento(oficial).equals(procurado))
                .findFirst();
    }

    private static String semAcento(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
