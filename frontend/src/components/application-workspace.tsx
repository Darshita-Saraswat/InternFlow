"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { ApiError, getCurrentAccount, signOut } from "@/lib/auth-api";
import type { Account } from "@/lib/auth-api";

export function ApplicationWorkspace({
  title,
  description,
  actions,
  children,
  activeSection = "applications",
}: {
  title: string;
  description: string;
  actions?: ReactNode;
  children: ReactNode;
  activeSection?: "applications" | "opportunities" | "tasks" | "profile";
}) {
  const router = useRouter();
  const [account, setAccount] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const [signingOut, setSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState("");

  useEffect(() => {
    getCurrentAccount()
      .then((currentAccount) => {
        setAccount(currentAccount);
        setError("");
      })
      .catch((caught: unknown) => {
        if (caught instanceof ApiError && caught.status === 401) {
          router.replace("/login");
          return;
        }
        setError(caught instanceof Error ? caught.message : "Your account could not be loaded.");
      })
      .finally(() => setLoading(false));
  }, [retryCount, router]);

  async function handleSignOut() {
    setSigningOut(true);
    setSignOutError("");
    try {
      await signOut();
      router.replace("/login");
    } catch (caught) {
      setSignOutError(caught instanceof Error ? caught.message : "You could not be signed out.");
      setSigningOut(false);
    }
  }

  if (loading) {
    return (
      <main className="workspace-loading" aria-live="polite">
        <span className="spinner workspace-spinner" aria-hidden="true" />
        <p>Checking your secure session…</p>
      </main>
    );
  }

  if (error || !account) {
    return (
      <main className="workspace-loading">
        <section className="workspace-error" role="alert">
          <h1>We couldn’t open your workspace.</h1>
          <p>{error || "Your account could not be loaded."}</p>
          <button className="button" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button>
          <Link className="text-link" href="/login">Back to sign in</Link>
        </section>
      </main>
    );
  }

  return (
    <div className="workspace-shell">
      <header className="workspace-header">
        <Link aria-label="InternFlow applications" className="brand" href="/applications">
          <span aria-hidden="true" className="brand-mark">i</span>
          <span>internflow</span>
        </Link>
        <nav aria-label="Workspace navigation" className="workspace-nav">
          <Link aria-current={activeSection === "applications" ? "page" : undefined} href="/applications">Applications</Link>
          <Link aria-current={activeSection === "opportunities" ? "page" : undefined} href="/opportunities">Opportunities</Link>
          <Link aria-current={activeSection === "tasks" ? "page" : undefined} href="/tasks">Tasks</Link>
          <Link aria-current={activeSection === "profile" ? "page" : undefined} href="/profile">Profile</Link>
        </nav>
        <div className="workspace-account">
          <span className="workspace-user">{account.displayName}</span>
          <button className="button button-outline" disabled={signingOut} onClick={() => void handleSignOut()} type="button">
            {signingOut ? "Signing out…" : "Sign out"}
          </button>
        </div>
      </header>
      {signOutError && <p className="workspace-toast" role="alert">{signOutError}</p>}
      <main className="workspace-main">
        <div className="workspace-heading">
          <div>
            <p className="form-eyebrow">YOUR CAREER WORKSPACE</p>
            <h1>{title}</h1>
            <p>{description}</p>
          </div>
          {actions && <div className="workspace-actions">{actions}</div>}
        </div>
        {children}
      </main>
    </div>
  );
}
