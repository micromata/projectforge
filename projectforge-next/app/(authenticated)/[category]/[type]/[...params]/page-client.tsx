"use client";

import { notFound, useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { fetchDynamic } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicPage } from "@/components/dynamic/dynamic-page";

/**
 * Categories that have a hand built page in this app. They own concrete routes (`books/[id]`),
 * which Next resolves before this catch-all - so reaching this component with one of them means
 * the url was wrong, not that the generic renderer should take over.
 *
 * Keep in sync with NextMigration.MIGRATED in projectforge-business: a category is either hand
 * built (listed here) or server-laid-out (rendered here), never both.
 */
const HAND_BUILT_CATEGORIES = ["book", "books"];

/**
 * Renders any server-laid-out edit page, e.g. `/next/address/edit/42`.
 *
 * Category, type and id are read from the url at runtime, because the static export cannot
 * pre-render one file per entity (see page.tsx).
 */
export function DynamicFormPageClient() {
  const { category, type, params } = useParams<{
    category: string;
    type: string;
    params: string[];
  }>();

  const id = params?.[0];
  const handBuilt = HAND_BUILT_CATEGORIES.includes(category);
  const queryKey = ["dynamic", category, type, id] as const;

  const { data: response, isLoading } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchDynamic(category, type, id, signal),
    enabled: !handBuilt,
  });

  if (handBuilt) notFound();

  if (isLoading) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
        </div>
      </PageShell>
    );
  }

  if (!response?.ui) {
    return (
      <PageShell>
        <div className="p-6 text-muted-foreground">Page not found.</div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <DynamicPage
        response={response}
        category={category}
        queryKey={queryKey}
      />
    </PageShell>
  );
}
