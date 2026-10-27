"use client";

import { useTranslations } from "next-intl";

/**
 * The mark of an unfinished feature beside a page title (see PageDef.experimental). Deliberately large
 * and solid red rather than one of the quiet badges: whoever opens the page must not take it for a
 * released feature and rely on it. [label] replaces "Experimental" where another word says it better, e.g.
 * "Demo" for a prototype without function.
 */
export function ExperimentalBadge({ label }: { label?: string }) {
  const t = useTranslations();
  return (
    <span className="shrink-0 rounded-md bg-red-600 px-3 py-1 text-base font-bold uppercase tracking-wider text-white">
      {label ?? t("experimental")}
    </span>
  );
}
