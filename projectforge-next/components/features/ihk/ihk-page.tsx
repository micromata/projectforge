"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ExportButton } from "@/components/shared/export-button";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useFormatContext } from "@/hooks/use-format";
import {
  downloadIhkReport,
  fetchIhkInit,
  fetchIhkMissingDescriptions,
} from "@/lib/rs/ihk";
import { toast } from "@/lib/toast";
import { todayOf } from "@/lib/user-zone";
import { IhkSettingsPanel } from "./ihk-settings-panel";
import { IhkWeekPicker, mondayOf } from "./ihk-week-picker";
import { MissingDescriptionList } from "./missing-description-list";

/**
 * The IHK training report of the IHK plugin (`/next/ihk`): the apprentice picks a week and downloads the weekly
 * report (Ausbildungsnachweis) built from their own time sheets, the successor of the plugin's Wicket page.
 *
 * Time sheets of the week without description are listed beforehand, each with a link to its edit page; the
 * download is still possible ("download anyway"). The apprentice's settings (training start, year, team) are a
 * user pref, entered on this page before the first report.
 */
export function IhkPage() {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [monday, setMonday] = useState(() => mondayOf(todayOf(ctx)));

  const init = useQuery({
    queryKey: ["ihk", "init"],
    queryFn: ({ signal }) => fetchIhkInit(signal),
  });
  const ready = !!init.data?.settings;
  const missing = useQuery({
    queryKey: ["ihk", "missingDescriptions", monday],
    queryFn: ({ signal }) => fetchIhkMissingDescriptions(monday, signal),
    enabled: ready,
  });
  const download = useMutation({
    mutationFn: () => downloadIhkReport(monday),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  const missingItems = missing.data ?? [];

  return (
    <PageShell>
      <PageTitleRow
        category={t("plugins.ihk.menu")}
        title={t("plugins.ihk.title")}
      >
        <LogViewerLink url={init.data?.logViewerUrl} />
      </PageTitleRow>
      <div className="flex max-w-4xl flex-col gap-4 px-4 pb-6">
        {init.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {init.isError && (
          <p className="text-sm text-destructive">
            {init.error instanceof Error
              ? init.error.message
              : String(init.error)}
          </p>
        )}
        <p className="text-sm text-muted-foreground">
          {t("plugins.ihk.intro")}
        </p>
        {init.data && <IhkSettingsPanel init={init.data} />}
        {ready && (
          <>
            <div className="flex flex-wrap items-end gap-4">
              <IhkWeekPicker monday={monday} onChange={setMonday} />
              <ExportButton
                variant="default"
                label={
                  missingItems.length > 0
                    ? t("plugins.ihk.downloadAnyway")
                    : t("plugins.ihk.download")
                }
                isPending={download.isPending}
                disabled={missing.isPending}
                onClick={() => download.mutate()}
              />
            </div>
            {missingItems.length > 0 && (
              <MissingDescriptionList items={missingItems} />
            )}
            <p className="text-sm text-muted-foreground">
              {t("plugins.ihk.descriptionFormat")}
            </p>
          </>
        )}
      </div>
    </PageShell>
  );
}
