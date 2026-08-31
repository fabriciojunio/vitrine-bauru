package br.com.vitrinebauru.plataforma.outbox;

import br.com.vitrinebauru.contratos.Evento;

/**
 * O que a regra de negocio enxerga do outbox.
 *
 * <p>A camada de dominio depende desta interface, e nao da classe concreta com
 * JPA dentro. Serve para duas coisas praticas: o teste de unidade do servico
 * troca isto por uma lista em memoria sem subir banco, e a regra de
 * arquitetura consegue proibir dominio de importar {@code jakarta.persistence}
 * sem proibir o dominio de publicar evento.
 */
public interface RegistroDeSaida {

    void gravar(String topico, Evento evento);
}
