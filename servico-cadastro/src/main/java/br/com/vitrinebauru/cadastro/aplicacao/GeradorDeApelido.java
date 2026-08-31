package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.contratos.tipos.ApelidoNaUrl;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;

/**
 * Escolhe o endereço público da loja sem pedir nada ao empreendedor.
 *
 * <p>Bauru tem mais de uma "Casa do Pastel", e a segunda não pode ser
 * impedida de se cadastrar por causa disso. Quando o apelido já existe, entra
 * um número no fim: casa-do-pastel, casa-do-pastel-2, casa-do-pastel-3.
 *
 * <p>Existe uma corrida possível entre conferir e gravar: dois cadastros
 * simultâneos com o mesmo nome podem passar os dois pela verificação. Quem
 * resolve isso de verdade é a restrição de unicidade no banco, e o cadastro
 * trata o erro dela; este método só evita que o caso comum vire erro.
 */
@Component
public class GeradorDeApelido {

    private static final int TENTATIVAS = 50;

    private final EmpreendedorRepository repositorio;

    public GeradorDeApelido(EmpreendedorRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ApelidoNaUrl paraNegocio(String nomeDoNegocio) {
        ApelidoNaUrl base = ApelidoNaUrl.deTexto(nomeDoNegocio);
        if (!repositorio.existsByApelidoNaUrl(base.valor())) {
            return base;
        }

        for (int sufixo = 2; sufixo <= TENTATIVAS; sufixo++) {
            ApelidoNaUrl candidato = base.comSufixo(sufixo);
            if (!repositorio.existsByApelidoNaUrl(candidato.valor())) {
                return candidato;
            }
        }
        throw new ErrosDeNegocio.Conflito(
                "Já existem muitos negócios com esse nome. Escreva o nome de outro jeito.");
    }
}
