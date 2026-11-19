import { PluginPageClient } from "./page-client";

// One placeholder (/plugin/): Spring answers every deeper url (/next/plugin/<id>/items/5) with this shell through
// the spa-shell-map, and the plugin outlet reads the plugin and its route from the pathname at runtime.
export function generateStaticParams() {
  return [{ path: [] }];
}

export default function PluginPage() {
  return <PluginPageClient />;
}
