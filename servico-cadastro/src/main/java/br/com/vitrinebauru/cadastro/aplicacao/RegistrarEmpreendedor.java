package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.PoliticaDeSenha;
import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.EmpreendedorCadastrado;
import br.com.vitrinebauru.contratos.Topicos;
import br.com.vitrinebauru.contratos.tipos.Cep;
import br.com.vitrinebauru.contratos.tipos.Documento;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.outbox.RegistroDeSaida;
import br.com.vitrinebauru.plataforma.seguranca.Papel;
import br.com.vitrinebauru.plataforma.texto.Sanitizador;
import br.com.vitrinebauru.plataforma.web.Correlacao;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * O cadastro do empreendedor, do zero ate a fila da SEDECON.
 *
 * <p>Tudo numa transacao so: a conta, o perfil e o evento que avisa o resto do
 * sistema. Meio cadastro nao existe. Se o evento nao puder ser gravado, o
 * cadastro tambem nao acontece, e o empreendedor tenta de novo em vez de ficar
 * com uma conta que nenhum outro servico conhece.
 */
@Component
public class RegistrarEmpreendedor {

    private final UsuarioRepository usuarios;
    private final EmpreendedorRepository empreendedores;
    private final GeradorDeApelido geradorDeApelido;
    private final PasswordEncoder codificador;
    private final Sanitizador sanitizador;
    private final RegistroDeSaida outbox;
    private final Auditor auditor;
    private final Clock relogio;

    public RegistrarEmpreendedor(UsuarioRepository usuarios, EmpreendedorRepository empreendedores,
                                 GeradorDeApelido geradorDeApelido, PasswordEncoder codificador,
                                 Sanitizador sanitizador, RegistroDeSaida outbox,
                                 Auditor auditor, Clock relogio) {
        this.usuarios = usuarios;
        this.empreendedores = empreendedores;
        this.geradorDeApelido = geradorDeApelido;
        this.codificador = codificador;
        this.sanitizador = sanitizador;
        this.outbox = outbox;
        this.auditor = auditor;
        this.relogio = relogio;
    }

    @Transactional
    public Resultado executar(Pedido pedido) {
        String email = Usuario.normalizarEmail(pedido.email());
        if (usuarios.existsByEmail(email)) {
            throw new ErrosDeNegocio.Conflito(
                    "Já existe uma conta com esse e-mail. Se for sua, use a opção de entrar.");
        }

        String nome = sanitizador.limpar(pedido.nome());
        PoliticaDeSenha.exigirValida(pedido.senha(), email, nome);

        Documento documento = Documento.de(pedido.documento());
        if (empreendedores.existsByDocumento(documento.valor())) {
            throw new ErrosDeNegocio.Conflito(
                    "Esse CPF ou CNPJ já está cadastrado na plataforma.");
        }

        Telefone telefone = Telefone.de(pedido.telefoneWhatsapp());
        if (!telefone.ehCelular()) {
            throw new ErrosDeNegocio.RegraDeNegocio(
                    "Informe um celular com WhatsApp. É por ele que o cliente vai falar com você.");
        }

        String cep = pedido.cep() == null || pedido.cep().isBlank()
                ? null
                : Cep.de(pedido.cep()).valor();

        var agora = relogio.instant();
        var usuario = usuarios.save(Usuario.novo(
                nome, email, codificador.encode(pedido.senha()), Papel.EMPREENDEDOR, agora));

        String nomeDoNegocio = sanitizador.limpar(pedido.nomeDoNegocio());
        var apelido = geradorDeApelido.paraNegocio(nomeDoNegocio);

        var empreendedor = empreendedores.save(Empreendedor.novo(
                usuario.id(),
                nomeDoNegocio,
                apelido,
                sanitizador.limpar(pedido.descricao()),
                pedido.categoriaPrincipal(),
                pedido.bairro(),
                cep,
                telefone,
                documento,
                agora));

        outbox.gravar(Topicos.EMPREENDEDORES, new EmpreendedorCadastrado(
                UUID.randomUUID(),
                Correlacao.atual(),
                agora,
                empreendedor.id(),
                usuario.id(),
                empreendedor.nomeDoNegocio(),
                empreendedor.apelidoNaUrl(),
                empreendedor.descricao(),
                empreendedor.categoriaPrincipal(),
                empreendedor.bairro(),
                empreendedor.telefoneWhatsapp(),
                documento.valor(),
                usuario.email(),
                usuario.nome()));

        auditor.registrar(usuario.id(), "cadastro_criado", "empreendedor", empreendedor.id(),
                "Negócio " + empreendedor.nomeDoNegocio() + " em " + empreendedor.bairro());

        return new Resultado(empreendedor.id(), usuario.id(), empreendedor.apelidoNaUrl());
    }

    /**
     * O que o formulario de cadastro manda.
     *
     * <p>Nao tem foto: subir imagem no mesmo passo do cadastro faria o
     * empreendedor com internet ruim perder o formulario inteiro por causa de
     * uma foto que nao subiu. A foto entra depois, no painel dele.
     */
    public record Pedido(
            String nome,
            String email,
            String senha,
            String nomeDoNegocio,
            String descricao,
            String categoriaPrincipal,
            String bairro,
            String cep,
            String telefoneWhatsapp,
            String documento) {
    }

    public record Resultado(UUID empreendedorId, UUID usuarioId, String apelidoNaUrl) {
    }
}
