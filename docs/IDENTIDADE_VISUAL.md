# Identidade visual

Guia curto para quem for mexer na aparência da Vitrine Bauru sem quebrar o
conjunto. Tudo que decide cor, fonte e forma está em um arquivo só:
`web/src/estilos/global.css`.

## De onde vem o visual

A referência é a **placa esmaltada de rua**: chapa branca com um filete fino
desenhado alguns milímetros para dentro da borda, cor chapada sem degradê, canto
reto e letra de serifa grossa. É o material das placas municipais, das fachadas
pintadas à mão do comércio antigo e da estação ferroviária, que é o que fez
Bauru existir como cidade. Nada aqui imita papel: é chapa pintada.

Dentro disso, três decisões fixas:

1. **O verde institucional é estrutura, não enfeite.** Barra de topo, rodapé e
   faixa do herói. É ele que faz a página ler como serviço da prefeitura no
   primeiro segundo, antes de qualquer texto.
2. **O amarelo de sinalização é a ação, e só ela.** Sempre com texto quase preto
   por cima, que é como sinalização viária funciona, e por isso passa contraste
   com folga.
3. **A arcada do Calçadão é a assinatura.** Sete quadras fechadas ao trânsito em
   1992 e cobertas por setenta arcos de ferro. Aparece desenhada com coluna e
   vão, sob o herói e sobre o rodapé. É o único elemento que pode chamar
   atenção; o resto fica quieto.

### O que ficou de fora, de propósito

Fundo creme quente, serifada de alto contraste, acento terracota, gradiente,
vidro fosco, sombra sólida deslocada e canto arredondado. A primeira versão
deste projeto tinha as três primeiras juntas, que é exatamente um dos padrões
que ferramenta de IA repete em qualquer briefing. Se você for mexer na paleta,
não volte para lá.

## Trocar uma cor

As cores são tokens do Tailwind 4 declarados no bloco `@theme`. Mudar o valor ali
muda o site inteiro, porque cada classe (`bg-selo`, `text-tinta`, `border-linha`)
lê o token.

| Token | Valor | Onde aparece |
| --- | --- | --- |
| `--color-fundo` | `#e8ebe6` | fundo da página, concreto pintado |
| `--color-faixa` | `#dbe0da` | seções alternadas, botão neutro no hover |
| `--color-chapa` | `#ffffff` | fundo de painel e de cartão |
| `--color-tinta` | `#15181a` | texto |
| `--color-tinta-suave` | `#454b4a` | texto secundário |
| `--color-concreto` | `#5c625f` | texto de apoio, contagem |
| `--color-linha` | `#c2cac2` | divisórias e o filete da placa |
| `--color-borda` | `#616a65` | contorno de painel e de etiqueta |
| `--color-borda-forte` | `#3b423e` | contorno de botão, de campo e as separações estruturais |
| `--color-selo` | `#0b5d3b` | verde SEDECON: topo, rodapé, herói |
| `--color-selo-escuro` | `#073d27` | rodapé, faixa de números, menu no celular |
| `--color-selo-claro` | `#dbe9df` | fundo de bloco informativo |
| `--color-sinal` | `#f2b705` | ação principal, e só ela |
| `--color-sinal-claro` | `#fbf0d2` | fundo de destaque, etiqueta de preço |
| `--color-alerta` | `#b3261e` | erro |

Três regras que valem a pena manter:

1. **Amarelo nunca é texto sobre fundo claro.** `text-sinal` só existe sobre o
   verde, no topo e no rodapé. Sobre branco ele fica em 1,9:1 e some.
2. **Texto claro só sobre `selo` ou `selo-escuro`.** Os dois passam em AA com
   branco. Verde mais claro que isso não passa.
3. **Contorno não é tinta.** Todo contorno tem 1px e usa `borda` ou
   `borda-forte`, nunca `tinta`. Com a moldura na mesma cor do texto, numa fila
   de doze etiquetas de categoria o olho lê primeiro as molduras.

## Trocar a fonte

```css
--font-display: 'Besley Variable', ...;   /* títulos */
--font-corpo:   'Archivo Variable', ...;  /* texto, etiquetas e preços */
```

Besley é uma Clarendon: serifa grossa, contraste baixo, letra de fachada
pintada. Não confunda com a serifada editorial de alto contraste, que é outra
coisa e puxa a página para o lado de revista.

O Archivo entra por `@fontsource-variable/archivo/wdth.css`, e não pelo `index`,
porque esse arquivo traz o eixo de largura junto com o de peso. É ele que
permite as três vozes tipográficas com duas famílias só: título em Besley, texto
em Archivo normal, etiqueta e preço em Archivo estreito (`font-stretch` entre 80%
e 88%) e maiúsculo. Sistema de sinalização funciona assim: uma letra, várias
larguras.

