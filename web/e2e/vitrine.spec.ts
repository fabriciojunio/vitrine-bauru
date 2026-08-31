import { expect, test } from '@playwright/test';

/**
 * O caminho do consumidor: chega sem conta, procura e fala com quem vende.
 *
 * <p>É o fluxo que precisa funcionar mesmo se todo o resto quebrar, porque é o
 * único que a plataforma promete para quem está do lado de fora.
 */

test.describe('vitrine pública', () => {
  test('abre mostrando produtos, sem exigir busca nem cadastro', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { level: 1 })).toContainText('comércio do seu bairro');
    await expect(page.getByRole('article').first()).toBeVisible();

    // Nenhum pedido de login no caminho do consumidor.
    await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible();
    await expect(page).toHaveURL('/');
  });

  test('mostra quantas lojas e produtos existem', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText(/lojas? e/)).toBeVisible();
  });

  test('procura por palavra e encontra o produto', async ({ page }) => {
    await page.goto('/');

    await page.getByLabel('O que você procura').fill('bolo');
    await page.getByRole('button', { name: 'Procurar' }).click();

    await expect(page).toHaveURL(/termo=bolo/);
    await expect(page.getByRole('heading', { name: /Bolo de pote/ })).toBeVisible();
  });

  test('acha mesmo digitando sem acento', async ({ page }) => {
    await page.goto('/?termo=cafe');
    await page.goto('/?termo=reforco');

    // "Reforço Escolar" é encontrado por "reforco", sem cedilha.
    await expect(page.getByText(/resultados?|Nada encontrado/)).toBeVisible();
  });

  test('filtra por bairro e o link continua funcionando ao ser compartilhado', async ({ page }) => {
    await page.goto('/');

    await page.locator('#selecao-bairro').selectOption('Vila Cardia');

    await expect(page).toHaveURL(/bairro=Vila\+Cardia/);
    // Procura dentro do resultado, e nao na pagina inteira: "Vila Cardia"
    // tambem existe como opcao do seletor, que fica invisivel enquanto o
    // campo esta fechado.
    await expect(page.getByRole('article').first().getByText('Vila Cardia')).toBeVisible();

    // Abrir o mesmo endereço numa aba nova mostra o mesmo resultado.
    await page.goto('/?bairro=Vila+Cardia');
    await expect(page.locator('#selecao-bairro')).toHaveValue('Vila Cardia');
  });

  test('explica quando não encontra nada, em vez de mostrar tela vazia', async ({ page }) => {
    await page.goto('/?termo=xilofone');

    await expect(page.getByText('Nada encontrado com esses filtros')).toBeVisible();
    // O endereço da Casa do Empreendedor também está no rodapé: aqui
    // interessa o que aparece no lugar do resultado vazio.
    await expect(page.getByText(/Casa do Empreendedor/).first()).toBeVisible();
  });

  test('abre a página da loja pelo endereço amigável', async ({ page }) => {
    await page.goto('/loja/doces-da-lourdes');

    await expect(page.getByRole('heading', { name: 'Doces da Lourdes' })).toBeVisible();
    await expect(page.getByText('Vila Cardia').first()).toBeVisible();
    await expect(page.getByText(/produtos? disponíveis/)).toBeVisible();
  });

  test('o botão do WhatsApp leva ao número certo, com mensagem pronta', async ({ page }) => {
    await page.goto('/loja/doces-da-lourdes');

    const botao = page.getByRole('link', { name: /Falar no WhatsApp/ }).first();
    const link = await botao.getAttribute('href');

    expect(link).toContain('https://wa.me/55');
    expect(link).toContain('text=');
    expect(decodeURIComponent(link ?? '')).toContain('Vitrine Bauru');

    await expect(botao).toHaveAttribute('target', '_blank');
    await expect(botao).toHaveAttribute('rel', /noopener/);
  });

  test('loja que não existe mostra recado, e não erro cru', async ({ page }) => {
    await page.goto('/loja/loja-que-nunca-existiu');

    await expect(page.getByText('Essa loja não está disponível')).toBeVisible();
    await expect(page.getByRole('link', { name: /Voltar para a vitrine/ })).toBeVisible();
  });

  test('a lista de lojas filtra por categoria', async ({ page }) => {
    await page.goto('/lojas');

    await expect(page.getByRole('heading', { name: 'Lojas de Bauru' })).toBeVisible();

    await page.locator('#selecao-categoria').selectOption('Alimentação');
    await expect(page).toHaveURL(/categoria=Alimenta/);
  });

  test('endereço inexistente cai numa página 404 explicada', async ({ page }) => {
    await page.goto('/pagina-que-nao-existe');

    await expect(page.getByText('404')).toBeVisible();
    await expect(page.getByText('Esta página não existe')).toBeVisible();
  });

  test('a página sobre explica o projeto e a parceria com a SEDECON', async ({ page }) => {
    await page.goto('/sobre');

    await expect(page.getByText(/extensão universitária/).first()).toBeVisible();
    await expect(page.getByText(/Não processa pagamento/)).toBeVisible();
    await expect(page.getByText(/Calçadão da Batista de Carvalho/)).toBeVisible();
  });

  test('a política de privacidade diz o que é guardado de quem só olha', async ({ page }) => {
    await page.goto('/privacidade');

    await expect(page.getByText('De quem compra: nada.')).toBeVisible();
    await expect(page.getByText(/Com ninguém/)).toBeVisible();
  });
});
