package br.com.vitrinebauru.plataforma.web;

import br.com.vitrinebauru.plataforma.web.erros.ErrosDeNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforma excecao em resposta que da para mostrar na tela.
 *
 * <p>Duas regras que valem para o projeto inteiro:
 *
 * <p>A primeira e que a mensagem que chega ao usuario e escrita em portugues,
 * com acento, dizendo o que fazer. "Error 400" na tela de um empreendedor que
 * mal usa computador nao e mensagem de erro, e um beco sem saida. Isso e
 * criterio de aceite do projeto, e nao preciosismo.
 *
 * <p>A segunda e que erro inesperado nunca devolve detalhe interno. O usuario
 * recebe um numero de ocorrencia (a correlacao) e o log guarda a pilha. Nome
 * de tabela e mensagem do banco na resposta e mapa para quem estiver
 * procurando brecha.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);
    private static final String BASE_DOS_TIPOS = "https://vitrinebauru.com.br/erros/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail campoInvalido(MethodArgumentNotValidException excecao) {
        Map<String, String> campos = new LinkedHashMap<>();
        excecao.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        excecao.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));

        var problema = montar(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Confira os campos destacados e tente de novo.");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail entradaInvalida(Exception excecao) {
        String detalhe = excecao instanceof IllegalArgumentException
                ? excecao.getMessage()
                : "Não foi possível ler os dados enviados.";
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhe);
    }

    @ExceptionHandler(ErrosDeNegocio.NaoAutenticado.class)
    public ProblemDetail naoAutenticado(ErrosDeNegocio.NaoAutenticado excecao) {
        return montar(HttpStatus.UNAUTHORIZED, "Não autenticado", excecao.getMessage());
    }

    @ExceptionHandler(ErrosDeNegocio.NaoEncontrado.class)
    public ProblemDetail naoEncontrado(ErrosDeNegocio.NaoEncontrado excecao) {
        return montar(HttpStatus.NOT_FOUND, "Não encontrado", excecao.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class, org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public ProblemDetail enderecoInexistente(Exception excecao) {
        return montar(HttpStatus.NOT_FOUND, "Não encontrado", "Este endereço não existe.");
    }

    @ExceptionHandler(ErrosDeNegocio.Conflito.class)
    public ProblemDetail conflito(ErrosDeNegocio.Conflito excecao) {
        return montar(HttpStatus.CONFLICT, "Operação em conflito", excecao.getMessage());
    }

    @ExceptionHandler(ErrosDeNegocio.RegraDeNegocio.class)
    public ProblemDetail regraDeNegocio(ErrosDeNegocio.RegraDeNegocio excecao) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível concluir", excecao.getMessage());
    }

    @ExceptionHandler(ErrosDeNegocio.Proibido.class)
    public ProblemDetail proibido(ErrosDeNegocio.Proibido excecao) {
        return montar(HttpStatus.FORBIDDEN, "Sem permissão", excecao.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail acessoNegado(org.springframework.security.access.AccessDeniedException excecao) {
        return montar(HttpStatus.FORBIDDEN, "Sem permissão",
                "Sua conta não tem permissão para esta ação.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception excecao) {
        String ocorrencia = MDC.get("correlacao");
        log.error("Erro nao tratado na ocorrencia {}", ocorrencia, excecao);

        var problema = montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro no servidor",
                "Algo deu errado do nosso lado. Tente de novo em alguns minutos.");
        problema.setProperty("ocorrencia", ocorrencia);
        return problema;
    }

    private ProblemDetail montar(HttpStatus situacao, String titulo, String detalhe) {
        var problema = ProblemDetail.forStatus(situacao);
        problema.setTitle(titulo);
        problema.setDetail(detalhe);
        problema.setType(URI.create(BASE_DOS_TIPOS + situacao.value()));
        problema.setProperty("correlacao", MDC.get("correlacao"));
        return problema;
    }
}
