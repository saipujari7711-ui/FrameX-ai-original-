"use client";

import { useMemo, useState } from "react";

const FRAME_X_ICON_DATA = "data:image/webp;base64,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";

type Screen =
  | "home" | "chat" | "history" | "search" | "projects" | "categories"
  | "providers" | "models" | "settings" | "drive" | "profile";

type Provider = {
  name: string;
  state: "Connected" | "Needs key";
  keys: number;
  models: number;
  accent: string;
};

const providers: Provider[] = [
  { name: "OpenAI", state: "Connected", keys: 2, models: 18, accent: "cyan" },
  { name: "Google Gemini", state: "Needs key", keys: 0, models: 0, accent: "gold" },
  { name: "Groq", state: "Connected", keys: 1, models: 12, accent: "green" },
  { name: "Anthropic", state: "Needs key", keys: 0, models: 0, accent: "wine" }
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
      <img src={FRAME_X_ICON_DATA} alt="FRAME X AI" />
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
        <article className="stat-card"><span>Connected providers</span><strong>2</strong><small>OpenAI · Groq</small></article>
        <article className="stat-card"><span>Saved conversations</span><strong>48</strong><small>Across 5 categories</small></article>
        <article className="stat-card"><span>Drive sync</span><strong>Ready</strong><small>Last synced 7 min ago</small></article>
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
      <div className="model-strip"><span className="model-spark">✦</span><span><b>Using OpenAI</b><small>GPT-5.6 · {mode === "automatic" ? "Automatically selected for this task" : "Manual selection"}</small></span><button className="text-button">Change model</button></div>
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
        <button className="send-button" aria-label="Send message" onClick={() => setNotice(input.trim() ? "Message is staged in the UI. Provider execution remains behind the existing AI gateway." : "Write a message first.")}>↑</button>
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
  const [connected, setConnected] = useState(true);
  return <div className="screen"><ScreenHeading eyebrow="STORAGE / GOOGLE DRIVE" title="Your sync layer" copy="Drive is user-owned storage. FRAME X keeps local work usable offline and surfaces conflicts instead of silently overwriting." /><div className="drive-hero"><div className="drive-icon">↕</div><div><StatusPill tone={connected ? "green" : "gold"}>{connected ? "Connected" : "Needs authorization"}</StatusPill><h2>{connected ? "Everything is in sync." : "Connect Google Drive."}</h2><p>{connected ? "Last synchronized 7 minutes ago · 48 chats · 12 attachments" : "Authorize access to create the FRAME X AI folder structure."}</p></div><button className="button button--outline" onClick={() => setConnected(v => !v)}>{connected ? "Disconnect" : "Connect Drive"}</button></div><div className="sync-states"><article><span>↕</span><b>Syncing</b><small>Use for active progress</small></article><article><span>◌</span><b>Offline</b><small>Local changes remain available</small></article><article className="sync-state--conflict"><span>!</span><b>Conflict detected</b><small>Review both revisions</small></article></div></div>;
}

function Settings() {
  return <div className="screen"><ScreenHeading eyebrow="SYSTEM / SETTINGS" title="Settings" copy="Keep the product calm. Put technical controls where they can be understood and changed deliberately." /><div className="settings-list">{[["Appearance","Dark environment · reduced motion available","›"],["Routing","Automatic · capability-aware","›"],["API keys","3 stored securely on this device","›"],["Google Drive","Connected · last sync 7 min ago","›"],["Privacy & security","Local-first · no secret logging","›"],["Accessibility","Scalable type · keyboard · screen reader","›"]].map(([title, desc, arrow]) => <button className="setting-row" key={title}><span><b>{title}</b><small>{desc}</small></span><span>{arrow}</span></button>)}</div></div>;
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
      <div className="sidebar-bottom"><div className="sync-mini"><span className="sync-ring">↕</span><span><b>Drive connected</b><small>Synced 7 min ago</small></span></div><button className="profile-mini" onClick={() => go("profile")}><span className="avatar">S</span><span><b>Sairaj</b><small>Personal workspace</small></span><span>···</span></button></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><button className="mobile-menu" onClick={() => setSidebarOpen(v => !v)} aria-label="Open navigation">☰</button><Brand compact /><div className="topbar-actions"><button className="global-search" onClick={() => go("search")}><span>⌕</span><span>Search workspace</span><kbd>⌘ K</kbd></button><IconButton label="Notifications">◌</IconButton><button className="avatar avatar--top" onClick={() => go("profile")}>S</button></div></header>
      {content}
    </main>
    <nav className="mobile-nav" aria-label="Primary navigation">{nav.map(item => <button key={item.id} className={screen === item.id ? "active" : ""} onClick={() => go(item.id)}><span>{item.glyph}</span><small>{item.label}</small></button>)}</nav>
  </div>;
}
