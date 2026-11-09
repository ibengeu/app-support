import { useRef, useState } from 'react'
import { BotIcon, RotateCcwIcon } from 'lucide-react'
import { Backdrop } from '@/components/backdrop'
import {
  Conversation,
  ConversationContent,
  ConversationScrollButton,
} from '@/components/ai-elements/conversation'
import { Message, MessageContent, MessageResponse } from '@/components/ai-elements/message'
import {
  PromptInput,
  PromptInputBody,
  PromptInputFooter,
  PromptInputSubmit,
  PromptInputTextarea,
  PromptInputTools,
  type PromptInputMessage,
} from '@/components/ai-elements/prompt-input'
import { Shimmer } from '@/components/ai-elements/shimmer'
import { Suggestion } from '@/components/ai-elements/suggestion'
import { Button } from '@/components/ui/button'
import { TooltipProvider } from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'

type ChatMessage = { id: string; role: 'user' | 'assistant'; text: string; error?: boolean }

const SUGGESTIONS = ['Why did PAY-10041 fail?', 'Are all services healthy?', 'How do I cancel a payment?']

// Same limit as the API's ChatRequest.message max_length.
const MAX_MESSAGE_LENGTH = 4000

const newId = () => crypto.randomUUID().replaceAll('-', '')

const COPY = {
  unreachable: "Couldn't reach the support agent. Check that the API server is running, then send your message again.",
  failed: "The agent couldn't answer. Send your message again.",
  invalid: `That message couldn't be sent. Keep it under ${MAX_MESSAGE_LENGTH.toLocaleString()} characters, then send it again.`,
  empty: 'The agent sent back an empty answer. Try asking in a different way.',
}

type AgentReply = { text: string; error: boolean }

// The API sends a readable string in `detail` for errors it handles itself.
// Validation errors (422) send a list instead, so they get our own wording.
function errorText(status: number, detail: unknown): string {
  if (status === 422) return COPY.invalid
  return typeof detail === 'string' ? detail : COPY.failed
}

async function askAgent(message: string, threadId: string): Promise<AgentReply> {
  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, thread_id: threadId }),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) return { text: errorText(res.status, data.detail), error: true }

    const reply = String(data.reply ?? '').trim()
    return reply ? { text: reply, error: false } : { text: COPY.empty, error: true }
  } catch {
    return { text: COPY.unreachable, error: true }
  }
}

function BotAvatar() {
  return (
    <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-accent text-primary">
      <BotIcon className="size-4" />
    </div>
  )
}

function Greeting({ onPick }: { onPick: (text: string) => void }) {
  return (
    <div className="flex flex-col items-center pt-[14vh] text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-[0_8px_24px_rgba(0,82,204,0.25)]">
        <BotIcon className="size-6" />
      </div>
      <h1 className="mt-6 font-display text-4xl leading-tight font-light tracking-[-0.03em] text-foreground sm:text-[56px] sm:leading-[60px]">
        How can I help today?
      </h1>
      <p className="mt-3 max-w-xl text-[15px] leading-6 text-muted-foreground">
        I check logs, payment records, service health and help articles before I answer.
      </p>

      <p className="mt-10 text-[11px] font-semibold tracking-[0.08em] text-muted-foreground uppercase">Try asking</p>
      <div className="mt-3 flex flex-wrap justify-center gap-2">
        {SUGGESTIONS.map((s) => (
          <Suggestion
            key={s}
            suggestion={s}
            onClick={onPick}
            className="h-9 border-border bg-card px-4 text-[14px] font-normal text-foreground hover:border-primary hover:bg-accent hover:text-accent-foreground"
          />
        ))}
      </div>
    </div>
  )
}

function ChatBubble({ message }: { message: ChatMessage }) {
  if (message.role === 'user') {
    return (
      <Message from="user" className="message-in max-w-[85%]">
        <MessageContent className="rounded-2xl! bg-primary! px-4! py-2.5! text-[15px] leading-6 wrap-anywhere text-primary-foreground!">
          {message.text}
        </MessageContent>
      </Message>
    )
  }

  return (
    <Message from="assistant" className="message-in max-w-full flex-row items-start gap-3">
      <BotAvatar />
      <MessageContent
        className={cn(
          'min-w-0 rounded-2xl px-5 py-4 text-[15px] leading-6 wrap-anywhere',
          message.error ? 'bg-destructive-surface text-destructive-text' : 'bg-muted',
        )}
      >
        <MessageResponse className="[&_code]:font-mono [&_code]:text-[13px] [&_table]:bg-card [&_td]:border-border [&_th]:border-border [&_th]:text-[11px] [&_th]:tracking-[0.08em] [&_th]:uppercase">
          {message.text}
        </MessageResponse>
      </MessageContent>
    </Message>
  )
}

