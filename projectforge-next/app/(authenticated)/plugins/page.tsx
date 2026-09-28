"use client";

import { PluginListPage } from "@/components/features/plugins/plugin-list-page";

/**
 * The Plugins (administration) route (`/next/plugins`), successor of Wicket's `PluginListPage`. A
 * standalone, admin-only action page; it takes no parameters, so no `<Suspense>`/`useSearchParams` is
 * needed.
 */
export default function PluginsRoute() {
  return <PluginListPage />;
}
