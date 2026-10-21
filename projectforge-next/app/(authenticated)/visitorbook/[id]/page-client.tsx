"use client";

import { notFound, useRouter } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { VISITORBOOK_PAGE } from "@/components/features/visitorbook/visitorbook.page";

export function VisitorbookEditPageClient() {
  const router = useRouter();
  const raw = useRouteParams<{ id: string }>("/visitorbook/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage
        page={VISITORBOOK_PAGE}
        id={isNew ? null : id}
        // A new visitor stays in the form: the visits can only be entered once the visitor is stored (as the
        // legacy page did).
        outcome={
          isNew
            ? {
                afterSave: (savedId) =>
                  router.push(
                    savedId != null ? `/visitorbook/${savedId}` : "/visitorbook"
                  ),
              }
            : undefined
        }
      />
    </PageShell>
  );
}
