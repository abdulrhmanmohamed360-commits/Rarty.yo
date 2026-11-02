# EduForge Frontend

A React + TypeScript + Vite frontend prototype for EduForge, an AI-assisted educational content production workspace.

## Run locally

```bash
npm install
cp .env.example .env
npm run dev
```

Open the local URL printed by Vite.

## Production build

```bash
npm run build
npm run preview
```

## Current scope

- Frontend only. No backend, database, real authentication, or AI processing is implemented.
- Dashboard, project listing, multi-step project creation, production workspace, login preview, and supporting sections are included.
- Sample data is centralized in `src/mock` and project preview behavior in `src/services`.
- Axios client is centralized in `src/services/api/client.ts`.
- Configure `VITE_API_BASE_URL` when the real backend contract is available. Do not enable real API mode until endpoint and response contracts are confirmed.

## Architecture

`src/features` contains product areas; `src/layouts` contains shared application chrome; `src/services` is the integration boundary; `src/types` contains shared contracts; `src/mock` contains demo fixtures.
