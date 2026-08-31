package br.com.vitrinebauru.plataforma.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Regras de limite de requisicao, por endereco.
 *
 * <p>Fica em configuracao, e nao no codigo, porque o numero certo depende do
 * ambiente: a demonstracao aberta ao publico aperta mais que o
 * desenvolvimento local, onde apertar so atrapalha quem esta programando.
 *
 * @param ativo  desliga tudo, util para teste de carga proprio
 * @param regras primeira regra que casar com o caminho e a que vale
 */
@ConfigurationProperties(prefix = "vitrine.limite")
public record PropriedadesDeLimite(boolean ativo, List<Regra> regras) {

    public PropriedadesDeLimite {
        if (regras == null) {
            regras = List.of();
        }
    }

    /**
     * @param padrao     caminho no formato do Spring, por exemplo /api/cadastro/auth/**
     * @param capacidade quantas requisicoes cabem na janela
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
