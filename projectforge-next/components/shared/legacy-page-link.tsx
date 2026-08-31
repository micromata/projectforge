"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CircleArrowReload01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { cn } from "@/lib/utils";

/**
 * The way back to the legacy version of the page at hand — the escape hatch of the migration: a
 * migrated page replaces its React counterpart everywhere (menu and all server side redirects, see
 * `NextMigration`), so without this the user is stuck here if the new page has a gap.
 *
 * Deliberately loud (brand yellow, with its label spelled out): as long as the new pages have gaps,
 * finding the way back has to be obvious, not a discovery. Yellow and not the primary teal, so it
 * doesn't compete with the action the page is actually for — "add", "save".
 *
 * The url comes from the server (`UILayout.legacyUrl` / `InitialListData.legacyEditPage`), because
 * it isn't derivable here: `books` is not `book`, and pages whose rows open something other than an
 * edit form point somewhere else entirely.
 *
 * Rendered as a plain anchor, not `next/link`: the target belongs to another app and needs a full
 * page load — which is exactly what `resolveMenuUrl` decides.
 */
export function LegacyPageLink({
  url,
  className,
}: {
  /** Nothing is rendered without one: pages with no legacy counterpart (login, 2FA) have none. */
  url?: string;
  className?: string;
}) {
  const t = useTranslations("goreact.menu");
  if (!url) return null;
  const label = t("classics");

  return (
    // No `HintTooltip`: its text would only repeat the label already spelled out beside the icon.
    // `aria-label` still carries the name where the row is tight and the label is `sr-only`.
    <Button
      asChild
      size="sm"
      aria-label={label}
      className={cn(
        "gap-1.5 bg-legacy text-legacy-foreground hover:bg-legacy-hover",
        className
      )}
    >
      <a href={toAbsoluteUrl(resolveMenuUrl(url))}>
        <HugeiconsIcon icon={CircleArrowReload01Icon} size={13} />
        {/* Icon only where the row is tight: `sr-only`, not `hidden`, keeps the accessible
              name on the link itself. */}
        <span className="sr-only md:not-sr-only">{label}</span>
      </a>
    </Button>
  );
}
