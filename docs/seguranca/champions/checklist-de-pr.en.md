# Secure PR checklist

*[Leia em português](checklist-de-pr.md)*

For whoever reviews a Vitrine PR. It is not meant to be ticked end to end: answer the questions that
apply to what changed. If an answer is "I don't know", the PR asks for help before it goes in.

## An API route changed

- [ ] Who may call it is in the Spring Security configuration, not only in the front?
- [ ] The resource owner comes from the token, not from the path, query or body?
- [ ] Another shop's resource is refused? If the resource is not public (account, document, erasure
      request), the answer is 404, so as not to confirm it exists. If it is already public, like a
      product in the showcase, 403 with a clear message is what the catalogue does.
- [ ] The input is a `record` with only the allowed fields, with Bean Validation?
- [ ] Is there a test that calls the route as another user and expects refusal?

## Stored data changed

- [ ] Is the new data necessary? Can we avoid storing it?
- [ ] Does it appear in a log, an error message or another route's response?
- [ ] Does the erasure saga ([ADR 0004](../../adr/0004-saga-de-exclusao-lgpd.md)) delete it?
- [ ] Does the "my data" file include it?

## Text one user writes and another reads changed

- [ ] Does it go through the sanitiser on input?
- [ ] Does the front show it as text, without `dangerouslySetInnerHTML`?

## A dependency changed

- [ ] Has the new version been published for at least 7 days, or is it a security fix?
- [ ] Did OSV-Scanner pass?
- [ ] Is the dependency maintained (last commit, number of maintainers)?

## The pipeline changed

- [ ] New action pinned by SHA, with the version in a comment?
- [ ] Is the job's `permissions` minimal?
- [ ] Does any new secret appear in `echo`, an artifact or a log?

## Authentication, resource ownership, stored data or the pipeline changed

- [ ] Were the [threat model](../modelo-de-ameacas.en.md) and the [ASVS checklist](../asvs.en.md)
      updated in the same PR?
- [ ] Did any new risk go into the [register](../riscos.en.md), with an owner and deadline?
