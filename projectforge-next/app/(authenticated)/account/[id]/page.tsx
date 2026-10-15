import { AccountEditPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g.
// /next/account/5) to the SPA shell, where the client reads the real id from the URL at runtime.
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function AccountEditPage() {
  return <AccountEditPageClient />;
}
