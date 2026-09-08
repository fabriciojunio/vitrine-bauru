package br.com.vitrinebauru.unico;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responde "estou de pé" sem tocar no banco.
 *
 * <h2>O problema que esta classe resolve, medido em 08/09/2026</h2>
 * O fluxo que mantém a API acordada batia em {@code /actuator/health} de dez em
 * dez minutos. O health do Spring inclui o indicador do banco: <b>cada ping
 * abria conexão e acordava o Postgres</b>.
 *
 * <p>O resultado foi este projeto consumir <b>175 das 192 horas de computação
 * gratuitas do mês em oito dias</b>, com o banco acordado cerca de vinte e duas
 * horas por dia sem nenhuma visita real. A cota do Neon é da conta inteira, e
 * não de cada projeto, então isso derrubaria junto todos os outros sistemas que
 * dividem a mesma conta.
 *
 * <p>Este endereço responde da memória: não abre conexão, não consulta nada e
 * não depende de nada. O serviço continua acordado do mesmo jeito, e o banco
 * volta a dormir quando ninguém está usando.
 *
 * <p>Quem quer saber se o sistema está <b>saudável</b>, e não apenas de pé,
 * continua usando {@code /actuator/health}, que confere o banco de propósito. A
 * diferença é que agora ele é chamado por quem monitora, e não por um relógio.
 *
 * <p>O nome termina em Controller porque há uma regra de arquitetura cobrando
 * isso, e ela existe para o mesmo motivo de sempre: convenção que vale só quando
 * alguém lembra não é convenção.
 */
@RestController
public class VivoController {

    private static final String RESPOSTA = "{\"vivo\":true}";

    @GetMapping(value = "/api/vivo", produces = MediaType.APPLICATION_JSON_VALUE)
    public String vivo() {
        return RESPOSTA;
    }
}
