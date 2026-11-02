"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { AppLoginPage } from "@/components/features/app-login/app-login-page";

/**
 * Connecting the ProjectForge app with its parameters packed into the path (`/next/appLogin/<login>`): an SSO proxy
 * redirecting a browser without a proxy session to its login may bring it back without the query, but keeps the
 * path (see readAppLoginParams).
 */
export function AppLoginPathPageClient() {
  const params = useRouteParams<{ login: string }>("/appLogin/[login]");
  // Prerender pass, or a client navigation still on the old URL.
  if (!params) return null;
  // The placeholder of the static export is no login: the query alone decides.
  return (
    <AppLoginPage packedLogin={params.login === "new" ? null : params.login} />
  );
}
