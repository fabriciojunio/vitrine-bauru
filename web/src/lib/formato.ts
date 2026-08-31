/**
 * Formatação de tela.
 *
 * O preço formatado vem pronto do servidor, que é onde mora a regra. Aqui
 * ficam as conversões que só a tela precisa: o que o empreendedor digita no
 * formulário, datas em fuso de Bauru e o link do WhatsApp.
 */

/** Converte o que a pessoa digita ("12,50", "R$ 12,50", "12") em centavos. */
export function paraCentavos(digitado: string): number | null {
  const limpo = digitado.replace(/[^\d,.]/g, '').trim();
  if (!limpo) {
    return null;
  }

  // No formato brasileiro o ponto é milhar e a vírgula é decimal, mas muita
  // gente digita "12.50" querendo dizer doze e cinquenta, principalmente quem
  // usa o teclado do celular. A regra que cobre os dois casos: só tratamos o
  // ponto como decimal quando não há vírgula nenhuma e ele separa exatamente
  // duas casas no fim.
  const temVirgula = limpo.includes(',');
  const pontoEhDecimal = !temVirgula && /^\d+\.\d{1,2}$/.test(limpo);

  const normalizado = pontoEhDecimal
    ? limpo
    : limpo.replace(/\./g, '').replace(',', '.');
  const valor = Number(normalizado);

  if (Number.isNaN(valor) || valor < 0) {
    return null;
  }
  return Math.round(valor * 100);
}

export function centavosParaCampo(centavos: number | null | undefined): string {
  if (centavos === null || centavos === undefined) {
    return '';
  }
  return (centavos / 100).toFixed(2).replace('.', ',');
}

export function formatarPreco(centavos: number | null | undefined): string {
  if (centavos === null || centavos === undefined) {
    return 'Sob consulta';
  }
  const reais = Math.floor(centavos / 100);
  const resto = centavos % 100;
  const inteiro = reais.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `R$ ${inteiro},${resto.toString().padStart(2, '0')}`;
}

export function formatarTelefone(digitos: string): string {
  const numeros = digitos.replace(/\D/g, '');
  if (numeros.length === 11) {
    return `(${numeros.slice(0, 2)}) ${numeros.slice(2, 7)}-${numeros.slice(7)}`;
  }
  if (numeros.length === 10) {
    return `(${numeros.slice(0, 2)}) ${numeros.slice(2, 6)}-${numeros.slice(6)}`;
  }
  return digitos;
}

/** Máscara aplicada enquanto a pessoa digita o telefone. */
export function mascararTelefone(valor: string): string {
  const numeros = valor.replace(/\D/g, '').slice(0, 11);
  if (numeros.length <= 2) {
    return numeros;
  }
  if (numeros.length <= 6) {
    return `(${numeros.slice(0, 2)}) ${numeros.slice(2)}`;
  }
  if (numeros.length <= 10) {
    return `(${numeros.slice(0, 2)}) ${numeros.slice(2, 6)}-${numeros.slice(6)}`;
  }
  return `(${numeros.slice(0, 2)}) ${numeros.slice(2, 7)}-${numeros.slice(7)}`;
}

export function mascararCep(valor: string): string {
  const numeros = valor.replace(/\D/g, '').slice(0, 8);
  return numeros.length > 5 ? `${numeros.slice(0, 5)}-${numeros.slice(5)}` : numeros;
}

/** CPF e CNPJ no mesmo campo: a máscara segue o que já foi digitado. */
export function mascararDocumento(valor: string): string {
  const limpo = valor.replace(/[^\dA-Za-z]/g, '').toUpperCase().slice(0, 14);

  if (limpo.length <= 11 && /^\d*$/.test(limpo)) {
    return limpo
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
  }

  return limpo
    .replace(/^(.{2})(.)/, '$1.$2')
    .replace(/^(.{2})\.(.{3})(.)/, '$1.$2.$3')
    .replace(/^(.{2})\.(.{3})\.(.{3})(.)/, '$1.$2.$3/$4')
    .replace(/\/(.{4})(.{1,2})$/, '/$1-$2');
}

export function formatarData(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso));
}

export function formatarDataEHora(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso));
}

/** "há 3 dias", que é mais útil que a data crua na fila de moderação. */
export function tempoDesde(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const dias = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);

  if (dias <= 0) {
    return 'hoje';
  }
  if (dias === 1) {
    return 'ontem';
  }
  if (dias < 30) {
    return `há ${dias} dias`;
  }
  const meses = Math.floor(dias / 30);
  return meses === 1 ? 'há 1 mês' : `há ${meses} meses`;
}

/**
 * Cor de fundo da foto que falta, escolhida pela categoria.
 *
 * Loja nova entra sem foto, e um retângulo cinza vazio faz a vitrine parecer
 * quebrada. Com uma cor por categoria, a grade continua com aspecto de coisa
 * cuidada até a pessoa mandar a foto de verdade.
 */
export function corDaCategoria(categoria: string | null | undefined): string {
  // Tintas chapadas, no registro da placa esmaltada: cor cheia e sem
  // degradê, clara o bastante para as iniciais escuras aparecerem por cima.
  const cores: Record<string, string> = {
    Alimentação: '#e3cf87',
    Artesanato: '#d9b48f',
    'Beleza e bem-estar': '#d9aab8',
    'Casa e construção': '#c2c8cc',
    'Moda e acessórios': '#b9bcd6',
    'Serviços gerais': '#aec4bb',
    'Assistência técnica': '#a9bccd',
    'Educação e aulas': '#c3ce9e',
    Pet: '#d5bd9a',
    Saúde: '#a8c8c2',
    'Eventos e festas': '#e0b79c',
    Automotivo: '#b8bcb8',
  };
  return cores[categoria ?? ''] ?? '#c8cec8';
}

/** Iniciais do nome, para a foto que falta ter alguma informação dentro. */
export function iniciais(nome: string | null | undefined): string {
  if (!nome) {
    return '·';
  }
  const partes = nome.trim().split(/\s+/).filter((parte) => parte.length > 2);
  if (partes.length === 0) {
    return nome.slice(0, 2).toUpperCase();
  }
  if (partes.length === 1) {
    return partes[0]!.slice(0, 2).toUpperCase();
  }
  return (partes[0]![0]! + partes[1]![0]!).toUpperCase();
}
