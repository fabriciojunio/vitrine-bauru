import { expect, test } from '@playwright/test';

/**
 * Os dois lados do sistema, pela porta da demonstração.
 *
 * <p>É o caminho que a professora, a SEDECON e quem abrir o portfólio vão
 * percorrer. Se algum destes testes quebrar, a demonstração publicada está
 * quebrada, que é o pior lugar para descobrir.
 */

test.describe('entrada em um clique', () => {
  test('a tela de entrar oferece os dois papéis e avisa que os dados são fictícios', async ({
    page,
  }) => {
    await page.goto('/entrar');

    await expect(page.getByRole('button', { name: /Entrar como empreendedora/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Entrar como SEDECON/ })).toBeVisible();
    await expect(page.getByText(/lojas fictícias/)).toBeVisible();
  });

  test('a faixa de demonstração aparece no topo de todas as telas', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText(/Ambiente de demonstração/)).toBeVisible();
  });
});

test.describe('lado do empreendedor', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/entrar');
    await page.getByRole('button', { name: /Entrar como empreendedora/ }).click();
    await page.waitForURL('**/painel');
  });

  test('entra e vê a situação da loja em primeiro lugar', async ({ page }) => {
    await expect(page.getByText('Sua loja está no ar')).toBeVisible();
  });

  test('vê os próprios números: produtos e contatos', async ({ page }) => {
    await expect(page.getByText('Produtos no catálogo')).toBeVisible();
    await expect(page.getByText('Contatos recebidos')).toBeVisible();
  });

  test('vê os dados da loja com o documento mascarado', async ({ page }) => {
    await expect(page.getByText('Doces da Lourdes').first()).toBeVisible();
    // A loja da demonstração tem CNPJ, cuja máscara começa com dois
    // asteriscos; a de CPF começa com três. O teste aceita as duas.
    await expect(page.getByText(/\*\*[.*]/).first()).toBeVisible();
  });

  test('abre o catálogo e vê os produtos cadastrados', async ({ page }) => {
    await page.getByRole('link', { name: /Cuidar dos meus produtos/ }).click();

    await expect(page.getByRole('heading', { name: 'Meus produtos' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Bolo de pote' })).toBeVisible();
  });

  test('cadastra um produto novo e ele aparece na vitrine pública', async ({ page }) => {
    await page.goto('/painel/produtos');

    await page.getByRole('button', { name: 'Cadastrar um produto' }).click();
    await page.getByLabel('Nome do produto ou serviço').fill('Pudim de leite');
    await page.getByLabel('Descrição').fill('Feito com leite condensado, serve 6 pessoas');
    await page.getByLabel('Preço').fill('35,00');
    await page.getByLabel('Categoria').selectOption('Alimentação');
    await page.getByRole('button', { name: 'Publicar produto' }).click();

    await expect(page.getByRole('heading', { name: 'Pudim de leite' })).toBeVisible();

    // O produto atravessa o sistema por evento e chega à busca pública.
    await expect(async () => {
      await page.goto('/?termo=pudim');
      await expect(page.getByRole('heading', { name: 'Pudim de leite' })).toBeVisible({
        timeout: 3000,
      });
    }).toPass({ timeout: 30_000 });
  });

  test('marca um produto como esgotado e ele some da vitrine', async ({ page }) => {
    await page.goto('/painel/produtos');

    const cartao = page.getByRole('article').filter({ hasText: 'Pudim de leite' });
    await cartao.getByRole('button', { name: 'Marcar esgotado' }).click();

    await expect(cartao.getByText(/não aparece na busca/)).toBeVisible();

    await expect(async () => {
      await page.goto('/?termo=pudim');
      await expect(page.getByText('Nada encontrado com esses filtros')).toBeVisible({
        timeout: 3000,
      });
    }).toPass({ timeout: 30_000 });
  });

  test('retira o produto de teste do catálogo', async ({ page }) => {
    await page.goto('/painel/produtos');

    page.on('dialog', (dialogo) => dialogo.accept());

    const cartao = page.getByRole('article').filter({ hasText: 'Pudim de leite' });
    await cartao.getByRole('button', { name: 'Retirar' }).click();

    await expect(page.getByRole('heading', { name: 'Pudim de leite' })).toHaveCount(0);
  });

  test('não entra na área da SEDECON', async ({ page }) => {
    await page.goto('/sedecon');

    // A rota devolve o empreendedor para o painel dele.
    await expect(page).toHaveURL(/\/painel/);
  });

  test('sai da conta e volta para a vitrine', async ({ page }) => {
    await page.getByRole('button', { name: 'Sair' }).first().click();

    await page.waitForURL('**/');
    await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible();
  });
});

test.describe('lado da SEDECON', () => {
  /**
   * Cada execução cria o próprio cadastro pendente.
   *
   * <p>Sem isso, o teste que aprova esvaziaria a fila e a rodada seguinte
   * falharia sem nada estar quebrado. Teste de ponta a ponta que depende do
   * estado deixado pela rodada anterior é teste que só passa uma vez.
   */
  test.beforeEach(async ({ page, request }) => {
    const marca = Date.now();
    await request.post('/api/cadastro/empreendedores', {
      data: {
        nome: 'Empreendedor de Teste',
        email: `teste-${marca}@exemplo.com`,
        senha: 'senhadeteste2026',
        nomeDoNegocio: `Loja de Teste ${marca}`,
        descricao: 'Cadastro criado pelo teste de ponta a ponta para a fila ter item.',
        categoriaPrincipal: 'Artesanato',
        bairro: 'Centro',
        cep: '17011066',
        telefoneWhatsapp: '14998887766',
        documento: gerarCpf(marca),
      },
    });

    await page.goto('/entrar');
    await page.getByRole('button', { name: /Entrar como SEDECON/ }).click();
    await page.waitForURL('**/sedecon');
  });

  test('vê a fila de moderação com o tempo de espera de cada cadastro', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Fila de moderação' })).toBeVisible();
    await expect(page.getByText(/esperando/).first()).toBeVisible();
  });

  test('vê o documento mascarado e a situação do CNPJ na Receita', async ({ page }) => {
    await expect(page.getByText('Documento').first()).toBeVisible();
    await expect(page.getByText('Situação na Receita').first()).toBeVisible();
  });

  test('a recusa exige motivo escrito antes de liberar o botão', async ({ page }) => {
    await page.getByRole('button', { name: /Recusar com motivo/ }).first().click();

    const confirmar = page.getByRole('button', { name: /Confirmar recusa/ });
    await expect(confirmar).toBeDisabled();

    await page.getByLabel(/Motivo da recusa/).fill('Falta');
    await expect(confirmar).toBeDisabled();

    await page.getByLabel(/Motivo da recusa/).fill('Falta detalhar o que o negócio vende.');
    await expect(confirmar).toBeEnabled();
  });

  test('aprova um cadastro e a loja entra na vitrine pública', async ({ page }) => {
    const primeiro = page.getByRole('article').first();
    const nomeDaLoja = await primeiro.getByRole('heading').first().textContent();

    await primeiro.getByRole('button', { name: /Aprovar e colocar no ar/ }).click();

    await expect(async () => {
      await page.goto('/lojas');
      await expect(page.getByText(nomeDaLoja ?? '')).toBeVisible({ timeout: 3000 });
    }).toPass({ timeout: 30_000 });
  });

  test('o painel de indicadores mostra o impacto da plataforma', async ({ page }) => {
    await page.goto('/sedecon/indicadores');

    await expect(page.getByRole('heading', { name: 'Impacto e engajamento' })).toBeVisible();
    await expect(page.getByText('Lojas no ar')).toBeVisible();
    await expect(page.getByText('Contatos em 30 dias')).toBeVisible();
    await expect(page.getByText('Lojas por bairro')).toBeVisible();
    await expect(page.getByText(/Aprovados que ainda não publicaram nada/)).toBeVisible();
  });

  test('não entra no painel de empreendedor', async ({ page }) => {
    await page.goto('/painel');

    await expect(page).toHaveURL(/\/sedecon/);
  });
});

/**
 * Gera um CPF que fecha o dígito verificador, para o cadastro do teste passar
 * pela mesma validação que um cadastro de verdade enfrenta.
 */
function gerarCpf(semente: number): string {
  const base = String(semente).slice(-9).padStart(9, '1');

  const digito = (parcial: string, pesoInicial: number): number => {
    let soma = 0;
    for (let posicao = 0; posicao < parcial.length; posicao++) {
      soma += Number(parcial[posicao]) * (pesoInicial - posicao);
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };

  const primeiro = digito(base, 10);
  const segundo = digito(base + primeiro, 11);
  return `${base}${primeiro}${segundo}`;
}
