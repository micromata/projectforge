"use client";

import { PluginOutlet } from "@/components/plugin-runtime/plugin-outlet";

/** PoC route `/next/pluginPoc/`: renders the page of a runtime plugin bundle served by Spring. */
export default function PluginPocRoute() {
  return <PluginOutlet entry="/next/plugins-assets/cveradar-poc/plugin.js" />;
}
