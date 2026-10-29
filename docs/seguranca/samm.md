# Avaliação OWASP SAMM 2.0

*[Read this in English](samm.en.md)*

Avaliado em 29/10/2026 por Fabrício Júnio, sobre o repositório e a operação da Vitrine Bauru. Escala
do SAMM: 0 (não faz), 1 (faz de forma pontual), 2 (faz de forma estruturada), 3 (faz, mede e
melhora). Meio ponto quando metade dos critérios do nível seguinte é atendida.

Avaliação feita por quem constrói o sistema, o que é um viés: o SAMM recomenda avaliador
independente. A nota vale como ponto de partida, não como certificado.

## Resultado

| Função | Prática | Atual | Alvo (jun/2027) | Evidência |
|---|---|---|---|---|
| Governança | Estratégia e métricas | 0,5 | 1 | não há metas de segurança escritas; há [registro de riscos](riscos.md) e o [relatório mensal de evidências](../../.github/workflows/evidencias.yml) |
| Governança | Política e conformidade | 1 | 1,5 | [conformidade LGPD](conformidade.md), SECURITY.md |
| Governança | Educação e orientação | 1 | 2 | [kit de Security Champions](champions/), CONTRIBUTING.md |
| Design | Avaliação de ameaças | 2 | 2 | [STRIDE](modelo-de-ameacas.md) mantido por PR, [análise PASTA e DREAD](analise-de-ameacas.md), [ATT&CK](attack.md) |
| Design | Requisitos de segurança | 1,5 | 2 | [checklist ASVS](asvs.md), requisitos RS-01 a RS-10 |
| Design | Arquitetura de segurança | 1,5 | 2 | ADRs 0001 a 0010; falta catálogo de padrões reutilizáveis |
| Implementação | Build seguro | 2 | 2,5 | actions por SHA, dependências fixadas, SBOM no build, Dependabot com 7 dias |
| Implementação | Implantação segura | 1,5 | 2 | back-end publicado só depois da CI verde em `produção` (deploy automático do Render desligado); segredos no GitHub e no Render; falta assinatura de artefato |
| Implementação | Gestão de defeitos | 1 | 2 | OSV com `ignoreUntil`, aceites do CodeQL com data de revisão, tempo de correção no relatório mensal; falta painel |
| Verificação | Avaliação de arquitetura | 1 | 1,5 | revisão contra o modelo de ameaças no PR |
| Verificação | Testes guiados por requisito | 2 | 2,5 | `SegurancaHttpTest` e testes de fluxo provam cada controle |
| Verificação | Testes de segurança | 2 | 2,5 | CodeQL, OSV, gitleaks e testes exigidos antes do merge em `produção`; ZAP contra o site publicado depois do deploy |
| Operações | Gestão de incidentes | 0,5 | 1,5 | contato em SECURITY.md e [política de divulgação](divulgacao.md); falta roteiro de incidente |
| Operações | Gestão de ambiente | 1 | 1,5 | cabeçalhos e CSP conferidos pelo ZAP; falta checagem de configuração do Render e do banco |
| Operações | Gestão operacional | 0,5 | 1,5 | sem backup próprio: depende do que o provedor do banco oferece no plano gratuito, que não foi testado; sem retenção definida |

Média atual: **1,3**. Alvo: **1,9**.

## Plano para chegar ao alvo

| Prazo | Ação | Práticas que sobem |
|---|---|---|
| dez/2026 | Segundo fator para moderador (RS-01) | Requisitos, Implantação |
| dez/2026 | Roteiro de incidente com dado pessoal, nos moldes do ConectAgente | Incidentes |
| mar/2027 | Log de auditoria em destino separado (RS-05) | Ambiente, Operacional |
| mar/2027 | Painel de achados com tempo médio de correção, alimentado pela CI | Defeitos, Métricas |
| mar/2027 | Imagem do back-end assinada com cosign e verificada no deploy | Implantação |
| jun/2027 | Teste de restauração do banco, com tempo medido | Operacional |
| jun/2027 | Treinamento de champions aplicado a uma pessoa de fora do projeto | Educação |

## O que não muda a nota de propósito

Ter ferramenta não é ter processo. O ZAP rodando sem ninguém olhar o resultado seria nível 1 em
testes de segurança; ele conta como 2 porque falha a CI e cada exceção tem motivo escrito, e os outros testes de segurança são exigidos antes do merge.
