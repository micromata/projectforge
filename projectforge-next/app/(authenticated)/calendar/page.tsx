import { Suspense } from "react";
import { CalendarPageClient } from "./page-client";

// The calendar reads `?gotoDate`/`?hash` (useGotoDate) after a save sends the user back here, which
// under `output: "export"` needs a Suspense boundary of its own. No `generateStaticParams`: this is a
// concrete route with no dynamic segment, so the static export emits it directly.
export default function CalendarRoutePage() {
  return (
    <Suspense>
      <CalendarPageClient />
    </Suspense>
  );
}
