"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { ExportButton } from "@/components/shared/export-button";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { useExportDownload } from "@/hooks/use-export-download";
import { useFormatContext } from "@/hooks/use-format";
import { formatMonthName } from "@/lib/format";
import {
  BIRTHDAY_BUTLER_QUERY_KEY,
  downloadBirthdayList,
  fetchBirthdayButlerInit,
} from "@/lib/rs/birthday-butler";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

/**
 * The birthday butler (`/next/birthdayButler`, formerly the dynamic React page `react/birthdayButler/dynamic`):
 * a month, and the Word document with the birthdays of the organization's staff in it.
 *
 * The current month is preselected. Without a configured organization there is nothing to create, which the
 * page says instead of offering a download that can only fail. Why a list can't be created otherwise (nobody
 * with a birthday in the month, …) comes from the backend and is shown as toast.
 *
 * ORGA team and HR group only; the endpoints check it and answer everybody else with 403.
 */
export function BirthdayButlerPage() {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const query = useQuery({
    queryKey: BIRTHDAY_BUTLER_QUERY_KEY,
    queryFn: ({ signal }) => fetchBirthdayButlerInit(signal),
  });
  // Null until the user picks one: until then the month the backend preselected.
  const [picked, setPicked] = useState<number | null>(null);
  const month = picked ?? query.data?.month;
  const download = useExportDownload((m: number) => downloadBirthdayList(m));
  const label = t("calendar.month._");

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.orga._")}
        title={t("menu.birthdayButler")}
      />
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        {query.isError && (
          <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
        )}
        {query.data && !query.data.configured && (
          <FormAlert tone="warning">
            {t("birthdayButler.organization.notSet")}
          </FormAlert>
        )}
        {query.data && (
          <div className="flex flex-wrap items-center gap-3">
            <Label htmlFor="birthdayButlerMonth">{label}</Label>
            <Select
              value={month ? String(month) : undefined}
              onValueChange={(value) => setPicked(Number(value))}
            >
              <SelectTrigger
                id="birthdayButlerMonth"
                className="w-48"
                aria-label={label}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {MONTHS.map((m) => (
                  <SelectItem key={m} value={String(m)}>
                    {formatMonthName(m, formatContext)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <ExportButton
              variant="default"
              label={t("download._")}
              isPending={download.isPending}
              disabled={!month || !query.data.configured}
              onClick={() => month && download.mutate(month)}
            />
          </div>
        )}
      </div>
    </PageShell>
  );
}
