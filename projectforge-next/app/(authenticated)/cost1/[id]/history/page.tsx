import { Cost1HistoryPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links
// (e.g. /next/cost1/8692225/history) to the SPA shell, where the client reads the real id from the
// URL at runtime (see ../page.tsx).
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function Cost1HistoryPage() {
  return <Cost1HistoryPageClient />;
}
