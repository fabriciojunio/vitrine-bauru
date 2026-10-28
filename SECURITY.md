# Segurança

*[Read this in English](SECURITY.en.md)*

A plataforma guarda CPF, CNPJ, telefone e e-mail de pequenos empreendedores de
verdade, e leva o nome da SEDECON. Este documento diz o que foi feito, o que
não foi, e como avisar de um problema.

## Como avisar de uma falha

Use o aviso privado do GitHub: aba **Security**, botão **Report a
vulnerability**. Descreva o que encontrou e como reproduzir. A resposta inicial
sai em até sete dias.

Não abra issue pública com detalhe de falha explorável.

Este repositório é a versão de portfólio. A instalação da SEDECON é mantida
pela prefeitura à parte; falha nela vai para `desenvolvimento@bauru.sp.gov.br`.

## O que está feito

O detalhe por ameaça está no [modelo de ameaças](docs/seguranca/modelo-de-ameacas.md), e a leitura
capítulo a capítulo do OWASP ASVS 5.0 no [checklist](docs/seguranca/asvs.md).

**Senha.** Bcrypt com custo 12. O custo fica gravado no hash, então subi-lo no
futuro não invalida as senhas já cadastradas. A política exige oito caracteres
e barra as senhas mais óbvias, sem exigir símbolo e maiúscula: quem usa esta
plataforma tem pouca familiaridade digital, e regra de composição produz
"Senha@123" anotada num papel colado no balcão.

**Sessão.** Token de acesso de quinze minutos, token de renovação de sete dias
guardado como resumo SHA-256, com rotação a cada uso. Token queimado que
reaparece derruba todas as sessões daquele usuário, porque só há duas
explicações para isso e as duas são motivo para desconfiar.

**Bloqueio de conta.** Cinco senhas erradas travam a conta por quinze minutos.
O contador sobrevive à exceção que recusa o login, o que parece óbvio e não é:
a primeira versão perdia a contagem no rollback da própria transação, e foi um
teste de integração que mostrou.

**Enumeração de usuário.** A resposta para e-mail inexistente é idêntica à de
senha errada, inclusive no tempo: quando o e-mail não existe, o sistema confere
a senha contra um hash de mentira, para o tempo de resposta não entregar o que
a mensagem esconde.

**Limite de requisição.** Por endereço de origem, com regras por caminho. O
filtro usa `getRemoteAddr`, depois que o Tomcat resolve os proxies confiáveis
com `server.forward-headers-strategy=native`. A cadeia é percorrida da direita
para a esquerda: prefixar um IP inventado no `X-Forwarded-For` não cria outro
balde. A implantação deve restringir `server.tomcat.remoteip.internal-proxies`
e `server.tomcat.remoteip.trusted-proxies` aos proxies da sua rede; nunca
configure uma expressão que confie em qualquer origem. No acesso direto por
IP público, o cabeçalho informado pelo cliente é ignorado.

**Upload.** O tipo é descoberto pelos primeiros bytes do arquivo, nunca pela
extensão nem pelo cabeçalho enviado. Um arquivo chamado `foto.jpg`, anunciado
como `image/jpeg`, pode ser HTML com script dentro; servido de volta no mesmo
domínio, ele executaria. Limite de 5 MB por arquivo e 50 MB por loja, com nome
sorteado, o que também resolve travessia de caminho.

**Texto livre.** Limpo com jsoup na entrada, além do escape que o React já faz
na saída. Duas camadas porque a descrição do produto também vai para o corpo do
e-mail e para o JSON da API, que não passam pelo navegador.

**SQL.** Tudo por JPA ou consulta com parâmetro. Não há concatenação de string
em consulta em lugar nenhum, e há teste com carga de injeção no campo de busca.

**Cabeçalhos.** CSP, HSTS, `X-Frame-Options: DENY`, `X-Content-Type-Options:
nosniff` e `Referrer-Policy: no-referrer` em toda resposta, conferidos por
teste.

**CORS.** Lista explícita de origem, nunca `*`, com o domínio vindo do
ambiente. Origem desconhecida é recusada, e há teste para isso.

**Segredos.** Nada de credencial no repositório. O segredo do token vem do
ambiente e a aplicação se recusa a subir se ele tiver menos de 32 bytes ou se a
validade do acesso passar de uma hora.

