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
 * A target of another frontend is a full page load (`resolveMenuUrl`), a route of this app is a
 * client-side navigation that asks before it drops unsaved changes ([GuardedLink]) — the form is left
 * either way, but only the second can be taken back.
 */
export function EntityCrossLinks<Data>({
  links,
  data,
}: EntityCrossLinksProps<Data>) {
  const t = useTranslations();
  if (data === undefined) return null;
  const entries = links
    .map((link) => ({ labelKey: link.labelKey, href: link.href(data) }))
    .filter((entry): entry is { labelKey: string; href: string } =>
      Boolean(entry.href)
    );
  if (entries.length === 0) return null;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          aria-label={t("more")}
          className="h-7 shrink-0 px-1.5 text-muted-foreground hover:text-foreground"
        >
          <HugeiconsIcon icon={MoreHorizontalIcon} size={16} aria-hidden />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
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
