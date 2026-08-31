package br.com.vitrinebauru.borda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A porta de entrada quando os serviços rodam separados.
 *
 * <p>Existe para o frontend conhecer um endereco so. Sem ela, o navegador
 * precisaria saber que cadastro esta numa porta, catalogo em outra e busca em
 * uma terceira, e cada mudanca de topologia viraria alteracao no codigo do
 * frontend e no CORS de quatro servicos.
 *
 * <p>Nao ha regra de negocio aqui, e isso e proposital. Gateway que decide
 * coisa vira o lugar onde toda regra acaba parando, porque e sempre o caminho
 * mais curto. Aqui ele roteia, cuida do CORS e limita ritmo. Autenticacao
 * continua sendo conferida por cada servico, que e quem sabe o que proteger.
 */
@SpringBootApplication
public class AplicacaoDaBorda {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoDaBorda.class, argumentos);
    }
}
