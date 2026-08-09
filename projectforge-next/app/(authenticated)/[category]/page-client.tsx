"use client";

import { notFound } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useRouteParams } from "@/hooks/use-route-params";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import { fetchInitialList } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicPage } from "@/components/dynamic/dynamic-page";
import { isHandBuilt } from "@/lib/hand-built-categories";

/**
 * Renders any server-laid-out list page, e.g. `/next/vacation`.
 *
 * The category is read from the url at runtime: the static export cannot
 * pre-render one file per entity, so Spring forwards the deep link to the SPA
 * shell (see use-route-params.ts for why `useParams()` cannot be used).
 */
export function DynamicListPageClient() {
  // Undefined while the url doesn't match this route, which disables the query below.
  const category = useRouteParams<{ category: string }>(
    "/[category]"
  )?.category;
  // A hand built list owns its own concrete route, which Next resolves first;
  // arriving here with one of those categories means the url was wrong.
  const handBuilt = category !== undefined && isHandBuilt(category);

  const queryKey = ["initialList", category] as const;
  const { data: response, isLoading } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchInitialList(category!, signal),
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

  const resultInfo = (response.data as Record<string, unknown>)?.resultInfo as
    | string
    | undefined;

  return (
    <PageShell>
      <DynamicPage response={response} category={category} queryKey={queryKey}>
        {resultInfo && (
          <div className="mt-4 rounded-md bg-sky-50 px-4 py-3 text-sm dark:bg-sky-950">
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              rehypePlugins={[rehypeRaw]}
              components={{
                ul: ({ children }) => (
                  <ul className="list-disc pl-4 space-y-1">{children}</ul>
                ),
                li: ({ children }) => (
                  <li className="text-muted-foreground">{children}</li>
                ),
              }}
            >
              {resultInfo}
            </ReactMarkdown>
          </div>
        )}
      </DynamicPage>
    </PageShell>
  );
}
