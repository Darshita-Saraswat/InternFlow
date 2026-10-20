"use client";

import Link from "next/link";
import { useState } from "react";
import type { FormEvent } from "react";
import { registerAccount, signIn } from "@/lib/auth-api";
import type { Account } from "@/lib/auth-api";

type AuthMode = "login" | "register";

export function AuthForm({ mode }: { mode: AuthMode }) {
  const isRegister = mode === "register";
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [account, setAccount] = useState<Account | null>(null);
  const [registered, setRegistered] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (isRegister && password !== confirmPassword) {
      setError("Those passwords don’t match yet.");
      return;
    }
    if (new TextEncoder().encode(password).length > 72) {
      setError("Please use a password of 72 UTF-8 bytes or fewer.");
      return;
    }
    setBusy(true);
    try {
      if (isRegister) {
        await registerAccount({ email: email.trim(), displayName: displayName.trim(), password });
        setRegistered(true);
      } else {
        setAccount(await signIn({ email: email.trim(), password }));
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Something went wrong. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  if (account) {
    return (
      <div aria-live="polite" className="auth-success">
        <span aria-hidden="true" className="success-mark">✓</span>
        <p className="form-eyebrow">YOU&apos;RE SIGNED IN</p>
        <h2>Welcome, {account.displayName}.</h2>
        <p>Your secure session is active for <strong>{account.email}</strong>.</p>
        <p className="workspace-note">Your saved applications are available in your private workspace.</p>
        <Link className="button form-submit" href="/applications">Open my applications <span aria-hidden="true">→</span></Link>
      </div>
    );
  }

  if (registered) {
    return (
      <div aria-live="polite" className="auth-success">
        <span aria-hidden="true" className="success-mark">✓</span>
        <p className="form-eyebrow">ACCOUNT CREATED</p>
        <h2>You&apos;re on your way.</h2>
        <p>Your account is ready. Sign in with your email and password to continue.</p>
        <Link className="button form-submit" href="/login">Continue to sign in <span aria-hidden="true">→</span></Link>
      </div>
    );
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <p className="form-eyebrow">{isRegister ? "START WITH A FRESH PAGE" : "GOOD TO HAVE YOU BACK"}</p>
        <h1>{isRegister ? "Create your account" : "Welcome back"}</h1>
        <p>{isRegister ? "A few details, then you can get started." : "Sign in to pick up where you left off."}</p>
      </div>

      {isRegister && (
        <label className="field">
          <span>Your name</span>
          <input
            autoComplete="name"
            maxLength={120}
            minLength={2}
            name="name"
            onChange={(event) => setDisplayName(event.target.value)}
            placeholder="e.g. Alex Morgan"
            required
            value={displayName}
          />
        </label>
      )}
      <label className="field">
        <span>Email address</span>
        <input
          autoComplete="email"
          maxLength={320}
          name="email"
          onChange={(event) => setEmail(event.target.value)}
          placeholder="you@university.edu"
          required
          type="email"
          value={email}
        />
      </label>
      <label className="field">
        <span>Password</span>
        <span className="password-control">
          <input
            autoComplete={isRegister ? "new-password" : "current-password"}
            maxLength={72}
            minLength={isRegister ? 12 : undefined}
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder={isRegister ? "At least 12 characters" : "Enter your password"}
            required
            type={showPassword ? "text" : "password"}
            value={password}
          />
          <button
            aria-label={showPassword ? "Hide password" : "Show password"}
            aria-pressed={showPassword}
            className="password-toggle"
            onClick={() => setShowPassword((visible) => !visible)}
            type="button"
          >
            {showPassword ? "Hide" : "Show"}
          </button>
        </span>
      </label>
      {isRegister && (
        <label className="field">
          <span>Confirm password</span>
          <input
            autoComplete="new-password"
            maxLength={72}
            name="confirm-password"
            onChange={(event) => setConfirmPassword(event.target.value)}
            placeholder="Type it once more"
            required
            type="password"
            value={confirmPassword}
          />
        </label>
      )}

      {error && <p aria-live="assertive" className="form-error" role="alert">{error}</p>}
      <button className="button form-submit" disabled={busy} type="submit">
        {busy ? <><span aria-hidden="true" className="spinner" /> {isRegister ? "Creating your account…" : "Signing you in…"}</> : <>{isRegister ? "Create account" : "Sign in"} <span aria-hidden="true">→</span></>}
      </button>
      <p className="form-terms">
        {isRegister ? "Your password is securely hashed before it is stored." : "Your sign-in uses a private, server-side session."}
      </p>
      <p className="form-switch">
        {isRegister ? "Already have an account?" : "New to InternFlow?"}{" "}
        <Link href={isRegister ? "/login" : "/register"}>{isRegister ? "Sign in" : "Create an account"}</Link>
      </p>
    </form>
  );
}
