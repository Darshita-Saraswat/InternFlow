import Link from "next/link";
import { AuthForm } from "@/components/auth-form";

export default function LoginPage() {
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
        <div className="auth-story">
          <div className="eyebrow"><span className="eyebrow-dot" /> A LITTLE MORE CLARITY</div>
          <h2>Big goals. <span>Small next steps.</span></h2>
          <p>Give your internship search a home, so you can spend less time keeping track and more time moving forward.</p>
          <div className="story-card">
            <span aria-hidden="true" className="story-quote">“</span>
            <p>Your path doesn&apos;t have to be perfectly mapped to be moving in the right direction.</p>
            <span className="story-caption">A NOTE FROM INTERNFLOW</span>
          </div>
          <div className="story-footnote"><span aria-hidden="true">✳</span> Designed for the in-between moments of your search.</div>
        </div>
        <div className="form-panel"><AuthForm mode="login" /></div>
      </section>
      <footer className="auth-footer"><span>© 2026 InternFlow</span><span>Thoughtful tools for your next step.</span></footer>
    </main>
  );
}
