package br.com.vitrinebauru.cadastro.infraestrutura.mensageria;

import br.com.vitrinebauru.cadastro.dominio.ContatoRegistrado;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.ContatoRegistradoRepository;
import br.com.vitrinebauru.contratos.ContatoIniciado;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Guarda cada clique em "falar no WhatsApp" para virar indicador.
 *
 * <p>O identificador do registro e o proprio id do evento. Isso torna a
 * gravacao idempotente por construcao, alem do inbox: se a mesma mensagem
 * chegar duas vezes, a segunda sobrescreve a primeira linha em vez de inflar a
 * estatistica. Numero de impacto que cresce com reentrega de mensagem e
 * numero que nao serve para relatorio.
 */
@Component
public class OuvinteDeContatos implements ConsumidorDeEventos {

    private final ContatoRegistradoRepository contatos;

    public OuvinteDeContatos(ContatoRegistradoRepository contatos) {
        this.contatos = contatos;
    }

    @Override
    public String nome() {
        return "cadastro-registrador-de-contatos";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.CONTATOS);
    }

    @Override
    public void consumir(Evento evento) {
        if (evento instanceof ContatoIniciado contato) {
            contatos.save(new ContatoRegistrado(
                    contato.id(),
                    contato.empreendedorId(),
                    contato.produtoId(),
                    contato.canal(),
                    contato.origem(),
                    contato.ocorridoEm()));
        }
    }
}
