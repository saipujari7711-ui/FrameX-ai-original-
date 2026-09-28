"use client";

import { useMemo, useState } from "react";

type Screen =
  | "home" | "chat" | "history" | "search" | "projects" | "categories"
  | "providers" | "models" | "settings" | "drive" | "profile";

type Provider = {
  name: string;
  state: "Connected" | "Needs key" | "Not configured";
  keys: number;
  models: number;
  accent: string;
};

const providers: Provider[] = [
  { name: "OpenAI", state: "Not configured", keys: 0, models: 0, accent: "cyan" },
  { name: "Google Gemini", state: "Not configured", keys: 0, models: 0, accent: "gold" },
  { name: "Groq", state: "Not configured", keys: 0, models: 0, accent: "green" },
  { name: "Anthropic", state: "Not configured", keys: 0, models: 0, accent: "wine" }
];

const nav: { id: Screen; label: string; glyph: string }[] = [
  { id: "home", label: "Home", glyph: "⌂" },
  { id: "chat", label: "Chat", glyph: "✦" },
  { id: "history", label: "History", glyph: "◷" },
  { id: "projects", label: "Projects", glyph: "▱" },
  { id: "search", label: "Search", glyph: "⌕" }
];

const moreNav: { id: Screen; label: string; glyph: string }[] = [
  { id: "categories", label: "Categories", glyph: "◈" },
  { id: "providers", label: "Providers", glyph: "◎" },
  { id: "models", label: "Models", glyph: "◇" },
  { id: "drive", label: "Google Drive", glyph: "↕" },
  { id: "settings", label: "Settings", glyph: "⚙" },
  { id: "profile", label: "Profile", glyph: "○" }
];

function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className={compact ? "brand brand--compact" : "brand-lockup"}>
      <img src="/brand/framex-logo-master.png" alt="FRAME X AI" />
      {!compact && <span>Personal intelligence, deliberately yours.</span>}
    </div>
  );
}

function IconButton({ label, onClick, children }: { label: string; onClick?: () => void; children: React.ReactNode }) {
  return <button className="icon-button" aria-label={label} title={label} onClick={onClick}>{children}</button>;
}

function StatusPill({ children, tone = "cyan" }: { children: React.ReactNode; tone?: "cyan" | "gold" | "green" | "wine" | "muted" }) {
  return <span className={"status-pill status-pill--" + tone}><i />{children}</span>;
}

function ScreenHeading({ eyebrow, title, copy, action }: { eyebrow: string; title: string; copy?: string; action?: React.ReactNode }) {
  return (
    <div className="screen-heading">
      <div>
        <div className="eyebrow">{eyebrow}</div>
        <h1>{title}</h1>
        {copy && <p>{copy}</p>}
      </div>
      {action}
    </div>
  );
}

function Home({ go }: { go: (screen: Screen) => void }) {
  return (
    <div className="screen">
      <ScreenHeading eyebrow="Tuesday · 28 September" title="Good morning, Sairaj." copy="Your workspace is ready. Pick up where you left off or start a new line of thought." action={<StatusPill>Routing · Automatic</StatusPill>} />
      <section className="hero-panel">
        <div className="hero-copy">
          <span className="kicker">FRAME X / INTELLIGENCE LAYER</span>
          <h2>One workspace.<br /><em>Every capable model.</em></h2>
          <p>FRAME X AI routes each task through the providers you connect, while keeping your chats, projects and files organized around you.</p>
          <div className="hero-actions">
            <button className="button button--primary" onClick={() => go("chat")}>Open new chat <span>→</span></button>
            <button className="button button--quiet" onClick={() => go("providers")}>Manage providers</button>
          </div>
        </div>
        <div className="hero-orbit" aria-hidden="true"><div className="orbit orbit--one" /><div className="orbit orbit--two" /><div className="orbit-core">FX<span>AI</span></div></div>
      </section>
      <div className="section-label">Workspace pulse</div>
      <section className="stat-grid">
        <article className="stat-card"><span>Connected providers</span><strong>0</strong><small>Connect providers to begin</small></article>
        <article className="stat-card"><span>Saved conversations</span><strong>0</strong><small>Local conversations appear here</small></article>
        <article className="stat-card"><span>Drive sync</span><strong>Not connected</strong><small>Google Drive is not configured</small></article>
        <article className="stat-card"><span>Current mode</span><strong>Automatic</strong><small>Capability-aware routing</small></article>
      </section>
      <section className="content-grid content-grid--two">
        <div className="panel-block">
          <div className="panel-title"><div><span className="eyebrow">Continue</span><h3>Recent conversations</h3></div><button className="text-button" onClick={() => go("history")}>View history →</button></div>
          <div className="conversation-list">
            {[
              ["Frame X architecture", "Coding", "12 min ago"],
              ["Japan learning roadmap", "Planning", "Yesterday"],
              ["Research: AI automation", "Research", "2 days ago"]
            ].map(([title, cat, time]) => (
              <button className="conversation-row" key={title} onClick={() => go("chat")}><span className="conversation-mark">FX</span><span><b>{title}</b><small>{cat} · {time}</small></span><span className="row-arrow">→</span></button>
            ))}
          </div>
        </div>
        <div className="panel-block">
          <div className="panel-title"><div><span className="eyebrow">System</span><h3>Connection status</h3></div></div>
          <div className="provider-mini-list">
            <div><span><i className="dot dot--cyan" />OpenAI</span><StatusPill>2 keys</StatusPill></div>
            <div><span><i className="dot dot--green" />Groq</span><StatusPill tone="green">1 key</StatusPill></div>
            <div><span><i className="dot dot--gold" />Google Drive</span><StatusPill tone="gold">Connected</StatusPill></div>
          </div>
        </div>
      </section>
    </div>
  );
}

