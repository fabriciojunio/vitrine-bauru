package br.com.vitrinebauru.cadastro.infraestrutura.mensageria;

import br.com.vitrinebauru.cadastro.aplicacao.ConduzirExclusao;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.ExpurgoConcluido;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Recebe as confirmacoes de expurgo dos outros servicos.
 *
 * <p>O cadastro publica no mesmo topico em que escuta: ele pede a exclusao e
 * recebe as respostas. O proprio pedido volta para ca e e ignorado, o que e o
 * comportamento certo para um topico por assunto, e nao por remetente.
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
