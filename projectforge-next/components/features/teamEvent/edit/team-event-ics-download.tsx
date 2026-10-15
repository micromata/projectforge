"use client";

import { useTranslations } from "next-intl";
import { ExportButton } from "@/components/shared/export-button";
import { useExportDownload } from "@/hooks/use-export-download";
import { downloadTeamEventIcs } from "@/lib/rs/team-event";

/**
 * Downloads the stored event as an ics file — the "Export ics" of the Wicket edit page, for handing one
 * event to another calendar app without subscribing the whole calendar.
 *
 * Offered for a stored event only: the file is built from the database, so a new event has nothing to
 * export yet.
 */
export function TeamEventIcsDownload({ id }: { id?: number | null }) {
  const t = useTranslations();
  const download = useExportDownload((eventId: number) =>
    downloadTeamEventIcs(eventId)
  );
  if (id == null) return null;
  const label = t("plugins.teamcal.exportIcsButton");
  return (
    <ExportButton
      tooltip={label}
      label={label}
      isPending={download.isPending}
      onClick={() => download.mutate(id)}
    />
  );
}
