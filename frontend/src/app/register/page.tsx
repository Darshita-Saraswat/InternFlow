import Link from "next/link";
import { AuthForm } from "@/components/auth-form";

export default function RegisterPage() {
  return (
    <main className="auth-shell">
      <header className="site-header auth-header">
        <Link aria-label="InternFlow home" className="brand" href="/">
          <span aria-hidden="true" className="brand-mark">i</span>
          <span>internflow</span>
        </Link>
        <Link className="nav-sign-in" href="/">Back to home</Link>
      </header>
      <section className="auth-layout">
        <div className="auth-story register-story">
          <div className="eyebrow"><span className="eyebrow-dot" /> YOUR NEXT CHAPTER STARTS HERE</div>
          <h2>Start where <span>you are.</span></h2>
          <p>No perfect plan needed. Create a space for the opportunities and ideas you want to come back to.</p>
          <div className="story-card register-card">
            <div className="onboarding-steps">
              <span className="step-number">01</span><span className="step-line" /><span className="step-number step-muted">02</span><span className="step-line" /><span className="step-number step-muted">03</span>
            </div>
            <p>Make space for the next possibility.</p>
            <span className="story-caption">ONE STEP AT A TIME</span>
          </div>
          <div className="story-footnote"><span aria-hidden="true">♡</span> A welcoming space for the work ahead.</div>
        </div>
        <div className="form-panel"><AuthForm mode="register" /></div>
      </section>
      <footer className="auth-footer"><span>© 2026 InternFlow</span><span>Thoughtful tools for your next step.</span></footer>
    </main>
  );
}
