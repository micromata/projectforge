import { LogViewerPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g. /next/logViewer/7)
// to the SPA shell, where the client reads the real id from the URL at runtime.
export function generateStaticParams() {
  return [{ id: "0" }];
}

export default function LogViewerPage() {
  return <LogViewerPageClient />;
}
