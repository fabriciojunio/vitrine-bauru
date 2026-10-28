# Modelo de ameaças

*[Read this in English](modelo-de-ameacas.en.md)*

Revisado em 28/10/2026, sobre o branch `homologação`. Método STRIDE aplicado ao que roda hoje:
o servidor único no Render, o front na Vercel e o banco no Neon. A versão em quatro serviços com
borda e Kubernetes tem os mesmos fluxos; o que muda nela está no fim.

## O sistema em uma figura

```
 consumidor ──HTTPS──> front (Vercel) ──HTTPS──> API Spring (Render) ──TLS──> PostgreSQL (Neon)
 empreendedor ─┘                                    │
 SEDECON ──────┘                                    └──HTTPS──> Resend (e-mail), BrasilAPI (CNPJ)
```

Fronteiras de confiança: o navegador e tudo o que é nosso; a API e o banco; a API e os serviços de
terceiro. O GitHub Actions também cruza uma fronteira: ele tem a senha do banco da demonstração e o
gatilho de deploy, então o pipeline é parte da superfície.

Ativos, do mais sensível para o menos: CPF ou CNPJ, telefone e e-mail do empreendedor; hash de
senha e token de renovação; o registro de auditoria da moderação; produtos e fotos.

## Ameaças

### Falsificação de identidade (S)

| Ameaça | Controle | Onde está a prova |
|---|---|---|
| Adivinhar senha | bcrypt 12, bloqueio de 15 min após 5 erros, 5 tentativas por minuto por endereço | `FluxoDoCadastroTest`, `SegurancaHttpTest` |
| Descobrir quem tem conta | mesma resposta e mesmo tempo para e-mail inexistente e senha errada | `FluxoDoCadastroTest` |
| Forjar token | HMAC com segredo de 32+ bytes validado na subida, emissor conferido | `EmissorDeTokenTest` |
| Reusar token de renovação roubado | renovação guardada como SHA-256, rotação a cada uso; reuso derruba todas as sessões | `FluxoDoCadastroTest` |
| Trocar de balde no limite forjando `X-Forwarded-For` | só proxies confiáveis são lidos, da direita para a esquerda | `FiltroDeLimiteTest` |

### Adulteração (T)

| Ameaça | Controle | Onde está a prova |
|---|---|---|
| Injeção de SQL | JPA e consulta com parâmetro, carga de injeção testada na busca | `SegurancaHttpTest` |
| HTML com script na descrição | jsoup na entrada, escape do React na saída | `SanitizadorTest` |
| Foto que é HTML disfarçado | tipo lido dos primeiros bytes, `nosniff`, nome sorteado | `TipoDeImagemTest` |
| Alterar produto de outra loja | o dono sai do token, nunca da URL, e o serviço confere a posse | `FluxoDoCatalogoTest` |
| Linha falsa no log | quebra de linha removida do caminho e da origem antes de registrar | `FiltroDeLimiteTest` |

### Repúdio (R)

Aprovação, recusa, suspensão, reativação, pedido de exclusão e entrada na demonstração vão para a
tabela de auditoria com autor, data e correlação. O registro sobrevive à exclusão de dados do
titular, porque quem responde pelo ato é o moderador.

O que falta: o log de aplicação fica no Render, e quem tem acesso à conta apaga. Não há cópia num
destino separado.

### Vazamento de informação (I)

| Ameaça | Controle | Onde está a prova |
|---|---|---|
| Documento e contato em log | documento mascarado; e-mail registra destinatário mascarado e assunto, nunca o corpo | `DocumentoTest` para a máscara; o log do e-mail só por revisão de código |
| Detalhe de erro para o cliente | Problem Details genérico com correlação; pilha só no log | `SegurancaHttpTest` |
| Consumidor rastreado | sem conta, sem cookie, contato registrado sem IP nem sessão | `RegistrarContato.java` (revisão de código) |
| XSS levando o token de renovação | CSP na API (`default-src 'none'`) e na Vercel, restrita ao host exato da API desde 28/10/2026 | `web/vercel.json` |

O token de renovação fica no `localStorage`. É uma troca assumida no SECURITY.md: cookie
`HttpOnly` com front e API em domínios diferentes traria o CSRF de volta. O e-mail do usuário vai
dentro do token de acesso, legível por quem tiver o token.

### Negação de serviço (D)

Limite por endereço em login, cadastro, renovação, upload, busca, contato e, desde 28/10/2026, na
entrada da demonstração, que grava uma sessão por chamada. Upload de 5 MB por arquivo e 50 MB por
loja. O limite mora na memória de cada instância; com várias réplicas ele multiplica.

A demonstração pública tem um risco próprio. Até 28/10/2026, qualquer visitante podia entrar em um
clique na loja compartilhada e pedir a exclusão dela, ou suspendê-la entrando como SEDECON, e o
botão de entrada ficava quebrado para todos até o reinício. Agora essa loja recusa as duas ações
com o modo demonstração ligado (`DemonstracaoTest`). O reinício da madrugada também apagava o banco
antes de saber se conseguiria subir a aplicação de novo; a ordem foi invertida.

### Elevação de privilégio (E)

Moderação, notificações e actuator exigem `ADMIN_SEDECON` na configuração do Spring Security, e
`SegurancaHttpTest` confere cada rota com e sem o papel. O empreendedor só alcança a própria loja.
O papel vem do token, então rebaixar alguém só vale quando o token de acesso expira, em até
15 minutos. A suspensão já bloqueia publicação na hora, porque o catálogo confere a situação da
loja a cada escrita.

## Pipeline

| Ameaça | Controle |
|---|---|
| Action comprometida trocando código da tag | todas as actions fixadas por SHA |
| Token do fluxo usado para escrever no repositório | `permissions` mínimo em cada fluxo; os do Render não recebem nenhuma |
| Dependência com falha conhecida | OSV-Scanner no SBOM do Maven e no lockfile do npm, Dependabot com 7 dias de espera |
| Segredo no histórico | gitleaks no histórico inteiro em todo push |

## Na versão em quatro serviços

A borda é o único ponto exposto, os serviços falam por rede interna com `NetworkPolicy`, os pods
rodam sem root e com sistema de arquivos somente leitura. Não há mTLS entre os serviços: dentro do
cluster a confiança é da rede, não de certificado.

## Como este documento é mantido

Mudou um fluxo de autenticação, uma rota com dono, um dado guardado ou o pipeline, este arquivo e o
[checklist ASVS](asvs.md) mudam no mesmo PR.
