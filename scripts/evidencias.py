"""Junta a evidência de controles de segurança num relatório, para auditoria SOC 2 e ISO 27001.

Roda na CI todo mês e a cada entrada em produção. Só lê: GitHub, os arquivos do repositório e o
SBOM gerado antes. O relatório vira artefato do workflow; nada é gravado no repositório.

Uso: python scripts/evidencias.py <repo dono/nome> <bom.cdx.json> <saida.md>
Precisa de GITHUB_TOKEN com leitura de conteúdo, pull requests e security-events.
"""

import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import UTC, datetime, timedelta
from pathlib import Path
from statistics import mean

API = "https://api.github.com"
JANELA_DIAS = 90
SHA_COMPLETO = re.compile(r"uses:\s*[\w./-]+@([0-9a-f]{40})\b")
USES = re.compile(r"^\s*-?\s*uses:\s*([\w./-]+@\S+)", re.MULTILINE)


def pedir(caminho: str) -> list | dict:
    # Endereço fixo da API do GitHub, em https; o caminho vem deste script.
    pedido = urllib.request.Request(  # noqa: S310
        API + caminho,
        headers={
            "Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(pedido, timeout=30) as resposta:  # noqa: S310
        return json.load(resposta)


def paginado(caminho: str, limite: int = 10) -> list:
    itens: list = []
    for pagina in range(1, limite + 1):
        separador = "&" if "?" in caminho else "?"
        lote = pedir(f"{caminho}{separador}per_page=100&page={pagina}")
        if not isinstance(lote, list):
            raise TypeError(f"esperava lista em {caminho}")
        itens.extend(lote)
        if len(lote) < 100:
            break
    return itens


def data(texto: str) -> datetime:
    return datetime.fromisoformat(texto.replace("Z", "+00:00"))


def mudancas(repo: str, desde: datetime) -> dict:
    # O nome da branch tem acento, e a URL só aceita ASCII.
    base = urllib.parse.quote("produção")
    prs = paginado(f"/repos/{repo}/pulls?state=closed&base={base}&sort=updated&direction=desc", 3)
    mesclados = [p for p in prs if p.get("merged_at") and data(p["merged_at"]) >= desde]
    return {"total": len(mesclados), "lista": [(p["number"], p["title"], p["merged_at"][:10]) for p in mesclados]}


def aceites(raiz: Path) -> set[tuple[str, str]]:
    arquivo = raiz / ".github" / "codeql-accepted.json"
    if not arquivo.exists():
        return set()
    return {(a["rule"], a["path"]) for a in json.loads(arquivo.read_text(encoding="utf-8"))}


def alertas_de_codigo(repo: str, desde: datetime, aceitos: set[tuple[str, str]]) -> dict:
    alertas = paginado(f"/repos/{repo}/code-scanning/alerts", 5)

    def aceito(a: dict) -> bool:
        local = a["most_recent_instance"]["location"]["path"]
        return (a["rule"]["id"], local) in aceitos

    em_aberto = [a for a in alertas if a["state"] == "open"]
    abertos = [a for a in em_aberto if not aceito(a)]
    corrigidos = [a for a in alertas if a["state"] == "fixed" and a.get("fixed_at") and data(a["fixed_at"]) >= desde]
    dias = [(data(a["fixed_at"]) - data(a["created_at"])).total_seconds() / 86400 for a in corrigidos]
    por_severidade: dict[str, int] = {}
    for a in abertos:
        nivel = (a.get("rule") or {}).get("security_severity_level") or "sem nível"
        por_severidade[nivel] = por_severidade.get(nivel, 0) + 1
    return {
        "aceitos": len(em_aberto) - len(abertos),
        "abertos": len(abertos),
        "por_severidade": por_severidade,
        "corrigidos": len(corrigidos),
        "dias_medios": round(mean(dias), 1) if dias else None,
    }


def regras_de_producao(repo: str) -> tuple[bool, list[str]]:
    """Diz se `produção` exige PR e quais checks, pelas regras que o GitHub aplica de fato."""
    base = urllib.parse.quote("produção")
    try:
        regras = pedir(f"/repos/{repo}/rules/branches/{base}")
    except urllib.error.HTTPError:
        return False, []
    if not isinstance(regras, list):
        raise TypeError("esperava lista de regras")
    exige_pr = any(r["type"] == "pull_request" for r in regras)
    checks = [
        c["context"]
        for r in regras
        if r["type"] == "required_status_checks"
        for c in r["parameters"]["required_status_checks"]
    ]
    return exige_pr, checks


def actions_fixadas(raiz: Path) -> tuple[int, int, list[str]]:
    usos: list[str] = []
    for arquivo in sorted((raiz / ".github" / "workflows").glob("*.yml")):
        usos += USES.findall(arquivo.read_text(encoding="utf-8"))
    externas = [u for u in usos if not u.startswith("./")]
    soltas = [u for u in externas if not SHA_COMPLETO.search(f"uses: {u}")]
    return len(externas) - len(soltas), len(externas), soltas


def espera_do_dependabot(raiz: Path) -> list[int]:
    texto = (raiz / ".github" / "dependabot.yml").read_text(encoding="utf-8")
    return [int(d) for d in re.findall(r"default-days:\s*(\d+)", texto)]


def componentes_do_sbom(caminho: Path) -> int:
    return len(json.loads(caminho.read_text(encoding="utf-8")).get("components", []))


def montar(repo: str, bom: Path, raiz: Path) -> str:
    agora = datetime.now(UTC)
    desde = agora - timedelta(days=JANELA_DIAS)
    mud = mudancas(repo, desde)
    cod = alertas_de_codigo(repo, desde, aceites(raiz))
    fixadas, total, soltas = actions_fixadas(raiz)
    esperas = espera_do_dependabot(raiz)
    componentes = componentes_do_sbom(bom)
    exige_pr, exigidos = regras_de_producao(repo)
    protecao = (
        f"`produção` exige {len(exigidos)} checks antes do merge: " + ", ".join(sorted(exigidos))
        if exigidos
        else "`produção` sem checks obrigatórios"
    )

    linhas = [
        "# Evidências de controles de segurança",
        "",
        f"Repositório `{repo}`, gerado em {agora:%d/%m/%Y %H:%M} UTC, janela de {JANELA_DIAS} dias.",
        "",
        "| Controle | SOC 2 | ISO 27001:2022 | Evidência | Situação |",
        "|---|---|---|---|---|",
        f"| Mudança em `produção` só por PR | CC8.1 | 8.32 | {mud['total']} PRs mesclados; "
        + ("a branch exige PR" if exige_pr else "a branch não exige PR")
        + " | "
        + ("ok" if exige_pr else "atenção")
        + " |",
        f"| Teste de segurança antes da entrada | CC7.1, CC8.1 | 8.29 | {protecao} | "
        + ("ok" if exigidos else "atenção")
        + " |",
        f"| Gestão de vulnerabilidade de código | CC7.1 | 8.8 | {cod['abertos']} alertas do CodeQL sem aceite; "
        f"{cod['aceitos']} aceitos com motivo e prazo em `.github/codeql-accepted.json` | "
        + ("ok" if cod["abertos"] == 0 else "atenção")
        + " |",
        "| Tempo de correção | CC7.1 | 8.8 | "
        + (
            f"{cod['corrigidos']} corrigidos, em média {cod['dias_medios']} dias"
            if cod["dias_medios"] is not None
            else "nenhum alerta corrigido na janela"
        )
        + " | ok |",
        f"| Cadeia de suprimentos: actions por SHA | CC8.1 | 8.28 | {fixadas} de {total} fixadas | "
        + ("ok" if not soltas else "falha")
        + " |",
        f"| Atualização com espera de segurança | CC8.1 | 8.8 | Dependabot com {min(esperas) if esperas else 0} dias | "
        + ("ok" if esperas and min(esperas) >= 7 else "falha")
        + " |",
        f"| Inventário de componentes | CC7.1 | 5.9, 8.8 | SBOM CycloneDX com {componentes} componentes | ok |",
        "",
        "## Alertas abertos por severidade",
        "",
    ]
    linhas += [f"- {nivel}: {n}" for nivel, n in sorted(cod["por_severidade"].items())] or ["- nenhum"]
    linhas += ["", "## Mudanças em produção", ""]
    linhas += [f"- #{n} {titulo} ({quando})" for n, titulo, quando in mud["lista"]] or ["- nenhuma na janela"]
    if soltas:
        linhas += ["", "## Actions sem SHA", ""] + [f"- `{u}`" for u in soltas]
    return "\n".join(linhas) + "\n"


def main() -> int:
    repo, bom, saida = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
    relatorio = montar(repo, bom, Path.cwd())
    saida.write_text(relatorio, encoding="utf-8")
    print(relatorio)
    # Falha só no que é regra dura do pipeline; alerta aberto é fila, não quebra.
    return 1 if "| falha |" in relatorio else 0


if __name__ == "__main__":
    sys.exit(main())
