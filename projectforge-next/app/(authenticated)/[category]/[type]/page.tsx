import { Suspense } from "react";
import { DynamicCreatePageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g.
// /next/timesheet/edit?startDate=…) to the SPA shell via NextSpaResourceResolver, where the client
// reads the real category, type and query string from the URL at runtime (see page-client.tsx).
export function generateStaticParams() {
  return [{ category: "timesheet", type: "edit" }];
}

// DynamicFormPage reads useSearchParams(), which requires a Suspense boundary under static export.
export default function DynamicCreatePage() {
  return (
    <Suspense>
      <DynamicCreatePageClient />
    </Suspense>
  );
}
