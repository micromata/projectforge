"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CheckmarkSquare02Icon, SquareIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/**
 * Switches a list into selection mode and out again — what makes the checkboxes, the keyboard and the
 * selecting click appear (see useListSelection).
 *
 * An explicit mode, as the legacy app has it: without one, a click on a row means "open" or "select"
 * depending on whether anything happens to be ticked, which is not something a user can see. Icon
 * only, like "add": the tooltip says what it does, and the bar it opens says the list is in the mode.
 */
export function SelectionModeToggle({
  active,
  onToggle,
}: {
  active: boolean;
  onToggle: () => void;
}) {
  const t = useTranslations();
  return (
    <HintTooltip
      // No `title`: it would only repeat the button's own label, which is spelled out beside the icon.
      // How to pick rows once the mode is on, straight from the bundle — the same text the bar's help
      // icon shows, so the gestures are stated once. `._`: the shortcut key has a child (.title).
      text={`${t("multiselection.aggrid.selection.info.message")}\n\n* **${t(
        "tooltip.shortcut.selectAll.title"
      )}**: ${t("tooltip.shortcut.selectAll._")}`}
    >
      {/* Which mode the list is in is said three ways at once, because a toggle that only changes
          shade is easy to misread: the icon is a ticked box while the mode is on and the same box
          empty while it is off (`SquareIcon` is `CheckmarkSquare02Icon` minus the tick — not
          `Square01Icon`, which is the "x²" glyph), the button is filled rather than quiet, and
          `aria-pressed` says the same to a screen reader. */}
      <Button
        type="button"
        variant={active ? "default" : "ghost"}
        size="sm"
        className="gap-1.5"
        aria-pressed={active}
        onClick={onToggle}
      >
        <HugeiconsIcon
          icon={active ? CheckmarkSquare02Icon : SquareIcon}
          size={14}
          strokeWidth={active ? 2.5 : 2}
          aria-hidden
        />
        {t("multiselection.button")}
      </Button>
    </HintTooltip>
  );
}
