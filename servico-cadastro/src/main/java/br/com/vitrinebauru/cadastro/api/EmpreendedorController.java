package br.com.vitrinebauru.cadastro.api;

import br.com.vitrinebauru.cadastro.aplicacao.AtualizarPerfil;
import br.com.vitrinebauru.cadastro.aplicacao.ConsultarIndicadores;
import br.com.vitrinebauru.cadastro.aplicacao.RegistrarEmpreendedor;
import br.com.vitrinebauru.contratos.BairrosDeBauru;
import br.com.vitrinebauru.contratos.CategoriasDoComercio;
import br.com.vitrinebauru.cadastro.infraestrutura.persistencia.EmpreendedorRepository;
import br.com.vitrinebauru.plataforma.seguranca.UsuarioAutenticado;
import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * O lado do empreendedor: cadastrar-se e cuidar da própria loja.
 *
 * <p>Todo endereço daqui fala de "meu" e resolve o empreendedor pelo token,
 * nunca por identificador na URL. É o que impede o erro mais comum de
 * autorização em API: aceitar o id do dono como parâmetro e esquecer de
 * conferir se ele bate com quem está logado.
 */
@RestController
@RequestMapping("/api/cadastro")
@Tag(name = "Empreendedor", description = "Cadastro e perfil de quem vende")
public class EmpreendedorController {

    private final RegistrarEmpreendedor registrarEmpreendedor;
    private final AtualizarPerfil atualizarPerfil;
    private final ConsultarIndicadores indicadores;
    private final EmpreendedorRepository empreendedores;

    public EmpreendedorController(RegistrarEmpreendedor registrarEmpreendedor,
                                  AtualizarPerfil atualizarPerfil,
                                  ConsultarIndicadores indicadores,
                                  EmpreendedorRepository empreendedores) {
        this.registrarEmpreendedor = registrarEmpreendedor;
        this.atualizarPerfil = atualizarPerfil;
        this.indicadores = indicadores;
        this.empreendedores = empreendedores;
    }

    @PostMapping("/empreendedores")
    @Operation(summary = "Criar conta de empreendedor e entrar na fila da SEDECON")
    public ResponseEntity<Respostas.CadastroCriado> cadastrar(
            @Valid @RequestBody Requisicoes.Cadastro pedido) {

        var resultado = registrarEmpreendedor.executar(new RegistrarEmpreendedor.Pedido(
                pedido.nome(), pedido.email(), pedido.senha(), pedido.nomeDoNegocio(),
                pedido.descricao(), pedido.categoriaPrincipal(), pedido.bairro(),
                pedido.cep(), pedido.telefoneWhatsapp(), pedido.documento()));

        var corpo = new Respostas.CadastroCriado(
                resultado.empreendedorId(),
                resultado.apelidoNaUrl(),
                "Cadastro enviado. A SEDECON vai analisar e você recebe a resposta por e-mail.");

        return ResponseEntity.created(URI.create("/api/cadastro/minha-loja")).body(corpo);
    }

    @GetMapping("/minha-loja")
    @Operation(summary = "Ver a própria loja")
    public Respostas.MinhaLoja minhaLoja(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return Respostas.MinhaLoja.de(carregarMinha(autenticado));
    }

    @PutMapping("/minha-loja")
    @Operation(summary = "Alterar os dados da própria loja")
    public Respostas.MinhaLoja atualizar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                         @Valid @RequestBody Requisicoes.Perfil pedido) {
        var minha = carregarMinha(autenticado);

        var atualizado = atualizarPerfil.executar(minha.id(), autenticado.id(),
                new AtualizarPerfil.Pedido(pedido.nomeDoNegocio(), pedido.descricao(),
                        pedido.categoriaPrincipal(), pedido.bairro(), pedido.cep(),
                        pedido.telefoneWhatsapp()));

        return Respostas.MinhaLoja.de(atualizado);
    }

    @PutMapping("/minha-loja/foto-de-capa")
    @Operation(summary = "Trocar a foto de capa da loja")
    public Respostas.MinhaLoja trocarFoto(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                                          @Valid @RequestBody Requisicoes.FotoDeCapa pedido) {
        var minha = carregarMinha(autenticado);
        return Respostas.MinhaLoja.de(
                atualizarPerfil.trocarFotoDeCapa(minha.id(), autenticado.id(), pedido.url()));
    }

    @PostMapping("/minha-loja/reenviar")
    @Operation(summary = "Devolver o cadastro corrigido para a fila da SEDECON")
    public ResponseEntity<Respostas.MinhaLoja> reenviar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        var minha = carregarMinha(autenticado);
        atualizarPerfil.reenviarParaAnalise(minha.id(), autenticado.id());
        return ResponseEntity.status(HttpStatus.OK).body(Respostas.MinhaLoja.de(carregarMinha(autenticado)));
    }

    @GetMapping("/minha-loja/indicadores")
    @Operation(summary = "Quantos produtos e quantos contatos a loja teve")
    public ConsultarIndicadores.PainelDoEmpreendedor meusNumeros(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return indicadores.doEmpreendedor(carregarMinha(autenticado).id());
    }

    @GetMapping("/bairros")
    @Operation(summary = "Bairros de Bauru aceitos no cadastro")
    public List<String> bairros() {
        return BairrosDeBauru.todos();
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias de negócio aceitas")
    public List<String> categorias() {
        return CategoriasDoComercio.todas();
    }

    private br.com.vitrinebauru.cadastro.dominio.Empreendedor carregarMinha(
            UsuarioAutenticado autenticado) {
        UUID empreendedorId = autenticado.empreendedorId();
        if (empreendedorId == null) {
            throw new ErrosDeNegocio.NaoEncontrado("Sua conta não tem uma loja vinculada.");
        }
        return empreendedores.findById(empreendedorId).orElseThrow(() ->
                new ErrosDeNegocio.NaoEncontrado("Loja não encontrada."));
    }
}
