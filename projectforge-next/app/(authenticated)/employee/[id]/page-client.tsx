"use client";

import { notFound, useRouter } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { EMPLOYEE_PAGE } from "@/components/features/employee/employee.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function EmployeeEditPageClient() {
  const router = useRouter();
  const raw = useRouteParams<{ id: string }>("/employee/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage
        page={EMPLOYEE_PAGE}
        id={isNew ? null : id}
        // A new employee stays in the form: its status, weekly hours and annual leave can only be entered
        // once it is stored (as the legacy page did).
        outcome={
          isNew
            ? {
                afterSave: (savedId) =>
                  router.push(
                    savedId != null ? `/employee/${savedId}` : "/employee"
                  ),
              }
            : undefined
        }
      />
    </PageShell>
  );
}
