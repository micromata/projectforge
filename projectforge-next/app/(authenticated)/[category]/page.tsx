"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { fetchInitialList, fetchListData } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicLayoutProvider } from "@/components/dynamic/dynamic-context";
import { DynamicRenderer } from "@/components/dynamic/dynamic-renderer";
import { DynamicActionGroup } from "@/components/dynamic/dynamic-action-group";

export default function DynamicListPage() {
  const { category } = useParams<{ category: string }>();

  const { data: initial, isLoading: isLoadingInit } = useQuery({
    queryKey: ["initialList", category],
    queryFn: ({ signal }) => fetchInitialList(category, signal),
  });

  const filter = (initial as unknown as Record<string, unknown>)?.filter as
    | Record<string, unknown>
    | undefined;

  const { data: listResponse, isLoading: isLoadingList } = useQuery({
    queryKey: ["listData", category, filter],
    queryFn: ({ signal }) =>
      fetchListData(category, filter as never, signal),
    enabled: !!initial && !!filter,
  });

  const isLoading = isLoadingInit || isLoadingList;

  if (isLoading && !initial) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
        </div>
      </PageShell>
    );
  }

  if (!initial?.ui) {
    return (
      <PageShell>
        <div className="p-6 text-muted-foreground">Page not found.</div>
      </PageShell>
    );
  }

  const mergedData = { ...(initial.data ?? {}), ...(listResponse?.data ?? {}) };
  const resultInfo = (listResponse?.data as Record<string, unknown>)
    ?.resultInfo as string | undefined;

  return (
    <PageShell>
      <DynamicLayoutProvider
        ui={initial.ui}
        initialData={mergedData}
        initialVariables={initial.variables}
        initialValidationErrors={initial.validationErrors}
      >
        <div className="flex flex-1 flex-col overflow-hidden">
          {initial.ui.title && (
            <h1 className="px-6 pt-4 pb-2 text-xl font-semibold">
              {initial.ui.title}
            </h1>
          )}
          <div className="flex-1 overflow-auto px-6 pb-6">
            <DynamicRenderer content={initial.ui.layout} />
            {resultInfo && (
              <div
                className="mt-4 text-sm text-muted-foreground prose prose-sm max-w-none"
                dangerouslySetInnerHTML={{ __html: resultInfo }}
              />
            )}
          </div>
          <DynamicActionGroup />
        </div>
      </DynamicLayoutProvider>
    </PageShell>
  );
}
