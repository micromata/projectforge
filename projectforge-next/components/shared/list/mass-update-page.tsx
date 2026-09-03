"use client";

import type { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { PageShell } from "@/components/shared/page-shell";
import { Spinner } from "@/components/shared/spinner";
import { fetchMultiSelectMeta } from "@/lib/rs/multi-select";
import { useReadAccessGuard } from "@/hooks/use-read-access-guard";
import { useSelectionStore } from "@/store/selection-store";
import type { MassUpdateDef } from "@/lib/page-def/types";
import { MassUpdateForm } from "./mass-update-form";

/**
 * The mass update of a list selection, rendered from what the backend answers about it.
 *
 * Nothing here knows the entity: the fields, their types, their options and the note above them come
 * from `{page}/meta`, and which page that is comes from the list's declaration (see
 * `PageDef.massUpdate`). The selection itself is not in the url — it lives in the HTTP session, put
 * there by the list before it routed here, which is also why a reload works and a deep link does not.
 */
export function MassUpdatePage({
  entity,
  massUpdate: def,
  listRoute,
  selectedEntries,
  actions,
}: {
  /** REST category of the list this came from, so leaving can drop its selection mode. */
  entity: string;
  massUpdate: MassUpdateDef;
  /** Where "back" leads — this app's list, not the legacy one the session remembers. */
  listRoute: string;
  /**
   * The collapsible list of the picked entries, as a function of how many those are.
   *
   * Built by the route rather than here, since it renders the list's columns (see
   * SelectedEntriesPanel); the count comes from the metadata this page fetches, which is also what
   * makes the panel refetch after the selection changed in another tab.
   */
  selectedEntries?: (count: number) => ReactNode;
  /** A page-specific action beside the title (see `MassUpdateForm.actions`), e.g. the SEPA export. */
  actions?: ReactNode;
}) {
  const t = useTranslations();
  const router = useRouter();
  const leaveSelection = useSelectionStore((state) => state.leave);
  const meta = useQuery({
    queryKey: ["massUpdateMeta", def.endpoint],
    queryFn: ({ signal }) => fetchMultiSelectMeta(def.endpoint, signal),
    // The selection is session state a second tab could have changed; nothing is gained by caching it.
    staleTime: 0,
  });

  // The entity of the list this came from, not the mass update endpoint: a user who may not see the
  // entries may not update them in bulk either, and `{page}/meta` performs no rights check of its own
  // (AbstractMultiSelectedPage.requestMeta).
  const readAccess = useReadAccessGuard(entity);

  if (readAccess.denied) {
    return null;
  }
  if (meta.isPending || readAccess.isPending) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <Spinner />
        </div>
      </PageShell>
    );
  }
  if (meta.isError || !meta.data) {
    return (
      <PageShell>
        <div className="p-4">
          <Alert variant="destructive">
            <AlertDescription>
              {meta.error instanceof Error
                ? meta.error.message
                : t("massUpdate.error.unspecifiedError")}
            </AlertDescription>
          </Alert>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <MassUpdateForm
        endpoint={def.endpoint}
        meta={meta.data}
        statisticsLine={def.statisticsLine}
        selectedEntries={selectedEntries?.(meta.data.selectedCount)}
        actions={actions}
        // The form's own leave already told the backend to forget the selection (`{page}/cancel`), so
        // the list's mode has to go with it — otherwise it would come back showing ticks that only
        // this app still believes in.
        onLeave={() => {
          leaveSelection(entity);
          router.push(listRoute);
        }}
      />
    </PageShell>
  );
}
