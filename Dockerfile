# Imagem do servidor único: os quatro serviços num processo só.
#
# É a imagem da implantação gratuita. Para rodar os serviços separados, use o
# docker-compose, que constrói cada um com o mesmo Dockerfile passando outro
# MODULO na construção.
#
# Duas etapas: a primeira compila com Maven e some; a final leva só o JRE e o
# jar. A imagem que vai para o ar não carrega compilador, código-fonte nem
# repositório do Maven, que é o que faria uma imagem de 800 MB virar a norma.

# ---------------------------------------------------------------- construção
FROM maven:3.9-eclipse-temurin-21 AS construcao

WORKDIR /construcao

# Os POMs primeiro, sozinhos: enquanto nenhuma dependência mudar, esta camada
# fica em cache e a construção seguinte não baixa a internet inteira de novo.
COPY pom.xml .
COPY contratos/pom.xml contratos/
COPY plataforma/pom.xml plataforma/
COPY servico-cadastro/pom.xml servico-cadastro/
COPY servico-catalogo/pom.xml servico-catalogo/
COPY servico-busca/pom.xml servico-busca/
COPY servico-notificacoes/pom.xml servico-notificacoes/
COPY borda/pom.xml borda/
COPY servidor-unico/pom.xml servidor-unico/

RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY . .

ARG MODULO=servidor-unico
RUN mvn -B -q -pl ${MODULO} -am package -DskipTests

# Descobre o jar executável do módulo escolhido sem depender do nome exato.
RUN cp $(ls ${MODULO}/target/*.jar | grep -v sources | head -n 1) /construcao/aplicacao.jar

# -------------------------------------------------------------------- imagem
FROM eclipse-temurin:21-jre-alpine

# Usuário sem privilégio: um processo que não precisa de root não roda como
# root, e container Java é justamente o caso em que isso nunca é necessário.
RUN addgroup -S vitrine && adduser -S vitrine -G vitrine

WORKDIR /app
COPY --from=construcao /construcao/aplicacao.jar aplicacao.jar
RUN chown -R vitrine:vitrine /app

USER vitrine
EXPOSE 8080

# MaxRAMPercentage em vez de -Xmx fixo: a memória disponível na camada
# gratuita muda conforme o plano, e um valor cravado ou desperdiça memória ou
# derruba o processo quando o teto baixa.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Duser.timezone=UTC -Dfile.encoding=UTF-8"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/aplicacao.jar"]
