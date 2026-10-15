"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { LogViewer } from "@/components/features/log-viewer/log-viewer";

/**
 * The log viewer of one of the user's log subscriptions (`/next/logViewer/<id>`), linked from the pages that log
 * for the user (DATEV import, Merlin, scripting, mass update; see LogViewerRest.viewerUrl). Reads the id from
 * the URL at runtime, so any id works under the static export (see page.tsx and use-route-params.ts).
 */
export function LogViewerPageClient() {
  const raw = useRouteParams<{ id: string }>("/logViewer/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isInteger(id) || id < 0) notFound();
  // Keyed by the id, so moving from one subscription to another starts with that one's filter.
  return <LogViewer key={id} admin={false} id={id} />;
}
