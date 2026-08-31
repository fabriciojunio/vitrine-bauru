import { describe, expect, it } from 'vitest';
import {
  centavosParaCampo,
  corDaCategoria,
  formatarData,
  formatarDataEHora,
  formatarPreco,
  formatarTelefone,
  iniciais,
  mascararCep,
  mascararDocumento,
  mascararTelefone,
  paraCentavos,
  tempoDesde,
} from './formato';

describe('preço digitado pelo empreendedor', () => {
  it.each([
    ['12,50', 1250],
    ['12.50', 1250],
    ['R$ 12,50', 1250],
    ['1.234,56', 123456],
    ['0,99', 99],
    ['10', 1000],
    ['0', 0],
    ['  25,00  ', 2500],
    ['1.000', 100000],
    ['90', 9000],
  ])('lê "%s" como %i centavos', (digitado, esperado) => {
    expect(paraCentavos(digitado)).toBe(esperado);
  });

  it.each(['', '   ', 'combinar', 'sob consulta', 'abc'])(
    'devolve nulo para "%s", que significa sem preço',
    (digitado) => {
      expect(paraCentavos(digitado)).toBeNull();
    },
  );

  it('ignora o sinal de menos: preço negativo não existe', () => {
    expect(paraCentavos('-10')).toBe(1000);
  });

  it.each([
    [1250, '12,50'],
    [99, '0,99'],
    [0, '0,00'],
    [123456, '1234,56'],
  ])('devolve %i centavos para o campo como "%s"', (centavos, esperado) => {
    expect(centavosParaCampo(centavos)).toBe(esperado);
  });

  it('campo vazio quando não há preço', () => {
    expect(centavosParaCampo(null)).toBe('');
    expect(centavosParaCampo(undefined)).toBe('');
  });
});

describe('preço mostrado na tela', () => {
  it.each([
    [0, 'R$ 0,00'],
    [5, 'R$ 0,05'],
    [50, 'R$ 0,50'],
    [100, 'R$ 1,00'],
    [1200, 'R$ 12,00'],
    [1250, 'R$ 12,50'],
    [9000, 'R$ 90,00'],
    [28000, 'R$ 280,00'],
    [100000, 'R$ 1.000,00'],
    [123456789, 'R$ 1.234.567,89'],
  ])('formata %i centavos como "%s"', (centavos, esperado) => {
    expect(formatarPreco(centavos)).toBe(esperado);
  });

  it('sem preço vira sob consulta, que é uma resposta legítima', () => {
    expect(formatarPreco(null)).toBe('Sob consulta');
    expect(formatarPreco(undefined)).toBe('Sob consulta');
  });

  it('bate com o formato que o servidor devolve', () => {
    // O servidor formata igual. Se um dos dois mudar, a tela mostra dois
    // formatos diferentes para o mesmo preço, dependendo da rota.
    expect(formatarPreco(1250)).toBe('R$ 12,50');
  });
});

describe('telefone', () => {
  it.each([
    ['14997123456', '(14) 99712-3456'],
    ['1432277819', '(14) 3227-7819'],
    ['14997010101', '(14) 99701-0101'],
  ])('formata %s como %s', (digitos, esperado) => {
    expect(formatarTelefone(digitos)).toBe(esperado);
  });

  it('devolve o que veio quando o formato não é reconhecido', () => {
    expect(formatarTelefone('123')).toBe('123');
  });

  it.each([
    ['1', '1'],
    ['14', '14'],
    ['149', '(14) 9'],
    ['149971', '(14) 9971'],
    ['1499712345', '(14) 9971-2345'],
    ['14997123456', '(14) 99712-3456'],
    ['149971234567890', '(14) 99712-3456'],
  ])('vai mascarando enquanto digita: "%s" vira "%s"', (digitado, esperado) => {
    expect(mascararTelefone(digitado)).toBe(esperado);
  });

  it('ignora o que não é número enquanto a pessoa digita', () => {
    expect(mascararTelefone('(14) 99712-3456')).toBe('(14) 99712-3456');
    // Com sete dígitos ainda não dá para saber se é fixo ou celular, então a
    // máscara segue o formato de fixo e se reorganiza no nono dígito. É o
    // comportamento de qualquer máscara brasileira de telefone.
    expect(mascararTelefone('14 abc 99712')).toBe('(14) 9971-2');
  });
});

