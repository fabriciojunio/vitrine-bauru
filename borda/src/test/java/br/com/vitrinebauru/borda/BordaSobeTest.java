package br.com.vitrinebauru.borda;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a borda de verdade. Sem este teste, uma versão do Spring Cloud feita para
 * outro Spring Boot passou no CI e a borda não subia: compilava, e nada a ligava.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BordaSobeTest {

    private static final HttpServer BUSCA = servico("busca");

    @Autowired
    private TestRestTemplate cliente;

    private static HttpServer servico(String nome) {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            servidor.createContext("/", troca -> {
                byte[] corpo = (nome + " " + troca.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
                troca.sendResponseHeaders(200, corpo.length);
                troca.getResponseBody().write(corpo);
                troca.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void enderecos(DynamicPropertyRegistry registro) {
        registro.add("SERVICO_BUSCA", () -> "http://127.0.0.1:" + BUSCA.getAddress().getPort());
    }

    @AfterAll
    static void desligar() {
        BUSCA.stop(0);
    }

    @Test
    void respondeSaude() {
        assertThat(cliente.getForEntity("/actuator/health", String.class).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void encaminhaPeloPrefixo() {
        assertThat(cliente.getForObject("/api/busca/resumo", String.class)).isEqualTo("busca /api/busca/resumo");
    }

    @Test
    void caminhoForaDaListaNaoVaiParaServicoNenhum() {
        assertThat(cliente.getForEntity("/qualquer/coisa", String.class).getStatusCode().value()).isEqualTo(404);
    }
}