function WorkingRow() {
  return (
    <div className="message-in flex items-center gap-3">
      <BotAvatar />
      {/* Local override makes the shimmer use the teal accent. */}
      <div className="[--color-muted-foreground:var(--teal-text)]">
        <Shimmer className="text-[15px]">Checking the evidence…</Shimmer>
      </div>
    </div>
  )
}

export default function App() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [threadId, setThreadId] = useState(newId)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  async function send(text: string) {
    const message = text.trim()
    if (!message || busy) return

    setMessages((m) => [...m, { id: newId(), role: 'user', text: message }])
    setInput('')
    setBusy(true)
    // Keep keyboard focus in the text box; a clicked suggestion chip disappears.
    textareaRef.current?.focus()
    const reply = await askAgent(message, threadId)
    setMessages((m) => [...m, { id: newId(), role: 'assistant', ...reply }])
    setBusy(false)
    // Give the text back after a failure so "send it again" needs no retyping.
    if (reply.error) setInput((current) => current || message)
  }

  const handleSubmit = (message: PromptInputMessage) => send(message.text ?? '')

  function resetChat() {
    setMessages([])
    setInput('')
    setThreadId(newId())
    textareaRef.current?.focus()
  }

  return (
    <TooltipProvider>
      <Backdrop />

      {messages.length > 0 && (
        <Button
          variant="outline"
          size="sm"
          onClick={resetChat}
          disabled={busy}
          className="fixed top-4 right-4 z-20 rounded-full border-border bg-card text-foreground shadow-[0_4px_16px_rgba(9,30,66,0.08)] hover:bg-muted sm:top-6 sm:right-8"
        >
          <RotateCcwIcon className="size-4" /> New chat
        </Button>
      )}

      <main className="flex h-svh flex-col">
        <Conversation className="flex-1">
          <ConversationContent className="mx-auto w-full max-w-[760px] gap-6 px-4 pt-16 pb-8 sm:px-6">
            {messages.length === 0 ? (
              <Greeting onPick={send} />
            ) : (
              messages.map((m) => <ChatBubble key={m.id} message={m} />)
            )}
            {busy && <WorkingRow />}
          </ConversationContent>
          <ConversationScrollButton aria-label="Scroll to latest message" className="bg-card" />
        </Conversation>

        <div className="px-4 pb-4 sm:px-6 sm:pb-6">
          <div className="mx-auto w-full max-w-[760px]">
            <PromptInput
              onSubmit={handleSubmit}
              className="[&_[data-slot=input-group]]:rounded-3xl [&_[data-slot=input-group]]:border-border [&_[data-slot=input-group]]:bg-card [&_[data-slot=input-group]]:p-1.5 [&_[data-slot=input-group]]:shadow-[0_8px_24px_rgba(9,30,66,0.08)]"
            >
              <PromptInputBody>
                <PromptInputTextarea
                  ref={textareaRef}
                  aria-label="Message the support agent"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder={messages.length ? 'Ask a follow-up…' : 'Ask about a payment, a service or a log…'}
                  maxLength={MAX_MESSAGE_LENGTH}
                  className="text-[15px] placeholder:text-muted-foreground"
                />
              </PromptInputBody>
              <PromptInputFooter>
                <PromptInputTools />
                <PromptInputSubmit
                  aria-label="Send message"
                  status={busy ? 'submitted' : 'ready'}
                  disabled={!input.trim() || busy}
                  className="size-9 rounded-full bg-primary text-primary-foreground hover:bg-primary-hover"
                />
              </PromptInputFooter>
            </PromptInput>
            <p className="mt-2 text-center text-xs text-muted-foreground">Don't share passwords, card numbers or API keys.</p>
          </div>
        </div>
      </main>
    </TooltipProvider>
  )
}
