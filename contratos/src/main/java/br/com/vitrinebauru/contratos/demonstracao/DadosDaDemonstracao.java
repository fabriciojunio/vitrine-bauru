package br.com.vitrinebauru.contratos.demonstracao;

import java.util.List;
import java.util.UUID;

/**
 * As lojas e os produtos do modo demonstração.
 *
 * <p>Fica no módulo de contratos porque os identificadores precisam ser os
 * mesmos nos quatro serviços: o produto que o catálogo semeia tem que apontar
 * para o empreendedor que o cadastro semeou, senão a demonstração sobe com
 * produto órfão e loja vazia.
 *
 * <p>Os identificadores são fixos, e não sorteados, pelo mesmo motivo: semear
 * duas vezes precisa dar no mesmo resultado, e o mesmo endereço de loja
 * precisa continuar funcionando depois de a demonstração ser reiniciada.
 *
 * <p>Tudo aqui é fictício. Os negócios não existem, os telefones são da faixa
 * de teste e os documentos foram gerados só para fechar o dígito verificador.
 * Nenhum empreendedor de verdade atendido pela SEDECON aparece nesta lista.
 */
public final class DadosDaDemonstracao {

    public static final String SENHA_PADRAO = "demonstracao2026";

    public static final UUID ADMIN_USUARIO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    public static final String ADMIN_EMAIL = "sedecon@demo.vitrinebauru.com.br";
    public static final String ADMIN_NOME = "Analista da SEDECON";

    private DadosDaDemonstracao() {
    }

    /**
     * @param fotoDeCapaUrl deixado nulo de propósito: a interface desenha um
     *                      fundo próprio a partir da categoria, e assim a
     *                      demonstração não depende de nenhum servidor de
     *                      imagem de terceiro para não ficar quebrada.
     */
    public record Loja(
            UUID empreendedorId,
            UUID usuarioId,
            String responsavel,
            String email,
            String nomeDoNegocio,
            String apelidoNaUrl,
            String descricao,
            String categoria,
            String bairro,
            String telefone,
            String documento,
            String fotoDeCapaUrl) {
    }

    public record Produto(
            UUID produtoId,
            UUID empreendedorId,
            String nome,
            String descricao,
            Long precoEmCentavos,
            String categoria,
            boolean disponivel) {
    }

    private static UUID loja(int numero) {
        return UUID.fromString("d0000000-0000-4000-8000-1000000000%02d".formatted(numero));
    }

    private static UUID usuario(int numero) {
        return UUID.fromString("d0000000-0000-4000-8000-2000000000%02d".formatted(numero));
    }

    private static UUID produto(int numero) {
        return UUID.fromString("d0000000-0000-4000-8000-3000000000%02d".formatted(numero));
    }

