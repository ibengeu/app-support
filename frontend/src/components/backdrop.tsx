// Abstract page background: blurred colour orbs, fine contour lines and grain.
// It is decorative only, so it is hidden from screen readers and ignores the pointer.

const GRAIN =
  "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E\")"

// Each line is the same sweep, shifted down a little, so they read as one flowing shell.
const CONTOURS = Array.from({ length: 9 }, (_, i) => {
  const y = i * 26
  return `M 120 ${-40 + y} C 380 ${40 + y}, 520 ${220 + y}, 920 ${180 + y * 1.4}`
})

export function Backdrop() {
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div className="orb absolute -top-48 -right-40 size-[38rem] rounded-full bg-primary/20 blur-[120px]" />
      <div
        className="orb absolute top-24 -right-16 size-[22rem] rounded-full bg-teal/20 blur-[100px]"
        style={{ animationDelay: '-9s' }}
      />
      <div
        className="orb absolute -bottom-56 -left-48 size-[36rem] rounded-full bg-sky/20 blur-[120px]"
        style={{ animationDelay: '-17s' }}
      />

      <svg
        className="absolute top-0 right-0 h-[28rem] w-[56rem] max-w-full text-primary/10"
        viewBox="0 0 920 448"
        preserveAspectRatio="xMaxYMin slice"
        fill="none"
      >
        {CONTOURS.map((d) => (
          <path key={d} d={d} stroke="currentColor" strokeWidth="1" />
        ))}
      </svg>

      <div className="absolute inset-0 opacity-[0.03] mix-blend-multiply" style={{ backgroundImage: GRAIN }} />
    </div>
  )
}
