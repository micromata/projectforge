"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import { fetchInitialList } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicPage } from "@/components/dynamic/dynamic-page";

export default function DynamicListPage() {
  const { category } = useParams<{ category: string }>();

  const queryKey = ["initialList", category] as const;
  const { data: response, isLoading } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchInitialList(category, signal),
  });

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
