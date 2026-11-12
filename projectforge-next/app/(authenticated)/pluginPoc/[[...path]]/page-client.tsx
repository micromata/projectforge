"use client";

import { PluginOutlet } from "@/components/plugin-runtime/plugin-outlet";

/** PoC route `/next/pluginPoc/**`: renders the pages of a runtime plugin bundle served by Spring. */
export function PluginPocPageClient() {
  return (
    <PluginOutlet
      entry="/next/plugins-assets/cveradar-poc/plugin.js"
      basePath="/pluginPoc"
    />
  );
}
