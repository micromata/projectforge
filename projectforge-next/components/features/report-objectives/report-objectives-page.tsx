"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { MarkdownText } from "@/components/shared/markdown-text";
import { MonthInput } from "@/components/shared/month-input";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SectionCard } from "@/components/shared/section-card";
import { Spinner } from "@/components/shared/spinner";
import {
  clearReportObjectives,
  createReport,
  fetchReportObjectives,
  pasteReportObjectives,
  REPORT_OBJECTIVES_QUERY_KEY,
  selectReport,
  uploadReportObjectives,
} from "@/lib/rs/report-objectives";
import { toast } from "@/lib/toast";
import { ReportObjectivesHelp } from "./report-objectives-help";
import { ReportTable } from "./report-table";
import type { ReportObjectivesData } from "./types";

/**
 * The report objectives page (`/next/reportObjectives`, formerly Wicket's `wa/reportObjectives`): upload a
 * ReportObjective XML (or paste it), choose a month range and drill down through the BWA of the report and its children.
 * The help texts of the user guide are shown on the page itself.
 *
 * PF_Finance and PF_Controlling only; the endpoint checks it and answers everybody else with 403.
 */
export function ReportObjectivesPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: REPORT_OBJECTIVES_QUERY_KEY,
    queryFn: ({ signal }) => fetchReportObjectives(signal),
  });
  // Every call answers the whole new state, which simply replaces the cached one.
  const mutation = useMutation({
    mutationFn: (action: () => Promise<ReportObjectivesData>) => action(),
    onSuccess: (data) =>
      queryClient.setQueryData(REPORT_OBJECTIVES_QUERY_KEY, data),
    onError: (error) => toast.error(error.message),
  });
  const run = (action: () => Promise<ReportObjectivesData>) =>
    mutation.mutate(action);
  const data = query.data;

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.reporting")}
        title={t("menu.fibu.reporting.reportObjectives")}
      />
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        <MarkdownText
          text={t("fibu.kost.reporting.intro")}
          className="text-sm text-muted-foreground"
        />
        {query.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {query.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {data && !data.report && (
          <UploadCard
            busy={mutation.isPending}
            onUpload={(file) => run(() => uploadReportObjectives(file))}
            onPaste={(xml) => run(() => pasteReportObjectives(xml))}
          />
        )}
        {data?.report && (
          <PeriodCard
            // Remounted with the stored period, so the fields show what the backend evaluated.
            key={`${data.fileName}|${data.fromMonth}|${data.toMonth}`}
            data={data}
            busy={mutation.isPending}
            onCreate={(from, to) => run(() => createReport(from, to))}
            onClear={() => run(clearReportObjectives)}
          />
        )}
        {data?.report && !data.loaded && (
          <p className="text-sm text-muted-foreground">
            {t("fibu.kost.reporting.notLoaded")}
          </p>
        )}
        {data?.report && data.loaded && (
          <ReportTable
            report={data.report}
            canShowRecords={data.canShowRecords}
            busy={mutation.isPending}
            onSelect={(reportId) => run(() => selectReport(reportId))}
          />
        )}
        {data && <ReportObjectivesHelp />}
      </div>
    </PageShell>
  );
}

