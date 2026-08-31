package br.com.vitrinebauru.plataforma.inbox;

import br.com.vitrinebauru.contratos.Evento;

/** Interface do inbox vista por quem despacha evento. Ver {@link Inbox}. */
public interface RegistroDeEntrada {

    boolean registrar(Evento evento, String consumidor);
}
