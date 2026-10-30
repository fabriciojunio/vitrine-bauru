# Checklist OWASP ASVS 5.0

*[Read this in English](asvs.en.md)*

Avaliação feita em 28/10/2026 por capítulo, com o nível 2 como alvo. Não é certificação: é a leitura
do código contra o padrão, com o arquivo ou o teste que sustenta cada resposta. "Parcial" quer dizer
que uma parte do capítulo está coberta e a parte que falta está escrita.

| Capítulo | Situação | Evidência e o que falta |
|---|---|---|
| V1 Codificação e sanitização | Atende | Bean Validation em todo corpo; jsoup no texto livre; JPA com parâmetro; quebra de linha removida antes do log (`SanitizadorTest`, `FiltroDeLimiteTest`). |
| V2 Validação e lógica de negócio | Atende | Moderação como máquina de estados no domínio (`EmpreendedorTest`); a loja de entrada da demonstração não pode ser excluída nem suspensa (`DemonstracaoTest`). |
| V3 Segurança do front | Parcial | CSP na API e na Vercel restrita ao host da API, HSTS, `nosniff`, `frame-ancestors 'none'`. Falta: o token de renovação fica no `localStorage` (ADR 0010). |
| V4 API e serviços web | Atende | CORS com lista explícita, API sem cookie (CSRF não se aplica, aceite em `.github/codeql-accepted.json`), limite por endereço nas rotas sensíveis. |
| V5 Arquivos | Atende | Tipo pelos bytes, 5 MB por arquivo e 50 MB por loja, imagem no banco com nome sorteado e `nosniff` na entrega (`TipoDeImagemTest`). |
| V6 Autenticação | Parcial | bcrypt 12, bloqueio após 5 erros, mesma resposta e mesmo tempo para conta inexistente. Falta: 2FA para a SEDECON e checagem contra senhas vazadas. |
| V7 Sessão | Atende | Acesso de 15 min em memória, renovação de 7 dias guardada como SHA-256, rotação e detecção de reuso; exclusão derruba as sessões. |
| V8 Autorização | Atende | Negação por padrão, papel por rota conferido em `SegurancaHttpTest`, dono da loja sempre do token. |
| V9 Tokens autocontidos | Parcial | Segredo de 32+ bytes validado na subida, emissor e validade conferidos. Falta: `aud` no token, e o e-mail do usuário vai no corpo do token. |
| V10 OAuth e OIDC | Não se aplica | A API emite os próprios tokens. |
| V11 Criptografia | Atende | Nada cifrado à mão; senha com bcrypt, renovação com SHA-256. O TLS até o banco vem do `sslmode=require` na URL de implantação (`docs/IMPLANTACAO.md`); o código não força. |
| V12 Comunicação | Atende no ar | HTTPS na Vercel e no Render, TLS no Neon. Na versão Kubernetes não há mTLS entre serviços. |
| V13 Configuração | Atende | Segredos só por ambiente, aplicação recusa subir com segredo curto, imagem sem root, modo demonstração desligado por padrão, actions por SHA. |
| V14 Proteção de dados | Atende | Exportação e exclusão LGPD com saga e prazo (ADR 0004); documento mascarado; consumidor sem rastreio. |
| V15 Código e arquitetura | Atende | CodeQL `security-extended` em Java e TypeScript, OSV no SBOM, gitleaks, Dependabot com espera de 7 dias, ArchUnit separando as camadas. Bibliotecas do Boot 3.5 com falha estão fixadas em versão corrigida no `pom.xml`. |
| V16 Logs e erros | Parcial | JSON com correlação, erro genérico para o cliente, auditoria da moderação. Falta: log fora do Render e alerta. |
| V17 WebRTC | Não se aplica | |

## O que vem primeiro

Pela ordem de risco: migrar para o Spring Boot 4, porque a linha 3.5 não recebe mais correção
gratuita e as duas falhas aceitas do Spring MVC vencem em 28/01/2027; 2FA para a conta da SEDECON,
que modera a cidade inteira; e log fora do servidor.
