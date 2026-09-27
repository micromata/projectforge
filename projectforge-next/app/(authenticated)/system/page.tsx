"use client";

import { SystemPage } from "@/components/features/system/system-page";

/**
 * The System (administration) route (`/next/system`), successor of Wicket's `wa/admin`. A standalone,
 * admin-only action page; it takes no parameters, so no `<Suspense>`/`useSearchParams` is needed.
 */
export default function SystemRoute() {
  return <SystemPage />;
}
