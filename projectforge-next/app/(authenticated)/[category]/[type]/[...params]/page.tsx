import { Suspense } from "react";
import { DynamicFormPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g.
// /next/contract/edit/42) to the SPA shell via NextSpaResourceResolver, where the client reads the
// real category, type and id from the URL at runtime (see page-client.tsx). The placeholder must be no
// category, see ../../page.tsx.
export function generateStaticParams() {
  return [{ category: "generic", type: "edit", params: ["new"] }];
}

// DynamicFormPage reads useSearchParams(), which requires a Suspense boundary under static export.
export default function DynamicFormPage() {
  return (
    <Suspense>
      <DynamicFormPageClient />
    </Suspense>
  );
}