describe('CEP', () => {
  it.each([
    ['17011066', '17011-066'],
    ['17011', '17011'],
    ['170110', '17011-0'],
    ['17011-066', '17011-066'],
    ['170110669999', '17011-066'],
  ])('mascara "%s" como "%s"', (digitado, esperado) => {
    expect(mascararCep(digitado)).toBe(esperado);
  });
});

describe('CPF e CNPJ no mesmo campo', () => {
  it.each([
    ['529', '529'],
    ['5299822', '529.982.2'],
    ['52998224725', '529.982.247-25'],
  ])('mascara CPF: "%s" vira "%s"', (digitado, esperado) => {
    expect(mascararDocumento(digitado)).toBe(esperado);
  });

  it('mascara CNPJ de 14 dígitos', () => {
    expect(mascararDocumento('11222333000181')).toBe('11.222.333/0001-81');
  });

  it('aceita o CNPJ alfanumérico que a Receita passou a emitir em 2026', () => {
    expect(mascararDocumento('12ABC34501DE35')).toBe('12.ABC.345/01DE-35');
  });

  it('passa letra para maiúscula sozinho', () => {
    expect(mascararDocumento('12abc34501de35')).toBe('12.ABC.345/01DE-35');
  });

  it('não deixa passar do tamanho do CNPJ', () => {
    expect(mascararDocumento('11222333000181999999').replace(/\D/g, '')).toHaveLength(14);
  });
});

describe('datas em fuso de Bauru', () => {
  it('formata a data no padrão brasileiro', () => {
    expect(formatarData('2026-09-22T15:30:00Z')).toBe('22/09/2026');
  });

  it('formata data com hora', () => {
    expect(formatarDataEHora('2026-09-22T15:30:00Z')).toContain('22/09/2026');
  });

  it('devolve vazio quando não há data', () => {
    expect(formatarData(null)).toBe('');
    expect(formatarData(undefined)).toBe('');
    expect(formatarDataEHora(null)).toBe('');
  });

  it.each([
    [0, 'hoje'],
    [1, 'ontem'],
    [3, 'há 3 dias'],
    [10, 'há 10 dias'],
    [45, 'há 1 mês'],
    [120, 'há 4 meses'],
  ])('mostra %i dias atrás como "%s"', (dias, esperado) => {
    const data = new Date(Date.now() - dias * 86_400_000 - 1000).toISOString();
    expect(tempoDesde(data)).toBe(esperado);
  });

  it('devolve vazio sem data', () => {
    expect(tempoDesde(null)).toBe('');
  });
});

describe('foto que ainda não existe', () => {
  it.each([
    'Alimentação',
    'Artesanato',
    'Beleza e bem-estar',
    'Casa e construção',
    'Moda e acessórios',
    'Serviços gerais',
    'Assistência técnica',
    'Educação e aulas',
    'Pet',
    'Saúde',
    'Eventos e festas',
    'Automotivo',
  ])('tem uma cor própria para %s', (categoria) => {
    expect(corDaCategoria(categoria)).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it('categoria desconhecida cai numa cor neutra, e não em branco', () => {
    expect(corDaCategoria('Mineração')).toMatch(/^#[0-9a-f]{6}$/i);
    expect(corDaCategoria(null)).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it.each([
    ['Bolo de pote', 'BP'],
    ['Doces da Lourdes', 'DL'],
    ['Açaí', 'AÇ'],
    ['Marmita', 'MA'],
  ])('tira as iniciais de "%s" como "%s"', (nome, esperado) => {
    expect(iniciais(nome)).toBe(esperado);
  });

  it('não quebra com nome vazio', () => {
    expect(iniciais('')).toBe('·');
    expect(iniciais(null)).toBe('·');
  });
});
