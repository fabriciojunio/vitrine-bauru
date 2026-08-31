# Identidade visual

Guia curto para quem for mexer na aparência da Vitrine Bauru sem quebrar o
conjunto. Tudo que decide cor, fonte e forma está em um arquivo só:
`web/src/estilos/global.css`.

## De onde vem o visual

São duas referências, nesta ordem.

A primeira é institucional. A plataforma leva o nome da SEDECON e pede o CPF de
gente de verdade, então precisa parecer serviço público sério. O verde
institucional é estrutura, não enfeite: barra de topo, rodapé e faixa do herói.
É ele que faz a página ler como prefeitura no primeiro segundo, antes de
qualquer texto.

A segunda é o Calçadão da Batista de Carvalho, sete quadras fechadas ao trânsito
em 1992 e cobertas por setenta arcos de ferro. O arco aparece como arcada de
verdade, com coluna e vão, desenhada em SVG por baixo do herói e do rodapé.

O que ficou de fora de propósito: gradiente colorido, vidro fosco, sombra
flutuante e canto totalmente arredondado. É o visual padrão de ferramenta gerada
por IA e não tem nada a ver com quem vende bolo de pote na Vila Cardia.

## Trocar uma cor

As cores são tokens do Tailwind 4 declarados no bloco `@theme`. Mudar o valor ali
muda o site inteiro, porque cada classe (`bg-selo`, `text-terracota`,
`border-linha`) lê o token.

| Token | Valor | Onde aparece |
| --- | --- | --- |
| `--color-papel` | `#f7f3ea` | fundo da página |
| `--color-papel-fundo` | `#efe7d6` | seções alternadas, fundo de campo |
| `--color-papel-claro` | `#fffdf8` | fundo de cartão |
| `--color-tinta` | `#1a1917` | texto e todas as bordas |
| `--color-tinta-suave` | `#4b463f` | texto secundário |
| `--color-concreto` | `#6f6960` | texto de apoio, contagem |
| `--color-linha` | `#ddd2bd` | divisórias claras |
| `--color-selo` | `#0b5d3b` | verde SEDECON: topo, rodapé, herói |
| `--color-selo-escuro` | `#084029` | rodapé e menu aberto no celular |
| `--color-selo-claro` | `#e3efe7` | fundo de bloco informativo |
| `--color-terracota` | `#b4471f` | ação principal, e só ela |
| `--color-mostarda` | `#dda42c` | destaque, faixa de demonstração |
| `--color-alerta` | `#99291a` | erro |

Duas regras que valem a pena manter:

1. **Terracota é ação.** Se aparecer em algo que não é botão principal, perde a
   função de dizer onde clicar.
2. **Texto claro só sobre `selo` ou `selo-escuro`.** Os dois passam em AA com
   `papel-claro`. Verde mais claro que isso não passa.

## Trocar a fonte

```css
--font-display: 'Fraunces Variable', ...;  /* títulos */
--font-corpo:   'Archivo Variable', ...;   /* texto */
```

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
  contorno sobre fundo claro.
- `.quadro` — cartão de borda preta, o recipiente padrão de tudo.
- `.carimbo` e `.carimbo-leve` — o deslocamento de sombra sólida, forte e fraco.
- `.botao` mais `.botao-principal` (terracota), `.botao-selo` (verde),
  `.botao-neutro` (contorno) e `.botao-texto`.
- `.campo`, `.etiqueta` — entrada de formulário, com estado de erro por
  `aria-invalid`.
- `.preco`, `.preco-sob-consulta` — o preço tem peso próprio na grade.
- `.selo-categoria`, `.selo-no-verde` — a etiqueta de categoria, na versão
  normal e na que vai sobre o verde.
- `.aviso` mais `.aviso-erro`, `.aviso-certo`, `.aviso-atencao`.
- `.sem-foto`, `.iniciais-da-foto` — o lugar da foto que ainda não existe.
- `.duas-linhas`, `.tres-linhas` — corte de texto por número de linhas.

## Alterações comuns

**Mudar o tom do verde para o da gestão atual.** Troque `--color-selo` e
`--color-selo-escuro`. Confira o contraste com texto branco em qualquer
verificador de WCAG antes de subir; abaixo de 4.5:1 o site fica ilegível no sol,
que é onde metade do público usa.

**Aumentar o corpo do texto.** `font-size` do `body`, no `@layer base`. Está em
16px porque parte do público é idosa.

**Tirar a textura de papel.** Apague o `background-image` do `body`. É um SVG
embutido de ruído com 4% de opacidade; sem ele o fundo fica chapado.

**Deixar os cantos redondos.** `--radius-caixa`. Está em `2px` de propósito:
arredondamento total em tudo é a cara de template pronto.

## Antes de subir uma mudança visual

```bash
cd web
npm test          # inclui teste de acessibilidade nos componentes
npx playwright test --project=celular
```

O projeto `celular` roda num Pixel 5 e verifica o que mais quebra em mudança de
layout: rolagem horizontal, alvo de toque menor que 44px e menu que não abre.
