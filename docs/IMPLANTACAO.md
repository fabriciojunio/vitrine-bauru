# Implantação

O objetivo aqui é ter a plataforma no ar sem custo nenhum, e sem cartão de
crédito em serviço algum. São três peças gratuitas:

| Peça | Onde | Por quê |
|---|---|---|
| Banco | [Neon](https://neon.tech) | PostgreSQL com camada gratuita que não expira |
| Back-end | [Render](https://render.com) | Docker, camada gratuita sem cartão |
| Front-end | [Vercel](https://vercel.com) | Site estático, camada gratuita generosa |

O back-end sobe como servidor único: os quatro serviços num processo, com o
transporte de eventos dentro do processo. O porquê está na
[decisão 2](adr/0002-transporte-de-eventos.md).

## 1. Banco no Neon

1. Entre em neon.tech e crie a conta (dá para entrar com a conta do GitHub).
2. Crie um projeto. Região: `AWS us-west-2 (Oregon)`, a mesma das máquinas
   gratuitas do Render. Banco e serviço na mesma região economiza uma ida e
   volta pela internet em toda consulta, o que pesa num plano que hiberna.
3. No painel, em **Connection Details**, copie a string de conexão. Ela vem
   assim:

   ```
   postgresql://usuario:senha@ep-algo-123456-pooler.c-2.us-west-2.aws.neon.tech/neondb?sslmode=require
   ```

   **Tire o `-pooler` do endereço.** O Neon oferece dois: o com `-pooler` passa
   por PgBouncer em modo transação, e o Flyway usa `pg_advisory_lock`, que é de
   sessão e não sobrevive a isso. Com um pool de cinco conexões e uma instância
   só, a conexão direta é a certa.

4. Guarde as três partes separadas, que é como o serviço espera:

   ```
   BANCO_URL      jdbc:postgresql://ep-algo-123456.c-2.us-west-2.aws.neon.tech/neondb?sslmode=require
   BANCO_USUARIO  usuario
   BANCO_SENHA    senha
   ```

   Repare no `jdbc:` no começo e no `sslmode=require` no fim. Sem o `sslmode`,
   o Neon recusa a conexão; sem o `jdbc:`, o driver não entende o endereço.

Não é preciso criar tabela nenhuma. O Flyway cria tudo na primeira subida.

## 2. Back-end no Render

O repositório tem um `render.yaml`, então o Render monta o serviço sozinho:

1. Entre em render.com e conecte a conta do GitHub.
2. **New > Blueprint**, escolha o repositório `vitrine-bauru` e confirme.
3. O Render lê o `render.yaml`, cria o serviço e pede os valores que faltam.
   Preencha:

   | Variável | Valor |
   |---|---|
   | `BANCO_URL` | o que você guardou no passo 1 |
   | `BANCO_USUARIO` | idem |
   | `BANCO_SENHA` | idem |
   | `ORIGENS_PERMITIDAS` | deixe `http://localhost:5173` por enquanto |

   O `VITRINE_SEGREDO_JWT` é gerado pelo próprio Render, e é assim que deve
   ser: segredo não entra no repositório.

4. A primeira construção leva de cinco a dez minutos, porque compila o projeto
   inteiro dentro do contêiner.
5. Quando terminar, confira:

   ```bash
   curl https://vitrine-bauru-api.onrender.com/actuator/health
   curl https://vitrine-bauru-api.onrender.com/api/busca/resumo
   ```

   O segundo já deve trazer dez lojas e trinta produtos: com `DEMO_ATIVO=true`,
   a demonstração é semeada na primeira subida.

**A camada gratuita hiberna depois de 15 minutos sem acesso.** A primeira
requisição depois disso demora de trinta a sessenta segundos para responder,
porque a máquina precisa acordar e a JVM precisa subir. Não é defeito, é o
plano. Se isso atrapalhar numa apresentação, abra a página cinco minutos antes.

## 3. Front-end na Vercel

```bash
cd web
npx vercel --prod
```

Ou pelo painel: **Add New > Project**, escolha o repositório, e configure:

| Campo | Valor |
|---|---|
| Root Directory | `web` |
| Framework Preset | Vite |
| Build Command | `npm run build` |
| Output Directory | `dist` |

E uma variável de ambiente:

| Variável | Valor |
|---|---|
| `VITE_API_URL` | `https://vitrine-bauru-api.onrender.com` |

Depois do primeiro deploy, volte ao Render e troque `ORIGENS_PERMITIDAS` para o
endereço que a Vercel deu (algo como `https://vitrine-bauru.vercel.app`). Sem
isso, o navegador recusa as chamadas por CORS, e a tela fica vazia sem
explicação.

## 4. Conferindo que ficou de pé

Abra o endereço da Vercel e verifique, nesta ordem:

- a vitrine abre com produtos, sem pedir login;
- a busca por "bolo" encontra alguma coisa;
- a tela de entrar mostra os dois botões de demonstração;
- entrar como empreendedora abre o painel com a situação da loja;
- entrar como SEDECON mostra a fila de moderação com cadastros esperando.

Se a vitrine abrir vazia, quase sempre é uma destas três: `ORIGENS_PERMITIDAS`
sem o endereço certo, `VITE_API_URL` errado, ou o serviço ainda acordando.

## E-mail de verdade (opcional)

Sem configuração, os e-mails são escritos no log em vez de enviados, o que é o
certo para a demonstração, cheia de endereço fictício.

Para enviar de verdade, crie uma conta no [Resend](https://resend.com) (3 mil
e-mails por mês na camada gratuita), verifique um domínio e configure no
Render:

```
EMAIL_ATIVO      true
RESEND_CHAVE     re_...
EMAIL_REMETENTE  Vitrine Bauru <nao-responda@seudominio.com.br>
```

## Reiniciar a demonstração

A demonstração é pública, e qualquer pessoa aprova cadastro e publica produto
nela. Sem reiniciar, em uma semana a vitrine vira um amontoado de teste alheio.

O fluxo `.github/workflows/reiniciar-demonstracao.yml` faz isso de madrugada.
Ele precisa destes segredos no repositório:

```
DEMO_BANCO_HOST      ep-algo-123456.us-east-2.aws.neon.tech
DEMO_BANCO_NOME      neondb
DEMO_BANCO_USUARIO   usuario
DEMO_BANCO_SENHA     senha
DEMO_URL             https://vitrine-bauru-api.onrender.com
```

## Voltando para quatro processos

Nada do que está aqui impede a topologia de verdade. Com um broker e quatro
bancos disponíveis, é trocar variável de ambiente:

```
TRANSPORTE_DE_EVENTOS=kafka
KAFKA_SERVIDORES=seu-broker:9092
```

e subir os quatro serviços com o `docker-compose.yml` ou com os manifestos em
[`k8s/`](../k8s/). O código é o mesmo.
