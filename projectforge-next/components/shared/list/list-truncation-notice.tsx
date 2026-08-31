"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { formatNumber } from "@/lib/format";
import { useFormatContext } from "@/hooks/use-format";

interface ListTruncationNoticeProps {
  /**
   * The row cap that was hit — the count the backend returned once the result was capped, so the same
   * number the message names ("Maximum number 50,000 …"). Formatted with the user's grouping separator.
   */
  count: number;
}

/**
 * Warns that the list is cut off: more rows match the filter than the backend's row limit lets through
 * (`ResultSet.resultSetTruncated`), so what the table shows is incomplete and the user should narrow.
 *
 * In the filter row above the pills — right where the narrowing is done — and in the destructive colours
 * a deleted row is marked with (see [EntityDeletedBanner]). A typed warning built here rather than the
 * server's red-span markdown: the colour is a css token and the text is the user's, from `next-intl`.
 */
export function ListTruncationNotice({ count }: ListTruncationNoticeProps) {
  const t = useTranslations("search");
  const ctx = useFormatContext();
  return (
    <div
      // Not an alert role: it is the state of the result on screen, not an event, so a screen reader
      // gets it in reading order beside the filter rather than as an interruption.
      className="flex items-center gap-2 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-1.5 text-xs font-medium text-destructive"
    >
      <HugeiconsIcon icon={AlertCircleIcon} size={14} aria-hidden />
      <span>{t("maxRowsExceeded", { arg0: formatNumber(count, ctx) })}</span>
    </div>
  );
}
