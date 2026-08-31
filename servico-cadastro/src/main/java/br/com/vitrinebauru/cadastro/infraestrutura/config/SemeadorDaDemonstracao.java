package br.com.vitrinebauru.cadastro.infraestrutura.config;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.CadastroAprovado;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.demonstracao.DadosDaDemonstracao;
import br.com.vitrinebauru.contratos.tipos.ApelidoNaUrl;
import br.com.vitrinebauru.contratos.tipos.Documento;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.seguranca.Papel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Enche a demonstracao com as lojas ficticias na primeira subida.
 *
 * <p>Semeia pelos mesmos caminhos que o sistema usa de verdade: cria a conta,
 * cria o cadastro, chama {@code aprovar} do dominio e grava os eventos no
 * outbox. Nao ha atalho por SQL. Assim a demonstracao exercita o fluxo
 * completo, e a busca publica se enche sozinha por evento, do jeito que
 * aconteceria com um cadastro real.
 *
 * <p>Duas lojas ficam pendentes de proposito: sem elas, quem entra como
 * SEDECON encontra uma fila de moderacao vazia e nao consegue ver a parte mais
 * importante do sistema funcionando.
 *
 * <p>Roda so quando as tabelas estao vazias. Reiniciar a demonstracao e
 * apagar o banco, e nao rodar isto de novo por cima.
 */
@Component
@ConditionalOnProperty(name = "vitrine.demo.ativo", havingValue = "true")
public class SemeadorDaDemonstracao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SemeadorDaDemonstracao.class);

    /** As duas ultimas lojas da lista ficam esperando analise. */
    private static final int LOJAS_PENDENTES = 2;

    private final UsuarioRepository usuarios;
    private final EmpreendedorRepository empreendedores;
    private final PasswordEncoder codificador;
    private final RegistroDeSaida outbox;
    private final PropriedadesDaDemonstracao propriedades;
    private final Clock relogio;

    public SemeadorDaDemonstracao(UsuarioRepository usuarios, EmpreendedorRepository empreendedores,
                                  PasswordEncoder codificador, RegistroDeSaida outbox,
                                  PropriedadesDaDemonstracao propriedades, Clock relogio) {
        this.usuarios = usuarios;
        this.empreendedores = empreendedores;
        this.codificador = codificador;
        this.outbox = outbox;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments argumentos) {
        if (usuarios.count() > 0) {
            log.info("Demonstracao ja semeada, seguindo sem mexer nos dados");
            return;
        }

        var agora = relogio.instant();
        String senha = codificador.encode(propriedades.senha());

        var admin = usuarios.save(Usuario.comId(
                DadosDaDemonstracao.ADMIN_USUARIO_ID, DadosDaDemonstracao.ADMIN_NOME,
                DadosDaDemonstracao.ADMIN_EMAIL, senha, Papel.ADMIN_SEDECON, agora));

        var lojas = DadosDaDemonstracao.lojas();
        int aprovadas = lojas.size() - LOJAS_PENDENTES;

        for (int posicao = 0; posicao < lojas.size(); posicao++) {
            var loja = lojas.get(posicao);
            // Espalha as datas de cadastro pelos ultimos meses para o painel de
            // indicadores nao mostrar tudo criado no mesmo segundo.
            var cadastradoEm = agora.minus(90L - posicao * 7L, ChronoUnit.DAYS);

            var dono = usuarios.save(Usuario.comId(loja.usuarioId(), loja.responsavel(),
                    loja.email(), senha, Papel.EMPREENDEDOR, cadastradoEm));

            var empreendedor = empreendedores.save(Empreendedor.comId(
                    loja.empreendedorId(), dono.id(), loja.nomeDoNegocio(),
                    new ApelidoNaUrl(loja.apelidoNaUrl()), loja.descricao(), loja.categoria(),
                    loja.bairro(), null, Telefone.de(loja.telefone()),
                    Documento.de(loja.documento()), cadastradoEm));

            outbox.gravar(Topicos.EMPREENDEDORES, new EmpreendedorCadastrado(
                    UUID.randomUUID(), UUID.randomUUID(), cadastradoEm,
                    empreendedor.id(), dono.id(), empreendedor.nomeDoNegocio(),
                    empreendedor.apelidoNaUrl(), empreendedor.descricao(),
                    empreendedor.categoriaPrincipal(), empreendedor.bairro(),
                    empreendedor.telefoneWhatsapp(), loja.documento(), dono.email(), dono.nome()));

            if (posicao < aprovadas) {
                var aprovadoEm = cadastradoEm.plus(2, ChronoUnit.DAYS);
                empreendedor.aprovar(admin.id(), aprovadoEm);

                outbox.gravar(Topicos.EMPREENDEDORES, new CadastroAprovado(
                        UUID.randomUUID(), UUID.randomUUID(), aprovadoEm,
                        empreendedor.id(), admin.id(), empreendedor.nomeDoNegocio(),
                        dono.email(), dono.nome()));
            }
        }

        log.info("Demonstracao semeada: {} lojas, {} aprovadas e {} na fila da SEDECON",
                lojas.size(), aprovadas, LOJAS_PENDENTES);
    }
}