function Chat() {
  const [mode, setMode] = useState<"automatic" | "manual">("automatic");
  const [taskMode, setTaskMode] = useState(false);
  const [input, setInput] = useState("");
  const [notice, setNotice] = useState("");
  return (
    <div className={"screen chat-screen " + (taskMode ? "chat-screen--task" : "")}>
      <div className="chat-topbar">
        <div><div className="eyebrow">CHAT / NEW CONVERSATION</div><h1>{taskMode ? "Challenge mode" : "Untitled conversation"}</h1></div>
        <div className="chat-controls">
          <label className="segmented"><button className={mode === "automatic" ? "active" : ""} onClick={() => setMode("automatic")}>Automatic</button><button className={mode === "manual" ? "active" : ""} onClick={() => setMode("manual")}>Manual</button></label>
          <button className={taskMode ? "mode-toggle mode-toggle--task" : "mode-toggle"} onClick={() => setTaskMode(v => !v)}>{taskMode ? "Challenge" : "Task mode"}</button>
          <IconButton label="More chat actions">⋯</IconButton>
        </div>
      </div>
      <div className="model-strip"><span className="model-spark">✦</span><span><b>Model not selected</b><small>{mode === "automatic" ? "Automatic routing waits for a compatible configured provider" : "Choose a configured model"}</small></span><button className="text-button">Change model</button></div>
      <div className="chat-thread" aria-live="polite">
        <div className="chat-date">Today · 07:18</div>
        <div className="message message--user"><div className="message-label">YOU</div><div className="bubble bubble--user">Help me structure the next phase of FRAME X AI around the user experience without touching the provider core.</div></div>
        <div className="message message--ai">
          <div className="message-label">FRAME X / AI</div>
          <div className="bubble bubble--ai">
            <p>Absolutely. The product should make the underlying complexity feel <strong>quiet</strong> while keeping the user in control.</p>
            <h4>A useful design principle</h4><p><em>Expose decisions, hide implementation noise.</em></p>
            <ul><li>Show the provider and model when it matters.</li><li>Keep routing explanations concise.</li><li>Make sync state visible without turning it into infrastructure UI.</li></ul>
            <pre><code>{"routing.mode = \\"automatic\\"\\ncapability.state = \\"known\\""}</code></pre>
            <div className="message-actions"><button>Copy</button><button>Regenerate</button><button>Retry</button></div>
          </div>
        </div>
        <div className="message message--system"><span>Attachments and voice input are available when a compatible provider capability is connected.</span></div>
      </div>
      {notice && <div className="inline-notice" role="status">{notice}</div>}
      <div className="composer">
        <div className="composer-tools"><IconButton label="Attach file" onClick={() => setNotice("Attachment picker is ready for the next integration step.")}>＋</IconButton><IconButton label="Voice input" onClick={() => setNotice("Voice interface is capability-gated; connect a provider that supports audio input.")}>◉</IconButton></div>
        <textarea value={input} onChange={e => setInput(e.target.value)} placeholder={taskMode ? "Describe the challenge..." : "Ask FRAME X anything..."} aria-label="Message" />
        <button className="send-button" aria-label="Send message" onClick={() => setNotice(input.trim() ? "No provider is connected to this chat surface yet. Your message was not sent." : "Write a message first.")}>↑</button>
      </div>
      <div className="composer-meta"><span>AI output can be reviewed before it is saved to your workspace.</span><span>Enter ↵ · Shift+Enter newline</span></div>
    </div>
  );
}

