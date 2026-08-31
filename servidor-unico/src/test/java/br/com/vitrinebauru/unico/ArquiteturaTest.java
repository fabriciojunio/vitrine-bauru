package br.com.vitrinebauru.unico;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * As regras que sustentam a separação entre os serviços.
 *
 * <p>Este é o teste que dá sentido a rodar os quatro módulos num processo só.
 * Quando tudo está no mesmo classpath, nada impede o catálogo de chamar o
 * repositório do cadastro e economizar um evento; funcionaria, e a separação
 * viraria enfeite. É aqui que essa tentação vira build vermelho.
 *
 * <p>Cada regra abaixo já foi, em algum projeto, uma decisão de arquitetura
 * que ninguém lembrou de seguir seis meses depois.
 */
@DisplayName("Regras de arquitetura")
class ArquiteturaTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.vitrinebauru");
    }

    @Nested
    @DisplayName("fronteira entre os serviços")
    class FronteiraEntreOsServicos {

        @Test
        @DisplayName("o cadastro não conhece as classes do catálogo, da busca nem das notificações")
        void cadastroNaoConheceOsOutros() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage("br.com.vitrinebauru.cadastro..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "br.com.vitrinebauru.catalogo..",
                            "br.com.vitrinebauru.busca..",
                            "br.com.vitrinebauru.notificacoes..")
                    .because("serviços conversam por evento, e não por chamada direta de classe");

            regra.check(classes);
        }

        @Test
        @DisplayName("o catálogo não conhece as classes dos outros serviços")
        void catalogoNaoConheceOsOutros() {
            noClasses()
                    .that().resideInAPackage("br.com.vitrinebauru.catalogo..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "br.com.vitrinebauru.cadastro..",
                            "br.com.vitrinebauru.busca..",
                            "br.com.vitrinebauru.notificacoes..")
                    .check(classes);
        }

        @Test
        @DisplayName("a busca não conhece as classes dos outros serviços")
        void buscaNaoConheceOsOutros() {
            noClasses()
                    .that().resideInAPackage("br.com.vitrinebauru.busca..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "br.com.vitrinebauru.cadastro..",
                            "br.com.vitrinebauru.catalogo..",
                            "br.com.vitrinebauru.notificacoes..")
                    .check(classes);
        }

        @Test
        @DisplayName("as notificações não conhecem as classes dos outros serviços")
        void notificacoesNaoConhecemOsOutros() {
            noClasses()
                    .that().resideInAPackage("br.com.vitrinebauru.notificacoes..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "br.com.vitrinebauru.cadastro..",
                            "br.com.vitrinebauru.catalogo..",
                            "br.com.vitrinebauru.busca..")
                    .check(classes);
        }

        @Test
        @DisplayName("nenhum serviço acessa o repositório de outro")
        void ninguemUsaRepositorioAlheio() {
            noClasses()
                    .that().resideOutsideOfPackage("br.com.vitrinebauru.cadastro..")
                    .and().resideOutsideOfPackage("br.com.vitrinebauru.unico..")
                    .should().dependOnClassesThat()
                    .haveNameMatching("br\\.com\\.vitrinebauru\\.cadastro\\.infraestrutura\\.persistencia\\..*")
                    .because("ler a tabela do vizinho é o jeito silencioso de acabar com a separação")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("camadas dentro de cada serviço")
    class CamadasDentroDeCadaServico {

        @Test
        @DisplayName("o domínio não depende de web, de API nem de mensageria")
        void dominioNaoDependeDeInfraestrutura() {
            noClasses()
                    .that().resideInAnyPackage(
                            "br.com.vitrinebauru.cadastro.dominio..",
                            "br.com.vitrinebauru.catalogo.dominio..",
                            "br.com.vitrinebauru.busca.dominio..",
                            "br.com.vitrinebauru.notificacoes.dominio..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..api..",
                            "..infraestrutura..",
                            "org.springframework.web..",
                            "org.springframework.kafka..")
                    .because("regra de negócio não pode saber por onde a requisição chegou")
                    .check(classes);
        }

        @Test
        @DisplayName("a API não conversa direto com a mensageria")
        void apiNaoFalaComMensageria() {
            noClasses()
                    .that().resideInAPackage("..api..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.kafka..", "org.apache.kafka..")
                    .because("publicar evento é trabalho do caso de uso, dentro da transação")
                    .check(classes);
        }

        @Test
        @DisplayName("as camadas de cadastro respeitam a ordem api, aplicação, domínio")
        void camadasDoCadastro() {
            layeredArchitecture()
                    .consideringOnlyDependenciesInAnyPackage("br.com.vitrinebauru.cadastro..")
                    .layer("Api").definedBy("br.com.vitrinebauru.cadastro.api..")
                    .layer("Aplicacao").definedBy("br.com.vitrinebauru.cadastro.aplicacao..")
                    .layer("Dominio").definedBy("br.com.vitrinebauru.cadastro.dominio..")
                    .layer("Infraestrutura").definedBy("br.com.vitrinebauru.cadastro.infraestrutura..")
                    .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Aplicacao").mayOnlyBeAccessedByLayers("Api", "Infraestrutura")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("convenções que evitam erro bobo")
    class Convencoes {

        @Test
        @DisplayName("controller se chama Controller")
        void controllerSeChamaController() {
            classes()
                    .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .check(classes);
        }

        @Test
        @DisplayName("repositório fica na camada de persistência")
        void repositorioFicaNaPersistencia() {
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().resideInAnyPackage("..persistencia..", "..outbox..", "..inbox..")
                    .check(classes);
        }

        @Test
        @DisplayName("ninguém escreve na saída padrão: log existe para isso")
        void ninguemUsaSystemOut() {
            noClasses()
                    .should().accessField(System.class, "out")
                    .orShould().accessField(System.class, "err")
                    .because("mensagem em System.out não tem nível, não tem correlação e "
                            + "some no servidor de produção")
                    .check(classes);
        }

        @Test
        @DisplayName("ninguém usa o relógio do sistema direto")
        void ninguemUsaInstantNow() {
            noClasses()
                    .that().resideInAnyPackage(
                            "br.com.vitrinebauru.cadastro.aplicacao..",
                            "br.com.vitrinebauru.catalogo.aplicacao..",
                            "br.com.vitrinebauru.busca.aplicacao..",
                            "br.com.vitrinebauru.notificacoes.aplicacao..")
                    .should().callMethod(java.time.Instant.class, "now")
                    .because("o relógio é injetado, senão testar prazo e expiração exige "
                            + "dormir de verdade no teste")
                    .check(classes);
        }

        @Test
        @DisplayName("nenhum endereço da API devolve entidade JPA")
        void entidadeNaoViraResposta() {
            // A regra é sobre o que sai como JSON, e não sobre o que o
            // controller toca: converter a entidade em resposta é justamente o
            // trabalho dele. O que não pode é a entidade ser serializada, com
            // senha, documento inteiro e todo campo que alguém acrescentar
            // nela amanhã.
            noMethods()
                    .that().arePublic()
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
                    .should().haveRawReturnType(describe("uma entidade JPA",
                            tipo -> tipo.isAnnotatedWith(jakarta.persistence.Entity.class)))
                    .because("entidade carrega senha e documento inteiro, e um campo novo "
                            + "nela vazaria para o JSON sem ninguém decidir isso")
                    .check(classes);
        }
    }
}