**Dado sensível em log.** O documento aparece mascarado em tela e em log. O
enviador de e-mail registra destinatário mascarado e assunto, nunca o corpo,
que carrega nome e motivo de recusa.

**Auditoria.** Quem aprovou, recusou ou suspendeu fica registrado com data e
correlação, e o registro sobrevive à exclusão de dados do titular: quem
responde pelo ato é quem o praticou.

**Demonstração pública.** Com `DEMO_ATIVO`, a loja em que o botão de
empreendedora entra recusa exclusão e suspensão, para um visitante não tirar a
entrada do ar para os outros, e a entrada em um clique é limitada a dez por
minuto por endereço. Fora do modo demonstração, o endereço de entrada responde
404.

**Dependência e pipeline.** Desde 28/10/2026, todo push e todo PR passam por
CodeQL (`security-extended`, Java e TypeScript), gitleaks no histórico inteiro e
OSV-Scanner no SBOM CycloneDX gerado pelo Maven e no lockfile do npm. O
Spring Boot 3.5 saiu do suporte gratuito em junho de 2026; as bibliotecas que
ele traz com falha conhecida (Tomcat, Jackson, driver do Postgres, log4j-api,
httpcomponents, lz4, snappy, OpenTelemetry, commons-lang3, BouncyCastle) estão
fixadas em versão corrigida no `pom.xml` raiz. As duas falhas do Spring MVC que
só o Spring 7 corrige não se aplicam aqui e estão aceitas com prazo em
`osv-scanner.toml`. Achado sem aceite registrado em
`.github/codeql-accepted.json` reprova o fluxo, e aceite vencido também. O
Dependabot propõe atualização semanal com espera de sete dias, as actions são
fixadas por SHA e cada fluxo declara só a permissão que usa.

## O que não está feito, e por quê

**CSRF está desligado.** A autenticação vai no cabeçalho `Authorization`, e não
em cookie. Token que o navegador não anexa sozinho não é vulnerável a
requisição forjada de outro site. Se um dia a sessão virar cookie, a proteção
precisa voltar, e há um comentário no código dizendo isso.

**O token de renovação fica no `localStorage`.** É exposto a XSS. A alternativa
seria cookie `HttpOnly`, que com front-end e API em domínios diferentes exige
`SameSite=None` e traz o problema de CSRF de volta. A troca foi assumida, e o
que a sustenta é a CSP do back-end, a CSP da Vercel e a regra de nunca injetar
HTML de usuário na tela. O token de acesso, esse, fica só em memória.

**O limite de requisição é por instância.** O balde vive na memória do
processo. Com várias instâncias, o limite efetivo multiplica. Um contador
compartilhado exigiria Redis, que custa dinheiro e vira ponto único de falha
num projeto que precisa caber em camada gratuita. Contra força bruta, limite
aproximado resolve; contra tentativa distribuída de verdade, quem resolve é o
bcrypt e o bloqueio de conta.

**Não há verificação de telefone por SMS.** Tem custo por mensagem. A aprovação
manual da SEDECON cobre boa parte do risco, que é justamente o papel do fluxo
pendente para aprovado.

**Não há proteção contra robô no cadastro.** O Cloudflare Turnstile está
previsto para a fase seguinte. Hoje o que existe é o limite de dez cadastros
por hora por endereço e a moderação humana antes de qualquer coisa aparecer.

## LGPD

Os dois direitos que mais aparecem estão implementados e funcionando, com
endereço próprio na API e botão na tela:

- **Ver o que a plataforma guarda**: `GET /api/cadastro/privacidade/meus-dados`
  devolve um arquivo com a conta, o negócio e o histórico de ações.
- **Pedir a exclusão**: `DELETE /api/cadastro/privacidade/minha-conta` abre a
  saga que apaga os dados nos quatro serviços, com prazo, reenvio e alerta se
  o prazo legal estourar. O detalhe está na
  [decisão 4](docs/adr/0004-saga-de-exclusao-lgpd.md).

Do consumidor que só olha a vitrine, nada é guardado: não há conta, não há
cookie de rastreamento e o registro de contato não tem IP, sessão nem qualquer
identificador. Dá para contar sem rastrear.
