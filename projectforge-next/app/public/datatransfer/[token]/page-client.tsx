"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { DataTransferPublicPage } from "@/components/features/datatransfer-public/datatransfer-public-page";

/**
 * Outside the (authenticated) group on purpose: the visitor has no ProjectForge login, the area's
 * token and password are all there is (see DataTransferPublicPage).
 */
export function DataTransferPublicPageClient() {
  const params = useRouteParams<{ token: string }>(
    "/public/datatransfer/[token]"
  );
  // Prerender pass, or a client navigation still on the old URL.
  if (!params) return null;
  // The placeholder of the static export is no token.
  if (params.token === "new") notFound();
  return <DataTransferPublicPage token={params.token} />;
}
