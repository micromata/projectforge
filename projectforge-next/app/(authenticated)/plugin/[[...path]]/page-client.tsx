"use client";

import { PluginOutlet } from "@/components/plugin-runtime/plugin-outlet";

/** `/next/plugin/<id>/**`: the pages of a plugin that brings its own frontend (see PluginFrontend in the backend). */
export function PluginPageClient() {
  return <PluginOutlet />;
}
