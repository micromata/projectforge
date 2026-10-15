"use client";

import { useTranslations } from "next-intl";

/**
 * The last row of a picker's list while its next page is on its way (see useEntityLookup).
 *
 * Shared by both pickers — EntityAutocomplete and DynamicSelect — so the wait looks the same in a
 * hand-built form as in a server-laid-out one. No "load more" button: the page is asked for by
 * scrolling, and this row is what says so is happening.
 */
export function LookupLoadingRow() {
  const t = useTranslations();

  return (
    <div className="px-2 py-1.5 text-center text-xs text-muted-foreground">
      {t("loading")}
    </div>
  );
}
