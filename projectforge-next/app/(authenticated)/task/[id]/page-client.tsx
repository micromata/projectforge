"use client";

import { Suspense } from "react";
import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { TASK_PAGE } from "@/components/features/task/task.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function TaskEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/task/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  // "new" adds a task — the same form, just without an id to load.
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      {/* The page reads `?returnTo=` to get the user back to the tree (useEditReturn), and
          useSearchParams may not be called unwrapped in a statically rendered route. */}
      <Suspense fallback={null}>
        <EntityEditPage page={TASK_PAGE} id={isNew ? null : id} />
      </Suspense>
    </PageShell>
  );
}
