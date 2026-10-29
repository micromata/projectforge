import { CveRadarClosurePageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links to the SPA shell, where
// the client reads the real id from the URL at runtime.
export function generateStaticParams() {
  return [{ id: "0" }];
}

export default function CveRadarClosurePage() {
  return <CveRadarClosurePageClient />;
}
