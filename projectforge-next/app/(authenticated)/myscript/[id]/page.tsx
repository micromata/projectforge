import { Suspense } from "react";
import { MyScriptPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links
// (e.g. /next/myscript/5) to the SPA shell, where the client reads the real id from the URL at runtime.
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function MyScriptPage() {
  // The boundary is required: the client reads `?from=` via `useSearchParams` under the static export.
  return (
    <Suspense>
      <MyScriptPageClient />
    </Suspense>
  );
}
