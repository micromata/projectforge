import { Suspense } from "react";
import { SearchPage } from "@/components/features/search/search-page";

/**
 * The global search route (`/next/search`), the successor of Wicket's `wa/search`.
 *
 * A concrete route, not a category of the generic list page: the search spans many entities and is
 * served by `SearchRest`, not by a list layout. The feature reads `?q=`/`?areas=` through
 * `useSearchParams`, which needs a `<Suspense>` boundary under `output: "export"`; there is no dynamic
 * segment, so the static export emits this route itself (no `generateStaticParams`).
 */
export default function Page() {
  return (
    <Suspense>
      <SearchPage />
    </Suspense>
  );
}
