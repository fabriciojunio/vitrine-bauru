// Varredura de celular: abre cada tela em duas larguras estreitas e reporta
// o que costuma quebrar em tela pequena.
import { chromium, devices } from '@playwright/test';

const BASE = process.env.BASE ?? 'http://localhost:4173';
const DESTINO = process.argv[2];

const ROTAS = [
  ['/', 'vitrine'],
  ['/lojas', 'lojas'],
  ['/loja/doces-da-lourdes', 'loja'],
  ['/entrar', 'entrar'],
  ['/cadastrar', 'cadastrar'],
  ['/sobre', 'sobre'],
  ['/privacidade', 'privacidade'],
];

const LARGURAS = [
  { nome: '320', width: 320, height: 700 },
  { nome: '393', width: 393, height: 850 },
];

const navegador = await chromium.launch();

for (const larg of LARGURAS) {
  const contexto = await navegador.newContext({
    ...devices['Pixel 5'],
    viewport: { width: larg.width, height: larg.height },
  });
  const pagina = await contexto.newPage();

  const erros = [];
  pagina.on('pageerror', (e) => erros.push(String(e).slice(0, 120)));

  for (const [rota, nome] of ROTAS) {
    erros.length = 0;
    await pagina.goto(BASE + rota, { waitUntil: 'networkidle' });
    await pagina.waitForTimeout(600);

    // Rolagem horizontal: o defeito mais comum em tela estreita.
    const medida = await pagina.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      cliente: document.documentElement.clientWidth,
    }));
    const vazando = medida.scroll > medida.cliente + 1;

    // Quem exatamente passa da largura da tela.
    const culpados = vazando
      ? await pagina.evaluate(() => {
          const limite = document.documentElement.clientWidth;
          const fora = [];
          for (const el of document.querySelectorAll('body *')) {
            const r = el.getBoundingClientRect();
            if (r.width === 0) continue;
            if (r.right > limite + 1 || r.left < -1) {
              fora.push(
                `${el.tagName.toLowerCase()}.${String(el.className).slice(0, 45)} [${Math.round(r.left)}..${Math.round(r.right)}]`,
              );
            }
          }
          return fora.slice(0, 4);
        })
      : [];

    // Alvo de toque menor que 44px, que é o mínimo aceitável no dedo.
    const alvosPequenos = await pagina.evaluate(() => {
      const ruins = [];
      for (const el of document.querySelectorAll('a, button, input, select, [role="button"]')) {
        const r = el.getBoundingClientRect();
        if (r.width === 0 || r.height === 0) continue;
        if (r.height < 44) {
          ruins.push(`${el.tagName.toLowerCase()}"${(el.textContent || '').trim().slice(0, 24)}" h=${Math.round(r.height)}`);
        }
      }
      return ruins.slice(0, 5);
    });

    // Texto abaixo de 12px não se lê no sol.
    const textoMiudo = await pagina.evaluate(() => {
      const ruins = new Set();
      for (const el of document.querySelectorAll('body *')) {
        if (!el.textContent || el.children.length > 0) continue;
        const tam = parseFloat(getComputedStyle(el).fontSize);
        if (tam && tam < 12) ruins.add(`${Math.round(tam)}px "${el.textContent.trim().slice(0, 24)}"`);
      }
      return [...ruins].slice(0, 4);
    });

    const problemas = [];
    if (vazando) problemas.push(`ROLAGEM ${medida.scroll}>${medida.cliente} :: ${culpados.join(' | ')}`);
    if (alvosPequenos.length) problemas.push(`ALVO ${alvosPequenos.join(' | ')}`);
    if (textoMiudo.length) problemas.push(`TEXTO ${textoMiudo.join(' | ')}`);
    if (erros.length) problemas.push(`ERRO ${erros.join(' | ')}`);

    console.log(`${larg.nome} ${rota} ${problemas.length ? '\n    ' + problemas.join('\n    ') : 'ok'}`);

    if (DESTINO && larg.nome === '393') {
      await pagina.screenshot({ path: `${DESTINO}/cel-${nome}.png`, fullPage: true });
    }
  }
  await contexto.close();
}

await navegador.close();
