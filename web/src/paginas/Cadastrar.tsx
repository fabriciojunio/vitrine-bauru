import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { chamar, ErroDaApi } from '@/lib/api';
import { useSessao } from '@/lib/sessao';
import { mascararCep, mascararDocumento, mascararTelefone } from '@/lib/formato';
import { Arco, AreaDeTexto, Aviso, Botao, Campo, Selecao } from '@/componentes/Basicos';

/**
 * O cadastro do empreendedor.
 *
 * <p>É a tela mais importante do produto, e a mais fácil de errar. Quem
 * preenche pode ter pouca familiaridade com formulário, e cada campo a mais é
 * gente desistindo no meio. Por isso:
 *
 * <ul>
 *   <li>o formulário é um só, sem etapas: barra de progresso em cadastro curto
 *       assusta mais do que orienta;</li>
 *   <li>o CEP preenche o bairro sozinho, consultando o ViaCEP, e o bairro
 *       continua editável porque o CEP nem sempre acerta;</li>
 *   <li>a foto não entra aqui: sobe depois, no painel, para uma foto que falhou
 *       não derrubar o cadastro inteiro;</li>
 *   <li>o erro aparece embaixo do campo, com texto que diz o que fazer.</li>
 * </ul>
 */
export function Cadastrar() {
  const navegar = useNavigate();
  const { entrar } = useSessao();

  const [bairros, definirBairros] = useState<string[]>([]);
  const [categorias, definirCategorias] = useState<string[]>([]);
  const [campos, definirCampos] = useState({
    nome: '',
    email: '',
    senha: '',
    nomeDoNegocio: '',
    descricao: '',
    categoriaPrincipal: '',
    bairro: '',
    cep: '',
    telefoneWhatsapp: '',
    documento: '',
  });

  const [errosDosCampos, definirErrosDosCampos] = useState<Record<string, string>>({});
  const [erroGeral, definirErroGeral] = useState<string | null>(null);
  const [enviando, definirEnviando] = useState(false);
  const [buscandoCep, definirBuscandoCep] = useState(false);

  useEffect(() => {
    chamar<string[]>('/api/cadastro/bairros', { semAutenticacao: true })
      .then(definirBairros)
      .catch(() => definirBairros([]));
    chamar<string[]>('/api/cadastro/categorias', { semAutenticacao: true })
      .then(definirCategorias)
      .catch(() => definirCategorias([]));
  }, []);

  const trocar = (campo: string, valor: string) => {
    definirCampos((atual) => ({ ...atual, [campo]: valor }));
    definirErrosDosCampos((atual) => {
      const copia = { ...atual };
      delete copia[campo];
      return copia;
    });
  };

  /**
   * Busca o endereço pelo CEP no ViaCEP.
   *
   * Chamada direto do navegador, e não pelo back-end: é um serviço público, sem
   * chave e sem dado sensível, e passar pelo servidor só acrescentaria um salto
   * e um ponto de falha. Se falhar, o cadastro segue: o bairro é escolhido na
   * lista do mesmo jeito.
   */
  const buscarCep = async () => {
    const numeros = campos.cep.replace(/\D/g, '');
    if (numeros.length !== 8) {
      return;
    }

    definirBuscandoCep(true);
    try {
      const resposta = await fetch(`https://viacep.com.br/ws/${numeros}/json/`);
      const endereco = await resposta.json();

      if (!endereco.erro && endereco.bairro) {
        const encontrado = bairros.find(
          (bairro) => semAcento(bairro) === semAcento(endereco.bairro),
        );
        if (encontrado) {
          trocar('bairro', encontrado);
        }
      }
    } catch {
      // ViaCEP fora do ar não pode travar o cadastro.
    } finally {
      definirBuscandoCep(false);
    }
  };

  const enviar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    definirErroGeral(null);
    definirErrosDosCampos({});
    definirEnviando(true);

    try {
      await chamar('/api/cadastro/empreendedores', {
        metodo: 'POST',
        semAutenticacao: true,
        corpo: {
          ...campos,
          telefoneWhatsapp: campos.telefoneWhatsapp.replace(/\D/g, ''),
          documento: campos.documento.replace(/[^\dA-Za-z]/g, ''),
          cep: campos.cep.replace(/\D/g, ''),
        },
      });

      // Entra direto: pedir para a pessoa fazer login logo depois de criar a
      // conta é a forma mais rápida de perder quem acabou de se cadastrar.
      await entrar(campos.email, campos.senha);
      navegar('/painel?novo=1');
    } catch (falha) {
      if (falha instanceof ErroDaApi) {
        definirErrosDosCampos(falha.campos);
        definirErroGeral(Object.keys(falha.campos).length === 0 ? falha.message : null);
      } else {
        definirErroGeral('Não foi possível enviar o cadastro agora. Tente de novo.');
      }
    } finally {
      definirEnviando(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <Arco className="text-selo mb-2" />
      <h1 className="text-3xl sm:text-4xl">Cadastre seu negócio</h1>
      <p className="text-tinta-suave mt-2">
        É de graça. A SEDECON confere os dados e sua loja entra na vitrine. Você recebe a resposta
        por e-mail.
      </p>

      <form className="mt-8 flex flex-col gap-5" onSubmit={enviar} noValidate>
        {erroGeral && (
          <Aviso tipo="erro" titulo="Não foi possível concluir o cadastro">
            {erroGeral}
          </Aviso>
        )}

        <fieldset className="quadro p-5 flex flex-col gap-4">
          <legend className="px-2 font-display text-xl font-bold">Sobre você</legend>

          <Campo
            etiqueta="Seu nome completo"
            value={campos.nome}
            erro={errosDosCampos.nome}
            onChange={(evento) => trocar('nome', evento.target.value)}
            autoComplete="name"
            required
          />

          <Campo
            etiqueta="Seu e-mail"
            type="email"
            inputMode="email"
            ajuda="É por aqui que a SEDECON avisa quando o cadastro for aprovado."
            value={campos.email}
            erro={errosDosCampos.email}
            onChange={(evento) => trocar('email', evento.target.value)}
            autoComplete="email"
            required
          />

          <Campo
            etiqueta="Crie uma senha"
            type="password"
            ajuda="Pelo menos 8 caracteres. Pode ser uma frase que você lembre."
            value={campos.senha}
            erro={errosDosCampos.senha}
            onChange={(evento) => trocar('senha', evento.target.value)}
            autoComplete="new-password"
            required
          />

          <Campo
            etiqueta="CPF ou CNPJ"
            ajuda="Se você é MEI, use o CNPJ. Se ainda não formalizou, use o seu CPF."
            value={campos.documento}
            erro={errosDosCampos.documento}
            onChange={(evento) => trocar('documento', mascararDocumento(evento.target.value))}
            inputMode="text"
            required
          />
        </fieldset>

        <fieldset className="quadro p-5 flex flex-col gap-4">
          <legend className="px-2 font-display text-xl font-bold">Sobre o negócio</legend>

          <Campo
            etiqueta="Nome do negócio"
            ajuda="É o nome que aparece na vitrine para o cliente."
            value={campos.nomeDoNegocio}
            erro={errosDosCampos.nomeDoNegocio}
            onChange={(evento) => trocar('nomeDoNegocio', evento.target.value)}
            required
          />

          <AreaDeTexto
            etiqueta="O que você faz"
            ajuda="Escreva como se estivesse explicando para um vizinho. Isso ajuda o cliente a te achar."
            rows={4}
            maxLength={600}
            value={campos.descricao}
            erro={errosDosCampos.descricao}
            onChange={(evento) => trocar('descricao', evento.target.value)}
          />

          <Selecao
            etiqueta="Categoria"
            vazio="Escolha uma categoria"
            opcoes={categorias}
            value={campos.categoriaPrincipal}
            erro={errosDosCampos.categoriaPrincipal}
            onChange={(evento) => trocar('categoriaPrincipal', evento.target.value)}
            required
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <Campo
              etiqueta="CEP"
              ajuda={buscandoCep ? 'Procurando o endereço…' : 'Preenche o bairro sozinho.'}
              value={campos.cep}
              erro={errosDosCampos.cep}
              onChange={(evento) => trocar('cep', mascararCep(evento.target.value))}
              onBlur={buscarCep}
              inputMode="numeric"
              autoComplete="postal-code"
            />

            <Selecao
              etiqueta="Bairro"
              vazio="Escolha o bairro"
              opcoes={bairros}
              value={campos.bairro}
              erro={errosDosCampos.bairro}
              onChange={(evento) => trocar('bairro', evento.target.value)}
              required
            />
          </div>

          <Campo
            etiqueta="Celular com WhatsApp"
            ajuda="É por aqui que o cliente vai falar com você. Confira com atenção."
            value={campos.telefoneWhatsapp}
            erro={errosDosCampos.telefoneWhatsapp}
            onChange={(evento) => trocar('telefoneWhatsapp', mascararTelefone(evento.target.value))}
            inputMode="tel"
            autoComplete="tel"
            required
          />
        </fieldset>

        <div className="flex flex-col sm:flex-row gap-3 items-center">
          <Botao type="submit" carregando={enviando} className="w-full sm:w-auto">
            Enviar cadastro
          </Botao>
          <p className="text-sm text-tinta-suave">
            Ao enviar, você concorda que a SEDECON confira seus dados.{' '}
            <Link to="/privacidade" className="underline underline-offset-2">
              Como cuidamos deles
            </Link>
            .
          </p>
        </div>
      </form>
    </div>
  );
}

function semAcento(texto: string): string {
  // A faixa vai escapada, e não com os caracteres combinantes escritos
  // direto: eles são invisíveis no editor e somem em qualquer cópia.
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}
