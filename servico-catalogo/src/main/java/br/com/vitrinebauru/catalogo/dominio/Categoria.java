package br.com.vitrinebauru.catalogo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Categoria do produto.
 *
 * <p>É tabela, e não enum no código, porque a SEDECON pode querer uma
 * categoria nova sem esperar implantação. O conjunto inicial vem da migração,
 * com os mesmos nomes usados no cadastro do negócio, para o consumidor não
 * encontrar "Alimentação" na busca de loja e "Comida" na busca de produto.
 */
@Entity
@Table(name = "categoria", schema = "catalogo")
public class Categoria {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false)
    private int ordem;

    protected Categoria() {
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String slug() {
        return slug;
    }

    public int ordem() {
        return ordem;
    }
}
