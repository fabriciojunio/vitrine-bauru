package br.com.vitrinebauru.cadastro.infraestrutura.mensageria;

import br.com.vitrinebauru.cadastro.aplicacao.ConduzirExclusao;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Recebe as confirmações de expurgo dos outros serviços.
 *
 * <p>O cadastro publica no mesmo tópico em que escuta: ele pede a exclusão e
 * recebe as respostas. O próprio pedido volta para cá e é ignorado, o que é o
 * comportamento certo para um tópico por assunto, e não por remetente.
 */
@Component
public class OuvinteDasConfirmacoes implements ConsumidorDeEventos {

    private final ConduzirExclusao conduzirExclusao;

    public OuvinteDasConfirmacoes(ConduzirExclusao conduzirExclusao) {
        this.conduzirExclusao = conduzirExclusao;
    }

    @Override
    public String nome() {
        return "cadastro-coordenador-da-exclusao";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.PRIVACIDADE);
    }

    @Override
    public void consumir(Evento evento) {
        if (evento instanceof ExpurgoConcluido confirmacao) {
            conduzirExclusao.registrarConfirmacao(
                    confirmacao.empreendedorId(),
                    confirmacao.participante(),
                    confirmacao.registrosRemovidos());
        }
    }
}
