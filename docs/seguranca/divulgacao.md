# Política de divulgação de vulnerabilidades

*[Read this in English](divulgacao.en.md)*

Vale desde 29/10/2026. Também publicada em
[`/.well-known/security.txt`](https://vitrine-bauru.vercel.app/.well-known/security.txt), no
formato da RFC 9116.

## Escopo

| Dentro | Fora |
|---|---|
| `vitrine-bauru.vercel.app` | a instalação da SEDECON, mantida pela prefeitura à parte: falha nela vai para `desenvolvimento@bauru.sp.gov.br` |
| `vitrine-bauru-api.onrender.com` | Vercel, Render, o provedor do banco, Resend, BrasilAPI e ViaCEP |
| o código deste repositório | engenharia social, ataque físico, negação de serviço por volume |

## Como reportar

Pelo aviso privado do GitHub:
[Report a vulnerability](https://github.com/fabriciojunio/vitrine-bauru/security/advisories/new).
Diga o que encontrou, como reproduzir e o impacto que imagina. Em português ou inglês.

## O que pedimos

- Use a [demonstração](https://vitrine-bauru.vercel.app) e as contas dela. Não use, não guarde e
  não divulgue dado de empreendedor real que você eventualmente veja.
- Pare assim que provar a falha: um registro lido basta, não é preciso baixar a base.
- Nada de teste que derrube o serviço ou que apague dado.
- Não divulgue antes de corrigirmos ou de passarem 90 dias do aviso, o que vier primeiro.

## O que você recebe

| Etapa | Prazo |
|---|---|
| Confirmação de recebimento | até 7 dias |
| Avaliação inicial e severidade (CVSS 4.0) | até 14 dias |
| Correção de falha crítica ou alta | até 30 dias |
| Correção de falha média ou baixa | até 90 dias |
| Aviso de segurança publicado no GitHub, com crédito se você quiser | junto com a correção |

Não há recompensa em dinheiro: é um projeto sem receita. Há crédito público e agradecimento.

## Safe harbor

Pesquisa feita de boa-fé, dentro do escopo e seguindo esta política, é tratada como autorizada.
Nesse caso:

- não vamos tomar medida legal contra você nem pedir que outros tomem;
- se um terceiro processar você por essa pesquisa, vamos dizer publicamente que ela seguiu esta
  política;
- regras de uso do site que impediriam a pesquisa não se aplicam a ela.

O que esta política não pode fazer: autorizar teste em sistema de terceiro (a lista de "fora" do
escopo) nem afastar a lei. Na dúvida sobre se algo está no escopo, pergunte antes pelo mesmo canal.
