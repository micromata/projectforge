"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { PageShell } from "@/components/shared/page-shell";
import { Spinner } from "@/components/shared/spinner";
import { fetchMultiSelectMeta } from "@/lib/rs/multi-select";
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
  massUpdate: def,
  listRoute,
}: {
  massUpdate: MassUpdateDef;
  /** Where "back" leads — this app's list, not the legacy one the session remembers. */
  listRoute: string;
}) {
  const t = useTranslations();
  const router = useRouter();
  const meta = useQuery({
    queryKey: ["massUpdateMeta", def.endpoint],
    queryFn: ({ signal }) => fetchMultiSelectMeta(def.endpoint, signal),
    // The selection is session state a second tab could have changed; nothing is gained by caching it.
    staleTime: 0,
  });

  if (meta.isPending) {
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
        onLeave={() => router.push(listRoute)}
      />
    </PageShell>
  );
}
