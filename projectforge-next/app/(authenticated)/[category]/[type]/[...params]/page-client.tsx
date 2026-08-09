"use client";

import { notFound } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useRouteParams } from "@/hooks/use-route-params";
import { fetchDynamic } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicPage } from "@/components/dynamic/dynamic-page";
import { isHandBuilt } from "@/lib/hand-built-categories";

/**
 * Renders any server-laid-out edit page, e.g. `/next/address/edit/42`.
 *
 * Category, type and id are read from the url at runtime, because the static export cannot
 * pre-render one file per entity (see page.tsx and use-route-params.ts).
 */
export function DynamicFormPageClient() {
  // Undefined while the url doesn't match this route, which disables the query below.
  const route = useRouteParams<{
    category: string;
    type: string;
    params: string[];
  }>("/[category]/[type]/[...params]");

  const { category, type, params } = route ?? {};
  const id = params?.[0];
  const handBuilt = category !== undefined && isHandBuilt(category);
  const queryKey = ["dynamic", category, type, id] as const;

  const { data: response, isLoading } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchDynamic(category!, type!, id, signal),
    enabled: route !== null && !handBuilt,
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
