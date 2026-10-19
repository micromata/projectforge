"use client";

import { useTranslations } from "next-intl";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { parseGroupNomenclatureConfig } from "@/lib/group-nomenclature";

/** The naming convention of groups in one line, for the value column of the list. */
export function GroupNomenclatureConfigSummary({
  json,
  highlight,
}: {
  json: string | null | undefined;
  highlight?: string;
}) {
  const t = useTranslations("group.nomenclature");
  const { config, invalid } = parseGroupNomenclatureConfig(json);
  if (invalid) {
    return <span className="text-warning">{t("invalidJson")}</span>;
  }
  const suffixes = [
    config.managerSuffix,
    config.teamSuffix,
    config.externalSuffix,
  ]
    .filter((suffix) => suffix?.trim())
    .map((suffix) => `-${suffix}`);
  const letterCase =
    config.letterCase === "LOWER"
      ? t("letterCase.lower")
      : config.letterCase === "UPPER"
        ? t("letterCase.upper")
        : null;
  const text = [
    config.pattern?.trim(),
    ...suffixes,
    letterCase && `(${letterCase})`,
  ]
    .filter(Boolean)
    .join(" ");
  if (!text) return null;
  return (
    <span className="line-clamp-2 font-mono">
      <HighlightedText text={text} query={highlight} />
    </span>
  );
}
