package br.com.vitrinebauru.cadastro.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O que entra pela API.
 *
 * <p>As mensagens de erro são escritas para o empreendedor ler, e não para o
 * programador. "não pode ser nulo" não ajuda ninguém a preencher um
 * formulário; "Escreva o nome do seu negócio" ajuda.
 *
 * <p>A validação aqui é de formato. Regra de negócio (bairro existir em Bauru,
 * documento fechar o dígito verificador, senha não ser óbvia) fica no domínio,
 * onde vale para qualquer caminho de entrada, inclusive semeadura e teste.
 */
public final class Requisicoes {

    private Requisicoes() {
    }

    public record Cadastro(
            @NotBlank(message = "Escreva o seu nome completo")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "Informe o seu e-mail")
            @Email(message = "Esse e-mail não parece válido")
            @Size(max = 160, message = "O e-mail pode ter no máximo 160 caracteres")
            String email,

            @NotBlank(message = "Escolha uma senha")
            String senha,

            @NotBlank(message = "Escreva o nome do seu negócio")
            @Size(max = 120, message = "O nome do negócio pode ter no máximo 120 caracteres")
            String nomeDoNegocio,

            @Size(max = 600, message = "A descrição pode ter no máximo 600 caracteres")
            String descricao,

            @NotBlank(message = "Escolha a categoria do seu negócio")
            String categoriaPrincipal,

            @NotBlank(message = "Escolha o seu bairro")
            String bairro,

            @Pattern(regexp = "^$|^[0-9]{5}-?[0-9]{3}$", message = "O CEP precisa ter 8 dígitos")
            String cep,

            @NotBlank(message = "Informe o celular com WhatsApp")
            String telefoneWhatsapp,

            @NotBlank(message = "Informe o seu CPF ou o CNPJ do negócio")
            String documento) {
    }

    public record Perfil(
            @NotBlank(message = "Escreva o nome do seu negócio")
            @Size(max = 120, message = "O nome do negócio pode ter no máximo 120 caracteres")
            String nomeDoNegocio,

            @Size(max = 600, message = "A descrição pode ter no máximo 600 caracteres")
            String descricao,

            @NotBlank(message = "Escolha a categoria do seu negócio")
            String categoriaPrincipal,

            @NotBlank(message = "Escolha o seu bairro")
            String bairro,

            @Pattern(regexp = "^$|^[0-9]{5}-?[0-9]{3}$", message = "O CEP precisa ter 8 dígitos")
            String cep,

            @NotBlank(message = "Informe o celular com WhatsApp")
            String telefoneWhatsapp) {
    }

    public record Login(
            @NotBlank(message = "Informe o seu e-mail")
            String email,

            @NotBlank(message = "Informe a sua senha")
            String senha) {
    }

    public record Renovacao(
            @NotBlank(message = "Sessão inválida")
            String tokenDeRenovacao) {
    }

    public record Motivo(
            @NotBlank(message = "Escreva o motivo")
            @Size(min = 10, max = 400, message = "O motivo precisa ter de 10 a 400 caracteres")
            String motivo) {
    }

    public record FotoDeCapa(
            @NotBlank(message = "Informe o endereço da imagem")
            @Size(max = 400, message = "O endereço da imagem é longo demais")
            String url) {
    }

    public record Demonstracao(
            @NotBlank(message = "Escolha o papel da demonstração")
            String papel) {
    }
}
