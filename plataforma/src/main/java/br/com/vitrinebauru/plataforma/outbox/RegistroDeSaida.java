package br.com.vitrinebauru.plataforma.outbox;

import br.com.vitrinebauru.contratos.Evento;

/**
 * O que a regra de negócio enxerga do outbox.
 *
 * <p>A camada de domínio depende desta interface, e não da classe concreta com
 * JPA dentro. Serve para duas coisas práticas: o teste de unidade do serviço
 * troca isto por uma lista em memória sem subir banco, e a regra de
 * arquitetura consegue proibir domínio de importar {@code jakarta.persistence}
 * sem proibir o domínio de publicar evento.
 */
public interface RegistroDeSaida {

    void gravar(String topico, Evento evento);
}
