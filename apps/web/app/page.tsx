import { ProviderStatus } from "./provider-status";

export default function HomePage() {
  return (
    <main className="shell">
      <header className="header">
        <div>
          <div className="brand">FRAME X AI</div>
          <p className="muted">Personal AI platform · local-first foundation</p>
        </div>
        <ProviderStatus />
      </header>

      <section className="panel">
        <h1>Core foundation</h1>
        <p>
          The application shell is connected to the shared contracts and provider
          architecture. AI credentials are intentionally not bundled into this
          client.
        </p>
        <div className="grid">
          <article><strong>Provider adapters</strong><span>Ready for isolated implementations</span></article>
          <article><strong>Model registry</strong><span>Capability-aware descriptors</span></article>
          <article><strong>Local-first data</strong><span>Versioned records + sync queue</span></article>
          <article><strong>Google Drive</strong><span>Separate synchronization boundary</span></article>
        </div>
      </section>
    </main>
  );
}
