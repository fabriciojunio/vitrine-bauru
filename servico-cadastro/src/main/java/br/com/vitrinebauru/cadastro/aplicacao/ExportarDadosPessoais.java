package br.com.vitrinebauru.cadastro.aplicacao;

import br.com.vitrinebauru.cadastro.dominio.Empreendedor;
import br.com.vitrinebauru.cadastro.dominio.Usuario;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.AuditoriaRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.SessaoDeRenovacaoRepository;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.UsuarioRepository;
import br.com.vitrinebauru.contratos.tipos.Documento;
import br.com.vitrinebauru.contratos.tipos.Telefone;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entrega ao titular tudo que a plataforma guarda sobre ele (LGPD, artigo 18,
 * incisos I e II).
 *
 * <p>Aqui o documento sai inteiro, e não mascarado como no resto do sistema:
 * quem pede é o dono, e o objetivo do artigo é justamente mostrar o que está
 * guardado. Mascarar aqui seria confundir sigilo com transparência.
 *
 * <p>O arquivo cobre só o que este serviço guarda. Produto e foto são do
 * catálogo, e o próprio arquivo diz isso, com o endereço de onde pedir o
 * resto. Fingir que um serviço sabe tudo que os outros guardam seria a mentira
 * mais fácil de contar é a mais difícil de sustentar.
 */
@Component
public class ExportarDadosPessoais {

    private final UsuarioRepository usuarios;
    private final EmpreendedorRepository empreendedores;
    private final AuditoriaRepository auditoria;
    private final SessaoDeRenovacaoRepository sessoes;
    private final Auditor auditor;

    public ExportarDadosPessoais(UsuarioRepository usuarios, EmpreendedorRepository empreendedores,
                                 AuditoriaRepository auditoria, SessaoDeRenovacaoRepository sessoes,
                                 Auditor auditor) {
        this.usuarios = usuarios;
        this.empreendedores = empreendedores;
        this.auditoria = auditoria;
        this.sessoes = sessoes;
        this.auditor = auditor;
    }

    @Transactional
    public Arquivo executar(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Conta não encontrada."));

        Empreendedor empreendedor = empreendedores.findByUsuarioId(usuarioId).orElse(null);

        List<Acontecimento> historico = auditoria.findByUsuarioIdOrderByOcorridoEmDesc(usuarioId)
                .stream()
                .map(registro -> new Acontecimento(
                        registro.acao(), registro.entidade(), registro.detalhe(), registro.ocorridoEm()))
                .toList();

        auditor.registrar(usuarioId, "dados_exportados", "usuario", usuarioId, null);

        return new Arquivo(
                new Conta(usuario.nome(), usuario.email(), usuario.papel().name(),
                        usuario.criadoEm(), usuario.ultimoAcessoEm()),
                empreendedor == null ? null : new Negocio(
                        empreendedor.nomeDoNegocio(),
                        empreendedor.apelidoNaUrl(),
                        empreendedor.descricao(),
                        empreendedor.categoriaPrincipal(),
                        empreendedor.bairro(),
                        empreendedor.cep(),
                        Telefone.de(empreendedor.telefoneWhatsapp()).formatado(),
                        new Documento(empreendedor.documento(), empreendedor.documentoTipo()).formatado(),
                        empreendedor.status().name(),
                        empreendedor.criadoEm(),
                        empreendedor.moderadoEm(),
                        empreendedor.motivoDaModeracao()),
                historico,
                sessoes.findByUsuarioId(usuarioId).size(),
                """
                Este arquivo tem o que o serviço de cadastro guarda sobre você. \
                Seus produtos e fotos ficam no serviço de catálogo e podem ser \
                pedidos pelo mesmo canal. Nada aqui é compartilhado com terceiros.""");
    }

    public record Arquivo(Conta conta, Negocio negocio, List<Acontecimento> historico,
                          int sessoesRegistradas, String observacao) {
    }

    public record Conta(String nome, String email, String papel, Instant criadaEm, Instant ultimoAcessoEm) {
    }

    public record Negocio(String nomeDoNegocio, String apelidoNaUrl, String descricao,
                          String categoria, String bairro, String cep, String telefoneWhatsapp,
                          String documento, String situacao, Instant cadastradoEm,
                          Instant moderadoEm, String motivoDaModeracao) {
    }

    public record Acontecimento(String acao, String entidade, String detalhe, Instant quando) {
    }
}
