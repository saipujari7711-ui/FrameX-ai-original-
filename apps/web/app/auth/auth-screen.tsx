"use client";

import { useState } from "react";

export function AuthScreen({ mode }: { mode: "login" | "signup" }) {
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setNotice("Authentication wiring is ready for the existing auth service; no credentials are sent from this visual surface yet.");
    setBusy(false);
  };

  return (
    <main className="auth-shell">
      <div className="auth-brand"><div className="auth-mark">FX</div><div><b>FRAME X</b><span>AI</span></div></div>
      <section className="auth-card">
        <div className="eyebrow">FRAME X / ACCOUNT</div>
        <h1>{mode === "login" ? "Welcome back." : "Create your workspace."}</h1>
        <p>{mode === "login" ? "Continue to your personal intelligence workspace." : "Your chats, providers and projects — organized around you."}</p>
        <button className="google-button" onClick={() => setNotice("Google Sign-In entry point. Connect it to the auth gateway before production.")}><span>G</span> Continue with Google</button>
        <div className="auth-divider"><span>or</span></div>
        <form onSubmit={submit}>
          <label>Email<input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" required /></label>
          <label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" required /></label>
          {mode === "signup" && <label>Workspace name<input placeholder="My FRAME X workspace" /></label>}
          <button className="button button--primary auth-submit" disabled={busy}>{busy ? "Working…" : mode === "login" ? "Sign in" : "Create account"} <span>→</span></button>
        </form>
        {notice && <div className="inline-notice" role="status">{notice}</div>}
        <p className="auth-switch">{mode === "login" ? "New to FRAME X?" : "Already have an account?"} <a href={mode === "login" ? "/auth/signup" : "/auth/login"}>{mode === "login" ? "Create account" : "Sign in"}</a></p>
      </section>
      <span className="auth-foot">Local-first workspace · Your provider keys stay under your control.</span>
    </main>
  );
}
