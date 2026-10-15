"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CircleArrowReload01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";
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
/**
 * The label and resolved target of the way back, so the prominent button ({@link LegacyPageLink}) and
 * the demoted gear-menu entry ({@link LegacyMenuItem}) name and point at it the same way.
 *
 * Returns `null` when there is no legacy counterpart, so a caller renders nothing.
 */
export function useLegacyLink(
  url?: string
): { label: string; href: string } | null {
  const t = useTranslations("goreact.menu");
  if (!url) return null;
  return { label: t("classics"), href: toAbsoluteUrl(resolveMenuUrl(url)) };
}

export function LegacyPageLink({
  url,
  className,
}: {
  /** Nothing is rendered without one: pages with no legacy counterpart (login, 2FA) have none. */
  url?: string;
  className?: string;
}) {
  const link = useLegacyLink(url);
  if (!link) return null;
  const { label, href } = link;

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
      <a href={href}>
        <HugeiconsIcon icon={CircleArrowReload01Icon} size={13} />
        {/* Icon only where the row is tight: `sr-only`, not `hidden`, keeps the accessible
              name on the link itself. */}
        <span className="sr-only md:not-sr-only">{label}</span>
      </a>
    </Button>
  );
}

/**
 * The way back as an entry of a list's gear menu, the demoted form of {@link LegacyPageLink}: once a
 * page is trusted enough (see `NextMigration.NextPage.legacyListInMenu`), the escape hatch no longer
 * needs to compete with the page's own actions and moves in here.
 *
 * Two lines like the maintenance entries beside it (label over an explanation), so it reads as one of
 * the menu rather than an odd link. A plain anchor all the same: the target is another app and needs a
 * full page load, which `resolveMenuUrl` decides.
 */
export function LegacyMenuItem({ url }: { url?: string }) {
  const t = useTranslations("goreact.menu");
  const link = useLegacyLink(url);
  if (!link) return null;

  return (
    <DropdownMenuItem asChild className="flex-col items-start gap-0.5">
      <a href={link.href}>
        <span className="flex items-center gap-1.5">
          <HugeiconsIcon icon={CircleArrowReload01Icon} size={13} />
          {link.label}
        </span>
        {/* `whitespace-normal`: the menu primitive keeps its items on one line otherwise. */}
        <span className="text-[11px] whitespace-normal text-muted-foreground">
          {t("classicsInfo")}
        </span>
      </a>
    </DropdownMenuItem>
  );
}
