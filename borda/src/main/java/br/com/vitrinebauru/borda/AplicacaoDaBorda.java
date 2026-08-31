package br.com.vitrinebauru.borda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A porta de entrada quando os serviços rodam separados.
 *
 * <p>Existe para o frontend conhecer um endereço só. Sem ela, o navegador
 * precisaria saber que cadastro está numa porta, catálogo em outra e busca em
 * uma terceira, e cada mudança de topologia viraria alteração no código do
 * frontend e no CORS de quatro serviços.
 *
 * <p>Não há regra de negócio aqui, e isso é proposital. Gateway que decide
 * coisa vira o lugar onde toda regra acaba parando, porque é sempre o caminho
 * mais curto. Aqui ele roteia, cuida do CORS e limita ritmo. Autenticação
 * continua sendo conferida por cada serviço, que é quem sabe o que proteger.
 */
@SpringBootApplication
public class AplicacaoDaBorda {

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoDaBorda.class, argumentos);
    }
}
