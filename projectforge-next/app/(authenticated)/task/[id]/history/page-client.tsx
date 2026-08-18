"use client";

import { Suspense } from "react";
import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityHistoryPage } from "@/components/shared/edit/entity-history-page";
import { TASK_PAGE } from "@/components/features/task/task.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function TaskHistoryPageClient() {
  const raw = useRouteParams<{ id: string }>("/task/[id]/history")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  // A task that isn't saved yet ("new") has no history to show.
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      {/* `?returnTo=` again, so the way back from the history is the tree as well (useEditReturn). */}
      <Suspense fallback={null}>
        <EntityHistoryPage page={TASK_PAGE} id={id} />
      </Suspense>
    </PageShell>
  );
}
