import { DynamicListPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g.
// /next/contract) to the SPA shell via NextSpaResourceResolver, where the client reads the real
// category from the URL at runtime (see page-client.tsx). The placeholder must be no category: a hand
// built page of that name (as `address` once was here) would collide with its prerendered shell.
export function generateStaticParams() {
  return [{ category: "generic" }];
}

export default function DynamicListPage() {
  return <DynamicListPageClient />;
}
