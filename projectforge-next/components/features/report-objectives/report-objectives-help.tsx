"use client";

import { useTranslations } from "next-intl";
import { MarkdownText } from "@/components/shared/markdown-text";
import { SectionCard } from "@/components/shared/section-card";

/** Code spans and the XML example as code; the texts are markdown written in the bundle. */
const HELP_TEXT_CLASS =
  "text-xs text-muted-foreground [&_code]:rounded [&_code]:bg-muted [&_code]:px-1 [&_code]:font-mono [&_pre]:overflow-x-auto [&_pre]:rounded [&_pre]:bg-muted [&_pre]:p-2 [&_pre_code]:bg-transparent [&_pre_code]:p-0";

/**
 * The description of the report objectives format, taken from the user guide ("Reporting via Report
 * objectives"): an example, the selection rules, the expression syntax and the attributes.
 *
 * Read with `t.raw`: the texts carry XML and apostrophes, which ICU formatting would take for markup and
 * quotes.
 */
export function ReportObjectivesHelp() {
  const t = useTranslations();
  return (
    <SectionCard className="flex flex-col gap-2 bg-muted/40">
      <h3 className="text-sm font-semibold">
        {t("fibu.kost.reporting.help.title")}
      </h3>
      <MarkdownText
        text={t.raw("fibu.kost.reporting.help.intro") as string}
        className={HELP_TEXT_CLASS}
      />
      <MarkdownText
        text={t.raw("fibu.kost.reporting.help.example") as string}
        className={HELP_TEXT_CLASS}
      />
      <MarkdownText
        text={t.raw("fibu.kost.reporting.help.rules") as string}
        className={HELP_TEXT_CLASS}
      />
      <MarkdownText
        text={t.raw("fibu.kost.reporting.help.wildcards") as string}
        className={HELP_TEXT_CLASS}
      />
      <MarkdownText
        text={t.raw("fibu.kost.reporting.help.attributes") as string}
        className={HELP_TEXT_CLASS}
      />
      <MarkdownText
        text={t.raw("fibu.kost.reporting.help.drilldown") as string}
        className={HELP_TEXT_CLASS}
      />
    </SectionCard>
  );
}
