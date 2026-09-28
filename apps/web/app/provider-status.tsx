"use client";

import { useState } from "react";

export function ProviderStatus() {
  const [mode, setMode] = useState<"automatic" | "manual">("automatic");

  return (
    <label className="mode">
      <span>Routing</span>
      <select value={mode} onChange={event => setMode(event.target.value as typeof mode)}>
        <option value="automatic">Automatic</option>
        <option value="manual">Manual</option>
      </select>
    </label>
  );
}
