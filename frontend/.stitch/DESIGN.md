---
name: Support Agent — Clear Sky
colors:
  primary: '#0052CC'
  on-primary: '#FFFFFF'
  primary-container: '#DEEBFF'
  on-primary-container: '#0747A6'
  inverse-primary: '#4C9AFF'
  secondary: '#172B4D'
  on-secondary: '#FFFFFF'
  secondary-container: '#EBECF0'
  on-secondary-container: '#172B4D'
  tertiary: '#00B8D9'
  on-tertiary: '#FFFFFF'
  tertiary-container: '#E6FCFF'
  on-tertiary-container: '#006580'
  error: '#DE350B'
  on-error: '#FFFFFF'
  error-container: '#FFEBE6'
  on-error-container: '#BF2600'
  background: '#FFFFFF'
  on-background: '#172B4D'
  surface: '#FFFFFF'
  surface-dim: '#EBECF0'
  surface-bright: '#FFFFFF'
  surface-container-lowest: '#FFFFFF'
  surface-container-low: '#FAFBFC'
  surface-container: '#F4F5F7'
  surface-container-high: '#EBECF0'
  surface-container-highest: '#DFE1E6'
  surface-variant: '#F4F5F7'
  on-surface: '#172B4D'
  on-surface-variant: '#5E6C84'
  outline: '#97A0AF'
  outline-variant: '#DFE1E6'
  inverse-surface: '#172B4D'
  inverse-on-surface: '#F4F5F7'
  surface-tint: '#0052CC'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 56px
    fontWeight: '300'
    lineHeight: 60px
    letterSpacing: -0.03em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
    letterSpacing: -0.02em
  title-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-base:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: '0'
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: '0'
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.08em
  mono:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: '0'
rounded:
  sm: 0.5rem
  DEFAULT: 0.75rem
  md: 1rem
  lg: 1.5rem
  xl: 2rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 48px
---

# Design System: Support Agent — Clear Sky

## 1. Visual Theme & Atmosphere

Minimal, calm and airy. The product is **only a chat interface**: no top bar, no navigation, no sidebar, no dashboard cards. One centred column holds the conversation and the prompt input. The mood follows an architectural editorial style: a large light-weight greeting, generous white space and soft abstract shapes.

**Abstract background decoration** sits behind the whole page on a very light canvas (#FAFBFC):
- Two or three very large, heavily blurred colour fields ("orbs") in Atlassian Blue (#0052CC), Sky Blue (#4C9AFF) and Lagoon Teal (#00B8D9), at 15–25% opacity, placed off the page corners (top-right and bottom-left) and drifting very slowly.
- A few fine, flowing contour lines (1px, Atlassian Blue at 8–12% opacity) sweeping across one corner, like the curved shell of a building.
- A very subtle grain texture (3% opacity).
- Decoration never reduces text contrast. Messages always sit on solid white or solid light grey.

## 2. Color Palette & Roles

- **Atlassian Blue (#0052CC)** — Primary. Send button, user message bubbles, links, focus rings, active states.
- **Deep Atlassian Blue (#0747A6)** — Hover and pressed state for primary elements; text on light blue containers.
- **Sky Blue (#4C9AFF)** — Decorative orb colour and the primary colour in dark contexts.
- **Pale Sky (#DEEBFF)** — Selected states, suggestion chip hover, soft info highlights.
- **Midnight Navy (#172B4D)** — Secondary. All primary text and the greeting headline.
- **Slate Grey (#5E6C84)** — Secondary text and helper copy.
- **Lagoon Teal (#00B8D9)** — Tertiary accent. Decorative orbs and the "agent is working" shimmer. Never used for buttons.
- **Cloud Grey (#F4F5F7)** — Agent message bubbles and hover fills.
- **Hairline Grey (#DFE1E6)** — Borders and dividers.
- **Warm Red (#DE350B)** on **Blush (#FFEBE6)** — Errors only.
- **Pure White (#FFFFFF)** — Page sheet and card surfaces.

## 3. Typography Rules

- **Plus Jakarta Sans** for display and headings. Headlines are large and **light (300)** with tight negative letter-spacing, echoing the editorial reference. Never bold a display headline.
- **Inter** for body text, messages, inputs and buttons, at 15px for comfortable reading of long agent answers.
- **Small uppercase labels** (11px, 600, wide tracking) mark sections such as "SUGGESTED QUESTIONS" or "EVIDENCE".
- **JetBrains Mono** for payment references, transaction IDs and log lines inside agent answers.

## 4. Component Stylings

- **Greeting (empty state only):** Centred in the upper third of the page. A small round bot mark, the light-weight headline "How can I help today?" in Midnight Navy, and one line of Slate Grey text below it. It disappears when the conversation starts.
- **New chat:** A single small ghost pill button ("New chat" with a refresh icon) fixed at the top-right corner. It is the only control outside the column. It is not a bar.
- **Buttons:** Primary is pill-shaped or circular, Atlassian Blue fill, white icon or text, darkens to Deep Atlassian Blue on hover. Ghost buttons use navy text with a Cloud Grey hover.
- **Suggestion chips:** Three pill-shaped chips under the greeting, white fill, 1px Hairline Grey border, navy text; Pale Sky fill and blue border on hover.
- **Messages:** User bubbles are Atlassian Blue with white text, 1rem corners, right-aligned. Agent answers are Cloud Grey with navy text, left-aligned, with a small circular avatar holding a bot icon. Tables inside answers use hairline borders and white fill. No feedback icons or timestamps.
- **Working state:** The avatar and a Lagoon Teal shimmering line "Checking the evidence…".
- **Prompt input:** A large white card with 1.5rem rounded corners, a hairline border and a whisper-soft shadow (0 8px 24px rgba(9,30,66,0.08)). It holds only the text area and a circular Atlassian Blue send button at the bottom right. A tiny Slate Grey note under it: "Don't share passwords or card numbers."
- **Depth:** Mostly flat. Only the prompt input carries a soft diffused shadow.

## 5. Layout Principles

- Single centred column, max width 760px. No header, navigation or sidebar.
- 48px page margins on desktop and 16px on mobile. No horizontal scroll at any width.
- Empty state: greeting and chips sit in the upper-middle of the page, the prompt input sits at the bottom.
- Conversation state: messages fill the column from the top; 24px between messages.
- The prompt input is fixed at the bottom of the viewport with a soft fade to the canvas behind it.
