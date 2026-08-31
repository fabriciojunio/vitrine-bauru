import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';

/**
 * Os tijolos da interface.
 *
 * Ficam juntos porque são pequenos e sempre lidos em conjunto: quem for mexer
 * no visual de botão quase sempre vai mexer no de campo também. As classes
 * moram no CSS global, e não espalhadas em utilitário por aqui, para o visual
 * do sistema ter um lugar só.
 */

type VarianteDoBotao = 'principal' | 'selo' | 'neutro' | 'texto';

interface PropriedadesDoBotao extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: VarianteDoBotao;
  carregando?: boolean;
}

export function Botao({
  variante = 'principal',
  carregando = false,
  children,
  className = '',
  disabled,
  ...resto
}: PropriedadesDoBotao) {
  return (
    <button
      className={`botao botao-${variante} ${className}`}
      disabled={disabled || carregando}
      aria-busy={carregando}
      {...resto}
    >
      {carregando ? 'Aguarde…' : children}
    </button>
  );
}

interface PropriedadesDoCampo extends InputHTMLAttributes<HTMLInputElement> {
  etiqueta: string;
  erro?: string;
  ajuda?: string;
}

export function Campo({ etiqueta, erro, ajuda, id, className = '', ...resto }: PropriedadesDoCampo) {
  const identificador = id ?? `campo-${etiqueta.toLowerCase().replace(/\W+/g, '-')}`;
  const idDaAjuda = ajuda ? `${identificador}-ajuda` : undefined;
  const idDoErro = erro ? `${identificador}-erro` : undefined;

  return (
    <div className={className}>
      <label className="etiqueta" htmlFor={identificador}>
        {etiqueta}
      </label>
      {ajuda && (
        <p id={idDaAjuda} className="text-sm text-concreto mb-1">
          {ajuda}
        </p>
      )}
      <input
        id={identificador}
        className="campo"
        aria-invalid={erro ? 'true' : undefined}
        aria-describedby={[idDaAjuda, idDoErro].filter(Boolean).join(' ') || undefined}
        {...resto}
      />
      {erro && (
        <p id={idDoErro} role="alert" className="text-sm text-alerta font-semibold mt-1">
          {erro}
        </p>
      )}
    </div>
  );
}

interface PropriedadesDaAreaDeTexto {
  etiqueta: string;
  erro?: string;
  ajuda?: string;
  id?: string;
  value: string;
  onChange(evento: React.ChangeEvent<HTMLTextAreaElement>): void;
  maxLength?: number;
  rows?: number;
  placeholder?: string;
  className?: string;
}

export function AreaDeTexto({
  etiqueta,
  erro,
  ajuda,
  id,
  className = '',
  maxLength,
  value,
  ...resto
}: PropriedadesDaAreaDeTexto) {
  const identificador = id ?? `area-${etiqueta.toLowerCase().replace(/\W+/g, '-')}`;

  return (
    <div className={className}>
      <label className="etiqueta" htmlFor={identificador}>
        {etiqueta}
      </label>
      {ajuda && <p className="text-sm text-concreto mb-1">{ajuda}</p>}
      <textarea
        id={identificador}
        className="campo"
        value={value}
        maxLength={maxLength}
        aria-invalid={erro ? 'true' : undefined}
        {...resto}
      />
      <div className="flex justify-between gap-2">
        {erro ? (
          <p role="alert" className="text-sm text-alerta font-semibold">
            {erro}
          </p>
        ) : (
          <span />
        )}
        {maxLength && (
          <span className="text-xs text-concreto">
            {value.length}/{maxLength}
          </span>
        )}
      </div>
    </div>
  );
}

interface PropriedadesDaSelecao extends SelectHTMLAttributes<HTMLSelectElement> {
  etiqueta: string;
  erro?: string;
  opcoes: string[];
  vazio?: string;
}

export function Selecao({
  etiqueta,
  erro,
  opcoes,
  vazio,
  id,
  className = '',
  ...resto
}: PropriedadesDaSelecao) {
  const identificador = id ?? `selecao-${etiqueta.toLowerCase().replace(/\W+/g, '-')}`;

  return (
    <div className={className}>
      <label className="etiqueta" htmlFor={identificador}>
        {etiqueta}
      </label>
      <select
        id={identificador}
        className="campo"
        aria-invalid={erro ? 'true' : undefined}
        {...resto}
      >
        {vazio !== undefined && <option value="">{vazio}</option>}
        {opcoes.map((opcao) => (
          <option key={opcao} value={opcao}>
            {opcao}
          </option>
        ))}
      </select>
      {erro && (
        <p role="alert" className="text-sm text-alerta font-semibold mt-1">
          {erro}
        </p>
      )}
    </div>
  );
}

interface PropriedadesDoAviso {
  tipo?: 'erro' | 'certo' | 'atencao';
  titulo?: string;
  children: ReactNode;
  className?: string;
}

export function Aviso({ tipo = 'atencao', titulo, children, className = '' }: PropriedadesDoAviso) {
  return (
    <div
      className={`aviso aviso-${tipo} ${className}`}
      role={tipo === 'erro' ? 'alert' : 'status'}
    >
      {titulo && <p className="font-semibold mb-1">{titulo}</p>}
      <div className="text-sm leading-relaxed">{children}</div>
    </div>
  );
}

export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <p className="text-concreto py-8 text-center" role="status">
      {texto}
    </p>
  );
}

/**
 * Um arco da arcada do calçadão.
 *
 * Desenhado com coluna e vão, e não como meia-lua: o que se quer lembrar é a
 * arquitetura de ferro que cobre as sete quadras da Batista de Carvalho, e uma
 * meia-lua sozinha não lembra nada.
 */
export function Arco({ className = '' }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 40 26"
      className={`w-10 h-[26px] ${className}`}
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M3 25V13a17 17 0 0 1 34 0v12"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="square"
      />
      <path d="M3 25V13M37 25V13" fill="none" stroke="currentColor" strokeWidth="2.5" />
      <path d="M0 25.25h40" fill="none" stroke="currentColor" strokeWidth="2.5" />
    </svg>
  );
}

export function TituloDeSecao({
  children,
  descricao,
}: {
  children: ReactNode;
  descricao?: string;
}) {
  return (
    <div className="mb-6">
      <div className="flex items-end gap-2 text-terracota">
        <Arco />
        <span className="h-[2px] grow bg-linha mb-[2px]" aria-hidden="true" />
      </div>
      <h2 className="text-2xl sm:text-3xl mt-3">{children}</h2>
      {descricao && <p className="text-tinta-suave mt-1 max-w-2xl">{descricao}</p>}
    </div>
  );
}