function History({ go }: { go: (s: Screen) => void }) {
  const [query, setQuery] = useState("");
  const items = [
    ["Frame X architecture", "Coding", "Today", "OpenAI · GPT-5.6"],
    ["Japan learning roadmap", "Planning", "Yesterday", "Groq · Llama"],
    ["AI automation research", "Research", "24 Sep", "OpenAI · GPT-5.6"],
    ["C pointers revision", "Study", "22 Sep", "Groq · Llama"],
    ["Portfolio direction", "Other", "20 Sep", "OpenAI · GPT-5.6"]
  ];
  const filtered = useMemo(() => items.filter(x => x.join(" ").toLowerCase().includes(query.toLowerCase())), [query]);
  return <div className="screen"><ScreenHeading eyebrow="WORKSPACE / HISTORY" title="Conversation archive" copy="Every chat stays independently versioned and searchable." action={<button className="button button--primary" onClick={() => go("chat")}>+ New chat</button>} /><div className="search-field"><span>⌕</span><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search conversations, projects or models..." /><kbd>⌘ K</kbd></div><div className="history-table"><div className="history-head"><span>Conversation</span><span>Category</span><span>Last activity</span><span>Model</span><span /></div>{filtered.map(([title, cat, time, model]) => <button className="history-row" key={title} onClick={() => go("chat")}><span><b>{title}</b><small>48 messages · local revision</small></span><StatusPill tone={cat === "Research" ? "gold" : cat === "Study" ? "green" : "cyan"}>{cat}</StatusPill><span>{time}</span><span>{model}</span><span>→</span></button>)}</div></div>;
}

function Providers({ go }: { go: (s: Screen) => void }) {
  return <div className="screen"><ScreenHeading eyebrow="SYSTEM / PROVIDERS" title="Your model network" copy="Connect providers once. FRAME X keeps credentials local and routes tasks through capability-aware adapters." action={<button className="button button--primary">+ Add provider</button>} /><div className="provider-grid">{providers.map(p => <article className="provider-card" key={p.name}><div className="provider-logo">{p.name.slice(0, 1)}</div><div className="provider-info"><h3>{p.name}</h3><StatusPill tone={p.state === "Connected" ? (p.accent as "cyan" | "green") : "gold"}>{p.state}</StatusPill></div><div className="provider-stats"><div><span>Keys</span><b>{p.keys}</b></div><div><span>Models</span><b>{p.models || "—"}</b></div><div><span>Capability map</span><b>{p.state === "Connected" ? "Ready" : "Pending"}</b></div></div><button className="button button--outline" onClick={() => go("models")}>{p.state === "Connected" ? "Manage models" : "Connect key"} →</button></article>)}</div><div className="security-note"><span>⌁</span><div><b>Credential boundary</b><p>Secrets are never rendered in full, logged, or bundled into the web client. Provider requests stay behind the existing gateway boundary.</p></div></div></div>;
}

function Models() {
  return <div className="screen"><ScreenHeading eyebrow="SYSTEM / MODEL REGISTRY" title="Models & capabilities" copy="The registry describes what is known about each connected model. Unknown capabilities are never assumed." /><div className="model-table">{[["GPT-5.6", "OpenAI", "Reasoning · text", "Known"],["Llama 3.3 70B", "Groq", "Text · tools", "Known"],["Gemini", "Google Gemini", "Text · vision", "Needs key"]].map(([model, provider, caps, state]) => <div className="model-row" key={model}><span className="model-name"><b>{model}</b><small>{provider}</small></span><span>{caps}</span><StatusPill tone={state === "Known" ? "green" : "gold"}>{state}</StatusPill><button className="icon-button" aria-label={"Select " + model}>→</button></div>)}</div></div>;
}

function Drive() {
  const [connected, setConnected] = useState(false);
  return <div className="screen"><ScreenHeading eyebrow="STORAGE / GOOGLE DRIVE" title="Your sync layer" copy="Drive is user-owned storage. FRAME X keeps local work usable offline and surfaces conflicts instead of silently overwriting." /><div className="drive-hero"><div className="drive-icon">↕</div><div><StatusPill tone={connected ? "green" : "gold"}>{connected ? "Connected" : "Needs authorization"}</StatusPill><h2>{connected ? "Everything is in sync." : "Connect Google Drive."}</h2><p>{connected ? "No synchronization has been performed" : "Authorize access to create the FRAME X AI folder structure."}</p></div><button className="button button--outline" onClick={() => setConnected(v => !v)}>{connected ? "Disconnect" : "Connect Drive"}</button></div><div className="sync-states"><article><span>↕</span><b>Syncing</b><small>Use for active progress</small></article><article><span>◌</span><b>Offline</b><small>Local changes remain available</small></article><article className="sync-state--conflict"><span>!</span><b>Conflict detected</b><small>Review both revisions</small></article></div></div>;
}

