"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { MoreHorizontalIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { GuardedLink } from "@/components/shared/guarded-link";
import { useAuth } from "@/hooks/use-auth";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import type { CrossLinkDef } from "@/lib/page-def/types";

export interface EntityCrossLinksProps<Data> {
  links: readonly CrossLinkDef<Data>[];
  /** The stored entry every url names; nothing is rendered without one. */
  data: Data | undefined;
}

/**
 * What else can be done with the entry on screen — Wicket's top menu of an edit form
 * (`TaskEditPage.addTopMenuPanel`), as one menu beside the heading.
 *
 * A menu rather than a row of buttons: these are detours, not the action the page is for, and five of
 * them beside the breadcrumb would outweigh save. Wicket said the same by pushing the fifth into its
 * extended menu.
 *
 * An entry the declaration marks [CrossLinkDef.adminOnly] is left out for everyone else — the wizard
 * behind it answers 403 anyway, and an entry leading there would be an offer that isn't one.
 *
 * A target of another frontend is a full page load (`resolveMenuUrl`), a route of this app is a
 * client-side navigation that asks before it drops unsaved changes ([GuardedLink]) — the form is left
 * either way, but only the second can be taken back.
 */
export function EntityCrossLinks<Data>({
  links,
  data,
}: EntityCrossLinksProps<Data>) {
  const t = useTranslations();
  const { isAdmin } = useAuth();
  if (data === undefined) return null;
  const entries = links
    .filter((link) => !link.adminOnly || isAdmin)
    .map((link) => ({ labelKey: link.labelKey, href: link.href(data) }))
    .filter((entry): entry is { labelKey: string; href: string } =>
      Boolean(entry.href)
    );
  if (entries.length === 0) return null;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        {/* Outlined and spelled out, between the ghost icon it was (too quiet to be found at all) and
            the brand yellow of „Klassische Version" beside it, which has to stay the loudest thing on
            this row. The label carries the border; the icon alone said nothing about what is behind it.
            `sr-only` below `md`, where the row is tight — as the legacy link does it. */}
        <Button
          type="button"
          variant="outline"
          size="sm"
          aria-label={t("more")}
          className="gap-1.5"
        >
          <HugeiconsIcon icon={MoreHorizontalIcon} size={14} aria-hidden />
          <span className="sr-only md:not-sr-only">{t("more")}</span>
        </Button>
      </DropdownMenuTrigger>
      {/* As wide as its longest entry: the primitive's default is the *trigger's* width, which for a
          small button cuts „Neues Strukturunterelement" off (it also clips overflowing text rather
          than wrapping it). */}
      <DropdownMenuContent align="end" className="w-auto">
        {entries.map((entry) => {
          const target = resolveMenuUrl(entry.href);
          return (
            <DropdownMenuItem key={entry.labelKey} asChild>
              {target.kind === "internal" ? (
                <GuardedLink href={target.href}>
                  {t(entry.labelKey)}
                </GuardedLink>
              ) : (
                <a href={toAbsoluteUrl(target)}>{t(entry.labelKey)}</a>
              )}
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
