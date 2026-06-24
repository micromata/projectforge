"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { fetchInitialList } from "@/lib/rs/client";
import { PageShell } from "@/components/shared/page-shell";
import { DynamicLayoutProvider } from "@/components/dynamic/dynamic-context";
import { DynamicRenderer } from "@/components/dynamic/dynamic-renderer";
import { DynamicActionGroup } from "@/components/dynamic/dynamic-action-group";

export default function DynamicListPage() {
  const { category } = useParams<{ category: string }>();

  const { data: response, isLoading } = useQuery({
    queryKey: ["list", category],
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

  return (
    <PageShell>
      <DynamicLayoutProvider
        ui={response.ui}
        initialData={response.data ?? {}}
        initialVariables={response.variables}
        initialValidationErrors={response.validationErrors}
      >
        <div className="flex flex-1 flex-col overflow-hidden">
          {response.ui.title && (
            <h1 className="px-6 pt-4 pb-2 text-xl font-semibold">
              {response.ui.title}
            </h1>
          )}
          <div className="flex-1 overflow-auto px-6 pb-6">
            <DynamicRenderer content={response.ui.layout} />
          </div>
          <DynamicActionGroup />
        </div>
      </DynamicLayoutProvider>
    </PageShell>
  );
}
