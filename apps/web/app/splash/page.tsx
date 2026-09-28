"use client";
import { useEffect } from "react";

export default function SplashPage(){
  useEffect(()=>{const id=window.setTimeout(()=>{window.location.href="/onboarding"},1200);return()=>window.clearTimeout(id)},[]);
  return <main className="splash-shell"><div className="splash-mark">FX</div><h1>FRAME X <span>AI</span></h1><p>Personal intelligence, deliberately yours.</p><div className="splash-line"/></main>;
}
