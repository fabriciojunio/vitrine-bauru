# How to triage a SAST finding

*[Leia em português](triagem-de-sast.md)*

CodeQL flagged something in your PR. Before fixing it or marking it a false positive, four questions,
in this order.

## 1. Where does the data come from?

Follow the path the tool shows, from the source to the dangerous point. If the source is the user
(body, query, header, uploaded file), the finding is a candidate for real. If the source is a constant
or server configuration, it probably is not.

## 2. Does something on the path already neutralise it?

Allow-list validation, parameterised queries, output escaping, conversion to a number. The tool does
not always recognise the neutraliser. A real example from this repository (commit `af73a3d`): CodeQL
did not recognise `replaceAll("[\\r\\n]", "_")` as log sanitisation, and did recognise
`replaceAll("\\R", "_")`, which also covers `\r\n` and Unicode line breaks.

## 3. What is the impact if it is real?

Use the threat model: does the dangerous point touch any asset on the list? Documents, tokens or
password hashes come first; product text less so.

## 4. Decide and record

| Decision | What to do |
|---|---|
| Real | fix it in the same PR, with a test that fails without the fix |
| Real, but outside the PR | open an issue with the path and severity, and add it to the [risk register](../riscos.en.md) if not fixed in 30 days |
| False positive | add it to `.github/codeql-accepted.json` with the reason, mitigation, owner and review date |
| Not sure | tag the champion in the PR; do not dismiss it |

A false positive without a written reason comes back in review. Six months later nobody remembers why
it was dismissed, and the same pattern shows up somewhere else.

## Severity, for the queue

| CodeQL severity | Time to fix |
|---|---|
| critical | 7 days |
| high | 30 days |
| medium | 90 days |
| low | next time the file is touched |
