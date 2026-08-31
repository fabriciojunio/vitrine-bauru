# 4. Exclusão de dados como saga, com prazo

Data: 2026-08-30
Situação: aceita

## Contexto

O artigo 18 da LGPD dá ao titular o direito de pedir a eliminação dos dados
dele. Num sistema de um banco só, isso é um `delete`.

Aqui não é. Os dados do empreendedor estão em quatro lugares: a conta e o
perfil no cadastro, os produtos e as fotos no catálogo, a projeção pública na
busca, e o histórico de e-mail em notificações. Cada um no seu banco, e nenhum
serviço pode apagar a tabela do outro.

O pedido também tem prazo. A ANPD trabalha com quinze dias como referência para
resposta ao titular, e um pedido que fica pendente sem ninguém perceber é
exatamente o tipo de coisa que aparece numa fiscalização.

## Decisão

Uma saga coordenada pelo serviço de cadastro, com três participantes.

1. O titular pede a exclusão. **Na hora**, duas coisas acontecem: a loja sai da
   vitrine e todas as sessões dele caem. Não se espera a saga terminar para
   isso, porque quem pediu para sair não deve continuar aparecendo enquanto
   quatro serviços conversam.
2. O cadastro grava o pedido, com prazo, e publica `ExclusaoSolicitada`.
3. Catálogo, busca e notificações apagam a parte deles e respondem
   `ExpurgoConcluido`, cada um dizendo quantos registros removeu.
4. Quando os três confirmam, o cadastro anonimiza a conta e o perfil, fecha o
   pedido e publica `ExclusaoConcluida`.

Quem não responde recebe o pedido de novo, de dez em dez minutos. Passado o
prazo legal, entra log de erro, métrica e registro de auditoria, para uma
pessoa resolver na mão.

## Por que não há compensação

Uma saga de compra pode estornar. Exclusão de dados não desfaz: apagado é
apagado, e nem deveria voltar. O que existe no lugar da compensação é insistir
até confirmar, e um prazo que, estourado, vira alerta.

É a escolha honesta: melhor um pedido de exclusão atrasado e visível do que um
pedido dado como concluído com dado vivo em algum banco.

## Por que anonimizar em vez de apagar

A conta e o perfil não são removidos: os campos que identificam a pessoa são
substituídos e a linha fica. O motivo é a auditoria. Quem aprovou, recusou ou
suspendeu um cadastro precisa continuar respondendo por isso, e apagar a linha
quebraria o registro que aponta para ela.

O que sobra depois da anonimização não identifica ninguém: nome vira "Conta
removida", o e-mail vira um endereço inválido derivado do identificador, o
documento vira zeros e o telefone também. É o caminho que a lei chama de
anonimização, e não uma forma de manter o dado.

## Consequências

Reenviar o pedido é seguro porque apagar o que já foi apagado não faz nada. Foi
essa propriedade que permitiu ao coordenador insistir sem medo, e ela é testada
com o mesmo pedido chegando duas vezes.

Acrescentar um serviço que guarde dado pessoal significa acrescentá-lo ao enum
`Participante`, no módulo de contratos. Está lá, e não no coordenador, de
propósito: quem esquecer vai fechar saga com dado vivo em algum banco, e o
lugar certo para essa lista ficar é junto do contrato que todos os serviços
enxergam.
