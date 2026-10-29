# Conformidade: LGPD na Vitrine, e um exercício de PCI DSS e BACEN

*[Read this in English](conformidade.en.md)*

Revisado em 29/10/2026. A primeira parte é sobre a Vitrine de verdade. A segunda é um exercício
com um sistema financeiro fictício, porque a Vitrine não trata cartão nem é instituição
financeira, e as vagas de AppSec em banco e meio de pagamento pedem as duas normas.

Não é parecer jurídico. A base legal e o encarregado são decisão do controlador, a SEDECON.

## Parte 1. LGPD na Vitrine Bauru

### Quem e o quê

| Item | Situação |
|---|---|
| Titulares | empreendedores cadastrados; o consumidor que só olha a vitrine não tem dado guardado |
| Dados | nome, CPF ou CNPJ, e-mail, telefone, endereço do negócio, produtos e fotos |
| Dado sensível (art. 5º, II) | nenhum |
| Controlador | SEDECON, Prefeitura de Bauru |
| Base legal provável | execução de política pública pela administração (art. 7º, III); o cadastro é voluntário e o aviso diz para que serve |
| Encarregado | a definir pelo controlador |

### Obrigação por obrigação

| Obrigação | Como está | Prova |
|---|---|---|
| Transparência (art. 6º, VI e art. 9º) | aviso no envio do cadastro com link para a página de privacidade | `web/src/paginas/Cadastrar.tsx`, `Privacidade.tsx` |
| Minimização (art. 6º, III) | consumidor sem conta, sem cookie e sem IP no registro de contato | `RegistrarContato.java` |
| Acesso (art. 18, II) | `GET /api/cadastro/privacidade/meus-dados` devolve conta, negócio e histórico | botão "Meus dados e privacidade" no painel |
| Eliminação (art. 18, VI) | `DELETE /api/cadastro/privacidade/minha-conta` abre a saga nos quatro serviços, com prazo e alerta | [ADR 0004](../adr/0004-saga-de-exclusao-lgpd.md), `JornadaCompletaTest` |
| Segurança (art. 46) | controles do [modelo de ameaças](modelo-de-ameacas.md) | testes listados lá |
| Registro das operações (art. 37) | auditoria de moderação com autor e data, mantida após a exclusão | `FluxoDoCadastroTest.auditoriaRegistra` |
| Comunicação de incidente (art. 48) | contato em SECURITY.md | **falta roteiro**, ver SAMM |

### O que falta

1. Encarregado e canal do titular definidos pela SEDECON.
2. Relatório de impacto (RIPD): não é obrigatório para dado não sensível, mas a ANPD pode pedir, e
   um esboço custa pouco.
3. Prazo de retenção da conta inativa: hoje o dado fica até o titular pedir exclusão.
4. Roteiro de incidente com o prazo de três dias úteis da Resolução CD/ANPD nº 15/2024.

## Parte 2. Exercício: carteira digital fictícia "Pague Leve"

**Dados simulados.** Cenário: uma instituição de pagamento que emite cartão pré-pago e guarda o
número do cartão (PAN) para cobrança recorrente. Os controles citados existem nos projetos
[alicerce](https://github.com/fabriciojunio/alicerce) e [brecha](https://github.com/fabriciojunio/brecha),
e o exercício mostra onde cada um entraria.

### PCI DSS 4.0.1

| Requisito | O que exigiria da Pague Leve | Controle que já sei construir |
|---|---|---|
| 1. Controles de segurança de rede | ambiente do cartão (CDE) separado, só a API de pagamento alcança | NetworkPolicy e borda única do P2 |
| 2. Configuração segura | nada com padrão de fábrica, imagem mínima | distroless sem root do alicerce |
| 3. Proteger o dado guardado | PAN cifrado ou tokenizado; CVV nunca guardado | cifra de envelope com KMS, que entra no Terraform do P3 |
| 4. Cifra em trânsito | TLS forte em toda rede pública | TLS na borda, HSTS |
| 5. Proteção contra malware | varredura das imagens | Trivy na esteira |
| 6. Sistemas e software seguros | SDLC seguro, revisão, inventário de scripts da página de pagamento (6.4.3) | SAST, SCA, CSP com nonce (ConectAgente) |
| 7. Acesso por necessidade | negar por padrão, papel por função | autorização do alicerce |
| 8. Identificar e autenticar | MFA para todo acesso ao CDE | MFA do AuthCore |
| 9. Acesso físico | do provedor de nuvem | fora do código |
| 10. Log e monitoramento | log de todo acesso a dado de cartão, guardado à parte | log ECS com correlação do alicerce |
| 11. Testar regularmente | varredura trimestral, pentest anual, detecção de mudança na página de pagamento (11.6.1) | ZAP, relatório do brecha |
| 12. Política | política escrita, treinamento, resposta a incidente | processo do alicerce, champions |

O jeito mais eficaz de cumprir o PCI é **não guardar o PAN**: com tokenização pelo adquirente, a
maior parte dos requisitos sai do escopo. É a primeira recomendação que eu faria.

### Resolução CMN nº 4.893/2021

Vale para instituição autorizada pelo Banco Central. O que ela pediria à Pague Leve:

| Exigência | Como atender |
|---|---|
| Política de segurança cibernética aprovada pela diretoria | documento com objetivos, controles e responsáveis, revisado todo ano |
| Procedimentos para prevenir, detectar e reduzir vulnerabilidade a incidente | a esteira de segurança e a gestão de vulnerabilidades do alicerce |
| Plano de ação e de resposta a incidentes | roteiro como o do ConectAgente, adaptado ao BACEN |
| Registro, análise de causa e impacto dos incidentes relevantes | trilha de auditoria imutável e pós-incidente escrito |
| Relatório anual sobre a implementação do plano | métricas de tempo de correção e achados por severidade |
| Contratação de nuvem: avaliação do fornecedor e comunicação ao Banco Central antes de contratar serviço relevante | análise do provedor, contrato com cláusula de acesso aos dados e saída |

### O que o exercício mostra

As três normas pedem a mesma coisa com nomes diferentes: saber onde está o dado, controlar quem
chega nele, registrar quem chegou e ter um plano para quando der errado. O que muda é a evidência
que cada auditor aceita.
