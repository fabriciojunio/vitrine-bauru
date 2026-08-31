# Como mexer neste projeto

Escrito para o grupo, e para quem pegar o projeto depois da entrega.

## Subindo pela primeira vez

Precisa de Java 21, Node 22 e Docker.

```bash
docker compose up -d          # broker, quatro bancos, quatro serviços, borda
cd web && npm install && npm run dev
```

Sem Docker, dá para rodar tudo num processo com um PostgreSQL local:

```bash
mvn -pl servidor-unico -am package -DskipTests
BANCO_URL=jdbc:postgresql://localhost:5432/vitrine DEMO_ATIVO=true \
  java -jar servidor-unico/target/servidor-unico-1.0.0.jar
```

## Antes de abrir uma proposta de mudança

```bash
mvn verify                 # back-end
cd web && npm run typecheck && npm test
```

Se mexeu em tela, rode também os testes de navegador, com a aplicação no ar:

```bash
cd web && npm run e2e
```

## Branch e commit

Branch a partir de `main`, com prefixo pelo tipo:

```
feat/pagina-da-loja
fix/telefone-com-nono-digito
docs/decisao-do-transporte
```

Commit no padrão convencional, em português, sem travessão:

```
feat(catalogo): permite marcar produto como esgotado sem apagar
fix(cadastro): mantem a contagem de senha errada apos o rollback
docs(adr): registra por que o transporte tem dois adaptadores
test(busca): cobre a busca sem acento com amostra gerada
```

O corpo do commit, quando existir, explica **por que**, e não o que: o que
mudou já está no diff.

## Revisão

Toda mudança passa por proposta, mesmo entre nós. Não é burocracia: é o único
momento em que uma segunda pessoa lê o código antes de ele virar o sistema que
a SEDECON vai usar.

Na revisão, olhe primeiro estas três coisas:

1. **Tem teste?** Regra nova sem teste é regra que alguém vai apagar sem
   perceber.
2. **A mensagem de erro serve para o empreendedor?** "Error 400" na tela de
   quem mal usa computador não é mensagem de erro, é um beco sem saída. Isso é
   critério de aceite do projeto.
3. **O evento continua sendo gravado dentro da transação?** Publicar fora dela
   compila, roda, e é o único jeito de errar com outbox.

## Padrões do código

**Nome em português.** Classe, método, variável, tabela e coluna. A exceção é
o que vem de framework: `findByEmail`, `@RestController`, `application.yml`.
Misturar os dois idiomas no mesmo lugar é pior que escolher um errado.

**Comentário explica decisão, não mecânica.** `// incrementa o contador` não
ajuda ninguém. `// transação própria: a exceção logo abaixo desfaria isto`
ajuda quem for mexer daqui a seis meses.

**Método com mais de quarenta linhas** provavelmente é dois métodos. Extraia
com nome claro em vez de comentar o bloco complicado.

**Nada de `System.out`.** Existe teste de arquitetura que quebra o build. Log
tem nível, tem correlação, e não some no servidor.

**Nada de `Instant.now()` na camada de aplicação.** O relógio é injetado, senão
testar prazo e expiração exige dormir de verdade no teste. Também há teste de
arquitetura para isso.

**Entidade não vira resposta de API.** Ela carrega senha e documento inteiro, e
um campo novo nela vazaria para o JSON sem ninguém decidir isso.

## Mexendo em evento

Evento é contrato entre serviços, e os quatro não são implantados no mesmo
segundo. A regra:

- **Acrescentar campo, pode.** O leitor antigo ignora o que não conhece.
- **Remover ou renomear campo, não.** Quebra quem ainda não foi atualizado.
- **Evento novo** entra na lista `permits` da interface `Evento` e na de
  subtipos do Jackson. O compilador cobra o resto.

Todo evento tem teste de ida e volta em `MapeadorDeEventosTest`. Se você
acrescentou um e não passou por lá, o teste vai avisar.

## Mexendo no banco

Migração nova em `db/<servico>/`, com número na faixa do serviço:

| Faixa | Serviço |
|---|---|
| `V1_x` | plataforma (outbox e inbox) |
| `V2_x` | cadastro |
| `V3_x` | catálogo |
| `V4_x` | busca |
| `V5_x` | notificações |

As faixas existem porque, no servidor único, o Flyway junta as cinco pastas num
histórico só, e versão repetida quebraria a subida.

Nunca altere um arquivo de migração já aplicado. Crie outro.
