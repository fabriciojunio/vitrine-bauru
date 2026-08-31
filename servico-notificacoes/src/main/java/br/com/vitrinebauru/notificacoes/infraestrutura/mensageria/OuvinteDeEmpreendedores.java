package br.com.vitrinebauru.notificacoes.infraestrutura.mensageria;

import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.CadastroRejeitado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.EmpreendedorSuspenso;
import br.com.vitrinebauru.contratos.Evento;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.notificacoes.aplicacao.EscreverNotificacao;
import br.com.vitrinebauru.plataforma.mensageria.ConsumidorDeEventos;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Transforma decisão da SEDECON em e-mail.
 *
 * <p>Reativação não gera e-mail de propósito: a loja volta ao ar e o
 * empreendedor vê pelo painel. Escrever "sua loja foi reativada" para quem
 * talvez nem soubesse da suspensão cria mais dúvida do que resolve.
 */
@Component
public class OuvinteDeEmpreendedores implements ConsumidorDeEventos {

    private final EscreverNotificacao escrever;

    public OuvinteDeEmpreendedores(EscreverNotificacao escrever) {
        this.escrever = escrever;
    }

    @Override
    public String nome() {
        return "notificacoes-empreendedores";
    }

    @Override
    public Set<String> topicos() {
        return Set.of(Topicos.EMPREENDEDORES);
    }

    @Override
    public void consumir(Evento evento) {
        switch (evento) {
            case EmpreendedorCadastrado cadastrado -> escrever.boasVindas(
                    cadastrado.id(), cadastrado.empreendedorId(), cadastrado.email(),
                    cadastrado.nomeDoResponsavel(), cadastrado.nomeDoNegocio());

            case CadastroAprovado aprovado -> escrever.aprovado(
                    aprovado.id(), aprovado.empreendedorId(), aprovado.email(),
                    aprovado.nomeDoResponsavel(), aprovado.nomeDoNegocio());

            case CadastroRejeitado rejeitado -> escrever.rejeitado(
                    rejeitado.id(), rejeitado.empreendedorId(), rejeitado.email(),
                    rejeitado.nomeDoResponsavel(), rejeitado.nomeDoNegocio(), rejeitado.motivo());

            case EmpreendedorSuspenso suspenso -> escrever.suspenso(
                    suspenso.id(), suspenso.empreendedorId(), suspenso.email(),
                    suspenso.nomeDoResponsavel(), suspenso.nomeDoNegocio(), suspenso.motivo());

            default -> {
                // Os demais eventos do tópico não viram e-mail.
            }
        }
    }
}
