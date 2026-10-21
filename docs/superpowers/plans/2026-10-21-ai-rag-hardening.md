# AI/RAG hardening implementation plan

> Execute the user-approved design in this session using executing-plans and regression tests.

Goal: preserve draft payment details, isolate conversations and tools, improve grounded fallback and document the implemented system in Vietnamese.

Architecture: retain Spring AI, pgvector and the Next.js BFF. Preserve existing uncommitted work; no Docker changes, commits or deployments.

- [ ] Regression tests: draft form initialization, request context, conversation isolation, money parsing, grounded fallback, knowledge sync.
- [ ] Pass draft amount and note into the transfer form; rotate conversation on reset.
- [ ] Use request-scoped tool instances and explicit request state, without ThreadLocal. Scope memory keys to authenticated user IDs.
- [ ] Remove abandoned async LLM jobs; configure HTTP timeouts and bounded SSE execution.
- [ ] Ground fallback policies in Markdown, normalize money and preserve source citations; configure retrieval threshold.
- [ ] Restrict sync to admins; replace policy chunks transactionally without duplicates.
- [ ] Run backend/frontend checks, review the diff, document usage and known limitations.

Verification: Maven tests, frontend node tests, TypeScript and targeted lint. External LLM, PostgreSQL and payment integration checks require running services and will be reported separately.
