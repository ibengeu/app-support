# Payments Support Assistant

An internal chat assistant that helps support staff investigate payment and account issues. The assistant reads practice logs, transaction records, service status, and help articles. It summarizes the evidence and can create an internal support ticket when a person must take over.

This is a **learning prototype**. All data is fake. Do not connect it to real customer systems.

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Project layout](#project-layout)
- [API](#api)
- [Safety controls](#safety-controls)
- [Known gaps](#known-gaps)
- [Product brief](#product-brief)

## Features

- Chat in plain English through a web UI or a terminal.
- Look up practice transaction records by payment reference, such as `PAY-10041`.
- Read the end of a service log file, such as `payment-log.txt` or `transfer-log.txt`.
- Check the simulated health of the Transfer, Account, and Customer services.
- Answer how-to questions from the help articles.
- Create an internal support ticket. This is the only write action.
- Keep conversation memory per chat thread while the server runs.

## Architecture

```text
Browser (React + Vite, :5173)
   │  POST /api/chat   (Vite proxy)
   ▼
FastAPI server (:8000)  ──►  LangChain agent  ──►  OpenRouter model
                                 │
                                 ├─ check_logs            → backend/src/logs/
                                 ├─ check_database        → in-memory fake records
                                 ├─ check_services        → simulated status
                                 ├─ search_docs           → backend/src/articles/
                                 └─ create_support_ticket → in-memory ticket list
```

| Part | Technology |
| --- | --- |
| Agent | LangChain `create_agent`, LangGraph `InMemorySaver` checkpointer |
| Model | OpenRouter (`openrouter:openrouter/free`) |
| API | FastAPI, Uvicorn, Pydantic |
| UI | React 19, Vite, Tailwind CSS 4, shadcn/ui, AI Elements |
| Tooling | `uv` and Ruff for Python; npm and oxlint for the UI |

## Requirements

- Python 3.12 or later
- [`uv`](https://docs.astral.sh/uv/)
- Node.js 20 or later and npm
- An [OpenRouter](https://openrouter.ai/) API key

## Quick start

1. Install the backend dependencies:

   ```sh
   cd backend
   uv sync
   ```

2. Add your key to a `.env` file. `python-dotenv` searches upward from `backend/src/agent.py`, so `backend/src/.env`, `backend/.env`, or the repository root `.env` all work.

   Copy the example to the repository root, then set `OPENROUTER_API_KEY` in it:

   ```sh
   cp ../.env.example ../.env
   ```

3. Start the API from `backend/src`. The tools read `logs/` and `articles/` relative to this folder.

   ```sh
   cd src
   uv run uvicorn server:build_app --factory --host 127.0.0.1 --port 8000
   ```

4. In a second terminal, start the UI:

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

5. Open <http://localhost:5173>. Vite forwards `/api` requests to the API on port 8000.

### Terminal chat (optional)

Use the terminal client instead of the web UI:

```sh
cd backend/src
uv run python main.py
```

Type `quit` to exit.

### Example questions

- "Why did payment PAY-10042 fail?"
- "The logs say PAY-10044 failed. What does the database show?"
- "Is the transfer service healthy?"
- "How do I find my payment reference?"
- "I can't resolve this. Please hand it to a person."

## Configuration

| Variable | Required | Purpose |
| --- | --- | --- |
| `OPENROUTER_API_KEY` | Yes | Authenticates model calls to OpenRouter. |

Keep keys in `.env` files only. Git ignores `.env` and `.env.*`. Never commit a real key.

Other settings are constants in the code:

| Setting | Location | Default |
| --- | --- | --- |
| Model | `backend/src/agent.py` | `openrouter:openrouter/free` |
| Log output cap | `MAX_LOG_CHARS` in `backend/src/tools.py` | 5,000 characters |
| Message size | `ChatRequest` in `backend/src/server.py` | 1–4,000 characters |
| API port | Uvicorn command | `8000` |

## Project layout

```text
backend/
  pyproject.toml          Python dependencies and Ruff settings
  AGENTS.md               Code review guidance for this learning project
  src/
    agent.py              Agent, system prompt, and PII middleware
    server.py             FastAPI app with POST /api/chat
    main.py               Terminal chat client
    tools.py              Agent tools and fake transaction records
    models/               Ticket and transaction data models
    logs/                 Practice service logs
    articles/             Help articles for search_docs
    client-brief.md       Product brief from the hypothetical client
frontend/
  src/App.tsx             Chat screen
  src/components/         shadcn/ui and AI Elements components
  vite.config.ts          Dev server and /api proxy
```

## API

### `POST /api/chat`

Request:

```json
{ "message": "Why did PAY-10042 fail?", "thread_id": "abc-123" }
```

| Field | Rules |
| --- | --- |
| `message` | 1–4,000 characters |
| `thread_id` | 1–64 characters: letters, digits, `_`, `-` |

Responses:

| Status | Meaning |
| --- | --- |
| `200` | `{ "reply": "..." }` |
| `400` | The message seems to contain an API key. The model did not receive it. |
| `422` | The request failed validation. |
| `502` | The agent call failed. The server logs the details; the client gets a safe message. |

## Safety controls

- **Read-only investigation.** No tool can change accounts, balances, transactions, or services. Ticket creation is the only write.
- **Prompt rules.** The system prompt treats user messages and tool results as data, not instructions. It forbids invented facts, raw log quotes, and claims of fixes.
- **PII middleware.** Email addresses in user input are masked. Messages that contain an `sk-…` API key are blocked before they reach the model.
- **Path containment.** `check_logs` resolves the file path and rejects paths outside `logs/`.
- **Output cap.** `check_logs` returns at most the last 5,000 characters.
- **Input limits.** The API limits message size and validates `thread_id`.
- **Safe errors.** Internal errors are logged on the server. The client gets a generic message. Logs record only the PII type, never the matched value.
- **Local binding.** The documented commands bind the API to `127.0.0.1`.

## Known gaps

The prototype does not yet meet all of [the client brief](backend/src/client-brief.md):

- No limit of six model turns per message.
- Only email addresses and `sk-` keys are redacted. Phone numbers, account numbers, passwords, and tokens are not.
- Only log output is capped. Other tool results are not.
- `escalate_to_human` is a stub and is not a working tool.
- Tickets, conversation memory, and transaction records live in memory and reset on restart.
- No structured audit log with conversation ID and timestamp.
- No evaluation set, evaluation runner, or results report.
- No automated tests.
- The API has no authentication. Keep it on localhost.
- Approved tools, limits, and service names are not configurable outside the code.

## Product brief

See [`backend/src/client-brief.md`](backend/src/client-brief.md) for the intended behavior, safety boundaries, and acceptance criteria.
