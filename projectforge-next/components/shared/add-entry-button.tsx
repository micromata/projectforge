"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useAddEntryShortcut } from "@/hooks/use-add-entry-shortcut";

export interface AddEntryButtonProps {
  /** Route of the add page, e.g. `/task/new`. */
  href: string;
  /**
   * Whether [href] leaves this app — the add page of an entity whose list is migrated but whose form
   * is not (see useEditTargets). Rendered as a plain anchor then, because client-side routing would
   * not find a Wicket page.
   */
  isLegacy?: boolean;
}

/**
 * The "add" button every page that creates entities carries, together with its `N` shortcut.
 *
 * Its own component rather than a part of [ListToolbar], because the structure tree is not a list page
 * and needs exactly this: the same icon, the same tooltip, the same shortcut. Two spellings of "add"
 * would be two shortcuts to keep in sync.
 *
 * Icon only, and the same everywhere: what is added is said by the page's own heading, so a label
 * ("New book", "New order/offer") only repeats it — and reads badly wherever the noun has no good
 * article. The accessible name stays generic for the same reason; the tooltip beside it names the
 * shortcut.
 */
export function AddEntryButton({ href, isLegacy }: AddEntryButtonProps) {
  const t = useTranslations();
  useAddEntryShortcut(href, isLegacy);

  return (
    // With no label on the button the tooltip has to say what it does before it says how to reach it
    // by keyboard. `._`: the shortcut key has a child (.title), so it is nested under `_` in the
    // catalog.
    <HintTooltip
      title={t("menu.addNewEntry")}
      text={`${t("tooltip.shortcut.addEntry.title")}\n\n${t("tooltip.shortcut.addEntry._")}`}
    >
      <Button asChild size="icon-lg" aria-label={t("menu.addNewEntry")}>
        {isLegacy ? (
          <a href={href}>
            <HugeiconsIcon icon={PlusSignIcon} strokeWidth={2.5} />
          </a>
        ) : (
          <Link href={href}>
            {/* No `size` prop: the primitive sizes the icon with the button, and an explicit
                width/height attribute would win over that class. */}
            <HugeiconsIcon icon={PlusSignIcon} strokeWidth={2.5} />
          </Link>
        )}
      </Button>
    </HintTooltip>
  );
}
