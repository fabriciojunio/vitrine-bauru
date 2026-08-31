package br.com.vitrinebauru.plataforma.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Regras de limite de requisição, por endereço.
 *
 * <p>Fica em configuração, e não no código, porque o número certo depende do
 * ambiente: a demonstração aberta ao público aperta mais que o
 * desenvolvimento local, onde apertar só atrapalha quem está programando.
 *
 * @param ativo  desliga tudo, útil para teste de carga próprio
 * @param regras primeira regra que casar com o caminho é a que vale
 */
@ConfigurationProperties(prefix = "vitrine.limite")
public record PropriedadesDeLimite(boolean ativo, List<Regra> regras) {

    public PropriedadesDeLimite {
        if (regras == null) {
            regras = List.of();
        }
    }

    /**
     * @param padrão     caminho no formato do Spring, por exemplo /api/cadastro/auth/**
     * @param capacidade quantas requisições cabem na janela
     * @param janela     tempo para o balde encher de novo
     */
    public record Regra(String padrao, int capacidade, Duration janela) {

        public Regra {
            if (capacidade <= 0) {
                throw new IllegalArgumentException("Capacidade do limite precisa ser maior que zero");
            }
            if (janela == null || janela.isZero() || janela.isNegative()) {
                throw new IllegalArgumentException("Janela do limite precisa ser positiva");
            }
        }
    }
}