As duas vêm do pacote `@fontsource-variable`, instaladas junto com o projeto e
servidas do próprio domínio. Não há requisição para o Google Fonts, o que evita
tanto a lentidão quanto o problema de LGPD de mandar o IP do visitante para
terceiro. Para trocar: `npm i @fontsource-variable/<nome>`, ajuste o `@import` no
topo do arquivo e o token.

## As classes que existem

Ficam em `@layer components`, no mesmo arquivo. Use estas antes de inventar
combinação de utilitário solta:

- `.arcada`, `.arcada-faixa`, `.arcada-tinta` — a arcada do calçadão. A primeira
  é a faixa alta do herói, a segunda a versão baixa do rodapé, a terceira o
  contorno escuro para fundo claro.
- `.quadro` — painel de contorno fino, o recipiente padrão de tudo.
- `.placa`, `.placa-leve`, `.placa-no-verde` — o filete interno da placa
  esmaltada, desenhado por um `::before` inset. **Só em painel de texto**: em
  cartão com foto encostada na borda o filete atravessa a imagem.
- `.botao` mais `.botao-principal` (amarelo), `.botao-selo` (verde),
  `.botao-neutro` (branco com contorno) e `.botao-texto`.
- `.campo`, `.etiqueta` — entrada de formulário, com estado de erro por
  `aria-invalid`. A etiqueta é maiúscula estreita, no registro da placa.
- `.preco`, `.preco-sob-consulta` — o preço tem peso próprio na grade.
- `.selo-categoria`, `.selo-no-verde` — a etiqueta de categoria, na versão
  normal e na que vai sobre o verde.
- `.aviso` mais `.aviso-erro`, `.aviso-certo`, `.aviso-atencao`.
- `.sem-foto`, `.iniciais-da-foto` — o lugar da foto que ainda não existe.
- `.duas-linhas`, `.tres-linhas` — corte de texto por número de linhas.

## O lugar da foto que não existe

Vale explicar porque é o ajuste que mais mudou a página. A maioria das lojas se
cadastra pelo celular, publica os produtos e só depois volta para fotografar.
Numa cidade recém-cadastrada isso significa uma vitrine quase toda sem imagem.

Com o vazio ocupando o tamanho de uma foto (240px), a home virava uma parede de
retângulos coloridos e tinha 5529px de altura; no celular, 13473px. Com o vazio
reduzido a uma faixa de 56px, caiu para 4108px e 9122px, e o que se lê ao rolar
passou a ser nome e preço. Quando a foto existe, ela recebe o espaço inteiro,
porque aí ela é a informação principal.

O componente é `web/src/componentes/Foto.tsx`, e a altura da faixa é a prop
`alturaSemFoto`. O cartão de loja usa `h-20` porque tem duas etiquetas em cima; a
capa da página da loja usa `h-32`, porque ali o bloco é a identidade do negócio.

## Alterações comuns

**Mudar o tom do verde para o da gestão atual.** Troque `--color-selo` e
`--color-selo-escuro`. Confira o contraste com texto branco em qualquer
verificador de WCAG antes de subir; abaixo de 4,5:1 o site fica ilegível no sol,
que é onde metade do público usa.

**Trocar a cor de ação.** `--color-sinal`. Se sair do amarelo, confira o
contraste com `--color-tinta`, porque o botão principal é texto escuro sobre a
cor, e não branco.

**Aumentar o corpo do texto.** `font-size` do `body`, no `@layer base`. Está em
16px porque parte do público é idosa.

**Deixar os cantos redondos.** `--radius-caixa`. Está em `0px` de propósito:
placa de metal não tem canto arredondado.

**Mudar as cores de categoria.** Ficam em `corDaCategoria`, em
`web/src/lib/formato.ts`. São tintas chapadas, claras o bastante para as
iniciais escuras aparecerem por cima.

## Antes de subir uma mudança visual

```bash
cd web
npm test          # inclui teste de acessibilidade nos componentes
npx playwright test --project=celular
node scripts/auditoria-de-celular.mjs
```

A varredura abre as sete telas em 320px e 393px e reporta rolagem horizontal,
alvo de toque pequeno, texto abaixo de 12px e erro de script. Link no meio de
uma frase aparece com altura pequena e não é defeito: a norma isenta esse caso,
e forçá-lo a 44px estragaria o parágrafo. O que precisa de folga é controle de
verdade, como a lista do rodapé e os atalhos de categoria.

O projeto `celular` roda num Pixel 5 e verifica o que mais quebra em mudança de
layout: rolagem horizontal, alvo de toque menor que 44px e menu que não abre.
