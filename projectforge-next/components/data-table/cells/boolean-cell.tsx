import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import type { CellRenderProps } from "./cell-types";

/**
 * A tick for true, nothing for false — the same visual language the Wicket lists
 * use. The icon carries the localised "yes" as its accessible name so a screen
 * reader doesn't read the cell as empty; a false cell says "no" for the same
 * reason.
 */
export function BooleanCell({ value, t }: CellRenderProps) {
  if (!value) return <span className="sr-only">{t("no")}</span>;
  return (
    <HugeiconsIcon
      icon={Tick02Icon}
      size={15}
      className="text-primary"
      aria-label={t("yes")}
      role="img"
    />
  );
}
