# Deployment readiness plan — 2026-10-31

Approved scope: fix CI blockers, verify GitHub/GHCR, prepare a VPS demo with HTTPS and sandbox payment verification, then improve portfolio documentation. No VPS currently exists.

1. Reproduce and fix frontend lint errors and warnings without disabling rules. Preserve PIN confirmation, transfer drafts, token refresh, and translations. Validate lint, all frontend tests, TypeScript, production build.
2. Start local Docker, validate Java 21 backend and Docker builds; resolve reproducible build blockers. Review workflow gating, permissions, and runtime/build secret separation.
3. Commit only reviewed task files to a codex branch and push to GitHub where access exists. Obtain real CI results. Main-branch merge/release and missing GitHub access may require user action; do not claim registry publication without evidence.
4. Prepare production Compose and env template, public HTTPS reverse proxy, private database/cache/broker/media ports, deployment/health/backup/restore instructions. Validate Compose with disposable values. Server/domain access required before live deployment.
5. Update README and demo checklist with verified results; clearly separate prepared instructions from live evidence. Ask for VPS/domain details only when needed. Never claim real-money readiness.

Validation: eslint zero errors/warnings; full frontend tests; tsc; Next.js build; Maven Java21 verify; actionlint; Docker builds; production Compose validation; independent final review. Do not overwrite unrelated work or print secrets.
