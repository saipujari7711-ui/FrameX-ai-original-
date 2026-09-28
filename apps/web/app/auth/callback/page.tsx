"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { authGateway } from "@/lib/auth";

export default function AuthCallbackPage() {
  const router = useRouter();

  useEffect(() => {
    authGateway.getSession()
      .then(session => router.replace(session ? "/" : "/"))
      .catch(() => router.replace("/"));
  }, [router]);

  return <main className="shell"><p className="muted">Completing sign-in…</p></main>;
}
