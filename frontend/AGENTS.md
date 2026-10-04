<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# Frontend Coding Standards & Rules

## 1. Indentation & Tab Size (Mandatory: 4 Spaces)
- **All code** (`.ts`, `.tsx`, `.js`, `.jsx`, `.css`, `.json`) MUST strictly use **4 spaces** indentation.
- NEVER use 2 spaces indentation.

## 2. Vertical Line Spacing & Breathing Room (Gap Lines)
- Avoid cramped/dense code blocks. Maintain 1 blank line between logical sections:
  - Between imports and component/type declarations.
  - Between interface/type definitions.
  - Between React hooks (`useState`, `useForm`, `useEffect`).
  - Between hooks and handler functions (`handleSubmit`, `onAction`).
  - Between individual handler functions.
  - Exactly 1 blank line before the main `return (` JSX statement.
- In JSX templates, separate major sections with readable line breaks.

## 3. Formatting Command
- Run `pnpm format` to reformat files using Prettier (`tabWidth: 4`).

