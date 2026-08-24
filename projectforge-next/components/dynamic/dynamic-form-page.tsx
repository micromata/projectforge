"use client";

import { notFound, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { fetchDynamic } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicPage } from "@/components/dynamic/dynamic-page";
import { isHandBuilt } from "@/lib/hand-built-categories";

/**
 * Renders any server-laid-out page from `/rs/{category}/{type}[?…]`, shared by the two-segment route
 * (`/next/timesheet/edit?startDate=…`, a new entry carrying its defaults in the query string) and the
 * three-segment route (`/next/address/edit/42`). Category, type and id are read from the url at runtime
 * because the static export cannot pre-render one file per entity (see use-route-params.ts).
 *
 * `category === undefined` means the url doesn't match the caller's route (the prerender pass, or the
 * instant a client-side navigation is still on the old url) — the query stays disabled and nothing renders.
 */
export function DynamicFormPage({
  category,
  type,
  id,
}: {
  category: string | undefined;
  type: string | undefined;
  id: string | undefined;
}) {
  // Everything the create endpoints read besides the id (TimesheetPagesRest: startDate/endDate/firstHour).
  // `id` is passed on its own and would otherwise fragment the query key, so it is stripped here.
  const searchParams = useSearchParams();
  const rest = new URLSearchParams(searchParams);
  rest.delete("id");
  const search = rest.toString();

  const handBuilt = category !== undefined && isHandBuilt(category);
  const queryKey = ["dynamic", category, type, id, search] as const;

  const { data: response, isLoading } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchDynamic(category!, type!, id, search, signal),
    enabled: category !== undefined && !handBuilt,
  });

  if (category === undefined) return null;
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
