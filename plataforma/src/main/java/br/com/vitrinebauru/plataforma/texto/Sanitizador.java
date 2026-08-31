package br.com.vitrinebauru.plataforma.texto;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Tira marcacao de texto escrito por usuario.
 *
 * <p>O React ja escapa o que renderiza, entao isto e a segunda camada, nao a
 * primeira. Existe porque a descricao do produto nao vai so para o React: vai
 * para o corpo do e-mail de notificacao, para o JSON de quem consumir a API e
 * para o log. Limpar na entrada resolve nos tres, escapar na saida resolve so
 * no que passa pelo navegador.
 *
 * <p>{@code Safelist.none()} porque nenhum campo desta plataforma aceita HTML.
 * Nao ha editor de texto rico em lugar nenhum, e nao havendo, o conjunto certo
 * de marcacoes permitidas e o vazio.
 */
@Component
public class Sanitizador {

    public String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        // O Jsoup escapa o que sobra (& vira &amp;), o que estragaria "Doces &
        // Salgados" na tela. Desescapar depois devolve o texto puro, que e o
        // que se quer guardar: texto, sem marcacao e sem entidade.
        String semMarcacao = Jsoup.clean(texto, Safelist.none());
        return Jsoup.parse(semMarcacao).text().trim();
    }

    /** Verdadeiro quando o texto tinha marcacao, para poder registrar a tentativa. */
    public boolean tinhaMarcacao(String texto) {
        return texto != null && !texto.trim().equals(limpar(texto));
    }
}
