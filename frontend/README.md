# Support agent UI

A small chat UI for the LangChain support agent in `../backend`. It uses shadcn/ui and AI Elements components.

## Run

1. Start the API from `backend/src` (the tools read `logs/` and `articles/` relative to that folder):

   ```sh
   cd ../backend/src
   uv run uvicorn server:build_app --factory --host 127.0.0.1 --port 8000
   ```

2. Start the UI:

   ```sh
   npm install
   npm run dev
   ```

3. Open http://localhost:5173. Vite forwards `/api` requests to the API on port 8000.
