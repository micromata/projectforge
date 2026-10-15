import { ScriptEditPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links
// (e.g. /next/script/5/edit) to the SPA shell, where the client reads the real id from the URL
// at runtime (see ../page.tsx).
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function ScriptEditPage() {
  return <ScriptEditPageClient />;
}
