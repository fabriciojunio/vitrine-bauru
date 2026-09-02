# Vitrine Bauru

*[Leia em português](README.md)*

A public shop window for the small businesses supported by SEDECON, the economic development
department of Bauru, Brazil. Shoppers browse by product, neighbourhood or category and talk
straight to the maker on WhatsApp. No cart, no payment, no fee: the platform introduces the two
sides and gets out of the way.

Built as a university extension project at Unisagrado, and built to actually stay up rather than
be demoed once and switched off.

**Live:** [vitrine-bauru.vercel.app](https://vitrine-bauru.vercel.app)

`Java 21` · `Spring Boot 3.5` · `Kafka` · `Amazon SNS/SQS` · `PostgreSQL` · `React 19` ·
`Docker` · `Kubernetes`

## How it is put together

Four Spring Boot services talking over events, an edge gateway, and a React front end. Each
service owns its database and no service reads another's tables.

```
  shoppers (no account)        business owner            SEDECON
          │                          │                      │
          └──────────────┬───────────┴──────────────────────┘
                         │
                   edge gateway  (routing, CORS, rate limit)
                         │
     ┌──────────┬────────┴────────┬──────────────┐
     │          │                 │              │
  register   catalogue          search      notifications
     │          │                 │              │
   own DB     own DB            own DB         own DB
     │          │                 │              │
     └──────────┴────── events ───┴──────────────┘
              Kafka · Amazon SNS · in-process
```

## The five problems this project is really about

**1. Writing to the database and telling the others are two different things.** Approving a
registration and publishing "registration approved" do not fit in one transaction. Publish first
and a crash loses the message; save first and you get an approved shop that never shows up in
search. Solved with a transactional outbox: the event is written in the same transaction as the
state, and a separate publisher sends it. That trades "may lose an event" for "may send it
twice", which has a known answer.

**2. The message arrives twice.** Every consumer records the (event, consumer) pair in an inbox
before acting, inside the same transaction as the work. The second delivery finds the mark and
does nothing. Without it, a broker rebalance would send two approval emails to the same person.

**3. Erasing personal data is a conversation between four services.** A GDPR-style deletion
request under Brazil's LGPD is not a `delete`: the product is in the catalogue, the projection is
in search, and the email history is in notifications, each in its own database. It is a saga with
a deadline, retries for whoever did not answer, and an alert when the legal deadline is about to
pass. There is no compensation, and that is deliberate: erasure does not undo.

**4. It has to stay online at zero cost, which is why the transport became an interface.** No
managed Kafka has a permanent free tier, so the transport got interchangeable adapters.

Then a third one appeared, out of a mistake of mine. I had written in the decision record that no
managed messaging had a permanent free tier, because I searched for managed Kafka instead of
searching for the problem. Amazon SNS and SQS are permanently free, and the SNS adapter went in
without touching a line of domain code. What is lost is per-key ordering, and the record explains
why that is cheap here.

| Adapter | Where it runs |
|---|---|
| `TransporteKafka` | compose, Kubernetes, integration tests |
| `TransporteSns` | managed deployment, over SNS to SQS |
| `TransporteNoProcesso` | single-JAR deployment, no broker at all |

**5. The outbox cuts the trace in half.** The event is written inside the request's transaction
and published later, on another thread, after that request is gone. Trace context lives on the
thread, so it dies at commit and the dashboard shows two disconnected traces instead of one whole
request. The fix is the outbox idea applied to observability: whatever crosses a transaction has
to be persisted. The W3C `traceparent` goes in a column, then in a Kafka header or an SNS message
attribute.

## Stack

**Back end.** Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Cloud Gateway, Flyway,
PostgreSQL, Kafka, AWS SDK v2 for SNS and SQS, jjwt, Bucket4j for rate limiting.

**Testing.** JUnit 5, AssertJ, Mockito, ArchUnit, embedded PostgreSQL and embedded Kafka, plus
Playwright end to end.

**Observability.** Micrometer with Prometheus for metrics, Micrometer Tracing with OpenTelemetry
for distributed traces, Jaeger as the local dashboard, Actuator for health checks.

**Infrastructure.** Docker with a two-stage image, Docker Compose, Kubernetes manifests with
Service, PDB, Ingress and HPA, GitHub Actions in three jobs, Neon, Render and Vercel.

## Tests

**1,042 automated tests, and none of them needs Docker installed.** Embedded PostgreSQL and
embedded Kafka start inside the test process itself, which is what keeps the suite runnable on a
laptop and in CI without a container runtime.

Thirteen architecture rules are enforced by ArchUnit, among them that no controller returns a JPA
entity and that no service reads another service's classes.

```bash
mvn test                  # back end
cd web && npm test        # front end
cd web && npm run e2e     # end to end
```

## Running it

Needs Java 21, Node 22 and Docker.

```bash
git clone https://github.com/fabriciojunio/vitrine-bauru.git
cd vitrine-bauru

docker compose up -d              # broker, four databases, four services, gateway
cd web && npm install && npm run dev
```

The site opens on `http://localhost:5173` and the API on port 8080.

```bash
# See the distributed trace on a timeline
docker compose --profile rastro up -d jaeger
RASTRO_ATIVO=true docker compose up -d
# dashboard on http://localhost:16686

# Exercise the SNS transport with no AWS account
docker compose --profile aws up -d localstack
```

## Decision records

The reasoning behind each choice, including the options that were rejected and why, is in
[docs/adr](docs/adr). They are written in Portuguese.

## What this project does not do

No cart, no payment, no delivery, no rating. Every one of those was considered and left out on
purpose: they would turn a public directory into a marketplace, and a marketplace needs a
moderation and dispute team that a city department does not have.
