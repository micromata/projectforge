import { Suspense } from "react";
import { AddressPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links (e.g. /next/address/5) to
// the SPA shell, where the client reads the real id from the URL at runtime. The view reads `?returnTo=` and the
// form `?importIndex=`, which needs a Suspense boundary under static export.
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function AddressPage() {
  return (
    <Suspense>
      <AddressPageClient />
    </Suspense>
  );
}