    public static List<Loja> lojas() {
        return List.of(
                new Loja(loja(1), usuario(1), "Maria de Lourdes Prado",
                        "lourdes@demo.vitrinebauru.com.br", "Doces da Lourdes", "doces-da-lourdes",
                        "Bolo de pote, brigadeiro e torta salgada por encomenda. Faço há 12 anos, "
                                + "sempre com receita de família. Encomendas com 24h de antecedência.",
                        "Alimentação", "Vila Cardia", "14997010101", "17170037000160", null),

                new Loja(loja(2), usuario(2), "Antônio Pereira",
                        "pereira@demo.vitrinebauru.com.br", "Marcenaria Irmãos Pereira",
                        "marcenaria-irmaos-pereira",
                        "Móveis sob medida em madeira maciça e MDF. Fazemos projeto, montagem e "
                                + "reparo. Orçamento sem compromisso na sua casa.",
                        "Casa e construção", "Jardim Europa", "14997020202", "17170074000179", null),

                new Loja(loja(3), usuario(3), "Rosângela Matos",
                        "rosangela@demo.vitrinebauru.com.br", "Ateliê Fio de Prosa", "atelie-fio-de-prosa",
                        "Crochê e tricô feitos à mão: mantas, tapetes, roupinha de bebê e peças "
                                + "sob encomenda na cor que você quiser.",
                        "Artesanato", "Vila Falcão", "14997030303", "17031415985", null),

                new Loja(loja(4), usuario(4), "Nara Siqueira",
                        "nara@demo.vitrinebauru.com.br", "Studio Nara Cabelo e Estética",
                        "studio-nara-cabelo-e-estetica",
                        "Corte, coloração, escova e design de sobrancelha. Atendimento com hora "
                                + "marcada, de terça a sábado.",
                        "Beleza e bem-estar", "Jardim Estoril", "14997040404", "17170111000149", null),

                new Loja(loja(5), usuario(5), "Edson Ribeiro",
                        "edson@demo.vitrinebauru.com.br", "Conserta Tudo Eletro", "conserta-tudo-eletro",
                        "Conserto de máquina de lavar, geladeira, micro-ondas e fogão. Vou até "
                                + "sua casa. Orçamento na hora e garantia de 90 dias.",
                        "Assistência técnica", "Núcleo Habitacional Mary Dota", "14997050505",
                        "17062831808", null),

                new Loja(loja(6), usuario(6), "Cleusa Aparecida Dias",
                        "cleusa@demo.vitrinebauru.com.br", "Sabor da Roça Marmitas", "sabor-da-roca-marmitas",
                        "Marmita caseira congelada e do dia. Comida de verdade, arroz, feijão, "
                                + "mistura e salada. Entrego na região.",
                        "Alimentação", "Vila Independência", "14997060606", "17170148000177", null),

                new Loja(loja(7), usuario(7), "Fabiana Toledo",
                        "fabiana@demo.vitrinebauru.com.br", "Pet Amigo Banho e Tosa", "pet-amigo-banho-e-tosa",
                        "Banho, tosa higiênica e tosa na tesoura. Levo e trago no bairro sem "
                                + "custo. Ambiente calmo, sem gaiola de espera.",
                        "Pet", "Jardim Brasil", "14997070707", "17170185000185", null),

                new Loja(loja(8), usuario(8), "Ivone Rodrigues",
                        "ivone@demo.vitrinebauru.com.br", "Reforço Escolar Dona Ivone",
                        "reforco-escolar-dona-ivone",
                        "Reforço de matemática e português do 1º ao 9º ano. Aula em grupo pequeno "
                                + "ou individual. Professora aposentada da rede municipal.",
                        "Educação e aulas", "Vila Universitária", "14997080808", "17094247702", null),

                new Loja(loja(9), usuario(9), "José Carlos Amorim",
                        "zeca@demo.vitrinebauru.com.br", "Bicicletaria do Zé", "bicicletaria-do-ze",
                        "Conserto de bicicleta, troca de câmara, regulagem de marcha e freio. "
                                + "Serviço rápido, na hora, enquanto você espera.",
                        "Serviços gerais", "Vila Seabra", "14997090909", "17125663699", null),

                new Loja(loja(10), usuario(10), "Sueli Nakamura",
                        "sueli@demo.vitrinebauru.com.br", "Costura e Cia Reformas de Roupa",
                        "costura-e-cia-reformas-de-roupa",
                        "Ajuste de roupa, troca de zíper, barra e reforma de vestido de festa. "
                                + "Trabalho com peça delicada e alfaiataria.",
                        "Moda e acessórios", "Centro", "14997101010", "17170222000155", null),

                new Loja(loja(11), usuario(11), "Marcos Vinícius Leal",
                        "marcos@demo.vitrinebauru.com.br", "Festa Boa Locação", "festa-boa-locacao",
                        "Aluguel de mesa, cadeira, toalha e kit de festa infantil. Entrega e "
                                + "retirada inclusas em Bauru.",
                        "Eventos e festas", "Parque Vista Alegre", "14997111111", "17170259000183", null),

                new Loja(loja(12), usuario(12), "Wagner Alves",
                        "wagner@demo.vitrinebauru.com.br", "Auto Elétrica Central", "auto-eletrica-central",
                        "Auto elétrica, bateria, alternador e instalação de som. Atendimento de "
                                + "segunda a sábado, com socorro na região central.",
                        "Automotivo", "Distrito Industrial", "14997121212", "17157079585", null));
    }

