"""Small HTTP API so the web UI can talk to the support agent.

Run from the src/ folder (the tools read logs/ and articles/ relative to it):
    uv run uvicorn server:build_app --factory --host 127.0.0.1 --port 8000
"""

import logging

from fastapi import FastAPI, HTTPException
from langchain.agents.middleware import PIIDetectionError
from pydantic import BaseModel, Field

logger = logging.getLogger("support_agent.server")


class ChatRequest(BaseModel):
    # OWASP A04:2025 Insecure Design - size limits stop oversized prompts
    # from running up model cost or memory (CWE-770).
    message: str = Field(min_length=1, max_length=4000)
    thread_id: str = Field(min_length=1, max_length=64, pattern=r"^[A-Za-z0-9_-]+$")


class ChatResponse(BaseModel):
    reply: str


def _blocked_message(pii_type: str) -> str:
    if pii_type == "api_key":
        return (
            "Your message wasn't sent because it seems to contain an API key. "
            "Remove the key, then send it again."
        )
    return (
        "Your message wasn't sent because it seems to contain sensitive data. "
        "Remove it, then send it again."
    )


def create_app(agent) -> FastAPI:
    app = FastAPI(title="Support agent API")

    @app.post("/api/chat", response_model=ChatResponse)
    def chat(request: ChatRequest) -> ChatResponse:
        try:
            result = agent.invoke(
                {"messages": [{"role": "user", "content": request.message}]},
                config={"configurable": {"thread_id": request.thread_id}},
            )
        except PIIDetectionError as error:
            # The PII middleware stopped the message before the model saw it.
            # Log only the type, never the matched value (CWE-532).
            logger.warning(
                "Blocked %s in message for thread %s",
                error.pii_type,
                request.thread_id,
            )
            raise HTTPException(
                status_code=400, detail=_blocked_message(error.pii_type)
            )
        except Exception:
            # OWASP A02:2025 Security Misconfiguration - safe error response.
            # Details stay in the server log, not the browser (CWE-200).
            logger.exception("Agent call failed for thread %s", request.thread_id)
            raise HTTPException(
                status_code=502,
                detail="The agent couldn't answer. Send your message again.",
            )
        return ChatResponse(reply=result["messages"][-1].content)

    return app


def build_app() -> FastAPI:
    # Import here so create_app() can use a stand-in without loading the real model.
    from agent import agent

    return create_app(agent)
