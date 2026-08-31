package br.com.vitrinebauru.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A foto do produto, guardada no banco.
 *
 * <p>Guardar imagem em banco tem fama ruim, e com razao em sistema de volume
 * alto. Aqui a conta e outra: sao algumas centenas de fotos de no maximo 5 MB
 * de empreendedores de um municipio, e a alternativa (bucket S3) exige conta,
 * cartao e credencial que um projeto de graduacao nao tem. O acoplamento fica
 * atras de uma interface, entao trocar por Cloudflare R2 e escrever um
 * adaptador, sem tocar em regra nenhuma.
 *
 * <p>O nome original do arquivo nao e guardado. O identificador e sorteado, o
 * que resolve de uma vez colisao de nome, caractere estranho e a tentativa
 * classica de subir um arquivo chamado {@code ../../etc/passwd}.
 */
@Entity
@Table(name = "imagem", schema = "catalogo")
public class ImagemDeProduto {

    @Id
    private UUID id;

    @Column(name = "empreendedor_id", nullable = false)
    private UUID empreendedorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoDeImagem tipo;

    @Column(nullable = false)
    private int tamanho;

    /**
     * Sem {@code @Lob} de proposito. No PostgreSQL, {@code @Lob} em
     * {@code byte[]} vira {@code oid}, que guarda o arquivo fora da tabela,
     * num objeto grande com ciclo de vida proprio: apagar a linha nao apaga o
     * arquivo, e o banco vai enchendo de orfao ate alguem rodar limpeza. O
     * {@code bytea} guarda na propria linha, e apagar apaga.
     */
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] conteudo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected ImagemDeProduto() {
    }

    public static ImagemDeProduto nova(UUID empreendedorId, byte[] conteudo, Instant agora) {
        ImagemDeProduto imagem = new ImagemDeProduto();
        imagem.id = UUID.randomUUID();
        imagem.empreendedorId = empreendedorId;
        imagem.tipo = TipoDeImagem.descobrir(conteudo);
        imagem.conteudo = conteudo;
        imagem.tamanho = conteudo.length;
        imagem.criadaEm = agora;
        return imagem;
    }

    public UUID id() {
        return id;
    }

    public UUID empreendedorId() {
        return empreendedorId;
    }

    public TipoDeImagem tipo() {
        return tipo;
    }

    public int tamanho() {
        return tamanho;
    }

    public byte[] conteudo() {
        return conteudo;
    }

    public Instant criadaEm() {
        return criadaEm;
    }
}