function UploadCard({
  busy,
  onUpload,
  onPaste,
}: {
  busy: boolean;
  onUpload: (file: File) => void;
  onPaste: (xml: string) => void;
}) {
  const t = useTranslations();
  const inputRef = useRef<HTMLInputElement>(null);
  // Kept on a refusal (the card stays mounted), so a typo can be fixed instead of pasted again.
  const [xml, setXml] = useState("");
  return (
    <SectionCard className="flex flex-col gap-2">
      <h3 className="text-sm font-semibold">
        {t("fibu.kost.reporting.upload")}
      </h3>
      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-1.5"
          disabled={busy}
          onClick={() => inputRef.current?.click()}
        >
          {busy ? (
            <Spinner className="h-3 w-3 border-2" />
          ) : (
            <HugeiconsIcon icon={CloudUploadIcon} size={14} />
          )}
          {t("file.upload.choose")}
        </Button>
        <span className="text-xs text-muted-foreground">*.xml</span>
      </div>
      <input
        ref={inputRef}
        type="file"
        accept=".xml,application/xml,text/xml"
        // Out of the tab order and the accessibility tree: the button above is the control that opens it.
        className="sr-only"
        tabIndex={-1}
        aria-hidden
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) onUpload(file);
          // Cleared so choosing the same file again fires change again.
          e.target.value = "";
        }}
      />
      <form
        className="flex flex-col gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          if (xml.trim()) onPaste(xml);
        }}
      >
        <Label htmlFor="report-objectives-xml">
          {t("fibu.kost.reporting.pasteLabel")}
        </Label>
        <Textarea
          id="report-objectives-xml"
          value={xml}
          onChange={(e) => setXml(e.target.value)}
          placeholder={'<ReportObjective title="…" id="…">'}
          spellCheck={false}
          className="max-h-96 min-h-24 resize-y font-mono"
        />
        <div>
          <Button type="submit" size="sm" disabled={busy || !xml.trim()}>
            {busy && <Spinner className="h-3 w-3 border-2" />}
            {t("fibu.kost.reporting.pasteApply")}
          </Button>
        </div>
      </form>
      <p className="text-xs text-muted-foreground">
        {t("fibu.kost.reporting.help.storage")}
      </p>
    </SectionCard>
  );
}

function PeriodCard({
  data,
  busy,
  onCreate,
  onClear,
}: {
  data: ReportObjectivesData;
  busy: boolean;
  onCreate: (fromMonth: string, toMonth: string | null) => void;
  onClear: () => void;
}) {
  const t = useTranslations();
  // MonthInput holds ISO dates (first or last day of the month), the backend takes `yyyy-MM`.
  const [from, setFrom] = useState<string | null>(`${data.fromMonth}-01`);
  const [to, setTo] = useState<string | null>(`${data.toMonth}-01`);
  const invalid = !from || (!!to && from.slice(0, 7) > to.slice(0, 7));
  return (
    <SectionCard className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
        <span>
          <span className="text-muted-foreground">
            {t("fibu.kost.reporting.file")}:{" "}
          </span>
          <span className="font-medium">
            {data.fileName ?? t("fibu.kost.reporting.pasted")}
          </span>
        </span>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="gap-1.5 text-destructive"
          disabled={busy}
          onClick={onClear}
        >
          <HugeiconsIcon icon={Delete02Icon} size={14} />
          {t("fibu.kost.reporting.clearStorage")}
        </Button>
      </div>
      <form
        className="flex flex-wrap items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          if (!invalid && from)
            onCreate(from.slice(0, 7), to?.slice(0, 7) ?? null);
        }}
      >
        <Label className="mr-1">{t("timePeriod")}</Label>
        <MonthInput
          value={from}
          onChange={setFrom}
          bound="begin"
          defaultMonth={to}
          aria-label={`${t("timePeriod")}: ${t("date.from")}`}
        />
        <span aria-hidden>–</span>
        <MonthInput
          value={to}
          onChange={setTo}
          bound="end"
          defaultMonth={from}
          aria-label={`${t("timePeriod")}: ${t("date.until")}`}
        />
        <Button type="submit" size="sm" disabled={busy || invalid}>
          {busy && <Spinner className="h-3 w-3 border-2" />}
          {t("fibu.kost.reporting.createReport")}
        </Button>
        {invalid && from && (
          <span className="text-xs text-destructive">
            {t("fibu.buchungssatz.error.invalidTimeperiod")}
          </span>
        )}
      </form>
      <p className="text-xs text-muted-foreground">
        {t("fibu.kost.reporting.help.storage")}
      </p>
    </SectionCard>
  );
}