function Settings() {
  return <div className="screen"><ScreenHeading eyebrow="SYSTEM / SETTINGS" title="Settings" copy="Keep the product calm. Put technical controls where they can be understood and changed deliberately." /><div className="settings-list">{[["Appearance","Dark environment · reduced motion available","›"],["Routing","Automatic · capability-aware","›"],["API keys","No provider credentials configured","›"],["Google Drive","Not connected","›"],["Privacy & security","Local-first · no secret logging","›"],["Accessibility","Scalable type · keyboard · screen reader","›"]].map(([title, desc, arrow]) => <button className="setting-row" key={title}><span><b>{title}</b><small>{desc}</small></span><span>{arrow}</span></button>)}</div></div>;
}

function Generic({ screen }: { screen: Screen }) {
  const data: Record<string, [string, string, string]> = {
    projects: ["WORKSPACE / PROJECTS", "Project library", "Group chats, files and instructions around a meaningful outcome."],
    categories: ["WORKSPACE / CATEGORIES", "Your thinking, organized", "Study, Research, Coding, Planning, Automation and Other."],
    search: ["WORKSPACE / SEARCH", "Search across your workspace", "Find conversations, files and project context without leaving your flow."],
    profile: ["ACCOUNT / PROFILE", "Your FRAME X account", "Identity, devices, sync and account controls."]
  };
  const [eyebrow, title, copy] = data[screen];
  return <div className="screen"><ScreenHeading eyebrow={eyebrow} title={title} copy={copy} /><div className="empty-state"><div className="empty-mark">FX</div><h2>{screen === "projects" ? "Build around outcomes." : screen === "categories" ? "Choose a lane." : "Nothing hidden."}</h2><p>This surface is intentionally structured now so the real data layer can be connected without changing the visual system.</p><button className="button button--primary">{screen === "profile" ? "Account settings" : "Create new"}</button></div></div>;
}

export default function HomePage() {
  const [screen, setScreen] = useState<Screen>("home");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const go = (next: Screen) => { setScreen(next); setSidebarOpen(false); };
  const content = screen === "home" ? <Home go={go} /> : screen === "chat" ? <Chat /> : screen === "history" ? <History go={go} /> : screen === "providers" ? <Providers go={go} /> : screen === "models" ? <Models /> : screen === "drive" ? <Drive /> : screen === "settings" ? <Settings /> : <Generic screen={screen} />;
  return <div className="app-shell">
    <aside className={"sidebar " + (sidebarOpen ? "sidebar--open" : "")}>
      <Brand />
      <div className="sidebar-section"><span className="sidebar-label">Workspace</span>{nav.map(item => <button key={item.id} className={screen === item.id ? "nav-item active" : "nav-item"} onClick={() => go(item.id)}><span>{item.glyph}</span>{item.label}{item.id === "chat" && <kbd>N</kbd>}</button>)}</div>
      <div className="sidebar-section"><span className="sidebar-label">System</span>{moreNav.map(item => <button key={item.id} className={screen === item.id ? "nav-item active" : "nav-item"} onClick={() => go(item.id)}><span>{item.glyph}</span>{item.label}</button>)}</div>
      <div className="sidebar-bottom"><div className="sync-mini"><span className="sync-ring">↕</span><span><b>Drive not connected</b><small>Synchronization unavailable</small></span></div><button className="profile-mini" onClick={() => go("profile")}><span className="avatar">S</span><span><b>Sairaj</b><small>Personal workspace</small></span><span>···</span></button></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><button className="mobile-menu" onClick={() => setSidebarOpen(v => !v)} aria-label="Open navigation">☰</button><Brand compact /><div className="topbar-actions"><button className="global-search" onClick={() => go("search")}><span>⌕</span><span>Search workspace</span><kbd>⌘ K</kbd></button><IconButton label="Notifications">◌</IconButton><button className="avatar avatar--top" onClick={() => go("profile")}>S</button></div></header>
      {content}
    </main>
    <nav className="mobile-nav" aria-label="Primary navigation">{nav.map(item => <button key={item.id} className={screen === item.id ? "active" : ""} onClick={() => go(item.id)}><span>{item.glyph}</span><small>{item.label}</small></button>)}</nav>
  </div>;
}