    public static List<Produto> produtos() {
        return List.of(
                new Produto(produto(1), loja(1), "Bolo de pote",
                        "Massa de chocolate com recheio de brigadeiro. Pote de 250ml.",
                        1200L, "Alimentação", true),
                new Produto(produto(2), loja(1), "Cento de brigadeiro gourmet",
                        "Sabores: tradicional, ninho, churros e maracujá. Cento fechado ou sortido.",
                        9000L, "Alimentação", true),
                new Produto(produto(3), loja(1), "Torta salgada de frango",
                        "Serve 8 pessoas. Encomenda com 24h. Massa caseira.",
                        6500L, "Alimentação", true),
                new Produto(produto(4), loja(1), "Bolo de aniversário decorado",
                        "Decoração combinada por WhatsApp. Preço conforme tamanho e recheio.",
                        null, "Alimentação", true),

                new Produto(produto(5), loja(2), "Estante de madeira maciça",
                        "Feita sob medida. Preço varia com a madeira e o tamanho do vão.",
                        null, "Casa e construção", true),
                new Produto(produto(6), loja(2), "Banco de jardim rústico",
                        "Madeira tratada, 1,2m. Pronta entrega.",
                        38000L, "Casa e construção", true),
                new Produto(produto(7), loja(2), "Conserto e restauro de móvel antigo",
                        "Recuperação de cadeira, mesa e guarda-roupa. Avaliação sem custo.",
                        null, "Casa e construção", true),

                new Produto(produto(8), loja(3), "Manta de crochê de casal",
                        "Fio antialérgico, na cor que você escolher. Feita em 15 dias.",
                        22000L, "Artesanato", true),
                new Produto(produto(9), loja(3), "Tapete oval de barbante",
                        "1,00m x 0,60m. Lavável em máquina.",
                        8500L, "Artesanato", true),
                new Produto(produto(10), loja(3), "Saída de maternidade em tricô",
                        "Conjunto com casaco, touca e sapatinho.",
                        16000L, "Artesanato", false),

                new Produto(produto(11), loja(4), "Corte feminino",
                        "Corte com lavagem e escova. Hora marcada.",
                        6000L, "Beleza e bem-estar", true),
                new Produto(produto(12), loja(4), "Coloração completa",
                        "Tinta profissional. Preço a partir de, conforme comprimento.",
                        14000L, "Beleza e bem-estar", true),
                new Produto(produto(13), loja(4), "Design de sobrancelha com henna",
                        "Duração aproximada de 40 minutos.",
                        4500L, "Beleza e bem-estar", true),

                new Produto(produto(14), loja(5), "Conserto de máquina de lavar",
                        "Visita, diagnóstico e orçamento. Peça cobrada à parte.",
                        12000L, "Assistência técnica", true),
                new Produto(produto(15), loja(5), "Troca de resistência de chuveiro",
                        "Atendimento no mesmo dia na região.",
                        7000L, "Assistência técnica", true),
                new Produto(produto(16), loja(5), "Reparo de micro-ondas",
                        "Garantia de 90 dias no serviço.",
                        11000L, "Assistência técnica", true),

                new Produto(produto(17), loja(6), "Marmita do dia",
                        "Arroz, feijão, mistura, salada e guarnição. Retirada ou entrega.",
                        1800L, "Alimentação", true),
                new Produto(produto(18), loja(6), "Kit 10 marmitas congeladas",
                        "Cardápio variado da semana. Entrega às segundas.",
                        16000L, "Alimentação", true),
                new Produto(produto(19), loja(6), "Feijoada aos sábados",
                        "Porção individual, com couve, farofa e laranja.",
                        2800L, "Alimentação", true),

                new Produto(produto(20), loja(7), "Banho e tosa higiênica de porte pequeno",
                        "Cachorro até 10kg. Inclui corte de unha e limpeza de ouvido.",
                        5500L, "Pet", true),
                new Produto(produto(21), loja(7), "Tosa na tesoura",
                        "Para pelagem longa. Agendar com antecedência.",
                        9000L, "Pet", true),
                new Produto(produto(22), loja(7), "Leva e traz no bairro",
                        "Sem custo para Jardim Brasil e vizinhança.",
                        0L, "Pet", true),

                new Produto(produto(23), loja(8), "Reforço de matemática",
                        "Aula de 1 hora, individual, do 1º ao 9º ano.",
                        5000L, "Educação e aulas", true),
                new Produto(produto(24), loja(8), "Pacote mensal em grupo",
                        "Duas aulas por semana, grupo de até 4 alunos.",
                        28000L, "Educação e aulas", true),
                new Produto(produto(25), loja(8), "Acompanhamento de dever de casa",
                        "Segunda a quinta, das 14h às 16h.",
                        22000L, "Educação e aulas", true),

                new Produto(produto(26), loja(9), "Revisão geral da bicicleta",
                        "Freio, marcha, corrente e lubrificação.",
                        7000L, "Serviços gerais", true),
                new Produto(produto(27), loja(9), "Troca de câmara de ar",
                        "Aro 26 ao 29. Serviço na hora.",
                        3500L, "Serviços gerais", true),
                new Produto(produto(28), loja(9), "Montagem de bicicleta nova",
                        "Para bicicleta comprada pela internet.",
                        9000L, "Serviços gerais", true),

                new Produto(produto(29), loja(10), "Barra de calça",
                        "Comum ou original. Fica pronta em 2 dias.",
                        2500L, "Moda e acessórios", true),
                new Produto(produto(30), loja(10), "Troca de zíper de jaqueta",
                        "Zíper reforçado incluso.",
                        4500L, "Moda e acessórios", true),
                new Produto(produto(31), loja(10), "Ajuste de vestido de festa",
                        "Prova marcada. Preço conforme a peça.",
                        null, "Moda e acessórios", true),

                new Produto(produto(32), loja(11), "Kit mesa e 4 cadeiras",
                        "Diária. Entrega e retirada inclusas.",
                        3000L, "Eventos e festas", true),
                new Produto(produto(33), loja(11), "Kit festa infantil",
                        "Mesa decorada, 6 mesas de convidado e 24 cadeiras.",
                        28000L, "Eventos e festas", true),
                new Produto(produto(34), loja(11), "Toalha de mesa lavada e passada",
                        "Diversas cores. Diária por unidade.",
                        1200L, "Eventos e festas", true),

                new Produto(produto(35), loja(12), "Troca de bateria",
                        "Mão de obra. Bateria cobrada à parte.",
                        6000L, "Automotivo", true),
                new Produto(produto(36), loja(12), "Revisão de alternador",
                        "Teste, limpeza e troca de escova.",
                        18000L, "Automotivo", true),
                new Produto(produto(37), loja(12), "Instalação de som automotivo",
                        "Rádio, alto-falante e módulo. Orçamento na hora.",
                        null, "Automotivo", true),
                new Produto(produto(38), loja(12), "Socorro elétrico na região central",
                        "Atendimento de segunda a sábado.",
                        8000L, "Automotivo", false));
    }
}
