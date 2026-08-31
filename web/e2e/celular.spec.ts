import { expect, test } from '@playwright/test';

/**
 * A vitrine num celular de verdade.
 *
 * <p>A maioria dos consumidores vai abrir isto no celular, muitas vezes com
 * sinal ruim e tela pequena. Estes testes rodam num Pixel 5 emulado e checam o
 * que costuma quebrar em tela estreita: menu, rolagem lateral e alvo de toque.
 */

test.describe('vitrine no celular', () => {
  test('abre sem rolagem horizontal, que é o defeito mais comum em tela pequena', async ({
    page,
  }) => {
    await page.goto('/');

    const largura = await page.evaluate(() => ({
      documento: document.documentElement.scrollWidth,
      janela: window.innerWidth,
    }));

    expect(largura.documento).toBeLessThanOrEqual(largura.janela + 1);
  });

  test('o menu abre pelo botão em tela estreita', async ({ page }) => {
    await page.goto('/');

    const menu = page.getByRole('button', { name: 'Menu' });
    await expect(menu).toBeVisible();

    await menu.click();
    // O nome exato: a logo tambem e um link e se chama "Vitrine Bauru".
    await expect(page.getByRole('link', { name: 'Vitrine', exact: true })).toBeVisible();
  });

  test('os botões têm alvo de toque grande o bastante', async ({ page }) => {
    await page.goto('/');

    const botao = page.getByRole('button', { name: 'Procurar' });
    const tamanho = await botao.boundingBox();

    // 44px é o mínimo recomendado para toque, e parte do público é de idade
    // mais alta.
    expect(tamanho?.height ?? 0).toBeGreaterThanOrEqual(44);
  });

  test('a busca funciona pelo teclado do celular', async ({ page }) => {
    await page.goto('/');

    await page.getByLabel('O que você procura').fill('marmita');
    await page.getByRole('button', { name: 'Procurar' }).click();

    await expect(page).toHaveURL(/termo=marmita/);
  });

  test('a página da loja cabe na tela e mostra o botão de contato', async ({ page }) => {
    await page.goto('/loja/doces-da-lourdes');

    await expect(page.getByRole('heading', { name: 'Doces da Lourdes' })).toBeVisible();
    await expect(page.getByRole('link', { name: /Falar no WhatsApp/ }).first()).toBeVisible();

    const largura = await page.evaluate(() => ({
      documento: document.documentElement.scrollWidth,
      janela: window.innerWidth,
    }));
    expect(largura.documento).toBeLessThanOrEqual(largura.janela + 1);
  });

  test('o formulário de cadastro cabe na tela do celular', async ({ page }) => {
    await page.goto('/cadastrar');

    await expect(page.getByLabel('Seu nome completo')).toBeVisible();

    const largura = await page.evaluate(() => ({
      documento: document.documentElement.scrollWidth,
      janela: window.innerWidth,
    }));
    expect(largura.documento).toBeLessThanOrEqual(largura.janela + 1);
  });
});
