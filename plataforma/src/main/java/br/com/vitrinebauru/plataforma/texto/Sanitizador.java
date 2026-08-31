package br.com.vitrinebauru.plataforma.texto;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Tira marcação de texto escrito por usuário.
 *
 * <p>O React já escapa o que renderiza, então isto é a segunda camada, não a
 * primeira. Existe porque a descrição do produto não vai só para o React: vai
 * para o corpo do e-mail de notificação, para o JSON de quem consumir a API e
 * para o log. Limpar na entrada resolve nos três, escapar na saída resolve só
 * no que passa pelo navegador.
 *
 * <p>{@code Safelist.none()} porque nenhum campo desta plataforma aceita HTML.
 * Não há editor de texto rico em lugar nenhum, e não havendo, o conjunto certo
 * de marcações permitidas é o vazio.
 */
@Component
public class Sanitizador {

    public String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        // O Jsoup escapa o que sobra (& vira &amp;), o que estragaria "Doces &
        // Salgados" na tela. Desescapar depois devolve o texto puro, que é o
        // que se quer guardar: texto, sem marcação e sem entidade.
        String semMarcacao = Jsoup.clean(texto, Safelist.none());
        return Jsoup.parse(semMarcacao).text().trim();
    }

    /** Verdadeiro quando o texto tinha marcação, para poder registrar a tentativa. */
    public boolean tinhaMarcacao(String texto) {
        return texto != null && !texto.trim().equals(limpar(texto));
    }
}
