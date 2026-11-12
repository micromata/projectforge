import { PluginPocPageClient } from "./page-client";

// One placeholder (/pluginPoc/); Spring answers every deeper url of the plugin (/next/pluginPoc/items/5) with
// this shell, and the plugin outlet matches the rest of the path at runtime.
export function generateStaticParams() {
  return [{ path: [] }];
}

export default function PluginPocPage() {
  return <PluginPocPageClient />;
}
