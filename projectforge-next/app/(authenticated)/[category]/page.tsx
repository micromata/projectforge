import { DynamicListPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g.
// /next/vacation) to the SPA shell via NextSpaResourceResolver, where the client reads the real
// category from the URL at runtime (see page-client.tsx).
export function generateStaticParams() {
  return [{ category: "address" }];
}

export default function DynamicListPage() {
  return <DynamicListPageClient />;
}
