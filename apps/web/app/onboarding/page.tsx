"use client";
import { useState } from "react";

const steps = [
  ["Connect your intelligence", "Add the providers you already trust. FRAME X keeps their credentials separate."],
  ["Let tasks choose the model", "Automatic routing uses task intent and known capabilities. You can switch to Manual anytime."],
  ["Keep your workspace yours", "Chats stay local-first, with Google Drive available as a user-owned synchronization layer."]
];

export default function OnboardingPage(){
  const [step,setStep]=useState(0);
  const last=step===steps.length-1;
  return <main className="onboarding-shell">
    <div className="onboarding-brand"><span>FX</span><b>FRAME X <i>AI</i></b></div>
    <div className="onboarding-progress">{steps.map((_,i)=><span key={i} className={i<=step?"active":""}/>)}</div>
    <section className="onboarding-card">
      <div className="onboarding-orbit"><div>FX</div></div>
      <div className="eyebrow">01 / 0{steps.length}</div>
      <h1>{steps[step][0]}</h1>
      <p>{steps[step][1]}</p>
      <button className="button button--primary" onClick={()=>last?window.location.href="/":setStep(v=>v+1)}>{last?"Enter FRAME X AI":"Continue"} <span>→</span></button>
      {!last && <button className="text-button" onClick={()=>window.location.href="/"}>Skip for now</button>}
    </section>
  </main>;
}
