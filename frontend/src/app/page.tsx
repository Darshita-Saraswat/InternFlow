import Link from "next/link";

export default function Home() {
  return (
    <main className="landing-shell">
      <header className="site-header">
        <Link aria-label="InternFlow home" className="brand" href="/">
          <span aria-hidden="true" className="brand-mark">i</span>
          <span>internflow</span>
        </Link>
        <nav aria-label="Main navigation" className="header-actions">
          <Link className="nav-sign-in" href="/login">Sign in</Link>
          <Link className="button button-small" href="/register">Create your account</Link>
        </nav>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <div className="eyebrow"><span className="eyebrow-dot" /> YOUR NEXT CHAPTER STARTS HERE</div>
          <h1>Make your internship search <span>feel manageable.</span></h1>
          <p className="hero-description">
            A calmer place to keep track of the roles you want, the work you need to do, and the progress you&apos;re making.
          </p>
          <div className="hero-actions">
            <Link className="button" href="/register">
              Get started <span aria-hidden="true">→</span>
            </Link>
            <span className="free-note">Made for students, built around your pace.</span>
          </div>
          <div className="trust-note">
            <span aria-hidden="true" className="trust-icon">✓</span>
            Your search, your pace. Your data stays yours.
          </div>
        </div>

        <div aria-label="Illustrative preview of the applications workspace, not live account data" className="preview-wrap">
          <div className="preview-glow" />
          <div className="preview-card">
            <div className="preview-topbar">
              <div className="preview-brand"><span className="mini-mark">i</span> internflow</div>
              <span className="preview-avatar">S</span>
            </div>
            <div className="preview-content">
              <div className="preview-welcome">APPLICATIONS WORKSPACE</div>
              <h2>Your applications, in one place</h2>
              <p className="preview-subtitle">Saved roles and status, using records you add.</p>
              <div className="preview-stats">
                <div><span className="stat-number">—</span><span className="stat-label">Applications</span></div>
                <div><span className="stat-number green">—</span><span className="stat-label">Next steps</span></div>
                <div><span className="stat-number orange">—</span><span className="stat-label">Milestones</span></div>
              </div>
              <div className="preview-list-title"><span>REAL RECORDS ONLY</span><span className="preview-today">EMPTY UNTIL YOU ADD</span></div>
              <div className="preview-task">
                <span className="task-check" />
                <span className="task-lines"><b>Application tracker</b><small>Your records, when you add them</small></span>
                <span className="task-date">READY</span>
              </div>
              <div className="preview-task">
                <span className="task-check task-check-green" />
                <span className="task-lines"><b>Dates and next steps</b><small>Saved with each application</small></span>
                <span className="task-date">READY</span>
              </div>
            </div>
            <div className="preview-footer"><span className="footer-dot" /> NO SAMPLE ACCOUNT DATA</div>
          </div>
          <div aria-hidden="true" className="floating-note">
            <span className="floating-spark">✦</span>
            <span><b>One step at a time</b><small>Progress is progress.</small></span>
          </div>
        </div>
      </section>

      <section aria-label="Product principles" className="principles">
        <article><span className="principle-icon lavender">✳</span><div><h2>Everything in one place</h2><p>Keep applications and next steps together.</p></div></article>
        <article><span className="principle-icon mint">↗</span><div><h2>Progress you can see</h2><p>Let your real activity tell the story.</p></div></article>
        <article><span className="principle-icon peach">♡</span><div><h2>Built for your journey</h2><p>Thoughtful tools, without the pressure.</p></div></article>
      </section>

      <footer className="landing-footer">
        <span>Made for the work that comes before your first day.</span>
        <span>© 2026 InternFlow</span>
      </footer>
    </main>
  );
}
