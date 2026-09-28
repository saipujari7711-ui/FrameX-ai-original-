import { createClient } from "@supabase/supabase-js";
import type { AuthGateway, AuthSession } from "@frame-x/auth-core";

function client() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;
  if (!url || !key) throw new Error("Supabase configuration is missing.");
  return createClient(url, key, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true
    }
  });
}

function mapSession(session: Awaited<ReturnType<ReturnType<typeof client>["auth"]["getSession"]>>["data"]["session"]): AuthSession | null {
  if (!session) return null;
  return {
    accessToken: session.access_token,
    refreshToken: session.refresh_token,
    expiresAt: session.expires_at,
    user: {
      id: session.user.id,
      email: session.user.email,
      displayName: session.user.user_metadata?.full_name,
      avatarUrl: session.user.user_metadata?.avatar_url
    }
  };
}

export const authGateway: AuthGateway = {
  async getSession() {
    const { data, error } = await client().auth.getSession();
    if (error) throw error;
    return mapSession(data.session);
  },

  async signInWithPassword(email, password) {
    const { data, error } = await client().auth.signInWithPassword({ email, password });
    if (error || !data.session) throw error ?? new Error("No authentication session returned.");
    return mapSession(data.session)!;
  },

  async signUpWithPassword(email, password) {
    const { data, error } = await client().auth.signUp({ email, password });
    if (error) throw error;
    return mapSession(data.session);
  },

  async signInWithGoogle() {
    const { error } = await client().auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo: `${window.location.origin}/auth/callback` }
    });
    if (error) throw error;
  },

  async signOut() {
    const { error } = await client().auth.signOut();
    if (error) throw error;
  },

  async requestPasswordReset(email) {
    const { error } = await client().auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/auth/reset`
    });
    if (error) throw error;
  }
};
