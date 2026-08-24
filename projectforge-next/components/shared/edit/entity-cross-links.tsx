"use client";

import type { ComponentProps, ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { MoreHorizontalIcon } from "@hugeicons/core-free-icons";
import { Button, buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { GuardedLink } from "@/components/shared/guarded-link";
import { useAuth } from "@/hooks/use-auth";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { cn } from "@/lib/utils";
import type { CrossLinkDef } from "@/lib/page-def/types";

export interface EntityCrossLinksProps<Data> {
  links: readonly CrossLinkDef<Data>[];
  /** The stored entry every url names; nothing is rendered without one. */
  data: Data | undefined;
}

/**
 * Where one cross link leads, as a link and nothing else — the same target whether it is rendered as a
 * button of its own or as an entry of the menu.
 *
 * A target of another frontend is a full page load (`resolveMenuUrl`), a route of this app is a
 * client-side navigation that asks before it drops unsaved changes ([GuardedLink]) — the form is left
 * either way, but only the second can be taken back.
 */
function CrossLinkAnchor({
  href,
  children,
  ...rest
}: { href: string; children: ReactNode } & Omit<
  ComponentProps<"a">,
  "href" | "children"
>) {
  const target = resolveMenuUrl(href);
  // The rest of the props passed on, not just the class: as the child of a menu item this is handed the
  // ref and the keyboard handlers the menu needs to focus and drive it (Radix `asChild`).
  return target.kind === "internal" ? (
    <GuardedLink href={target.href} {...rest}>
      {children}
    </GuardedLink>
  ) : (
    <a href={toAbsoluteUrl(target)} {...rest}>
      {children}
    </a>
  );
}

/**
 * What else can be done with the entry on screen — Wicket's top menu of an edit form
 * (`TaskEditPage.addTopMenuPanel`), beside the heading.
 *
 * A menu rather than a row of buttons for most of them: these are detours, not the action the page is
 * for, and five of them beside the breadcrumb would outweigh save. Wicket said the same by pushing the
 * fifth into its extended menu. The one or two a declaration marks [CrossLinkDef.prominent] are the
 * exception — a click ahead of the rest, as the invoice's export button is.
 *
 * A prominent entry stays *in* the menu below `md`, where the row is too tight for a spelled-out label,
 * and the button takes over from there: the same target twice in the markup, never both visible at once.
 *
 * An entry the declaration marks [CrossLinkDef.adminOnly] is left out for everyone else — the wizard
 * behind it answers 403 anyway, and an entry leading there would be an offer that isn't one.
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
    .map((link) => ({
      labelKey: link.labelKey,
      href: link.href(data),
      prominent: link.prominent,
    }))
    .filter(
      (
        entry
      ): entry is {
        labelKey: string;
        href: string;
        prominent: boolean | undefined;
      } => Boolean(entry.href)
    );
  if (entries.length === 0) return null;
  // Every entry promoted to a button: then the menu has nothing left to show from `md` up and would be
  // a trigger onto an empty list.
  const menuOnlyAboveMd = entries.every((entry) => entry.prominent);

  return (
    <>
      {entries
        .filter((entry) => entry.prominent)
        .map((entry) => (
          <CrossLinkAnchor
            key={entry.labelKey}
            href={entry.href}
            className={cn(
              buttonVariants({ variant: "outline", size: "sm" }),
              "hidden shrink-0 md:inline-flex"
            )}
          >
            {t(entry.labelKey)}
          </CrossLinkAnchor>
        ))}
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
            className={cn("gap-1.5", menuOnlyAboveMd && "md:hidden")}
          >
            <HugeiconsIcon icon={MoreHorizontalIcon} size={14} aria-hidden />
            <span className="sr-only md:not-sr-only">{t("more")}</span>
          </Button>
        </DropdownMenuTrigger>
        {/* As wide as its longest entry: the primitive's default is the *trigger's* width, which for a
            small button cuts „Neues Strukturunterelement" off (it also clips overflowing text rather
            than wrapping it). */}
        <DropdownMenuContent align="end" className="w-auto">
          {entries.map((entry) => (
            <DropdownMenuItem
              key={entry.labelKey}
              asChild
              className={cn(entry.prominent && "md:hidden")}
            >
              <CrossLinkAnchor href={entry.href}>
                {t(entry.labelKey)}
              </CrossLinkAnchor>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
}
