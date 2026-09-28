"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { HighlightedText } from "@/components/shared/highlighted-text";

/**
 * The translated label (Parameter column) or description (Description column) of a configuration
 * parameter — the runtime i18n keys `i18nKey` / `descriptionI18nKey` the DTO carries, resolved through
 * [leafKeyOf] because a param key is both a text and the parent of its `.description` child (JSON cannot
 * hold both, so the generator exports the text as `<key>._`).
 *
 * The active search term is highlighted in the shown text (the search matches these very labels on the
 * backend, see ConfigurationEntityRest.preProcessMagicFilter), so the match is marked as in every other
 * list — `highlight` comes from the table meta at the call site (see configuration.page.tsx).
 */
export function ParamLabelCell({
  i18nKey,
  highlight,
}: {
  i18nKey: string | null | undefined;
  highlight?: string;
}) {
  const t = useTranslations();
  if (!i18nKey || !t.has(leafKeyOf(i18nKey, t.has))) return null;
  return (
    <span>
      <HighlightedText text={t(leafKeyOf(i18nKey, t.has))} query={highlight} />
    </span>
  );
}
